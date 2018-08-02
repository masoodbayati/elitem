package ir.sndu.server.group

import java.util.UUID

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ ActorRef, Props }
import akka.actor.{ ActorSystem, ExtendedActorSystem, Extension, ExtensionId }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, EntityRef, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.util.Timeout
import ir.sndu.server.ActorRefConversions._
import ir.sndu.server.group.GroupCommands.{ Create, CreateAck }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
class GroupExtensionImpl(system: ActorSystem) extends Extension {
  implicit val timeout: Timeout = 15.seconds
  implicit val scheduler = system.scheduler
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val _sys = system

  private val sharding = ClusterSharding(system.toTyped)

  private val shardRegion: ActorRef[ShardingEnvelope[GroupCommand]] = sharding.spawn(
    behavior = entityId ⇒ GroupProcessor.behavior,
    props = Props.empty,
    typeKey = GroupProcessor.ShardingTypeName,
    settings = ClusterShardingSettings(system.toTyped),
    maxNumberOfShards = GroupProcessor.MaxNumberOfShards,
    handOffStopMessage = StopOffice)

  val groupId = UUID.randomUUID().toString

  val entityRef: EntityRef[GroupCommand] =
    sharding.entityRefFor(GroupProcessor.ShardingTypeName, groupId)

  private def construct(r: ActorRef[CreateAck]): Create = Create(replyTo = r)

  val f: Future[CreateAck] = entityRef ? construct

}

object GroupExtension extends ExtensionId[GroupExtensionImpl] {
  override def createExtension(system: ExtendedActorSystem): GroupExtensionImpl = new GroupExtensionImpl(system)
}
