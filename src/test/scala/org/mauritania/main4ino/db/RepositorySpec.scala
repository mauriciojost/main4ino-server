package org.mauritania.main4ino.db

import eu.timepit.refined.types.numeric.PosInt
import io.circe.Json
import org.mauritania.main4ino.Fixtures._
import org.mauritania.main4ino.db.Repository.{FromTo, ReqType}
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.models.Device.Metadata.Status.Closed
import org.mauritania.main4ino.models.Device.{DbId, Metadata}
import org.mauritania.main4ino.models.ForTestRicherClasses._
import org.mauritania.main4ino.models.{Description, DeviceId}
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RepositorySpec extends AnyFlatSpec with Matchers with TransactorCtx with ParallelTestExecution {

  "The repository" should "create and read a report" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)
      repo.insertDevice(ReqType.Reports, Device1, 0L).unsafeRunSync() shouldBe 1L
      repo.selectDeviceWhereRequestId(ReqType.Reports, Device1.metadata.device, 1L).unsafeRunSync() shouldBe
        Right(DeviceId1.withId(1L))
    }
  }

  it should "create and read a target" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)
      repo.insertDevice(ReqType.Targets, Device1, 0L).unsafeRunSync() shouldBe 1L
      repo.selectDeviceWhereRequestId(ReqType.Targets, Device1.metadata.device, 1L).unsafeRunSync() shouldBe
        Right(DeviceId1.withId(1L))
    }
  }

  it should "create and read a target actor" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)
      val d = Device1.withoutActors().withStatus(Metadata.Status.Open)
      val dAfterInsert = DeviceId(DbId(1L, 0L), d.withStatus(Closed).withActorPropValue("actorx", "prop1", "val1"))

      repo.insertDevice(ReqType.Targets, d, 0L).unsafeRunSync() shouldBe 1L

      repo.insertDeviceActor(ReqType.Targets, Device1.name, "actorx", 1L, Map("prop1" -> "val1"), 0L).unsafeRunSync() shouldBe
        Right(1)
      repo.updateDeviceWhereRequestId(ReqType.Targets, Device1.name, List(1L), Metadata.Status.Closed).unsafeRunSync() shouldBe
        List(Right(1))
      repo.selectDeviceWhereRequestId(ReqType.Targets, Device1.name, 1L).unsafeRunSync() shouldBe
        Right(dAfterInsert)

      repo.insertDevice(ReqType.Targets, Device1.withStatus(Metadata.Status.Closed), 0L).unsafeRunSync() shouldBe 2L
      repo.insertDeviceActor(ReqType.Targets, Device1.name, "actorx", 2L, Map("" -> ""), 0L).unsafeRunSync() shouldBe
        Left("Request 2 is not open")
      repo.insertDeviceActor(ReqType.Targets, "dev2", "actorx", 2L, Map("" -> ""), 0L).unsafeRunSync() shouldBe
        Left("Request 2 does not relate to dev2")
      repo.updateDeviceWhereRequestId(ReqType.Targets, "dev2", List(1L), Metadata.Status.Closed).unsafeRunSync() shouldBe
        List(Left("Request 1 does not relate to dev2"))
      repo.updateDeviceWhereRequestId(ReqType.Targets, Device1.name, List(1L), Metadata.Status.Open).unsafeRunSync() shouldBe
        List(Left("State transition not allowed"))
    }
  }

  it should "create a target and read the latest image of it" in {
    withTransactor { transactor =>
      val Device1Modified = Device1.copy(actors = Device1.actors.updated("actory", Map("yprop1" -> "yvalue1updated")))
      val repo = new Repository(transactor)
      repo.insertDevice(ReqType.Targets, Device1, 77L).unsafeRunSync() shouldBe 1L
      repo.insertDevice(ReqType.Targets, Device1Modified, 88L).unsafeRunSync() shouldBe 2L
      repo.selectMaxDevice(ReqType.Targets, "dev1", None).unsafeRunSync() shouldBe Some(DeviceId(DbId(2L, 88L), Device1Modified))
    }
  }

  it should "read devices respecting from and to criteria" in {
    withTransactor { transactor =>

      val snap1 = Device1.withActorPropValue("a1", "v", "k")
      val snap2 = Device1.withActorPropValue("a2", "v", "k")
      val snap3 = Device1.withActorPropValue("a3", "v", "k")

      val repo = new Repository(transactor)
      repo.insertDevice(ReqType.Targets, snap1, 11L).unsafeRunSync() shouldBe 1L
      repo.insertDevice(ReqType.Targets, snap2, 12L).unsafeRunSync() shouldBe 2L
      repo.insertDevice(ReqType.Targets, snap3, 13L).unsafeRunSync() shouldBe 3L

      repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", fromTo = FromTo(Some(11L), None), st = None)
        .unsafeRunSync().map(_.device).toSet shouldBe Set(snap1, snap2, snap3)

      repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", fromTo = FromTo(Some(12L), None), st = None)
        .unsafeRunSync().map(_.device).toSet shouldBe Set(snap2, snap3)

      repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", fromTo = FromTo(None, Some(12L)), st = None)
        .unsafeRunSync().map(_.device).toSet shouldBe Set(snap1, snap2)

      repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", fromTo = FromTo(None, Some(11L)), st = None)
        .unsafeRunSync().map(_.device).toSet shouldBe Set(snap1)

      repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", fromTo = FromTo(Some(12L), Some(12L)), st = None)
        .unsafeRunSync().map(_.device).toSet shouldBe Set(snap2)
    }

  }

  it should "read devices respecting order (part 1)" in {
    withTransactor { transactor =>

      val snap1 = Device1.withActorPropValue("a", "v", "0") // third
    val snap2 = Device1.withActorPropValue("b", "v", "0") // first
    val snap3 = Device1.withActorPropValue("c", "v", "0") // second
    val snap4 = Device1.withActorPropValue("d", "v", "0") // fourth
    val snap5 = Device1.withActorPropValue("e", "v", "0") // fifth

      val repo = new Repository(transactor)
      repo.insertDevice(ReqType.Targets, snap1, 73L).unsafeRunSync() shouldBe 1L
      repo.insertDevice(ReqType.Targets, snap2, 71L).unsafeRunSync() shouldBe 2L
      repo.insertDevice(ReqType.Targets, snap3, 72L).unsafeRunSync() shouldBe 3L
      repo.insertDevice(ReqType.Targets, snap4, 74L).unsafeRunSync() shouldBe 4L
      repo.insertDevice(ReqType.Targets, snap5, 75L).unsafeRunSync() shouldBe 5L

      repo.selectDevicesWhereTimestampStatus(ReqType.Targets, "dev1", FromTo(None, None), st = None)
        .unsafeRunSync().toList.map(_.dbId.id) shouldBe List(2L, 3L, 1L, 4L, 5L) // retrieved in order of timestamp

    }
  }

  it should "read target/report ids from a device name" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)

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
  }

  it should "delete target/reports" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)

      val d1 = Device1.withDeviceName("device1")
      val d2 = Device1.withDeviceName("device2")

      ReqType.all.foreach { table =>
        repo.insertDevice(table, d1, 10L).unsafeRunSync() shouldBe 1L // created target for device 1, resulted in id 1
        repo.insertDevice(table, d2, 20L).unsafeRunSync() shouldBe 2L // for device 2, resulted in id 2

        repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L)
        repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L)

        repo.deleteDeviceWhereName(table, d1.metadata.device).unsafeRunSync()

        repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe Nil
        repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L)
      }
    }

  }


  it should "delete old target/reports and keep last one per device" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)

      val d1 = Device1.withDeviceName("device1")
      val d2 = Device1.withDeviceName("device2")

      ReqType.all.foreach { table =>
        repo.insertDevice(table, d1, 0L).unsafeRunSync() shouldBe 1L // created target for device 1, resulted in id 1
        repo.insertDevice(table, d1, 5L).unsafeRunSync() shouldBe 2L // another update
        repo.insertDevice(table, d1, 5L).unsafeRunSync() shouldBe 3L // another update
        repo.insertDevice(table, d1, 5L).unsafeRunSync() shouldBe 4L // another update

        repo.insertDevice(table, d2, 0L).unsafeRunSync() shouldBe 5L // for device 2, resulted in id 2
        repo.insertDevice(table, d2, 5L).unsafeRunSync() shouldBe 6L // another update
        repo.insertDevice(table, d2, 5L).unsafeRunSync() shouldBe 7L // another update
        repo.insertDevice(table, d2, 5L).unsafeRunSync() shouldBe 8L // another update

        repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L, 2L, 3L, 4L)
        repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(5L, 6L, 7L, 8L)

        repo.cleanup(table, 10, PosInt(10)).unsafeRunSync() shouldBe 0 // should preserve all (remove none)

        repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(1L, 2L, 3L, 4L)
        repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(5L, 6L, 7L, 8L)

        repo.cleanup(table, 10, PosInt(5)).unsafeRunSync() shouldBe 2 // should remove creations at 0L (so 2 in total)

        repo.selectRequestIdsWhereDevice(table, d1.metadata.device).compile.toList.unsafeRunSync() shouldBe List(2L, 3L, 4L)
        repo.selectRequestIdsWhereDevice(table, d2.metadata.device).compile.toList.unsafeRunSync() shouldBe List(6L, 7L, 8L)

        repo.cleanup(table, 10, PosInt(1)).unsafeRunSync() shouldBe 6 // remove all updates

      }
    }
  }

  it should "create and retrieve descriptions" in {
    withTransactor { transactor =>
      val repo = new Repository(transactor)
      val json = Json.fromString("{}")
      repo.getDescription("dev1").unsafeRunSync() shouldBe Left("No description for 'dev1'")
      repo.setDescription("dev1", VersionJson("1", Json.Null), 0L).unsafeRunSync() shouldBe 1
      repo.getDescription("dev1").unsafeRunSync() shouldBe Right(Description("dev1", 0L, VersionJson("1", Json.Null)))
      repo.setDescription("dev1", VersionJson("2", json), 0L).unsafeRunSync() shouldBe 1
      repo.getDescription("dev1").unsafeRunSync() shouldBe Right(Description("dev1", 0L, VersionJson("2", json)))
    }
  }

}
