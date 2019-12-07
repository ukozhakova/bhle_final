package Project


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import Project.model.{Doctor, ErrorResponse, Patient, SuccessfulResponse}
import Project.Service
import akka.util.Timeout
import akka.pattern.ask
import Project.Router
import Project.serializers.ElasticSerializer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

object Boot extends App with ElasticSerializer {
  implicit val system: ActorSystem = ActorSystem("healthcare-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val client = ElasticSearchClient.client

  val route = Router().route

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
}
