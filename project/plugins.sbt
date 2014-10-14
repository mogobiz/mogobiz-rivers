resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("com.ebiznext.sbt.plugins" % "sbt-groovy" % "0.1.3")

addSbtPlugin("com.github.shivawu" %% "sbt-maven-plugin" % "0.1.3-SNAPSHOT")