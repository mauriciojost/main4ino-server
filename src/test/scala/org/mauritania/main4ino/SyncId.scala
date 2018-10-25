package org.mauritania.main4ino

import cats.Id
import cats.catsInstancesForId
import cats.effect.Sync
import io.circe.Json
import org.http4s.EntityDecoder
import org.http4s.circe._

trait SyncId {

  implicit val SyncId = new Sync[Id] {
    // from Sync
    override def suspend[A](thunk: => Id[A]): Id[A] = thunk
    // from ApplicativeError
    override def raiseError[A](e: Throwable): Id[A] = throw e
    override def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] = fa
    // from Applicative
    override def pure[A](x: A): Id[A] = catsInstancesForId.pure(x)
    // from FlatMap
    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = catsInstancesForId.flatMap(fa)(f)
    override def tailRecM[A, B](a: A)(f: A => Id[Either[A, B]]): Id[B] = catsInstancesForId.tailRecM(a)(f)
  }

  implicit val DecoderIdString = implicitly[EntityDecoder[Id, String]]
  implicit val DecoderIdJson = implicitly[EntityDecoder[Id, Json]]


}
