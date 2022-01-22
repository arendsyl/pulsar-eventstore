package net.arendsyl

import org.apache.pulsar.client.api.Schema
import spray.json.{DeserializationException, JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import java.util.UUID

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val uuidJsonFormat: RootJsonFormat[UUID] = new RootJsonFormat[UUID] {
    def write(obj: UUID): JsValue = JsString(obj.toString)

    def read(json: JsValue): UUID = json match {
      case JsString(value) => UUID.fromString(value)
      case _ => throw DeserializationException(s"Invalid format $json")
    }
  }
  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat3(User)
  implicit val userCommandJsonFormat: RootJsonFormat[UserCommand] = jsonFormat2(UserCommand)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed] = jsonFormat1(ActionPerformed)

  implicit val userSchema: Schema[User] = com.sksamuel.pulsar4s.sprayjson.spraySchema
}
//#json-formats
