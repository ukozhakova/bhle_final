package Project.serializers

import Project.model.{Doctor, ErrorResponse, Patient, SuccessfulResponse, TelegramMessage, Gender}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, JsString, DeserializationException, JsValue}

class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
  override def write(obj: T#Value): JsValue = JsString(obj.toString)

  override def read(json: JsValue): T#Value = {
    json match {
      case JsString(txt) => enu.withName(txt)
      case somethingElse => throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
    }
  }
}
trait SprayJsonSerializer extends DefaultJsonProtocol{

  implicit val genderFormat = new EnumJsonConverter(Gender)
  implicit val doctorFormat = jsonFormat5(Doctor)
  implicit val patientFormat = jsonFormat6(Patient)
  implicit val successfulResponse = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse = jsonFormat2(ErrorResponse)
  implicit val messageFormat: RootJsonFormat[TelegramMessage] = jsonFormat2(TelegramMessage)
}
