package sbmeta

class Prefix (line: String) {
  val cols = line.split(',').map(_.trim)
  if (cols.length != 3) {
    throw new RuntimeException("Invalid format data")
  }
  val endpointId: String = cols(1)
  val uri: String = cols(2)
}