import net.juniper.yang._

name := "js-yang-model"

scalaVersion  := "2.11.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

organization := "net.juniper"

version := "0.1.0"

publishMavenStyle := true

publishTo := Some(Resolver.file("file",  new File(System.getProperty("user.home") + "/mavenrepo/release")))

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/mavenrepo/release"

resolvers += "JSpace Maven Repo" at "http://10.155.87.253:8080/mavenrepo/release"

val sprayV = "1.3.2"

val spray_jsonV = "1.2.6"

libraryDependencies ++= Seq(
  "net.juniper"         %   "jnc-library"          % "0.3.0"                  withSources(),
  "io.spray"            %%  "spray-can"            % sprayV                   withSources(),
  "io.spray"            %%  "spray-routing"        % sprayV                   withSources(),
  "io.spray"            %%  "spray-client"         % sprayV                   withSources(),
  "io.spray"            %%  "spray-json"           % spray_jsonV              withSources(),
  "net.juniper"         %%  "easy-rest-core"       % "0.3.0"                  withSources()
)

YangPlugin.yangSettings

YangPlugin.yangPackageName := Some("net.juniper.yang")