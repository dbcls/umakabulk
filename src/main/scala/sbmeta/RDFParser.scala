package sbmeta

import java.io.FileInputStream
import java.io.PrintWriter

import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.RDFParseException
import org.eclipse.rdf4j.rio.RDFHandler
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings

object RDFParser {

  def parseFile(file: String, handler: RDFHandler) {
    // See https://stackoverflow.com/questions/20482331/
    // whats-causing-these-parseerror-exceptions-when-reading-off-an-aws-sqs-queue-in
    System.setProperty("jdk.xml.entityExpansionLimit", "0")

    val format = Rio.getParserFormatForFileName(file)
    if (!format.isPresent()) {
      println(s"Unable to determine file format for: $file")
      return
    }
    val stream = new FileInputStream(file)
    val parser = Rio.createParser(format.get)
    import java.lang.Boolean
    val pc = parser.getParserConfig

    /**
     * For RDFXML parsers, this setting causes memory leaks over time
     * if enabled. All encountered IRIs are saved in a set. So we disable it.
     */
    pc.set(XMLParserSettings.FAIL_ON_DUPLICATE_RDF_ID, Boolean.FALSE)

    parser.setRDFHandler(handler)

    val baseUri = "file://" + file

    try {
      parser.parse(stream, baseUri)
    } catch {
      case e: RDFParseException => println(e.getMessage)
    }
  }

}