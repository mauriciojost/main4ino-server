
import fs2.Stream
import cats.effect.IO
import org.mauritania.botinobe.models.Device
/*
val eff = Stream.eval(
  IO {
    println("BEING RUN!!")
    1 + 1
  }
)

val eff2 = Stream.eval[IO, Int](
  IO {
    println("DB WRITE")
    1 + 1
  }
)

//val k = eff(1)
eff.compile.toList.unsafeRunSync()
*/


