package org.mauritania.botinobe.api.v1

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

// Use as reference:
// https://github.com/jaspervz/todo-http4s-doobie/tree/master/src/main/scala
object Service extends Http4sDsl[IO] {

  type Props = Map[String, String]
  case class Target(t: Props)
  case class TargetResponse(msg: String, t: Target)

  implicit val decoderTarget = jsonOf[IO, Target]

  val HelpMsg = // TODO: complete me
    s"""
      | API HELP
      | --- ----
      |
      | TODO
      |
      |
    """.stripMargin

  val service = {
    HttpService[IO] {

      case GET -> Root / "help" => {
        Ok(HelpMsg)
      }

      case req @ POST -> Root / "device" / device / "target" => {
        for {
          t <- req.as[Target]
          resp <- Ok(TargetResponse("created", t).asJson)
        } yield (resp)
      }

    }
  }

}
