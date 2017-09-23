package models.accounting

import common.accounting.Tag
import org.specs2.mutable._
import play.api.test.WithApplication

class TagTest extends Specification {

  "isValidTagName" in {
    Tag.isValidTagName("") mustEqual false
    Tag.isValidTagName("'") mustEqual false
    Tag.isValidTagName("single-illegal-char-at-end?") mustEqual false
    Tag.isValidTagName("]single-illegal-char-at-start") mustEqual false
    Tag.isValidTagName("space in middle") mustEqual false

    Tag.isValidTagName("a") mustEqual true
    Tag.isValidTagName("normal-string") mustEqual true
    Tag.isValidTagName("aC29_()_-_@_!") mustEqual true
    Tag.isValidTagName("aC29_()_-_@_!_&_$_+_=_._<>_;_:") mustEqual true
  }

  "parseTagsString" in {
    Tag.parseTagsString("a,b,c") mustEqual Seq(Tag("a"), Tag("b"), Tag("c"))
    Tag.parseTagsString(",,b,c") mustEqual Seq(Tag("b"), Tag("c"))
    Tag.parseTagsString("") mustEqual Seq()
  }
}
