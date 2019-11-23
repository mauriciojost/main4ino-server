import cats.effect.{IO, Resource}

val x = for {
  i <- Resource.liftF[IO, String](IO("hey"))
  j <- Resource.liftF[IO, String](IO(i + "you"))
} yield j

x.use(_ => IO.never).unsafeRunSync()
