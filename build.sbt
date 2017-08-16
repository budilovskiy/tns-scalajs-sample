import sbt.Keys._

name := "nativescript-scalajs"
version := "1.0"
scalaVersion in Scope.GlobalScope := "2.11.11"

lazy val outputCompiledJS = Def.setting(baseDirectory.value / "app" / "js")

lazy val scalaJS = project.in(file("scalaJS"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "ScalaJS",
    scalaVersion := scalaVersion.value
  )
  .settings(
    scalaJSStage in Global := FastOptStage,
    skip in packageJSDependencies := true,
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    emitSourceMaps := false,
    crossTarget in(Compile, fastOptJS) := outputCompiledJS.value,
    crossTarget in(Compile, fullOptJS) := outputCompiledJS.value
  )

lazy val createNsApp = TaskKey[Unit]("createNS", "Creates empty NativeScript application")
createNsApp := {
  import sys.process._
  val create =
    if (isWindows) Seq("cmd", "/c", "tns create nativeScript --template nativescript-template-ng-tutorial")
    else Seq("tns create nativeScript --template nativescript-template-ng-tutorial")
  create !
}

def isWindows: Boolean = {
  System.getProperty("os.name").startsWith("Windows")
}