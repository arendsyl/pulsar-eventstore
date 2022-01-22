package net.arendsyl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.server.Route
import com.sksamuel.pulsar4s.{ProducerConfig, PulsarClient, PulsarClientConfig, Topic}
import net.arendsyl.JsonFormats.userSchema
import org.apache.pulsar.client.api.Authentication
import org.apache.pulsar.client.impl.auth.{AuthenticationBasic, AuthenticationToken}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success

//#main-class
object QuickstartApp {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
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
    val pulsarAsyncClient = PulsarClient(PulsarClientConfig(
      serviceUrl = "pulsar+ssl://c2-pulsar-clevercloud-customers.services.clever-cloud.com:2002",
      authentication = Some(
        new AuthenticationToken("ChkIABIFYWRtaW44AUIMCgoIBBICCAASAggHEugDCAESDWxpbWl0ZWRfcmlnaHQSATISATMSD3RvcGljX29wZXJhdGlvbhITbmFtZXNwYWNlX29wZXJhdGlvbjgBUqYDCtQBCmYICBIrIil1c2VyXzRkMTQ0NWQ2LTIyMTItNGJkZi1hODBhLTM2NWJmYzY4OGI4NRItIitwdWxzYXJfODdhOGJkOWEtZjQ3Mi00NDFhLTk4Y2QtYTliNzAzNDUwNGNiEgIQCRICEAoSaggLEgIIARIrIil1c2VyXzRkMTQ0NWQ2LTIyMTItNGJkZi1hODBhLTM2NWJmYzY4OGI4NRItIitwdWxzYXJfODdhOGJkOWEtZjQ3Mi00NDFhLTk4Y2QtYTliNzAzNDUwNGNiEgIQCRICEAoKzAEKYggIEisiKXVzZXJfNGQxNDQ1ZDYtMjIxMi00YmRmLWE4MGEtMzY1YmZjNjg4Yjg1Ei0iK3B1bHNhcl84N2E4YmQ5YS1mNDcyLTQ0MWEtOThjZC1hOWI3MDM0NTA0Y2ISAhAJEmYIDBICCAESKyIpdXNlcl80ZDE0NDVkNi0yMjEyLTRiZGYtYTgwYS0zNjViZmM2ODhiODUSLSIrcHVsc2FyXzg3YThiZDlhLWY0NzItNDQxYS05OGNkLWE5YjcwMzQ1MDRjYhICEAkaIMJU5L2s6C4BdAXmQ6m0sdvHEZdi3yxSTM3UhNTKQo1MGiCe6hIOa9laYT3k7Rpcb-eTrWFJecPcpD3u5E5oqoTsGSJmCiAYIzi5qQrR2Znfzx0YpHeL4-put4hHhQose6UjAJWlJwogbjNZXpvYg7N56i_wCwQj7TyeCI6IB4WMPWU65lOxdQoSIDQllb6VZgFm4TIHWP63cvzGu36wErDL6gL0DnGHI80B")
      )
    ))
    val producer = pulsarAsyncClient.producer(ProducerConfig(Topic("persistent://user_4d1445d6-2212-4bdf-a80a-365bfc688b85/pulsar_87a8bd9a-f472-441a-98cd-a9b7034504cb/users")))
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val pulsarSendActor = context.spawn(PulsarSendActor(producer), "PulsarSenderActor")
      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      val pulsarReceiverActor = context.spawn(PulsarReceiverActor(pulsarAsyncClient, userRegistryActor), "PulsarReceiverActor")
      pulsarReceiverActor.tell(ReadTopic(1000))
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
