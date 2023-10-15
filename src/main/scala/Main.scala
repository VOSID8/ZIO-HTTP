import zio._
import zio.Console._
import zhttp._
import zhttp.http._
import zhttp.http.Http
import zhttp.service.Server
import zhttp.service._
import zhttp.http.middleware.Cors.CorsConfig


object ZIOHTTP extends ZIOAppDefault {
  // def run = myAppLogic
  // val myAppLogic =
  //   for {
  //     _    <- printLine("Hello! What is your name?")
  //     name <- readLine
  //     _    <- printLine(s"Hello, ${name}, welcome to ZIO!")
  //   } yield ()
  val port = 9000

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls" => Response.text("Hoot!")
  }@@ Middleware.csrfGenerate()

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" => //ZIO.succeed(Response.text("Hoot!"))
      Random.nextIntBetween(3,5).map(n => Response.text("Hello"*n + ", owls" ))
  }@@ Middleware.csrfValidate()

  val combined = app ++ zApp 
  //val combined = app <> zApp //if first fails, try second

  //middleware 
  //val wrapped = combined @@ middleware.debug
  //request -> wrapped -> combined -> response
  val Ourmiddleware = combined @@ Verbose.log

  val httpProgram = for {
    _ <- Console.printLine(s"Server is starting on port ${port}")
    _ <- Server.start(port, Ourmiddleware) //internally calls zio.never
  } yield ()

  override def run = httpProgram

  object Verbose {
    def log[R, E >: Throwable]
        : Middleware[R, E, Request, Response, Request, Response] =
      new Middleware[R, E, Request, Response, Request, Response] {

        override def apply[R1 <: R, E1 >: E](
            http: Http[R1, E1, Request, Response]
        ): Http[R1, E1, Request, Response] =
          http
            .contramapZIO[R1, E1, Request] { r =>
              for {
                _ <- Console.printLine(s"> ${r.method} ${r.path} ${r.version}")
                _ <- ZIO.foreach(r.headers.toList) { h =>
                        Console.printLine(s"> ${h._1}: ${h._2}")
                      }
              } yield r
            }
            .mapZIO[R1, E1, Response] { r =>
              for {
                _ <- Console.printLine(s"< ${r.status}")
                _ <- ZIO.foreach(r.headers.toList) { h =>
                        Console.printLine(s"< ${h._1}: ${h._2}")
                      }
              } yield r
            }
          }
      }

    val config: CorsConfig =
      CorsConfig(
        anyOrigin = false,
        anyMethod = false,
        allowedOrigins = s => s.equals("localhost"),
        allowedMethods = Some(Set(Method.GET, Method.POST))
    )
    // object Cors {
    //   final case class CorsConfig(
    //     anyOrigin: Boolean = true,
    //     anyMethod: Boolean = true,
    //     allowCredentials: Boolean = true,
    //     allowedOrigins: String => Boolean = _ => false,
    //     allowedMethods: Option[Set[Method]] = None,
    //     allowedHeaders: Option[Set[String]] = Some(
    //       Set(HttpHeaderNames.CONTENT_TYPE.toString, HttpHeaderNames.AUTHORIZATION.toString, "*"),
    //     ),
    //     exposedHeaders: Option[Set[String]] = Some(Set("*")),
    //   )
    // }

}