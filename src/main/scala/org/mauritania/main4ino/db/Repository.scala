package org.mauritania.main4ino.db

import cats.effect.Sync
import cats.free.Free
import cats.implicits._
import doobie._
import doobie.free.connection.{ConnectionOp, raw}
import doobie.implicits._
import doobie.util.transactor.Transactor
import eu.timepit.refined.types.numeric.PosInt
import fs2.Stream
import io.circe.Json
import org.mauritania.main4ino.api.{Attempt, ErrMsg}
import org.mauritania.main4ino.db.Config.DbSyntax
import org.mauritania.main4ino.db.Config.DbSyntax.DbSyntax
import org.mauritania.main4ino.db.Repository.ReqType.ReqType
import org.mauritania.main4ino.db.Repository.{ActorTup, ActorTupIdLess, Device1, FromTo, ReqType, Stats}
import org.mauritania.main4ino.models.Description.VersionJson
import org.mauritania.main4ino.models.Device.Metadata.Status
import org.mauritania.main4ino.models.Device.{DbId, Metadata}
import org.mauritania.main4ino.models.{RequestId, _}

// Naming regarding to SQL
class Repository[F[_]: Sync](syntax: DbSyntax, transactor: Transactor[F]) {

  implicit val StatusMeta: Meta[Status] =
    Meta[String].timap[Status](Status.withName(_))(_.entryName)
  implicit val JsonMeta: Meta[Json] =
    Meta[String].timap[Json](io.circe.parser.parse(_).getOrElse(Json.Null))(_.noSpaces)

  def setDescription(d: DeviceName, v: VersionJson, ts: EpochSecTimestamp): F[Int] = {
    val transaction = for {
      i <- sqlUpdateDescription(d, v, ts)
    } yield (i)
    transaction.transact(transactor)
  }

  def getDescription(device: String): F[Attempt[Description]] = {
    val transaction = for {
      d <- sqlSelectDescription(device)
      y = d.toRight(s"No description for '$device'")
    } yield (y)
    transaction.transact(transactor)
  }

  def cleanup(table: ReqType, now: EpochSecTimestamp, retentionSecs: PosInt): F[Int] = {
    val transaction = for {
      m <- sqlDeleteMetadataWhereCreationIsLess(table, now - retentionSecs.value)
      _ <- sqlDeleteActorTupOrphanOfRequest(table)
    } yield (m)
    transaction.transact(transactor)
  }

  def stats(): F[Stats] = {
    val transaction = for {
      repReq <- (fr"SELECT count(*) FROM " ++ Fragment.const(ReqType.Reports.code + "_requests")).query[Long].unique
      rep <- (fr"SELECT count(*) FROM " ++ Fragment.const(ReqType.Reports.code)).query[Long].unique
      tarReq <- (fr"SELECT count(*) FROM " ++ Fragment.const(ReqType.Targets.code + "_requests")).query[Long].unique
      tar <- (fr"SELECT count(*) FROM " ++ Fragment.const(ReqType.Targets.code)).query[Long].unique
      descr <- (fr"SELECT count(*) FROM descriptions").query[Long].unique
    } yield Stats(repReq, rep, tarReq, tar, descr)
    transaction.transact(transactor)
  }

  def deleteDeviceWhereName(table: ReqType, device: String): F[Int] = {
    val transaction = for {
      m <- sqlDeleteMetadataWhereDeviceName(table, device)
      _ <- sqlDeleteActorTupOrphanOfRequest(table)
    } yield (m)
    transaction.transact(transactor)
  }

  def insertDevice(table: ReqType, t: Device, ts: EpochSecTimestamp): F[RequestId] = {
    // TODO potential optimization: do all in one single sql query
    val transaction = for {
      deviceId <- sqlInsertMetadata(table, t.metadata, ts)
      _ <- sqlInsertActorTup(table, t.asActorTups, deviceId, ts)
    } yield (deviceId)
    transaction.transact(transactor)
  }

  def insertDeviceActor(
    table: ReqType,
    dev: DeviceName,
    actor: ActorName,
    requestId: RequestId,
    p: ActorProps,
    ts: EpochSecTimestamp
  ): F[Attempt[Int]] = {
    val transaction = for {
      mtd <- sqlSelectMetadataWhereRequestId(table, requestId)
      safe = mtd.exists { case (_, m) => m.device == dev }
      transit = mtd.exists { case (_, m) => m.status == Status.Open }
      inserts: ConnectionIO[Attempt[Int]] = if (!safe)
        Free.pure[ConnectionOp, Attempt[Int]](
          Left.apply[ErrMsg, Int](s"Request $requestId does not relate to $dev")
        )
      else if (!transit)
        Free.pure[ConnectionOp, Attempt[Int]](
          Left.apply[ErrMsg, Int](s"Request $requestId is not open")
        )
      else
        sqlInsertActorTup(table, ActorTup.from(actor, p), requestId, ts)
          .map(Right.apply[ErrMsg, Int])
      attempt <- inserts
    } yield (attempt)
    transaction.transact(transactor)
  }

  private def transitionAllowed(current: Status, target: Status): Boolean = {
    (current, target) match {
      case (Status.Open, Status.Closed) => true
      case (Status.Closed, Status.Consumed) => true
      case _ => false
    }
  }

  def updateDeviceWhereRequestId(
    table: ReqType,
    dev: DeviceName,
    requestIds: List[RequestId],
    status: Status
  ): F[List[Attempt[Int]]] = {
    val transactions = requestIds.map(r => updateDeviceWhereRequestIdTran(table, dev, r, status))
    transactions.traverse(i => i).transact(transactor)
  }

  def updateDeviceWhereRequestIdTran(
    table: ReqType,
    dev: DeviceName,
    requestId: RequestId,
    status: Status
  ): ConnectionIO[Attempt[Int]] = {
    val transaction = for {
      mtd <- sqlSelectMetadataWhereRequestId(table, requestId)
      safe = mtd.exists { case (i, m) => (m.device == dev) }
      trans = mtd.exists { case (i, m) => transitionAllowed(m.status, status) }
      updates: ConnectionIO[Attempt[Int]] = if (!safe)
        Free.pure[ConnectionOp, Attempt[Int]](
          Left.apply[ErrMsg, Int](s"Request $requestId does not relate to $dev")
        )
      else if (!trans)
        Free.pure[ConnectionOp, Attempt[Int]](
          Left.apply[ErrMsg, Int](s"State transition not allowed")
        )
      else
        sqlUpdateMetadataWhereRequestId(table, requestId, status).map(Right.apply[ErrMsg, Int])
      nroUpdates <- updates
    } yield (nroUpdates)
    transaction
  }

  def selectDeviceWhereRequestId(
    table: ReqType,
    dev: DeviceName,
    requestId: RequestId
  ): F[Attempt[DeviceId]] = {
    val transaction = for {
      t <- sqlSelectMetadataWhereRequestId(table, requestId)
      p <- sqlSelectActorTupWhereRequestIdActorStatus(table, requestId)
      j = t
        .map { case (i, m) => Repository.toDevice(i, m, p) }
        .filter(_.metadata.device == dev)
        .toRight(s"Request $requestId does not belong to $dev")
    } yield (j)
    transaction.transact(transactor)
  }

  def selectDevicesWhereTimestampStatus(
    table: ReqType,
    device: DeviceName,
    fromTo: FromTo,
    st: Option[Status]
  ): F[Iterable[DeviceId]] = {
    val transaction = sqlSelectMetadataActorTupWhereDeviceStatus(table, device, fromTo, st)
    val s = transaction.transact(transactor)
    val iol = s.compile.toList
    iol.map(l => Device1.asDeviceHistory(l).toSeq.sortBy(_.dbId.creation))
  }

  def selectDevicesLastWhereStatus(
    table: ReqType,
    device: DeviceName,
    st: Status,
  ): F[Iterable[DeviceId]] = {
    val transaction = sqlSelectMetadataActorTupWhereDeviceStatus2(table, device, st)
    val s = transaction.transact(transactor)
    val iol = s.compile.toList
    iol.map(l => Device1.asDeviceHistory(l).toSeq)
  }

  def selectMaxDevice(
    table: ReqType,
    device: DeviceName,
    status: Option[Status]
  ): F[Option[DeviceId]] = {
    val transaction = for {
      i <- sqlSelectLastRequestIdWhereDeviceStatus(table, device, status)
      t <- i
        .map(sqlSelectMetadataWhereRequestId(table, _))
        .getOrElse(raw[Option[(DbId, Metadata)]](x => None))
      p <- i
        .map(sqlSelectActorTupWhereRequestIdActorStatus(table, _))
        .getOrElse(raw[List[ActorTup]](x => List.empty[ActorTup]))
    } yield {
      (t, p) match {
        case (Some((i, m)), l) => Some(Repository.toDevice(i, m, l))
        case _ => None
      }
    }
    transaction.transact(transactor)
  }

  def selectRequestIdsWhereDevice(table: ReqType, d: DeviceName): Stream[F, RequestId] = {
    (fr"SELECT id FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$d")
      .query[RequestId]
      .stream
      .transact(transactor)
  }

  // SQL queries (private)

  private def sqlInsertActorTup(
    table: ReqType,
    tups: Iterable[ActorTupIdLess],
    requestId: RequestId,
    ts: EpochSecTimestamp
  ): ConnectionIO[Int] = {
    val sql =
      s"INSERT INTO ${table.code} (request_id, actor_name, property_name, property_value, creation) VALUES (?, ?, ?, ?, ?)"
    Update[ActorTup](sql).updateMany(tups.toList.map(t => ActorTup(requestId, t, ts)))
  }

  private def sqlInsertMetadata(
    table: ReqType,
    m: Metadata,
    ts: EpochSecTimestamp
  ): ConnectionIO[RequestId] = {
    (fr"INSERT INTO " ++ Fragment.const(table.code + "_requests") ++ fr" (creation, device_name, status) VALUES (${ts}, ${m.device}, ${m.status})").update
      .withUniqueGeneratedKeys[RequestId]("id")
  }

  private def sqlUpdateDescription(
    dev: DeviceName,
    d: VersionJson,
    ts: EpochSecTimestamp
  ): ConnectionIO[Int] = {
    syntax match {
      // $COVERAGE-OFF$
      // Too difficult to have integration tests with PostgreSQL
      case DbSyntax.Postgres =>
        (fr"INSERT INTO descriptions (device_name, updated, version, json) VALUES (${dev}, ${ts}, ${d.version}, ${d.json}) ON CONFLICT (device_name) DO UPDATE SET device_name = ${dev}").update.run
      // $COVERAGE-ON$
      case _ =>
        fr"MERGE INTO descriptions (device_name, updated, version, json) KEY (device_name) VALUES (${dev}, ${ts}, ${d.version}, ${d.json})".update.run
    }

  }

  private def sqlSelectDescription(d: DeviceName): ConnectionIO[Option[Description]] = {
    (fr"SELECT device_name, updated, version, json FROM descriptions WHERE device_name = ${d}")
      .query[Description]
      .option
  }

  private def sqlUpdateMetadataWhereRequestId(
    table: ReqType,
    r: RequestId,
    s: Status
  ): ConnectionIO[Int] = {
    (fr"UPDATE " ++ Fragment.const(table.code + "_requests") ++ fr" SET status = ${s} WHERE id=${r}").update.run
  }

  private def sqlDeleteMetadataWhereDeviceName(
    table: ReqType,
    device: DeviceName
  ): ConnectionIO[Int] = {
    (fr"DELETE FROM" ++ Fragment.const(table.code + "_requests") ++ fr"WHERE device_name=$device").update.run
  }

  private def sqlDeleteMetadataWhereCreationIsLess(
    table: ReqType,
    upperbound: EpochSecTimestamp
  ): ConnectionIO[Int] = {
    (fr"DELETE FROM" ++ Fragment.const(table.code + "_requests") ++ fr"WHERE creation < $upperbound").update.run
  }

  private def sqlDeleteActorTupOrphanOfRequest(table: ReqType): ConnectionIO[Int] = {
    (fr"DELETE FROM " ++ Fragment.const(table.code) ++ fr" tu WHERE NOT EXISTS (SELECT 1 FROM " ++ Fragment
      .const(table.code + "_requests") ++ fr" re where tu.request_id = re.id)").update.run
  }

  private def sqlSelectMetadataActorTupWhereDeviceStatus2(
    table: ReqType,
    d: DeviceName,
    st: Status
  ): Stream[ConnectionIO, Device1] = {
    val tableRequestsFr = Fragment.const(table.code + "_requests")
    val tableFr = Fragment.const(table.code)
    val fr2 = fr"""SELECT m.id, 0 as request_creation, m.device_name, $st as status, 0 as xxx, m.actor_name, m.property_name, x.property_value, 0 as creation
    FROM (
      SELECT device_name, actor_name, property_name, max(id) as id
      FROM (
        SELECT r.id, r.device_name, t.actor_name, t.property_name, t.property_value FROM""" ++ tableRequestsFr ++ fr"""r
        JOIN""" ++ tableFr ++ fr"""as t ON r.id = t.request_id
        WHERE r.device_name=$d and r.status=$st
      ) AS i 
      GROUP BY device_name, actor_name, property_name
    ) AS m
    JOIN """ ++ tableFr ++ fr""" as x ON m.id = x.request_id AND m.actor_name = x.actor_name AND m.property_name = x.property_name"""


    fr2.query[Device1].stream

  }

  private def sqlSelectMetadataActorTupWhereDeviceStatus(
    table: ReqType,
    d: DeviceName,
    fromTo: FromTo,
    st: Option[Status]
  ): Stream[ConnectionIO, Device1] = {
    val fromFr = fromTo.from match {
      case Some(a) => fr"AND r.creation >= $a"
      case None => fr""
    }
    val toFr = fromTo.to match {
      case Some(a) => fr"AND r.creation <= $a"
      case None => fr""
    }
    val stFr = st match {
      case Some(s) => fr"AND r.status = $s"
      case None => fr""
    }
    (fr"SELECT r.id, r.creation, r.device_name, r.status, t.request_id, t.actor_name, t.property_name, t.property_value, t.creation" ++
      fr"FROM" ++ Fragment.const(table.code + "_requests") ++ fr"as r LEFT OUTER JOIN" ++ Fragment
      .const(table.code) ++ fr"as t" ++
      fr"ON r.id = t.request_id" ++
      fr"WHERE r.device_name=$d" ++ fromFr ++ toFr ++ stFr)
      .query[Device1]
      .stream
  }

  private def sqlSelectMetadataWhereRequestId(
    table: ReqType,
    id: RequestId
  ): ConnectionIO[Option[(DbId, Metadata)]] = {
    (fr"SELECT id, creation, device_name, status FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE id=$id")
      .query[(DbId, Metadata)]
      .option
  }

  private def sqlSelectLastRequestIdWhereDeviceStatus(
    table: ReqType,
    device: DeviceName,
    status: Option[Status]
  ): ConnectionIO[Option[RequestId]] = {
    val statusFr = status match {
      case Some(s) => fr"AND status = $s"
      case None => fr""
    }
    (fr"SELECT MAX(id) FROM " ++ Fragment.const(table.code + "_requests") ++ fr" WHERE device_name=$device" ++ statusFr)
      .query[Option[RequestId]]
      .unique
  }

  private def sqlSelectActorTupWhereRequestIdActorStatus(
    table: ReqType,
    requestId: RequestId,
  ): ConnectionIO[List[ActorTup]] = {
    (fr"SELECT request_id, actor_name, property_name, property_value, creation FROM " ++ Fragment
      .const(table.code) ++ fr" WHERE request_id=$requestId")
      .query[ActorTup]
      .accumulate
  }

}

object Repository {

  case class Stats(reportRequests: Long, reports: Long, targetRequests: Long, targets: Long, descriptions: Long) {
    override def toString(): String = s"repReq=$reportRequests, rep=$reports, tarReq=$targetRequests, tar=$targets, desc=$descriptions"
  }
  case class FromTo(from: Option[EpochSecTimestamp], to: Option[EpochSecTimestamp])

  def toDevice(dbId: DbId, metadata: Metadata, ats: Iterable[ActorTup]): DeviceId =
    DeviceId(dbId, Device(metadata, ActorTup.asActorMap(ats)))

  /**
    * Intermediate device representation
    *
    * It contains a single actor tuple. Instances of this class
    * are meant to be aggregated to create instances of [[Device]].
    *
    * @param dbId       the database related ids (and creation timestamp)
    * @param metadata   the metadata
    * @param actorTuple the single actor tuple (if present, there may be a metadata without any property)
    */
  case class Device1(
    dbId: DbId,
    metadata: Metadata,
    actorTuple: Option[ActorTup] // optional (if device filled up)
  )

  object Device1 {

    def asDeviceHistory(s: Iterable[Device1]): Iterable[DeviceId] = {
      val g = s.groupBy(_.dbId)
      val dh = g.map {
        case (id, d1s) =>
          DeviceId(id, Device(d1s.head.metadata, ActorTup.asActorMap(d1s.flatMap(_.actorTuple))))
      }
      dh
    }

  }

  object ReqType {

    sealed abstract class ReqType(val code: String)

    case object Reports extends ReqType("reports")

    case object Targets extends ReqType("targets")

    val all = List(Reports, Targets)

    def resolve(s: String): Option[ReqType] = all.find(_.code == s)
  }

  case class ActorTup(
    requestId: RequestId,
    more: ActorTupIdLess,
    creation: EpochSecTimestamp
  ) {
    def actor: ActorName = more.actor

    def prop: PropName = more.prop

    def value: PropValue = more.value
  }

  case class ActorTupIdLess(
    actor: ActorName,
    prop: PropName,
    value: PropValue
  )

  object ActorTup {

    def apply(
      id: RequestId,
      actor: ActorName,
      prop: PropName,
      value: PropValue,
      creation: EpochSecTimestamp
    ): ActorTup =
      ActorTup(id, ActorTupIdLess(actor, prop, value), creation)

    def asActorMap(ats: Iterable[ActorTup]): DeviceProps = {
      ats.groupBy(_.actor).mapValues { byActor =>
        val indexed = byActor.zipWithIndex
        val byProp = indexed.groupBy { case (t, i) => t.prop }
        byProp.mapValues { a =>
          val (latestPropValue, _) = a.maxBy { case (t, i) => i }
          latestPropValue.value
        }
      }
    }

    def from(
      actorName: ActorName,
      pm: ActorProps
    ): Iterable[ActorTupIdLess] = {
      pm.map {
        case (name, value) =>
          ActorTupIdLess(actorName, name, value)
      }
    }
  }

}
