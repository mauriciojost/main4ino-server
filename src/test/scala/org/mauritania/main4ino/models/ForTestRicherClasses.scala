package org.mauritania.main4ino.models

import org.mauritania.main4ino.models.Device.DbId
import org.mauritania.main4ino.models.Device.Metadata.Status

object ForTestRicherClasses {

  implicit class DeviceRich(val d: Device) extends AnyVal {

    def withDeviceName(n: DeviceName): Device = d.copy(metadata = d.metadata.copy(device = n))

    def withStatus(s: Status): Device = Device(d.metadata.copy(status = s), d.actors)

    def withoutActors(): Device = d.copy(actors = Map())

    def withActorPropValue(a: ActorName, p: PropName, v: PropValue): Device = d.copy(actors = d.actors ++ Map(a -> Map(p -> v)))


  }

  implicit class DeviceIdRich(val d: DeviceId) extends AnyVal {

    def withId(i: RequestId): DeviceId = d.copy(dbId = DbId(i, d.dbId.creation))

    def withTimestamp(t: EpochSecTimestamp): DeviceId = d.copy(dbId = DbId(d.dbId.id, t))

    def withDeviceName(n: DeviceName): DeviceId = d.copy(device = d.device.withDeviceName(n))

  }
}
