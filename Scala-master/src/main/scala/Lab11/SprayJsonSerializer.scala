package Lab11

import Lab11.model.{ErrorResponse, PathModel, SuccessfulResponse}
import spray.json.DefaultJsonProtocol

trait SprayJsonSerializer extends DefaultJsonProtocol{
  implicit val successfulResponse = jsonFormat2(SuccessfulResponse)
  implicit val errorResponse = jsonFormat2(ErrorResponse)
  implicit val pathModel = jsonFormat1(PathModel)
}
