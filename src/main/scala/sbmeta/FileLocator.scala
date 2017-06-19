package sbmeta

import java.io.File

object FileLocator {
  
  val TargetDirectoryEnds = "data/bulkdownloads"
  val ReadFileExtensions = List[String]("rdf", "rdfs", "owl", "xml", "nt", "ttl", "n3", "xml", "trix", "trig", "brf", "nq", "jsonld", "rj", "xhtml", "html")
  
  def locateBulkFiles(filePath: String): Array[File] = {
    val file = new File(filePath)
    if (!file.exists) {
      println(s"Not found ${file.getAbsolutePath}")
      return Array()
    }
    if (file.getPath.endsWith(TargetDirectoryEnds)) {
      return file.listFiles.filter(_.isDirectory())
    }
    return Array(file)
  }
  
  def findRDFFiles(inputFile: File): List[File] = {
    val files: List[File] = recursiveListFiles(inputFile).toList
    files.filter { file => ReadFileExtensions.exists { file.getName.toLowerCase.endsWith(_) } }
  }

  def recursiveListFiles(directEntry: File): Array[File] = {
    val listFiles = directEntry.listFiles()
    if (listFiles == null) Array(directEntry)
    else listFiles.flatMap(recursiveListFiles)
  }
  
}