package org.mauritania.main4ino.models

import org.mauritania.main4ino.Fixtures.Device1
import org.mauritania.main4ino.Repository.ReqType
import org.mauritania.main4ino.models.Device.Metadata
import org.mauritania.main4ino.models.Device.Metadata.Status.Closed
import org.mauritania.main4ino.models.ForTestRicherClasses._
import org.mauritania.main4ino.{DbSuite, RepositoryIO}
import org.scalatest.Sequential

class RepositoryIOSpec extends DbSuite {

  Sequential

  "The repository" should "create and read a report" in {
    val repo = new RepositoryIO(transactor)
    repo.insertDevice(ReqType.Reports, Device1, 0L).unsafeRunSync() shouldBe 1L
    repo.selectDeviceWhereRequestId(ReqType.Reports, Device1.metadata.device, 1L).unsafeRunSync() shouldBe
      Right(Device1.withId(1L))
  }

  it should "create and read a target" in {
    val repo = new RepositoryIO(transactor)
    repo.insertDevice(ReqType.Targets, Device1, 0L).unsafeRunSync() shouldBe 1L
    repo.selectDeviceWhereRequestId(ReqType.Targets, Device1.metadata.device, 1L).unsafeRunSync() shouldBe
      Right(Device1.withId(1L))
  }

  it should "create and read a target actor" in {
    val repo = new RepositoryIO(transactor)
    val d = Device1.withoutActors().withStatus(Metadata.Status.Open)
    val dAfterInsert = d.withId(1L).withStatus(Closed).withActorPropValue("actorx", "prop1", "val1")

    repo.insertDevice(ReqType.Targets, d, 0L).unsafeRunSync() shouldBe 1L

    repo.insertDeviceActor(ReqType.Targets, Device1.name, "actorx",  1L, Map("prop1" -> "val1"), 0L).unsafeRunSync() shouldBe
      Right(1)
    repo.updateDeviceWhereRequestId(ReqType.Targets, Device1.name, 1L, Metadata.Status.Closed).unsafeRunSync() shouldBe
      Right(1)
    repo.selectDeviceWhereRequestId(ReqType.Targets, Device1.name, 1L).unsafeRunSync() shouldBe
      Right(dAfterInsert)

    repo.insertDevice(ReqType.Targets, Device1.withStatus(Metadata.Status.Closed), 0L).unsafeRunSync() shouldBe 2L
    repo.insertDeviceActor(ReqType.Targets, Device1.name, "actorx",  2L, Map("" -> ""), 0L).unsafeRunSync() shouldBe
      Left("Request 2 is not open")
    repo.insertDeviceActor(ReqType.Targets, "dev2", "actorx",  2L, Map("" -> ""), 0L).unsafeRunSync() shouldBe
      Left("Request 2 does not relate to dev2")
    repo.updateDeviceWhereRequestId(ReqType.Targets, "dev2", 1L, Metadata.Status.Closed).unsafeRunSync() shouldBe
      Left("Request 1 does not relate to dev2")
    repo.updateDeviceWhereRequestId(ReqType.Targets, Device1.name, 1L, Metadata.Status.Open).unsafeRunSync() shouldBe
      Left("State transition not allowed")
  }

  it should "create a target and read the latest image of it" in {
    val Device1Modified = Device1.copy(actors = Device1.actors.updated("actory", Map("yprop1" -> "yvalue1updated")))
    val repo = new RepositoryIO(transactor)
    repo.insertDevice(ReqType.Targets, Device1, 0L).unsafeRunSync() shouldBe 1L
    repo.insertDevice(ReqType.Targets, Device1Modified, 0L).unsafeRunSync() shouldBe 2L
    repo.selectMaxDevice(ReqType.Targets, "dev1", None).unsafeRunSync() shouldBe Some(Device1Modified.withId(2L))
  }

  it should "read devices respecting from and to criteria" in {

    val snap1 = Device1.withId(1L).withTimestamp(1L)
    val snap2 = Device1.withId(2L).withTimestamp(2L)
    val snap3 = Device1.withId(3L).withTimestamp(3L)

    val repo = new RepositoryIO(transactor)
    repo.insertDevice(ReqType.Targets, snap1, 0L).unsafeRunSync() shouldBe (1L)
    repo.insertDevice(ReqType.Targets, snap2, 0L).unsafeRunSync() shouldBe (2L)
    repo.insertDevice(ReqType.Targets, snap3, 0L).unsafeRunSync() shouldBe (3L)

    repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", from = Some(1L), to = None, st = None)
      .unsafeRunSync().toSet shouldBe Set(snap1, snap2, snap3)

    repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", from = Some(2L), to = None, st = None)
      .unsafeRunSync().toSet shouldBe Set(snap2, snap3)

    repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", from = None, to = Some(2L), st = None)
      .unsafeRunSync().toSet shouldBe Set(snap1, snap2)

    repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", from = None, to = Some(1L), st = None)
      .unsafeRunSync().toSet shouldBe Set(snap1)

    repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", from = Some(2L), to = Some(2L), st = None)
      .unsafeRunSync().toSet shouldBe Set(snap2)

  }

  it should "read devices respecting order (part 1)" in {

    val snap1 = Device1.withId(1L).withTimestamp(73L) // third
    val snap2 = Device1.withId(2L).withTimestamp(71L) // first
    val snap3 = Device1.withId(3L).withTimestamp(72L) // second
    val snap4 = Device1.withId(4L).withTimestamp(74L) // fourth
    val snap5 = Device1.withId(5L).withTimestamp(75L) // fifth

    val repo = new RepositoryIO(transactor)
    repo.insertDevice(ReqType.Targets, snap1, 0L).unsafeRunSync() shouldBe 1L
    repo.insertDevice(ReqType.Targets, snap2, 0L).unsafeRunSync() shouldBe 2L
    repo.insertDevice(ReqType.Targets, snap3, 0L).unsafeRunSync() shouldBe 3L
    repo.insertDevice(ReqType.Targets, snap4, 0L).unsafeRunSync() shouldBe 4L
    repo.insertDevice(ReqType.Targets, snap5, 0L).unsafeRunSync() shouldBe 5L

    repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", from = None, to = None, st = None)
      .unsafeRunSync().toList.flatMap(_.metadata.id) shouldBe List(2L, 3L, 1L, 4L, 5L) // retrieved in order of timestamp

  }

  it should "read target/report ids from a device name" in {
    val repo = new RepositoryIO(transactor)

    val t1 = Device1.withDeviceName("device1")
    val t2 = Device1.withDeviceName("device2")

    ReqType.all.foreach { table =>
      repo.insertDevice(table, t1, 0L).unsafeRunSync() shouldBe 1L // created target for device 1, resulted in id 1
      repo.insertDevice(table, t2, 0L).unsafeRunSync() shouldBe 2L // for device 2, resulted in id 2
      repo.insertDevice(table, t2, 0L).unsafeRunSync() shouldBe 3L // for device 2, resulted in id 3

      repo.selectRequestIdsWhereDevice(table, t1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L)
      repo.selectRequestIdsWhereDevice(table, t2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L, 3L)
    }
  }

  it should "delete target/reports" in {
    val repo = new RepositoryIO(transactor)

    val d1 = Device1.withDeviceName("device1").withTimestamp(10L)
    val d2 = Device1.withDeviceName("device2").withTimestamp(20L)

    ReqType.all.foreach { table =>
      repo.insertDevice(table, d1, 0L).unsafeRunSync() shouldBe (1L) // created target for device 1, resulted in id 1
      repo.insertDevice(table, d2, 0L).unsafeRunSync() shouldBe (2L) // for device 2, resulted in id 2

      repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L)
      repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L)

      repo.deleteDeviceWhereName(table, d1.metadata.device).unsafeRunSync()

      repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe Nil
      repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L)
    }
  }


  it should "delete old target/reports" in {
    val repo = new RepositoryIO(transactor)

    val d1 = Device1.withDeviceName("device1").withTimestamp(10L)
    val d2 = Device1.withDeviceName("device2").withTimestamp(20L)

    ReqType.all.foreach { table =>
      repo.insertDevice(table, d1, 0L).unsafeRunSync() shouldBe (1L) // created target for device 1, resulted in id 1
      repo.insertDevice(table, d2, 0L).unsafeRunSync() shouldBe (2L) // for device 2, resulted in id 2

      repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L)
      repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L)

      repo.cleanup(table, 30, 30).unsafeRunSync() shouldBe (0) // preserve all

      repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L)
      repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L)

      repo.cleanup(table, 30, 5).unsafeRunSync() shouldBe (2) // preserve nothing

      repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe Nil
      repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe Nil

    }
  }

}
