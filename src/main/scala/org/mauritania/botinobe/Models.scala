package org.mauritania.botinobe

import cats.Monoid

object Models {

  type RecordId = Long

  val monoidId = new Monoid[RecordId] {
    def combine(x: RecordId, y: RecordId): RecordId = x + y
    def empty: RecordId = 0
  }

  type DeviceName = String
  type ActorName = String

  type PropName = String
  type PropValue = String

  type PropsMap = Map[PropName, PropValue]
  type ActorPropsMap = Map[ActorName, PropsMap]

  /*
  sealed abstract class TargetStatus(val code: String)
  case object Created extends TargetStatus("created")
  case object Consumed extends TargetStatus("consumed")
  case object Unknown extends TargetStatus("unknown")
  object TargetStatus {
    val Values = Set(Created, Consumed, Unknown) // TODO: there must be smth is cats to handle enums
    def parse(s: String): TargetStatus = Values.find(_.code == s).getOrElse(Unknown)
  }
  */
  type TargetStatus = String
  val Created = "created"

  case class TargetActorProp(
    targetId: RecordId,
    actor: ActorName,
    prop: PropName,
    value: PropValue
  ) {
    //def asTarget: Target = Target(status, device, Map[ActorName, PropsMap](actor -> Map[PropName, PropValue](prop -> value)))
  }

  case class Target(
    status: TargetStatus,
    device: DeviceName,
    target: ActorPropsMap
  ) {
    def expand: Iterable[TargetActorProp] =
      for {
        (actor, props) <- target.toSeq
        (propName, propValue) <- props.toSeq
      } yield (TargetActorProp(-1, actor, propName, propValue))

    def merge(t: Target): Target = {
      assert(device == t.device)
      assert(status == t.status)
      Target(device, status, target ++ t.target)
    }
  }

  case object TargetNotFound

}

