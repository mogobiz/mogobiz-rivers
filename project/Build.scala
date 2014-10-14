import sbt._
import Keys._
import com.github.shivawu.sbt.maven.MavenBuild
import com.ebiznext.sbt.plugins._
import GroovyPlugin._

object Rivers extends MavenBuild with Keys with TestKeys{
  // "*" is a selector which selects all modules
  project("*")(
    groovy.settings ++ testGroovy.settings ++ Seq(
      groovyVersion := "2.1.8",
      scalaVersion := "2.11.2",
      crossPaths := false,
      publishMavenStyle := true
    ):_*
  )
  lazy override val root = Project("mogobiz-rivers", file(".")) aggregate(common, http_client, elasticsearch, google_shopping, cfp)
  lazy val cfp = Project("mogobiz-cfp", file("cfp"))
  lazy val common = Project("mogobiz-common", file("common"))
  lazy val http_client = Project("mogobiz-http-client", file("http-client"))
  lazy val elasticsearch = Project("mogobiz-elasticsearch", file("elasticsearch")) dependsOn(common, http_client)
  lazy val google_shopping = Project("mogobiz-google-shopping", file("google-shopping")) dependsOn(common, http_client)
}