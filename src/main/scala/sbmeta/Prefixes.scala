package sbmeta

import scala.collection.mutable.{ Set => MSet, Map => MMap }
import java.util.regex.Pattern

class Prefixes {

  val Domain_Regex = "^(https?|ftp)(:\\/+[-_.!~*\\'()a-zA-Z0-9;?:\\@&=+\\$,%#]+)".r

  val entities = MSet[Prefix]()
  val candidates = MMap[String, MSet[Prefix]]()

  def searchPrefix(prefix: String) = {
    val list = MSet[Prefix]()
    for (entity <- getCandidates(prefix)) {
      if (prefix.matches(Pattern.quote(entity.uri) + ".*")) list += entity
    }
    list
  }

  def getCandidates(prefix: String) = {
    val domain = Domain_Regex.findPrefixOf(prefix) match {
      case Some(result) => result
      case None         => null
    }

    if (candidates.contains(domain)) {
      candidates(domain)
    } else {
      val filterdEntities: MSet[Prefix] = entities.filter(entity => entity.uri.equals(domain) || entity.uri.startsWith(domain + "/"))
      candidates += (domain -> filterdEntities)
      filterdEntities
    }
  }

  def append(prefix: Prefix) {
    entities += prefix
  }

}