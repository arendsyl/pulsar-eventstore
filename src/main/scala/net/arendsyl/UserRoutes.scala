package net.arendsyl

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import net.arendsyl.UserRegistry._

import java.util.UUID
import scala.concurrent.Future

case class UserCommand(name: String, age: Int) {
  def user: User = User(UUID.randomUUID(), name, age)
}

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command], pulsarSendActor: ActorRef[PulsarSendCommand])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: UUID): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(command: UserCommand): Future[ActionPerformed] =
    pulsarSendActor.ask(SendUser(command.user, _))
  def deleteUser(name: UUID): Future[ActionPerformed] =
    pulsarSendActor.ask(SendDeletion(name, _))

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[UserCommand]) { command =>
                onSuccess(createUser(command)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        //#users-get-delete
        //#users-get-post
        path(Segment) { name =>
          concat(
            get {
              //#retrieve-user-info
              rejectEmptyResponse {
                onSuccess(getUser(UUID.fromString(name))) { response =>
                  complete(response.maybeUser)
                }
              }
              //#retrieve-user-info
            },
            delete {
              //#users-delete-logic
              onSuccess(deleteUser(UUID.fromString(name))) { performed =>
                complete((StatusCodes.OK, performed))
              }
              //#users-delete-logic
            })
        })
      //#users-get-delete
    }
  //#all-routes
}
