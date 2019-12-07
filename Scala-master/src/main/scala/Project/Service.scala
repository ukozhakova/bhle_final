package Project

import Project.model.{Doctor, ErrorResponse, Patient, SuccessfulResponse}
import akka.actor.{Actor, ActorLogging, Props}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.CreateIndexResponse
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
import Project.serializers.ElasticSerializer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.pattern.pipe

  object Service {

    val index = "patients"
    val mappingType = "_doc"

    case class PostPatient(patient: Patient)

    case class GetPatient(id: String)

    case class PutPatient(patient: Patient)

    case class DeletePatient(id: String)

    def props(esClient: HttpClient) = Props(new Service(esClient))

  }

  class Service(esClient: HttpClient) extends Actor with ActorLogging with ElasticSerializer {

    import Service._

    override def receive: Receive = {
      case PostPatient(patient) =>
        val replyTo = sender()
        val cmd = esClient.execute(indexInto(index / mappingType).id(s"${patient.id}").doc(patient))

        cmd.onComplete {
          case Success(_) =>
            log.info("Patient with ID: {} created.", patient.id)
            replyTo ! Right(SuccessfulResponse(201, s"Patient with ID: ${patient.id} created."))

          case Failure(_) =>
            log.warning(s"Could not create a patient with ID: ${patient.id}.")
            replyTo ! Left(ErrorResponse(500, s"Could not create a patient with ID: ${patient.id}."))
        }

      case msg: GetPatient =>
        val replyTo = sender()
        val cmd = esClient.execute {
          get(msg.id).from(index / mappingType)
        }

        cmd.onComplete {
          case Success(either) =>
            either.map(e => e.result.safeTo[Patient]).foreach { patient => {
              patient match {
                case Left(_) =>
                  log.info("Patient with ID: {} not found [GET].", msg.id);
                  replyTo ! Left(ErrorResponse(404, s"Patient with ID: ${msg.id} not found [GET]."))
                case Right(patient) =>
                  log.info("Patient with ID: {} found [GET].", patient.id)
                  replyTo ! Right(patient)
              }
            }
            }
          case Failure(fail) =>
            log.warning(s"Could not read a patient with ID: ${msg.id}. Exception with MESSAGE: ${fail.getMessage} occurred during this request. [GET]")
            replyTo ! Left(ErrorResponse(500, fail.getMessage))
        }

      case PutPatient(patient) =>
        val replyTo = sender()
        val cmd = esClient.execute {
          update(patient.id).in(index / mappingType).doc(patient)
        }

        cmd.onComplete {
          case Success(either) => either match {
            case Left(_) =>
              log.warning("Patient with ID: {} not found [UPDATE].", patient.id)
              replyTo ! Left(ErrorResponse(404, s"Patient with ID: ${patient.id} not found [UPDATE]."))
            case Right(_) =>
              log.info("Patient with ID: {} updated.", patient.id)
              replyTo ! Right(SuccessfulResponse(200, s"Patient with ID: ${patient.id} updated."))
          }
          case Failure(_) =>
            log.warning(s"Could not update a patient with ID: ${patient.id}. Internal Server Error.")
            replyTo ! Left(ErrorResponse(500, s"Could not update a patient with ID: ${patient.id}. Internal Server Error."))
        }

      case msg: DeletePatient =>
        val replyTo = sender()
        val cmd = esClient.execute {
          delete(msg.id).from(index / mappingType)
        }

        cmd.onComplete {
          case Success(either) =>
            either.map(e => e.result.result.toString).foreach { res => {
              res match {
                case "deleted" =>
                  log.info("Patient with ID: {} deleted.", msg.id);
                  replyTo ! Right(SuccessfulResponse(200, s"Patient with ID: ${msg.id} deleted."))
                case "not_found" =>
                  log.info("Patient with ID: {} not found [DELETE].", msg.id);
                  replyTo ! Left(ErrorResponse(404, s"Patient with ID: ${msg.id} not found [DELETE]."))
              }
            }
            }
          case Failure(fail) =>
            log.warning(s"Could not delete a patient with ID: ${msg.id}. Exception with MESSAGE: ${fail.getMessage} occurred during this request. [DELETE]")
            replyTo ! Left(ErrorResponse(500, fail.getMessage))
        }
    }
  }
