package Lab12
import java.io.{File, InputStream}

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{DeleteObjectRequest, GetObjectRequest, ObjectMetadata, PutObjectRequest}
import Lab12.models.{ErrorResponse,PhotoResponse, SuccessfulResponse}

object PhotoActor {

  case class UploadPhoto(inputStream: InputStream, fileName: String, contentType: String)

  case class GetPhoto(fileName: String)

  case class DeletePhoto(fileName: String)

  def props(client: AmazonS3, bucketName: String) = Props(new PhotoActor(client, bucketName))
}

class PhotoActor(client: AmazonS3, bucketName: String) extends Actor with ActorLogging {

  import PhotoActor._

  override def receive: Receive = {
    case UploadPhoto(inputStream, fileName, contentType) =>
      val rootSender = sender()
      val objectKey = s"photos/${fileName}"

      if (client.doesObjectExist(bucketName, objectKey)) {
        rootSender ! Left(ErrorResponse(409, s"Photo ${fileName} already exists"))
        log.info(s"Failed to upload photo ${fileName}. It already exists")
      } else {
        val metadata = new ObjectMetadata()
        metadata.setContentType(contentType)
        val request = new PutObjectRequest(bucketName, objectKey, inputStream, metadata)
        val result = client.putObject(request)

        rootSender ! Right(SuccessfulResponse(201, s"File version: ${result.getVersionId}"))
        log.info("Successfully put photo-object with FILENAME: {} to AWS S3", fileName)
      }

      context.stop(self)

    case GetPhoto(fileName) =>
      val rootSender = sender()
      val objectKey = s"photos/${fileName}"

      if (client.doesObjectExist(bucketName, objectKey)) {
        val fullObject = client.getObject(new GetObjectRequest(bucketName, objectKey)).getObjectContent
        val photoInBytes: Array[Byte] = Stream.continually(fullObject.read).takeWhile(_ != -1).map(_.toByte).toArray

        log.info(s"Status: 200. Successfully get ${fileName}")
        rootSender ! Right(PhotoResponse(200, photoInBytes))
      } else {
        log.info(s"Status: 400. Image ${fileName}  doesn't exist")
        rootSender ! Left(ErrorResponse(400, s"Image ${fileName}  doesn't exist"))

      }

      context.stop(self)

    case DeletePhoto(fileName) =>
      val rootSender = sender()
      val objectKey = s"photos/${fileName}"

      if (client.doesObjectExist(bucketName, objectKey)) {
        client.deleteObject(new DeleteObjectRequest(bucketName, objectKey))
        log.info("Successfully deleted {} from AWS S3", fileName)
        rootSender ! Right(SuccessfulResponse(200, s"Successfully deleted  ${fileName} from AWS S3"))
      } else {
        log.info(s"Status: 404. Failed to delete photo ${fileName}. It doesn't exist")
        rootSender ! Left(ErrorResponse(404, s"Failed to delete ${fileName}. It doest't exist"))
      }

      context.stop(self)
  }

}
