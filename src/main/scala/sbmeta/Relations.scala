package sbmeta

import scala.collection.mutable.{ Set => MSet, Map => MMap }
import util.control.Breaks._

class Relations {
  
  private val relationList = MSet[Relation]()
  
  def append(relation: Relation) {
    var isExist = false
    breakable {
      for(entity <- relationList) {
        if (relation.srcId == entity.srcId && relation.dstId == entity.dstId && relation.name == entity.name) {
          isExist = true
          break
        }
      }
    }
    if (!isExist) {
      relationList += relation
    }
  }
  
  def entities = relationList
  
}