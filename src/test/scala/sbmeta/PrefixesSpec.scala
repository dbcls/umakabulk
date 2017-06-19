package sbmeta

import collection.mutable.Stack
import scala.collection.mutable.{ Set => MSet, Map => MMap }

import org.scalatest._
import java.net.URI

class PrefixesSpec extends FreeSpec with Matchers {
  "searchPrefix" - {
    "Return empty Set collection prefix when scheme of prefix does not match given uri" - {
      val prefixes = new Prefixes
      prefixes.append(new Prefix("1,1,https://example.com"))

      val prefixSet = prefixes.searchPrefix("http://example.com/a")

      prefixSet should have size (0)
    }
    "Return empty Set collection prefix when hostname of prefix does not match given uri" - {
      val prefixes = new Prefixes
      prefixes.append(new Prefix("1,1,http://example.com"))

      val prefixSet = prefixes.searchPrefix("http://example.com.jp/")

      prefixSet should have size (0)
    }
    "Return Set collection contains 1 prefix when prefix starts with a given prefix" - {
      val prefixes = new Prefixes
      prefixes.append(new Prefix("1,1,http://example.com"))

      val prefixSet = prefixes.searchPrefix("http://example.com")
      val prefix = prefixSet.toList(0)

      prefix.uri should equal("http://example.com")
      prefixSet should have size (1)
    }
    "Return Set collection contains 2 prefix when prefixes object has 2 prefix contains same uri" - {
      val prefixes = new Prefixes
      prefixes.append(new Prefix("1,1,http://example.com"))
      prefixes.append(new Prefix("2,2,http://example.com"))

      val prefixSet = prefixes.searchPrefix("http://example.com/")
      val prefixList = prefixSet.toList
      val first = prefixList(0)
      val second = prefixList(1)

      first.uri should equal("http://example.com")
      second.uri should equal("http://example.com")
      prefixSet should have size (2)
    }
  }
}