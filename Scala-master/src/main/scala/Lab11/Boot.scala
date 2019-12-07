
package Lab11

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.stream.{ActorMaterializer, Materializer}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.slf4j.LoggerFactory
import Lab11.ManagerActor
import Lab11.ManagerActor.{DownloadAllFiles, GetFile, UploadAllFiles, UploadFile}
import Lab11.model.{ErrorResponse, PathModel, SuccessfulResponse}
import akka.http.scaladsl.server.Route
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._


object Boot extends App with SprayJsonSerializer {
  implicit val system: ActorSystem = ActorSystem("file-manager-system")
  implicit val materializer: Materializer = ActorMaterializer()

  implicit val timeout: Timeout = Timeout(10.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

  val clientRegion: Regions = Regions.EU_CENTRAL_1

  val credentials = new BasicAWSCredentials("AKIARTZMNRRGSIE6RKUA", "QzgPck0gRJ5yB+fr13W4mRGOCpyezXgq88albzae")

  val client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withRegion(clientRegion)
    .build()

  val bucketName = "lab11-task2"

  val manager = system.actorOf(ManagerActor.props(client, bucketName))

  createBucket(client, bucketName)

  val route: Route =
    concat(
      path("file") {
        concat(
          get {
            parameters('filename.as[String]) { fileName =>
              handle((manager ? ManagerActor.GetFile(fileName)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
              }
          },
          post {
            entity(as[PathModel]) { pathModel =>
              handle((manager ? ManagerActor.UploadFile(pathModel.path)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
          }
      }
    )
      },
      pathPrefix("task2") {
        concat(
          path("in") {
            get {
              complete {
                manager ! DownloadAllFiles
                "in completed"
              }
            }
          },
          path("out") {
            get {
              complete {
                manager ! UploadAllFiles
                "out completed"
              }
            }
          }
        )
      }
    )

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)


  def createBucket(s3client: AmazonS3, bucket: String): Unit = {
    if (!s3client.doesBucketExistV2(bucket)) {
      s3client.createBucket(bucket)
      log.info(s"Bucket with name: $bucket created")
    } else {
      log.info(s"Bucket $bucket already exists")
    }
  }

  def handle(output: Future[Either[ErrorResponse, SuccessfulResponse]]) = {
    onSuccess(output) {
      case Left(error) => {
        complete(error.status, error)
      }
      case Right(successful) => {
        complete(successful.status, successful)
      }
    }
  }
}
