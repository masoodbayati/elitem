package ir.sndu.server.user

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import ir.sndu.struct.UserEnvelope

object UserProcessorRegion {

  private val extractEntityId: ShardRegion.ExtractEntityId = {
    case UserEnvelope(id, command, query) ⇒ (id.toString, command.valueOption.getOrElse(query.value))
  }

  private val numberOfShards = 100

  private val extractShardId: ShardRegion.ExtractShardId = {
    case UserEnvelope(id, _, _) ⇒ (id % numberOfShards).toString
    case ShardRegion.StartEntity(id) ⇒
      (id.toLong % numberOfShards).toString
  }

  private val typeName = "UserProcessor"

  private def start(props: Props)(implicit system: ActorSystem): ActorRef =
    ClusterSharding(system).start(
      typeName = typeName,
      entityProps = props,
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)

  def start()(implicit system: ActorSystem): ActorRef = start(UserProcessor.props)

}
