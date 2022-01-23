package net.arendsyl

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.sksamuel.pulsar4s._
import net.arendsyl.JsonFormats.userSchema
import net.arendsyl.UserRegistry.{Command, CreateUser, DeleteUser}

import java.util.UUID
import java.util.concurrent.TimeUnit

sealed trait PulsarReceiveCommand
case object ReadTopic extends PulsarReceiveCommand

sealed trait PulsarSendCommand
case class SendUser(user: User, replyTo: ActorRef[ActionPerformed]) extends PulsarSendCommand
case class SendDeletion(id: UUID, replyTo: ActorRef[ActionPerformed]) extends PulsarSendCommand

final case class ActionPerformed(description: String)


object PulsarReceiverActor {
  def apply(consumer: Consumer[User], registry: ActorRef[Command]): Behavior[PulsarReceiveCommand] = Behaviors.setup { context =>
    implicit val ec = context.executionContext
    implicit val system = context.system
    implicit val logger = context.log

    def receive(consumer: Consumer[User]): Unit = {
      consumer.receiveAsync.foreach { message =>
        logger.info(s"received message : $message")
        Option(message.value)
          .fold(
            registry ! DeleteUser(UUID.fromString(message.key.get)))(
            registry ! CreateUser(_)
          )
        receive(consumer)
      }
    }

    Behaviors.receiveMessage {
      case ReadTopic =>
        receive(consumer)
        Behaviors.same
    }

  }
}

object PulsarSendActor {
  def apply(producer: Producer[User]): Behavior[PulsarSendCommand] = Behaviors.setup { context =>
    implicit val ec = context.executionContext
    implicit val system = context.system

    Behaviors.receiveMessage {
      case SendUser(user, replyTo) =>
        producer
          .sendAsync(msg = ProducerMessage(
            key = user.id.toString, t = user
          ))
          .foreach(_ => replyTo ! ActionPerformed(s"User ${user.id} created."))
        Behaviors.same
      case SendDeletion(id, replyTo) =>
        producer.sendAsync(msg = ProducerMessage(
          key = id.toString, t = null
        ))
        replyTo ! ActionPerformed(s"User $id deleted.")
        Behaviors.same
    }
  }
}
