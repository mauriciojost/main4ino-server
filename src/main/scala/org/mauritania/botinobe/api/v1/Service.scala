package org.mauritania.botinobe.api.v1

import cats.Monoid
import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{HttpService, MediaType}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.mauritania.botinobe.Repository
import org.mauritania.botinobe.api.v1.Service.{CountResponse, IdResponse, TargetsResponse}
import org.mauritania.botinobe.models._
import org.mauritania.botinobe.models.Target.Metadata
import org.http4s.headers.`Content-Type`
import fs2.Stream

// Guidelines for REST:
// - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

class Service(repository: Repository) extends Http4sDsl[IO] {

  object CountQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("count")
  object MergeQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("merge")
  object CleanQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Boolean]("clean")

  val HelpMsg = // TODO: complete me
    s"""
       | API HELP
       | --- ----
       |
       | ...
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
          id <- repository.createTarget(Target(Metadata(Target.Created, device), p))
          resp <- Created(IdResponse(id).asJson)
        } yield (resp)
      }

      case req@GET -> Root / "devices" / device / "targets" :? CountQueryParamMatcher(count) +& CleanQueryParamMatcher(clean) +& MergeQueryParamMatcher(merge) => {
        val targetIds = repository.readTargetIds(device)
        if (count.exists(_ == true)) {
          Ok(targetIds.map(_ => 1).reduce(_+_).lastOr(0).map(CountResponse(_).asJson),
            `Content-Type`(MediaType.`application/json`))
        } /*else if (clean) {
          val targets = for {
            id <- targetIds
            target <- Stream.eval[IO, Target](repository.readTarget(id))
          } yield (target)
          Ok(targets.map(a => Map(1L, a.props)).reduce(reducer(merge)).lastOr(List.empty[Target]).map(TargetsResponse(_).asJson),
            `Content-Type`(MediaType.`application/json`))
        } */else /*(!clean)*/ {
          val targets = for {
            id <- targetIds
            target <- Stream.eval[IO, Target](repository.readAndUpdateTargetAsConsumed(id))
          } yield (target)
          val v = targets.fold(List.empty[Target])(_:+_).map(Target.merge(device, Target.Created, _))
          Ok(v.map(TargetsResponse(_).asJson), `Content-Type`(MediaType.`application/json`))
        }
      }

      case req@GET -> Root / "devices" / device / "targets" / LongVar(id) => {
        for {
          t <- repository.readTarget(id)
          resp <- Created(t.asJson)
        } yield (resp)
      }

      /*

      TODO

      | // get all targets merged dev1 and clean (transactional)
      | >  GET /v1/devices/<dev1>/targets?merge=true?clean=true
      | <    200 {"actor1":{"prop1": "val1", "prop2": "val2"}}
      |
      |
      | >  GET /v1/devices/<dev2>/actors/actor3/targets?merge=true&clean=true
      | <    200 {"prop5": "val5"}

    */

    }
  }

}

object Service {

  case class IdResponse(id: RecordId)
  case class CountResponse(count: Int)
  case class TargetsResponse(ts: Seq[Target])

}
