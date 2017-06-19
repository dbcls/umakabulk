import AssemblyKeys._

assemblySettings

lazy val sbMeta = (project in file("."))

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    "org.eclipse.rdf4j" % "rdf4j-repository-sail" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-sail-memory" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-rdfxml" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-n3" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-nquads" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-jsonld" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-trig" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-turtle" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-rdfjson" % "2.0M1",
    "org.eclipse.rdf4j" % "rdf4j-rio-trix" % "2.0M1",
    "org.scalactic" %% "scalactic" % "2.2.6",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "com.opencsv" % "opencsv" % "3.9"
)

//Download source attachments

EclipseKeys.withSource := true

