package org.mauritania.main4ino.models

import org.mauritania.main4ino.db.Repository.{ActorTup, ActorTupIdLess}
import org.mauritania.main4ino.models.Device.{DbId, Metadata}
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.mauritania.main4ino.models.Device.Metadata.Status.Status

/**
  * Version of the [[Device]] after read from database.
  */
case class DeviceId(
  dbId: DbId,
  device: Device
) {
  def id: RequestId = dbId.id
  def creation: EpochSecTimestamp = dbId.creation
  def metadata: Metadata = device.metadata
  def actors: DeviceProps = device.actors
}

/**
  * Snapshot of a device before insertion into database (no ID or creation date).
  */
case class Device(
  metadata: Metadata,
  actors: DeviceProps = Device.EmptyActorMap
) {

  def asActorTups: Iterable[ActorTupIdLess] =
    for {
      (actor, ps) <- actors.toSeq
      (propName, propValue) <- ps.toSeq
    } yield (ActorTupIdLess(actor, propName, propValue))

  def actor(s: ActorName): Option[ActorProps] = actors.get(s)

  def name: DeviceName = metadata.device

}

object Device {

  val EmptyActorMap: DeviceProps = Map.empty[ActorName, ActorProps]

  /**
    * Represents data provided only AFTER insertion into database.
    */
  case class DbId(
    id: RequestId,
    creation: EpochSecTimestamp
  )

  /**
    * It represents a request that relates to a specific device
    */
  case class Metadata(
    device: DeviceName,
    status: Status
  )

  object Metadata {

    object Status {

      sealed abstract class Status(val code: String)

      // Natural progression: O -> C -> X
      case object Open extends Status("O") // just created, modifications can be done on it (like adding properties)
      case object Closed extends Status("C") // modifications cannot be done on it anymore, waiting for consumption
      case object Consumed extends Status("X") // it has been already consumed (no modifications can be done on it)

      case object Unknown extends Status("?") // status not defined

      val all = List(Open, Closed, Consumed, Unknown)

      def apply(code: String): Status = all.find(_.code == code).getOrElse(Unknown)
    }

  }

  def apply(dev: DeviceName, am: DeviceProps): Device = {
    val status = if (am.isEmpty) Status.Open else Status.Closed
    Device(Metadata(dev, status), am)
  }

  def merge(ds: Iterable[DeviceId]): Option[Device] = {
    def flat(r: RequestId, am: DeviceProps): Iterable[(ActorName, PropName, PropValue, RequestId)] = am.flatMap {
      case (an, ps) => ps.map { case (pn, pv) => (an, pn, pv, r) }
    }
    val names = ds.map(_.device.metadata.device).toSet
    assert(names.size <= 1) // at least one, but not more than 1
    val as = ds.flatMap(d => flat(d.dbId.id, d.actors))
    val props = as
      .groupBy { case (actName, _, _, _) => actName }
      .mapValues { byActorActorTups =>
        byActorActorTups
          .groupBy { case (_, propName, _, _) => propName }
          .mapValues { byActorPropActorTups =>
            val (_, _, propValue, _) = byActorPropActorTups.maxBy { case (_, _, _, reqId) => reqId }
            propValue
          }
      }
    names.headOption.map(n => Device(Metadata(n, Status.Unknown), props))
  }

}
