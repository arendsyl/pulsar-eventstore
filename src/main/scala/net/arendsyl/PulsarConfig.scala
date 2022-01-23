package net.arendsyl

import akka.actor.typed.ActorSystem


case class PulsarConfig(system: ActorSystem[_]) {
  private lazy val config = system.settings.config.getConfig("my-app.pulsar")

  lazy val url = config.getString("url")
  lazy val tenant = config.getString("tenant")
  lazy val ns = config.getString("ns")
  lazy val token = config.getString("token")
}
