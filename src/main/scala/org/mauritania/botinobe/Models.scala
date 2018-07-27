package org.mauritania.botinobe

object Models {

  // Main types
  type RecordId = Long

  type DeviceName = String
  type ActorName = String

  type PropName = String
  type PropValue = String

  type PropsMap = Map[PropName, PropValue]
  type ActorPropsMap = Map[ActorName, PropsMap]


  type TargetStatus = String
  val Created = "created"
  val Read = "read"

  case class Prop(
    targetId: RecordId,
    prop1: Prop1
  )

  case class Prop1(
    actor: ActorName,
    prop: PropName,
    value: PropValue
  ) {
    //def asTarget: Target = Target(status, device, Map[ActorName, PropsMap](actor -> Map[PropName, PropValue](prop -> value)))
  }

  case class Target1 (
    status: TargetStatus,
    device: DeviceName
  )

  case class TargetI (
    id: RecordId,
    target: Target
  )

  case class Target(
    target1: Target1,
    target2: ActorPropsMap
  ) {
    def expand: Iterable[Prop1] =
      for {
        (actor, props) <- target2.toSeq
        (propName, propValue) <- props.toSeq
      } yield (Prop1(actor, propName, propValue))
  }
  object Target {
    def fromListOfProps(target1: Target1, l: List[Prop1]): Target = {
      val byActor: Map[ActorName, List[Prop1]] = l.groupBy(_.actor)
      val props: ActorPropsMap = byActor.mapValues(_.map(i => (i.value, i.prop)).toMap)
      Target(target1, props)

    }
  }

  case object TargetNotFound

}

