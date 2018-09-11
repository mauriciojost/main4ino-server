package org.mauritania.main4ino.models

import org.mauritania.main4ino.{DbSuite, Repository}
import org.mauritania.main4ino.Fixtures.Device1
import org.mauritania.main4ino.Repository.Table
import org.scalatest.Sequential
import org.mauritania.main4ino.models.RicherBom._

class RepositorySpec extends DbSuite {

  Sequential

  "The repository" should "create and read a report" in {
    val repo = new Repository(transactor)
    repo.insertDevice(Table.Reports, Device1).unsafeRunSync() shouldBe(1L)
    repo.selectDeviceWhereRequestId(Table.Reports, 1L).unsafeRunSync() shouldBe(Some(Device1.withId(Some(1L))))
  }

  it should "create and read a target" in {
    val repo = new Repository(transactor)
    repo.insertDevice(Table.Targets, Device1).unsafeRunSync() shouldBe(1L)
    repo.selectDeviceWhereRequestId(Table.Targets, 1L).unsafeRunSync() shouldBe(Some(Device1.withId(Some(1L))))
  }

  it should "create a target and read the latest image of it" in {
    val Device1Modified = Device1.copy(actors = Device1.actors.updated("actory", Map("yprop1" -> ("yvalue1updated", Status.Created))))
    val repo = new Repository(transactor)
    repo.insertDevice(Table.Targets, Device1).unsafeRunSync() shouldBe(1L)
    repo.insertDevice(Table.Targets, Device1Modified).unsafeRunSync() shouldBe(2L)
    repo.selectMaxDevice(Table.Targets, "dev1").unsafeRunSync() shouldBe(Some(Device1Modified.withId(Some(2L))))
  }

  it should "create a target and read the latest image of its actors" in {

    val snap1 = Device1 // contains actorx and actory properties
    val snap2 = // contains only actory properties (does not contain any actorx properties)
      Device1.withId(Some(2L)).copy(actors = Device1.actors.updated("actorx", Map()))

    val repo = new Repository(transactor)
    repo.insertDevice(Table.Targets, snap1).unsafeRunSync() shouldBe(1L)
    repo.insertDevice(Table.Targets, snap2).unsafeRunSync() shouldBe(2L)

    repo.selectMaxActorTupsStatus(Table.Targets, "dev1", "actorx", Some(Status.Created)).unsafeRunSync() shouldBe
      snap1.asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actorx").toList

    repo.selectMaxActorTupsStatus(Table.Targets, "dev1", "actory", Some(Status.Created)).unsafeRunSync() shouldBe
      snap2.asActorTups.map(_.withRequestId(Some(2L))).filter(_.actor == "actory").toList

  }

  it should "read devices respecting from and to criteria" in {

    val snap1 = Device1.withId(Some(1L)).withTimestamp(Some(1L))
    val snap2 = Device1.withId(Some(2L)).withTimestamp(Some(2L))
    val snap3 = Device1.withId(Some(3L)).withTimestamp(Some(3L))

    val repo = new Repository(transactor)
    repo.insertDevice(Table.Targets, snap1).unsafeRunSync() shouldBe(1L)
    repo.insertDevice(Table.Targets, snap2).unsafeRunSync() shouldBe(2L)
    repo.insertDevice(Table.Targets, snap3).unsafeRunSync() shouldBe(3L)

    repo.selectDevicesWhereTimestamp(Table.Targets, "dev1", from = Some(1L), to = None)
      .unsafeRunSync().toSet shouldBe Set(snap1, snap2, snap3)

    repo.selectDevicesWhereTimestamp(Table.Targets, "dev1", from = Some(2L), to = None)
      .unsafeRunSync().toSet shouldBe Set(snap2, snap3)

    repo.selectDevicesWhereTimestamp(Table.Targets, "dev1", from = None, to = Some(2L))
      .unsafeRunSync().toSet shouldBe Set(snap1, snap2)

    repo.selectDevicesWhereTimestamp(Table.Targets, "dev1", from = None, to = Some(1L))
      .unsafeRunSync().toSet shouldBe Set(snap1)

    repo.selectDevicesWhereTimestamp(Table.Targets, "dev1", from = Some(2L), to = Some(2L))
      .unsafeRunSync().toSet shouldBe Set(snap2)

  }

  it should "read target/report ids from a device name" in {
    val repo = new Repository(transactor)

    val t1 = Device1.withDeviceName("device1")
    val t2 = Device1.withDeviceName("device2")

    Table.all.foreach { table =>
      repo.insertDevice(table, t1).unsafeRunSync() shouldBe(1L) // created target for device 1, resulted in id 1
      repo.insertDevice(table, t2).unsafeRunSync() shouldBe(2L) // for device 2, resulted in id 2
      repo.insertDevice(table, t2).unsafeRunSync() shouldBe(3L) // for device 2, resulted in id 3

      repo.selectRequestIdsWhereDevice(table, t1.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(1L))
      repo.selectRequestIdsWhereDevice(table, t2.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(2L, 3L))
    }
  }

  it should "read target/report ids and update them as consumed" in {
    val repo = new Repository(transactor)
    Table.all.foreach { table =>
      val ref = Device1.withId(Some(1L))
      repo.insertDevice(table, Device1).unsafeRunSync() shouldBe 1L

      // Created the device
      repo.selectDeviceWhereRequestId(table, 1L).unsafeRunSync() shouldBe Some(ref.withStatus(Status.Created))

      // Get properties for one actor, and set them to consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, Some("actorx"), Some(Status.Created), true)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actorx").toSet

      // Ensure they are consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, Some("actorx"), Some(Status.Consumed), false)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.withStatus(Status.Consumed).asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actorx").toSet

      // Take the other actor and ensure the properties are still not consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, Some("actory"), Some(Status.Created), false)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.withStatus(Status.Created).asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actory").toSet

      // Consume them
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, Some("actory"), Some(Status.Created), true)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actory").toSet

      // Ensure they are now consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, Some("actory"), Some(Status.Consumed), false)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.withStatus(Status.Consumed).asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actory").toSet
    }
  }

}
