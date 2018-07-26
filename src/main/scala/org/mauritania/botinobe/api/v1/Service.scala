package org.mauritania.botinobe.api.v1

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, MediaType}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.mauritania.botinobe.Models._
import org.mauritania.botinobe.{Models, Repository}
import io.circe.{Decoder, Encoder}

// Guidelines for REST:
// - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

class Service(repository: Repository) extends Http4sDsl[IO] {

  case class PostResponse(id: RecordId)

  implicit val decoderTarget = jsonOf[IO, Target]

  private implicit val l: Encoder[PropsMap] = Encoder.encodeMap[PropName, PropValue]
  private implicit val m: Decoder[PropsMap] = Decoder.decodeMap[PropName, PropValue]
  private implicit val e: Encoder[ActorPropsMap] = Encoder.encodeMap[ActorName, PropsMap]
  private implicit val d: Decoder[ActorPropsMap] = Decoder.decodeMap[ActorName, PropsMap]

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

      case req@POST -> Root / "devices" / device / "targets" => {
        for {
          p <- req.decodeJson[ActorPropsMap]
          id <- repository.createTarget(Target(Models.Created, device, p))
          resp <- Created(PostResponse(id).asJson)
        } yield (resp)
      }

        /*
      case req@GET -> Root / "devices" / device / "targets" / "last" => {
        Ok(Stream("[") ++ repository.getTarget(device).map(_.asJson).intersperse(",") ++ Stream("]"), `Content-Type`(MediaType.`application/json`))
      }
      */

      /*
  case GET -> Root / "todos" / LongVar(id) =>        to fix the parameter to a given type

      | >  POST /v1/devices/<dev1>/targets/  {"actor1":{"prop1": "val1"}}
      | <    200 {"id": 1}
      |
      | // existent targets for dev1
      | >  GET /v1/devices/<dev1>/targets/
        | <    200 {"targets":[1, 2], "count": 2}
      |
      | // get all targets merged dev1 and clean (transactional)
      | >  GET /v1/devices/<dev1>/targets?merge=true?clean=true
      | <    200 {"actor1":{"prop1": "val1", "prop2": "val2"}}
      |
      | // we can retrieve a specific target
      | >  GET /v1/devices/<dev2>/targets/<3>
        | <    200 {"actor3":{"prop3": "val3", "prop4": "val4"}}
      |
      |
      | >  GET /v1/devices/<dev2>/actors/actor3/targets?merge=true&clean=true
      | <    200 {"prop5": "val5"}

    case req @ POST -> Root / "device" / device / "target" => {
      for {
        t <- req.as[Target]
        resp <- Ok(TargetResponse("created", t).asJson)
      } yield (resp)
    }
    */

    }
  }

}
