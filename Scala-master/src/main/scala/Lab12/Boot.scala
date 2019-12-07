package Lab12
import java.io.InputStream
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import akka.stream.scaladsl.StreamConverters
import akka.util.Timeout
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.{ContentType, HttpEntity}
import akka.http.scaladsl.model.MediaTypes.`image/jpeg`
import Lab12.models.{ErrorResponse, PhotoResponse, SuccessfulResponse}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object Boot extends App with SprayJsonSerializer {

  implicit val system: ActorSystem = ActorSystem("photo-service")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val timeout: Timeout = Timeout(10.seconds)

  val log = LoggerFactory.getLogger("Boot")

  val clientRegion: Regions = Regions.EU_CENTRAL_1

  val credentials = new BasicAWSCredentials("AKIARTZMNRRGSIE6RKUA", "QzgPck0gRJ5yB+fr13W4mRGOCpyezXgq88albzae")

  val client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withRegion(clientRegion)
    .build()

  val bucketName = "lab12-photoservice"

  createBucket(client, bucketName)

  val route =
    path("healthcheck") {
      get {
        complete {
          "OK"
        }
      }
    } ~
      pathPrefix("photos") {
        concat(
          pathEndOrSingleSlash {
            concat(
              get {
                complete {
                  "all photos"
                }
              },
              post {
                extractRequestContext { ctx =>
                  fileUpload("file") {
                    case (metadata, byteSource) =>
                      // Convert Source[ByteString, Any] => InputStream
                      val inputStream: InputStream = byteSource.runWith(
                        StreamConverters.asInputStream(FiniteDuration(3, TimeUnit.SECONDS))
                      )

                      log.info(s"Content type: ${metadata.contentType}")
                      log.info(s"Field name: ${metadata.fieldName}")
                      log.info(s"File name: ${metadata.fileName}")

                      val worker = system.actorOf(PhotoActor.props(client, bucketName))
                      handle((worker ? PhotoActor.UploadPhoto(inputStream, metadata.fileName, metadata.contentType.toString())).mapTo[Either[ErrorResponse, SuccessfulResponse]])

                  }
                }
              }
            )
          },
          path(Segment) { photoName => // get by id
            concat(
              get {
                val worker = system.actorOf(PhotoActor.props(client, bucketName))
                val future = (worker ? PhotoActor.GetPhoto(photoName)).mapTo[Either[ErrorResponse, PhotoResponse]]

                onSuccess(future) {
                  case Left(error) => complete(error)
                  case Right(photo) => complete(photo.status, HttpEntity(ContentType(`image/jpeg`), photo.message))
                }
              },
              delete {
                val worker = system.actorOf(PhotoActor.props(client, bucketName))
                handle((worker ? PhotoActor.DeletePhoto(photoName)).mapTo[Either[ErrorResponse, SuccessfulResponse]])
              }
            )
          }
        )
      }


  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8082)

  def createBucket(s3client: AmazonS3, bucket: String): Unit = {
    if (!s3client.doesBucketExistV2(bucket)) {
      s3client.createBucket(bucket)
      log.info(s"Bucket with name: $bucket created")
    } else {
      log.info(s"Bucket $bucket already exists")
      s3client.listBuckets().asScala.foreach(b => log.info(s"Bucket: ${b.getName}"))
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
