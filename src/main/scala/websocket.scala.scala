
import zio._
import zio.Console._
import zhttp._
import zhttp.http._
import zhttp.http.Http
import zhttp.service.Server


object ZIOHTTP extends ZIOAppDefault {
  val port = 9000

  val sarcastically: String => String =
    txt =>
      txt.toList.zipWithIndex.map { case (c, i) =>
        if (i % 2 == 0) c.toUpper else c.toLower
      }.mkString

  val wsLogic: Http[Any, Throwable, WebSocketChannelEvent, Unit] =
    Http.collectZIO[WebSocketChannelEvent] {

      case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text(msg))) =>
        ch.writeAndFlush(WebSocketFrame.text(sarcastically(msg)))

      case ChannelEvent(ch, UserEventTriggered(event)) =>
        event match {
          case HandshakeComplete => ZIO.logInfo("Connection started!")
          case HandshakeTimeout  => ZIO.logInfo("Connection failed!")
        }

      case ChannelEvent(ch, ChannelUnregistered) =>
        ZIO.logInfo("Connection closed!")

    }

    val wsApp: Http[Any, Nothing, Request, Response] = Http.collectZIO[Request] {
        case Method.GET -> !! / "ws" => wsLogic.toSocketApp.toResponse
    }

  val httpProgram = for {
    _ <- Console.printLine(s"Server is starting on port ${port}")
    _ <- Server.start(port, wsApp) //internally calls zio.never
  } yield ()

  override def run = httpProgram

}