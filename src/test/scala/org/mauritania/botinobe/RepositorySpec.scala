package org.mauritania.botinobe.models

import org.mauritania.botinobe.{DbSuite, Repository}
import org.mauritania.botinobe.Fixtures.Device1
import org.mauritania.botinobe.Repository.Table
import org.scalatest.Sequential

class RepositorySpec extends DbSuite {

  Sequential

  "The repository" should "create and read a report" in {
    val repo = new Repository(transactor)
    repo.insertDevice(Table.Reports, Device1).unsafeRunSync() shouldBe(1L)
    repo.selectDeviceWhereRequestId(Table.Reports, 1L).unsafeRunSync() shouldBe(Device1.withId(Some(1L)))
  }

  it should "create and read a target" in {
    val repo = new Repository(transactor)
    repo.insertDevice(Table.Targets, Device1).unsafeRunSync() shouldBe(1L)
    repo.selectDeviceWhereRequestId(Table.Targets, 1L).unsafeRunSync() shouldBe(Device1.withId(Some(1L)))
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
      repo.selectDeviceWhereRequestId(table, 1L).unsafeRunSync() shouldBe ref.withStatus(Status.Created)

      // Get properties for one actor, and set them to consumed
      repo.selectActorTupChangeStatusWhereDeviceActorStatus(table, Device1.metadata.device, "actorx", Status.Created, Status.Consumed)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actorx").toSet

      // Ensure they are consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, "actorx", Status.Consumed)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.withStatus(Status.Consumed).asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actorx").toSet

      // Take the other actor and ensure the properties are still not consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, "actory", Status.Created)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.withStatus(Status.Created).asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actory").toSet

      // Consume them
      repo.selectActorTupChangeStatusWhereDeviceActorStatus(table, Device1.metadata.device, "actory", Status.Created, Status.Consumed)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actory").toSet

      // Ensure they are now consumed
      repo.selectActorTupWhereDeviceActorStatus(table, Device1.metadata.device, "actory", Status.Consumed)
        .compile.toList.unsafeRunSync().toSet shouldBe ref.withStatus(Status.Consumed).asActorTups.map(_.withRequestId(Some(1L))).filter(_.actor == "actory").toSet
    }
  }

}
