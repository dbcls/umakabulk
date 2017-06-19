package sbmeta

import collection.mutable.Stack
import scala.collection.mutable.{ Set => MSet, Map => MMap }

import org.scalatest._
import java.net.URI

class SBMetaSpec extends FreeSpec with Matchers {

  "getFirstPaths" - {
    "when prefix dose not have path" - {
      "should return same prefix" in {
        val paths = List(
          "http://example.com",
          "http://example.com/")

        val firstPaths = SBMeta.getFirstPaths(paths)

        firstPaths should contain("http://example.com")
        firstPaths should have size (1L)
      }

      "when prefix has path" - {
        "should return path contains only first path" in {
          val paths = List(
            "http://example.com/aaa1",
            "http://example.com/aaa1/bbb1",
            "http://example.com/aaa2/bbb1")

          val firstPaths = SBMeta.getFirstPaths(paths)

          firstPaths should contain("http://example.com")
          firstPaths should have size (1L)
        }
      }
    }
  }

  "addPath" - {
    "should return same uri when the end of uri is /" in {
      val prefix: String = "http://example.com/aaa"
      SBMeta.addNextPath("http://example.com/", prefix) should equal("http://example.com/")
    }

    "should return same uri when next path is / " in {
      val prefix: String = "http://example.com/aaa/bbb/ccc/"
      SBMeta.addNextPath("http://example.com/aaa/bbb/ccc", prefix) should equal("http://example.com/aaa/bbb/ccc")
    }

    "should return same uri when next path dose not exist" in {
      val prefix: String = "http://example.com/aaa-main/bbb/ccc"
      SBMeta.addNextPath("http://example.com/aaa", prefix) should equal("http://example.com/aaa")
    }

    "should return next path when path is host" in {
      val prefix: String = "http://example.com/aaa/bbb/ccc"
      SBMeta.addNextPath("http://example.com", prefix) should equal("http://example.com/aaa")
    }

    "should return next path" in {
      val prefix: String = "http://example.com/aaa/bbb/ccc"
      SBMeta.addNextPath("http://example.com/aaa/bbb", prefix) should equal("http://example.com/aaa/bbb/ccc")
    }
  }

  "getNextPath" - {
    "Next path" - {
      "when valid URI" - {
        "should return null when next path is /" - {
          val prefix: String = "http://example.com/aaa/"
          SBMeta.getNextPath("http://example.com/aaa", prefix) should equal(null)
        }

        "should return /bbb" - {
          val prefix: String = "http://example.com/aaa/bbb/cccc"
          SBMeta.getNextPath("http://example.com/aaa", prefix) should equal("/bbb")
        }

        "should return '/ddd' when path contains the character of regular expression" - {
          val prefix: String = "http://example.com/aaa/bbb+ccc/ddd/"
          SBMeta.getNextPath("http://example.com/aaa/bbb+ccc", prefix) should equal("/ddd")
        }

        "should return '//ccc' when URI contains serial slashes" - {
          val prefix: String = "http://example.com/aaa/bbb///ccc"
          SBMeta.getNextPath("http://example.com/aaa/bbb", prefix) should equal("///ccc")
        }
      }

      "when invalid URI" - {
        "should return when URI contains invalid character" - {
          val prefix: String = "http://example.com/aaa/{bbb}"
          SBMeta.getNextPath("http://example.com/aaa", prefix) should equal("/{bbb}")
        }

        "should return null when next path is //" - {
          val prefix: String = "http://example.com/aaa//"
          SBMeta.getNextPath("http://example.com/aaa", prefix) should equal(null)
        }
      }
    }

  }

  "extractNextPaths" - {
    "when path included in prefixes" - {
      "should return counted paths" in {
        val summarized1 = Range(0, 3).map(n => (s"http://example.com/aaa/bbb${n}/ccc" -> 1L)).toMap
        val summarized2 = Range(0, 5).map(n => (s"http://example.com/aaa/bbb/ccc${n}" -> 1L)).toMap
        val prefixes = SBMeta.extractNextPaths(summarized2 ++: summarized1, "http://example.com/aaa")

        prefixes should contain key ("http://example.com/aaa/bbb0")
        prefixes should contain key ("http://example.com/aaa/bbb1")
        prefixes should contain key ("http://example.com/aaa/bbb2")
        prefixes should contain key ("http://example.com/aaa/bbb")
        prefixes("http://example.com/aaa/bbb0") should be(1L)
        prefixes("http://example.com/aaa/bbb1") should be(1L)
        prefixes("http://example.com/aaa/bbb2") should be(1L)
        prefixes("http://example.com/aaa/bbb") should be(5L)
      }
    }
  }

  "roundRecursive" - {
    "should round prefix when target path appears less threshold times" in {
      val count = SBMeta.threshold - 1
      val summarized = Range(0, count).map(n => (s"http://exmaple.com/aaa/bbb/ccc${n}" -> 1L)).toMap
      val path = ("http://exmaple.com/aaa" -> 1L)

      val results = SBMeta.roundRecursive(summarized, path)

      for (i <- 0 to count - 1) {
        results should contain((s"http://exmaple.com/aaa/bbb/ccc${i}" -> 1L))
      }
      results should have size (count)
    }

    "should round prefix recursively when the target path appears more threshold times" in {
      val count = SBMeta.threshold + 2
      val summarized1 = Range(0, count - 3).map(n => (s"http://exmaple.com/aaa/bbb/ccc${n}" -> 1L)).toMap
      val summarized2 = Range(0, count - 5).map(n => (s"http://exmaple.com/aaa/bbb/ddd${n}" -> 1L)).toMap
      val path = ("http://exmaple.com/aaa" -> 1L)

      val prefixes = SBMeta.roundRecursive(summarized2 ++: summarized1, path)

      prefixes should contain(("http://exmaple.com/aaa/bbb/" -> (2 * count - 8)))
      prefixes should have size (1L)
    }

    "should round prefix recursively" in {
      val count = SBMeta.threshold + 2
      val summarized1 = Range(0, count).map(n => (s"http://exmaple.com/aaa/bbb/ccc/ddd${n}" -> 1L)).toMap
      val summarized2 = Range(0, count).map(n => (s"http://exmaple.com/aaa/bbb/eee/fff${n}" -> 1L)).toMap
      val path = ("http://exmaple.com/aaa" -> 1L)

      val prefixes = SBMeta.roundRecursive(summarized2 ++: summarized1, path)

      prefixes should contain(("http://exmaple.com/aaa/bbb/ccc/" -> count))
      prefixes should contain(("http://exmaple.com/aaa/bbb/eee/" -> count))
      prefixes should have size (2L)
    }

    "should return two paths even when there are next path" in {
      val summarized1 = Map("http://exmaple.com/aaa" -> 2L)
      val summarized2 = Map("http://exmaple.com/aaa/bbb" -> 3L)
      val summarized3 = Map("http://exmaple.com/aaa/ccc" -> 5L)
      val summarized4 = Map("http://exmaple.com/aaa/ccc/ddd" -> 7L)
      val path = ("http://exmaple.com/aaa" -> 2L)

      val prefixes = SBMeta.roundRecursive(summarized4 ++: summarized3 ++: summarized2 ++: summarized1, path)

      prefixes should contain(("http://exmaple.com/aaa" -> 2L))
      prefixes should contain(("http://exmaple.com/aaa/bbb" -> 3L))
      prefixes should contain(("http://exmaple.com/aaa/ccc" -> 5L))
      prefixes should contain(("http://exmaple.com/aaa/ccc/ddd" -> 7L))
      prefixes should have size (4L)
    }

    "should round prefix when the sum target paths appear more than threshold times" in {
      val count = SBMeta.threshold
      val summarized1 = Range(0, count - 1).map(n => (s"http://exmaple.com/aaa/bbb/ccc${n}" -> 1L)).toMap
      val summarized2 = Range(0, count + 3).map(n => (s"http://exmaple.com/aaa/bbb/ddd/eee${n}" -> 1L)).toMap
      val path = ("http://exmaple.com/aaa" -> 1L)

      val prefixes = SBMeta.roundRecursive(summarized1 ++: summarized2, path)

      prefixes should contain(("http://exmaple.com/aaa/bbb/" -> (2 * count + 2)))
      prefixes should have size (1L)
    }

  }
}