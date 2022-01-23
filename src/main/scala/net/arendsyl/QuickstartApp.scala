package net.arendsyl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.pulsar4s.{ConsumerConfig, ProducerConfig, PulsarClient, PulsarClientConfig, RollBack, Subscription, Topic}
import net.arendsyl.JsonFormats.userSchema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.impl.auth.AuthenticationToken

import java.util.UUID
import scala.util.{Failure, Success}

//#main-class
object QuickstartApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val config = PulsarConfig(context.system)
      val pulsarAsyncClient = PulsarClient(PulsarClientConfig(
        serviceUrl = config.url,
        authentication = Some(
          new AuthenticationToken(
            config.token
          )
        )
      ))
      val producer = pulsarAsyncClient.producer(ProducerConfig(Topic(s"persistent://${config.tenant}/${config.ns}/users")))
      val consumer = pulsarAsyncClient.consumer(ConsumerConfig(
        topics = Seq(Topic(s"persistent://${config.tenant}/${config.ns}/users")),
        readCompacted = Some(true),
        subscriptionName = Subscription(UUID.randomUUID().toString),
        subscriptionInitialPosition = Some(SubscriptionInitialPosition.Earliest)
      ))

      val pulsarSendActor = context.spawn(PulsarSendActor(producer), "PulsarSenderActor")
      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      val pulsarReceiverActor = context.spawn(PulsarReceiverActor(consumer, userRegistryActor), "PulsarReceiverActor")
      pulsarReceiverActor.tell(ReadTopic)
      context.watch(pulsarReceiverActor)

      val routes = new UserRoutes(userRegistryActor, pulsarSendActor)(context.system)
      startHttpServer(routes.userRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
