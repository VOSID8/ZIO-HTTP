import zio._
import zio.Console._
import zhttp._
import zhttp.http._
import zhttp.http.Http
import zhttp.service.Server


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
  }

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" => //ZIO.succeed(Response.text("Hoot!"))
      Random.nextIntBetween(3,5).map(n => Response.text("Hello"*n + ", owls" ))
  }

  val combined = app ++ zApp 
  //val combined = app <> zApp //if first fails, try second

  val httpProgram = for {
    _ <- Console.printLine(s"Server is starting on port ${port}")
    _ <- Server.start(port, combined) //internally calls zio.never
  } yield ()

  override def run = httpProgram
  
}