package sbmeta

import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files

import java.io.FileInputStream
import java.io.FileWriter
import java.io.File
import java.io.FileReader
import java.io.BufferedReader

import java.net.URI

import java.util.regex.Pattern

import scala.collection.mutable.{ Set => MSet, Map => MMap }

import com.opencsv.CSVWriter
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.RDFParseException
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.Resource
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.rio.RDFHandler
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings
import scala.collection.immutable.TreeMap

object SBMeta {

  var threshold = 10
  val SlashPattern = Pattern.compile("/")
  val NextPathRegex = "^\\/*[^\\/|\\r|\\n|\\r\\n]+".r
  val Domain_Regex = "^(https?|ftp)(:\\/+[-_.!~*\\'()a-zA-Z0-9;?:\\@&=+\\$,%#]+)".r

  def main(args: Array[String]) {
    val input: String = args(0)
    val inputFile = new File(input)
    if (!inputFile.exists) println(s"Not found ${inputFile.getAbsolutePath}")
    else aggregatePrefixes(inputFile)
  }

  def aggregatePrefixes(inputFile: File): Unit = {
    println(inputFile.getAbsolutePath)
    val rdfFiles: List[File] = FileLocator.findRDFFiles(inputFile)
    println(s"files => ${rdfFiles.size}")
    var subjectPrefixMap = Map[String, Long]()
    var objectPrefixMap = Map[String, Long]()
    rdfFiles.foreach { file =>
      RDFParser.parseFile(file.getAbsolutePath, phase1Handler)
      subjectPrefixMap = roundAndCountPrefixes(rawSubjectPrefixes, subjectPrefixMap)
      objectPrefixMap = roundAndCountPrefixes(rawObjectPrefixes, objectPrefixMap)
    }
    val sortedSubjectPrefixMap = TreeMap(roundPrefixes(subjectPrefixMap).toSeq: _*)
    val sortedObjectPrefixMap = TreeMap(roundPrefixes(objectPrefixMap).toSeq: _*)
    val prefixMap = mergePrefixes(sortedSubjectPrefixMap, sortedObjectPrefixMap)

    decideThreshold(prefixMap.size)
    val sortedPrefixMap = roundPrefixes(prefixMap) ++: TreeMap[String, Int]()
    val rows = sortedPrefixMap.map { case (prefix, count) => Array(prefix, count.toString) }.toList
    createCSV(s"${inputFile.getAbsolutePath}", Array("URI", "count"), rows)

    val subjectRows = sortedSubjectPrefixMap.map { case (prefix, count) => Array(prefix, count.toString, "subject") }.toList
    val objectRows = sortedObjectPrefixMap.map { case (prefix, count) => Array(prefix, count.toString, "object") }.toList
    createCSV(s"${inputFile.getAbsolutePath}_subject_and_object", Array("URI", "count", "type"), subjectRows ::: objectRows)
  }

  def roundAndCountPrefixes(rawPrefixes: MMap[String, Long], prefixMap: Map[String, Long]): Map[String, Long] = {
    val filteredPrefixes = rawPrefixes.filterKeys(prefix => schemes.exists(prefix.startsWith))
    val prefixes = roundPrefixes(filteredPrefixes ++: Map[String, Long]())
    rawPrefixes.clear
    mergePrefixes(prefixes, prefixMap)
  }

  def createCSV(inputFilePath: String, header: Array[String], rows: List[Array[String]]) = {
    val writer = new CSVWriter(new FileWriter(s"${inputFilePath}_prefix.csv"))
    writer.writeNext(header)
    rows.foreach(writer.writeNext)
    writer.flush
  }

  def decideThreshold(size: Long): Unit = {
    threshold = size match {
      case size if (0L <= size && size < 50L)   => 10
      case size if (50L <= size && size < 100L) => 3
      case size if (100L <= size)               => 2
    }
  }

  def mergePrefixes(prefixes: Map[String, Long], prefixMap: Map[String, Long]) = {
    var result = prefixMap
    prefixes.foreach { t =>
      val prefix = t._1
      val count = t._2
      if (result.contains(prefix)) {
        result = result + (prefix -> (count + prefixMap(prefix)))
      } else {
        result = result + t
      }
    }
    result
  }

  def roundPrefixes(summarized: Map[String, Long]): Map[String, Long] = {
    val domains = getFirstPaths(summarized.keys.toList)
    var targets: Map[String, Long] = summarized
    val firstPathsMap = domains.map { domain =>
      val partision = targets.partition(t => isStartWith(domain, t._1))
      val matches = partision._1
      targets = partision._2
      (domain -> matches.values.sum)
    }.toMap
    firstPathsMap.flatMap(path => roundRecursive(summarized, path))
  }

  def getFirstPaths(paths: List[String]): List[String] = {
    paths.map { prefix =>
      Domain_Regex.findPrefixOf(prefix) match {
        case Some(result) => result
        case None         => null
      }
    }.distinct.filter(_ != null)
  }

  def isStartWith(source: String, target: String) = {
    target.equals(source) || target.startsWith(source + "/")
  }

  def extractNextPaths(summarized: Map[String, Long], roundPrefix: String) = {
    var countPaths = Map[String, Long]()
    summarized.foreach { path_and_count =>
      val path = path_and_count._1
      val nextPath = addNextPath(roundPrefix, path)
      if (!nextPath.equals(roundPrefix)) {
        val count = if (countPaths.contains(nextPath))
          countPaths(nextPath) + summarized(path)
        else
          summarized(path)
        countPaths += (nextPath -> count)
      }
    }
    countPaths
  }

  def addNextPath(path: String, prefix: String): String = {
    if (path.endsWith("/")) return path
    val nextPath = getNextPath(path, prefix)
    if (nextPath == null) path
    else path + nextPath
  }

  def getNextPath(path: String, prefix: String): String = {
    val splited = prefix.split(Pattern.quote(path) + "/")
    if (splited.size < 2) return null

    val after = splited(1)
    NextPathRegex.findPrefixOf(after) match {
      case Some(result) => "/" + result
      case None         => null
    }
  }

  def roundRecursive(prefixes: Map[String, Long], path_and_count: (String, Long)): Map[String, Long] = {
    val path = path_and_count._1
    val nextPathCandidates = prefixes.filterKeys(target => isStartWith(path, target))
    if (nextPathCandidates.size == 1) return nextPathCandidates

    val nextPaths = extractNextPaths(nextPathCandidates, path)
    if (nextPaths.isEmpty) return nextPathCandidates

    val partition = nextPathCandidates.partition(t => (t._1 == path || t._1.matches(Pattern.quote(path) + "/+")))
    val currentPaths = partition._1
    val nextPathCandidatesExpectForCurrentPaths = partition._2
    if (currentPaths.size + nextPaths.size >= threshold) {
      Map(path + "/" -> (nextPathCandidates.values.sum))
    } else {
      currentPaths ++: nextPaths.flatMap(roundRecursive(nextPathCandidatesExpectForCurrentPaths, _))
    }
  }

  val rawSubjectPrefixes = MMap[String, Long]()
  val rawObjectPrefixes = MMap[String, Long]()

  val schemes = List("http://", "https://", "ftp://")

  lazy val phase1Handler = new AbstractRDFHandler {
    def getPrefix(v: Value) = {
      v match {
        case i: IRI => i.getNamespace
        case _      => ""
      }
    }

    override def handleStatement(s: Statement) {
      val subj = getPrefix(s.getSubject)
      val obj = getPrefix(s.getObject)

      if (rawSubjectPrefixes.contains(subj)) {
        val value = rawSubjectPrefixes(subj) + 1L
        rawSubjectPrefixes(subj) = value
      } else {
        rawSubjectPrefixes += (subj -> 1L)
      }

      if (rawObjectPrefixes.contains(obj)) {
        val value = rawObjectPrefixes(obj) + 1L
        rawObjectPrefixes(obj) = value
      } else {
        rawObjectPrefixes += (obj -> 1L)
      }
    }
  }

}
