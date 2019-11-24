import cats.effect.{IO, Resource}
import doobie.util.ExecutionContexts

val x = for {
  ec <- ExecutionContexts.cachedThreadPool[IO]
  i <- Resource.liftF[IO, String](IO("hey"))
  j <- Resource.liftF[IO, String](IO(i + "you"))
  k <- Resource.liftF[IO, Unit](IO(println(j)))
} yield k

val y = x.use(_ => IO.never).unsafeRunSync()

