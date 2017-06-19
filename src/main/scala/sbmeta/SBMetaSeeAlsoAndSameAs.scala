package sbmeta

import java.io.FileInputStream
import java.io.PrintWriter
import java.io.File

import scala.collection.mutable.{ Set => MSet, Map => MMap }
import scala.io.Source

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.rio.RDFHandler
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler
import util.control.Breaks._

object SBMetaSeeAlsoAndSameAs {

  val relations = new Relations()

  def main(args: Array[String]) {
    val bulkFilePath = args(0)
    val prefixFile = args(1)
    parsePrefixFile(prefixFile)
    val files = FileLocator.locateBulkFiles(bulkFilePath)
    files.foreach(createRelationCSV)
  }

  def createRelationCSV(inputFile: File): Unit = {
    val rdfFiles: List[File] = FileLocator.findRDFFiles(inputFile)
    rdfFiles.foreach(file => RDFParser.parseFile(file.getAbsolutePath, phase1Handler))
    var writer: PrintWriter = new PrintWriter(s"${inputFile.getAbsolutePath}_relation.csv")
    writer.println("src_id,dst_id,name")
    for (relation <- relations.entities) {
      writer.println(s"${relation.srcId},${relation.dstId},${relation.name}")
    }
    writer.close
    println("...done!")
  }

  val SEE_ALSO = "seeAlso"
  val SAME_AS = "sameAs"
  val prefixes = new Prefixes()

  def parsePrefixFile(filePath: String) {
    for (line <- Source.fromFile(filePath).getLines) {
      prefixes.append(new Prefix(line))
    }
  }

  /**
   * Primary processing, gather basic information.
   * The RDFHandler will store no data in memory.
   */

  lazy val phase1Handler = new AbstractRDFHandler {
    override def handleStatement(s: Statement) {
      val predicate = s.getPredicate.getLocalName()
      if (!predicate.contains(SEE_ALSO) && !predicate.contains(SAME_AS)) {
        return
      }
      val srcPrefix = prefixes.searchPrefix(s.getSubject.stringValue)
      if (srcPrefix.size == 0) return
      val dstPrefix = prefixes.searchPrefix(s.getObject.stringValue)
      if (dstPrefix.size == 0) return
      for (src <- srcPrefix) {
        for (dst <- dstPrefix) {
          val relation = new Relation(src.endpointId, dst.endpointId, predicate)
          relations.append(relation)
        }
      }
    }
  }

}