package Project

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.{Marshal, ToResponseMarshallable}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import Project.model.{Doctor, ErrorResponse, Patient, SuccessfulResponse, Gender, TelegramMessage}
import Project.Service
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.{Config, ConfigFactory}
import Project.serializers.{ElasticSerializer, SprayJsonSerializer}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class Router(implicit val system: ActorSystem, implicit val materializer: Materializer, implicit val client: HttpClient) extends SprayJsonSerializer {
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  val healtcareService = system.actorOf(Service.props(client), "healthcare-service")

  val config: Config = ConfigFactory.load() // config, default - application.conf.
  val log = LoggerFactory.getLogger(this.getClass)

  val token = "1005517677:AAF53-ZxndoF75E1DBGEqNAIbMsLiWe0BPs" // config.getString("telegram.token") // token
  log.info(s"Token: $token")

  val chatID = -352088280;

  val route: Route =
    (path("healthcheck") & get) {
      complete {
        "OK"
      }
    } ~
      pathPrefix("kbtu-healtcare-generator") {
        path("patient" / Segment) { patientId =>
          get {
            val output = (healtcareService ? Service.GetPatient(patientId)).mapTo[Either[ErrorResponse, Patient]]
            onSuccess(output) {
              case Left(error) => {
                sendMessageToBot(s"Status: ${error.status}. Response: ${error.message}")
                complete(error.status, error)
              }
              case Right(patient) => {
                sendMessageToBot(s"Status: 200. Response: ${patient}")
                complete(200, patient)
              }
            }
          } ~
            delete {
              handle((healtcareService ? Service.DeletePatient(patientId)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
            }
        } ~
          (path("patient")) {
            post {
              entity(as[Patient]) { patient =>
                handle((healtcareService ? Service.PostPatient(patient)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
              }
            } ~
              put {
                entity(as[Patient]) { patient =>
                  handle((healtcareService ? Service.PutPatient(patient)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
                }
              }
          }
      }

  def handle(output: Future[Either[ErrorResponse, SuccessfulResponse]]) = {
    onSuccess(output) {
      case Left(error) => {
        sendMessageToBot(s"Status: ${error.status}. Response: ${error.message}")
        complete(error.status, error)
      }
      case Right(successful) => {
        sendMessageToBot(s"Status: ${successful.status}. Response: ${successful.message}");
        complete(successful.status, successful)
      }
    }
  }

  def sendMessageToBot(msg: String): Unit = {
    val message: TelegramMessage = TelegramMessage(chatID, msg);

    val httpReq = Marshal(message).to[RequestEntity].flatMap { entity =>
      val request = HttpRequest(HttpMethods.POST, s"https://api.telegram.org/bot$token/sendMessage", Nil, entity)
      log.debug("Request: {}", request)
      Http().singleRequest(request)
    }

    httpReq.onComplete {
      case Success(value) =>
        log.info(s"Response: $value")
        value.discardEntityBytes()

      case Failure(exception) =>
        log.error(exception.getMessage)
    }
  }
}
