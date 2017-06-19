package sbmeta

import java.io.FileInputStream
import java.io.PrintWriter

import scala.collection.mutable.{ Set => MSet, Map => MMap }

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

object SBMetaURI {

  def main(args: Array[String]) {
    val input = args(0)
    uri = args(1)
    println("Start")
    subjectsWriter = new PrintWriter(s"results/statements_contain_uri_in_subjects_${input}.csv")
    subjectsWriter.println("Subject,Predicate,Object")
    objectsWriter = new PrintWriter(s"results/statements_contain_uri_in_objects_${input}.csv")
    objectsWriter.println("Subject,Predicate,Object")
    RDFParser.parseFile(input, phase1Handler)
    subjectsWriter.close
    objectsWriter.close
    println("Finish")
  }

  var uri = ""
  var subjectsWriter: PrintWriter = null
  var objectsWriter: PrintWriter = null

  /**
   * Primary processing, gather basic information.
   * The RDFHandler will store no data in memory.
   */

  lazy val phase1Handler = new AbstractRDFHandler {
    override def handleStatement(s: Statement) {
      val subj = s.getSubject.stringValue()
      if (subj.contains(uri)) {
        subjectsWriter.format("%s,%s,%s\n", s.getSubject.stringValue(), s.getPredicate.stringValue(), s.getObject.stringValue())
      }
      val obj = s.getObject.stringValue()
      if (obj.contains(uri)) {
        objectsWriter.format("%s,%s,%s\n", s.getSubject.stringValue(), s.getPredicate.stringValue(), s.getObject.stringValue())
      }
    }
  }

}
