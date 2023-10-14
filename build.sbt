
scalaVersion := "2.13.8"
name := "zio-http"

libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % "2.0.0",
    "io.d11" %% "zhttp" % "2.0.0-RC11"
)

