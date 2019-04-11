package org.mauritania.main4ino.models

import org.mauritania.main4ino.models.Device.Metadata.Status.Status

object ForTestRicherClasses {

  implicit class DeviceRich(val d: Device) extends AnyVal {

    def withoutId(): Device = d.copy(metadata = d.metadata.copy(id = None))

    def withId(i: RequestId): Device = d.copy(metadata = d.metadata.copy(id = Some(i)))

    def withDeviceName(n: DeviceName): Device = d.copy(metadata = d.metadata.copy(device = n))

    def withStatus(s: Status): Device = Device(d.metadata.copy(status = s), d.actors)

    def withTimestamp(t: EpochSecTimestamp): Device = d.copy(metadata = d.metadata.copy(creation = Some(t)))

    def withoutIdNortTimestamp(): Device = d.copy(metadata = d.metadata.copy(id = None, creation = None))

    def withoutActors(): Device = d.copy(actors = Map())

    def withActorPropValue(a: ActorName, p: PropName, v: PropValue): Device = d.copy(actors = d.actors ++ Map(a -> Map(p -> v)))


  }

}
