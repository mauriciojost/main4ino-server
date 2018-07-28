package org.mauritania.botinobe.models

import org.mauritania.botinobe.{DbSuite, Repository}
import org.mauritania.botinobe.Fixtures.TargetFixture1

class RepositorySpec extends DbSuite {

  "The repository" should "create and read a target" in {
    val repo = new Repository(transactor)

    repo.createTarget(TargetFixture1).unsafeRunSync() shouldBe(1L)
    repo.readTarget(1L).unsafeRunSync() shouldBe(TargetFixture1)

  }

  it should "read target ids from a device name" in {
    val repo = new Repository(transactor)

    val t1 = TargetFixture1.copy(metadata = TargetFixture1.metadata.copy(device = "device1"))
    val t2 = TargetFixture1.copy(metadata = TargetFixture1.metadata.copy(device = "device2"))

    repo.createTarget(t1).unsafeRunSync() shouldBe(1L) // created target for device 1, resulted in id 1
    repo.createTarget(t2).unsafeRunSync() shouldBe(2L) // for device 2, resulted in id 2
    repo.createTarget(t2).unsafeRunSync() shouldBe(3L) // for device 2, resulted in id 3

    repo.readTargetIds(t1.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(1L))
    repo.readTargetIds(t2.metadata.device).compile.toList.unsafeRunSync() shouldBe(List(2L, 3L))

  }

  it should "read target ids and update them as consumed" in {
    val repo = new Repository(transactor)

    val t1Created = TargetFixture1.copy(metadata = TargetFixture1.metadata.copy(status = Target.Created))
    val t1Consumed = TargetFixture1.copy(metadata = TargetFixture1.metadata.copy(status = Target.Consumed))

    repo.createTarget(TargetFixture1).unsafeRunSync() shouldBe(1L)

    repo.readAndUpdateTargetAsConsumed(1L).unsafeRunSync() shouldBe(t1Created)
    repo.readAndUpdateTargetAsConsumed(1L).unsafeRunSync() shouldBe(t1Consumed)
  }

}
