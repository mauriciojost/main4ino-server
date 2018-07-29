package org.mauritania.botinobe.models

import org.mauritania.botinobe.{DbSuite, Repository}
import org.mauritania.botinobe.Fixtures.Device1

class RepositorySpec extends DbSuite {

  "The repository" should "create and read a target/report" in {
    val repo = new Repository(transactor)

    repo.createTarget(Device1).unsafeRunSync() shouldBe(1L)
    repo.readTarget(1L).unsafeRunSync() shouldBe(Device1.withId(Some(1L)))

    repo.createReport(Device1).unsafeRunSync() shouldBe(1L)
    repo.readReport(1L).unsafeRunSync() shouldBe(Device1.withId(Some(1L)))

  }

  it should "read target/report ids from a device name" in {
    val repo = new Repository(transactor)

    val t1 = Device1.withDeviceName("device1")
    val t2 = Device1.withDeviceName("device2")

    repo.createTarget(t1).unsafeRunSync() shouldBe(1L) // created target for device 1, resulted in id 1
    repo.createTarget(t2).unsafeRunSync() shouldBe(2L) // for device 2, resulted in id 2
    repo.createTarget(t2).unsafeRunSync() shouldBe(3L) // for device 2, resulted in id 3

    repo.readTargetIds(t1.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(1L))
    repo.readTargetIds(t2.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(2L, 3L))

    repo.createReport(t1).unsafeRunSync() shouldBe(1L) // created report for device 1, resulted in id 1
    repo.createReport(t2).unsafeRunSync() shouldBe(2L) // for device 2, resulted in id 2
    repo.createReport(t2).unsafeRunSync() shouldBe(3L) // for device 2, resulted in id 3

    repo.readReportIds(t1.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(1L))
    repo.readReportIds(t2.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(2L, 3L))

  }

  it should "read target/report ids and update them as consumed" in {
    val repo = new Repository(transactor)

    val ref = Device1.withId(Some(1L))
    repo.createTarget(Device1).unsafeRunSync() shouldBe 1L

    repo.readTarget(1L).unsafeRunSync() shouldBe ref.withStatus(Status.Created)
    repo.readTargetConsume(1L).unsafeRunSync() shouldBe ref.withStatus(Status.Consumed)

    repo.createReport(Device1).unsafeRunSync() shouldBe 1L

    repo.readReport(1L).unsafeRunSync() shouldBe ref.withStatus(Status.Created)
    repo.readReportConsume(1L).unsafeRunSync() shouldBe ref.withStatus(Status.Consumed)

  }

}
