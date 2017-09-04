import com.lihaoyi.workbench.Plugin.{bootSnippet, updateBrowsers}

enablePlugins(ScalaJSPlugin)

workbenchSettings

name := "WeatherReport"
version := "0.1-SNAPSHOT"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
   "org.scala-js" %%% "scalajs-dom"       % "0.9.1"
  ,"org.scala-js" %%% "scalajs-java-time" % "0.2.2"
  ,"org.querki"   %%% "querki-jsext"      % "0.8"
  ,"com.lihaoyi"  %%% "scalatags"         % "0.6.5"
)

jsDependencies += "org.webjars.npm" % "leaflet" % "0.7.7" / "dist/leaflet.js"

// bootSnippet is used by SBT to auto-reload the web page
bootSnippet := "com.sap.demo.WeatherReport().main(document.getElementById('weatherDiv'));"

updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)

publishArtifact in packageDoc:= false
publishArtifact in packageSrc:= false
