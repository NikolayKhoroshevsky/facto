import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

/**
 * Application settings. Configure the build for your application here.
 * You normally don't have to touch the actual build definition after this.
 */
object Settings {
  /** The name of your application */
  val name = "facto"

  /** The version of your application */
  val version = "3.0"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.11.8"
    val play = "2.5.9" // Must be the same as the Play sbt-plugin in plugins.sbt

    val jQuery = "2.2.4"
    val bootstrap = "3.3.6"
    val uTest = "0.4.3"
    val react = "15.1.0"
    val scalajsReact = "0.11.1"
    val diode = "1.0.0"
  }

  /**
   * These dependencies are shared between JS and JVM projects
   * the special %%% function selects the correct version for each project
   */
  val sharedDependencies = Def.setting(Seq(
    "com.lihaoyi"            %%% "autowire"             % "0.2.5",
    "me.chrons"              %%% "boopickle"            % "1.2.4"
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "com.vmunier"            %% "play-scalajs-scripts"  % "0.5.0",
    "com.lihaoyi"            %% "utest"                 % versions.uTest % Test,

    "com.typesafe.play"      %% "play-jdbc"             % versions.play,
    "com.typesafe.play"      %% "play-cache"            % versions.play,
    "com.typesafe.play"      %% "play-ws"               % versions.play,
    "com.typesafe.play"      %% "play-specs2"           % versions.play % Test,

    "org.webjars"            %  "jquery"                % versions.jQuery,
    "org.yaml"               %  "snakeyaml"             % "1.14",
    "com.github.nscala-time" %% "nscala-time"           % "2.12.0",
    "com.typesafe.slick"     %% "slick"                 % "3.0.0",
    "commons-lang"           %  "commons-lang"          % "2.6",
    "mysql"                  %  "mysql-connector-java"  % "5.1.36",
    "org.xerial"             %  "sqlite-jdbc"           % "3.8.11.2",
    "org.webjars"            %% "webjars-play"          % "2.4.0-2",
    "org.webjars"            %  "bootstrap"             % versions.bootstrap,
    "org.webjars"            %  "datatables"            % "1.10.4",
    "org.webjars"            %  "datatables-plugins"    % "1.10.7",
    "org.webjars"            %  "flot"                  % "0.8.3",
    "org.webjars"            %  "font-awesome"          % "4.6.2",
    "org.webjars.bower"      %  "holderjs"              % "2.6.0",
    "org.webjars"            %  "metisMenu"             % "1.1.3",
    "org.webjars"            %  "morrisjs"              % "0.5.1",
    "org.webjars.bower"      %  "datatables-responsive" % "1.0.6",
    "org.webjars"            %  "bootstrap-social"      % "4.9.0",
    "org.webjars.bower"      %  "flot.tooltip"          % "0.8.5",
    "org.webjars"            %  "mousetrap"             % "1.5.3-1",
    "org.webjars.bower"      %  "bootstrap-tagsinput"   % "0.8.0",
    "org.webjars.bower"      %  "SHA-1"                 % "0.1.1",
    "org.webjars.bower"      %  "ladda-bootstrap"       % "0.1.0",
    "org.webjars"            %  "typeaheadjs"           % "0.11.1"
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "com.github.japgolly.scalajs-react" %%% "core"        % versions.scalajsReact,
    "com.github.japgolly.scalajs-react" %%% "extra"       % versions.scalajsReact,
    "com.github.japgolly.scalacss"      %%% "ext-react"   % "0.4.1",
    "me.chrons"                         %%% "diode"       % versions.diode,
    "me.chrons"                         %%% "diode-react" % versions.diode,
    "org.scala-js"                      %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi"                       %%% "utest"       % versions.uTest % Test
  ))

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(Seq(
    "org.webjars.bower"      % "react"                  % versions.react     / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    "org.webjars.bower"      % "react"                  % versions.react     / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
    "org.webjars"            % "jquery"                 % versions.jQuery    / "jquery.js" minified "jquery.min.js",
    "org.webjars"            % "bootstrap"              % versions.bootstrap / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js",
    "org.webjars"            % "chartjs"                % "2.1.3"            / "Chart.js" minified "Chart.min.js",
    "org.webjars"            % "log4javascript"         % "1.4.10"           / "js/log4javascript_uncompressed.js" minified "js/log4javascript.js"
  ))
}
