package net.arendsyl

//#user-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import java.util.UUID
import scala.collection.immutable

//#user-case-classes
final case class User(id: UUID = UUID.randomUUID(), name: String, age: Int)
final case class Users(users: immutable.Seq[User])
//#user-case-classes

object UserRegistry {

  // actor protocol
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User) extends Command
  final case class GetUser(id: UUID, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(id: UUID) extends Command

  final case class GetUserResponse(maybeUser: Option[User])

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[User]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! Users(users.toSeq)
        Behaviors.same
      case CreateUser(user) =>
        registry(users + user)
      case GetUser(id, replyTo) =>
        replyTo ! GetUserResponse(users.find(_.id == id))
        Behaviors.same
      case DeleteUser(id) =>
        registry(users.filterNot(_.id == id))
    }
  }
}
//#user-registry-actor
