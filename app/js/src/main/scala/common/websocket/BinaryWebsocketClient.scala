package common.websocket

import java.nio.ByteBuffer

import common.LoggingUtils.logExceptions
import org.scalajs.dom
import org.scalajs.dom.{CloseEvent, ErrorEvent, Event, MessageEvent, _}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, _}

final class BinaryWebsocketClient(name: String, jsWebsocket: WebSocket) {

  def send(message: ByteBuffer): Unit = logExceptions {
    jsWebsocket.send(toArrayBuffer(message))
  }

  def close(): Unit = {
    BinaryWebsocketClient.logLine(name, "Closing WebSocket...")
    jsWebsocket.onclose = (e: CloseEvent) => {}
    jsWebsocket.close()
  }

  private def toArrayBuffer(byteBuffer: ByteBuffer): ArrayBuffer = {
    val length = byteBuffer.remaining()
    val arrayBuffer = new ArrayBuffer(length)
    var arrayBufferView = new Int8Array(arrayBuffer)
    for (i <- 0 until length) {
      arrayBufferView.set(i, byteBuffer.get())
    }
    arrayBuffer
  }
}
object BinaryWebsocketClient {
  def open(name: String,
           websocketPath: String,
           onMessageReceived: ByteBuffer => Unit,
           onClose: () => Unit = () => {}): Future[BinaryWebsocketClient] = {
    require(!websocketPath.startsWith("/"))

    val protocol = if (dom.window.location.protocol == "https:") "wss:" else "ws:"
    val jsWebsocket = new dom.WebSocket(s"${protocol}//${dom.window.location.host}/$websocketPath")
    val resultPromise: Promise[BinaryWebsocketClient] = Promise()

    jsWebsocket.binaryType = "arraybuffer"
    jsWebsocket.onmessage = (e: MessageEvent) =>
      logExceptions {
        val bytes = TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])
        onMessageReceived(bytes)
    }
    jsWebsocket.onopen = (e: Event) =>
      logExceptions {
        resultPromise.success(new BinaryWebsocketClient(name, jsWebsocket))
        logLine(name, "Opened")
    }
    jsWebsocket.onerror = (e: ErrorEvent) =>
      logExceptions {
        // Note: the given event turns out to be of type "error", but has an undefined message. This causes
        // ClassCastException when accessing it as a String
        val errorMessage = s"Error when connecting to WebSocket"
        resultPromise.tryFailure(new RuntimeException(errorMessage))
        logLine(name, errorMessage)
    }
    jsWebsocket.onclose = (e: CloseEvent) =>
      logExceptions {
        val errorMessage = s"WebSocket was closed: ${e.reason}"
        resultPromise.tryFailure(new RuntimeException(errorMessage))
        logLine(name, errorMessage)
        onClose()
    }

    resultPromise.future
  }

  private def logLine(name: String, line: String): Unit = console.log(s"  [$name] $line")
}
