package Lab11

import java.io.{File, FileOutputStream, FilenameFilter}
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.{Files, Paths}
import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{AmazonS3Exception, GetObjectRequest, ListObjectsRequest, ListObjectsV2Request, ObjectListing, ObjectMetadata, PutObjectRequest, PutObjectResult, S3Object, S3ObjectInputStream, S3ObjectSummary}
import Lab11.model.{ErrorResponse, SuccessfulResponse}
import akka.event.jul.Logger
import akka.stream.alpakka.s3
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import akka.stream.alpakka.s3.scaladsl
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl
import akka.util.ByteString
import com.amazonaws.services.s3.transfer.{Download, MultipleFileDownload, TransferManager, TransferManagerBuilder, Upload}
import com.amazonaws.util.IOUtils
import com.amazonaws.AmazonServiceException

import scala.io.Source
import scala.util.{Failure, Success, Try}

object ManagerActor {
  private val path1 = "./src/main/resources/s3"
  private val path2 = "./src/main/resources/in"
  private val path3 = "./src/main/resources/out"

  val credentials = new BasicAWSCredentials("AKIARTZMNRRGSIE6RKUA", "QzgPck0gRJ5yB+fr13W4mRGOCpyezXgq88albzae")

  def downloadFile(client: AmazonS3, bucketName: String, objectKey: String, fullPath: String): Unit = {
    val file = new File(fullPath)
    file.getParentFile().mkdirs()
    client.getObject(new GetObjectRequest(bucketName, objectKey), file)
  }

  def uploadFile2(client: AmazonS3, bucketName: String, key: String, file: File, showProgress: Boolean = false): PutObjectResult = {
    val metadata: ObjectMetadata = new ObjectMetadata
    metadata.setContentEncoding("utf-8")
    val putObjectRequest: PutObjectRequest = new PutObjectRequest(bucketName, key, file)
    putObjectRequest.setMetadata(metadata)
    client.putObject(putObjectRequest)
  }

  case class GetFile(fileName: String)

  case class UploadFile(fileName: String)

  case object UploadAllFiles

  case object DownloadAllFiles

  def props(client: AmazonS3, bucketName: String) = Props(new ManagerActor(client, bucketName))

}

class ManagerActor(client: AmazonS3, bucketName: String) extends Actor with ActorLogging {

  import ManagerActor._

  override def receive: Receive = {
    case GetFile(fileName) =>
      val objectKey = fileName

      if (!client.doesObjectExist(bucketName, objectKey)) {
        sender() ! Left(ErrorResponse(404, s"File ${fileName} does not exist"))
        log.info(s"Failed to download file ${fileName}. Something went wrong")
      }
      else {
        val fullPath = s"${path1}/${fileName}"
        Try(downloadFile(client, bucketName, objectKey, fullPath)) match {
          case Success(_) =>
            sender() ! Right(SuccessfulResponse(200, s"File ${fileName} downloaded successfully"))
            log.info("File {} downloaded from AWS S3", fileName)
          case Failure(exception) =>
            sender() ! Left(ErrorResponse(500, s"Internal error occurred while downloading a file ${fileName}"))
            log.info(s"Failed to download ${fileName}. Error message: ${exception.getMessage}")
        }
      }


    case UploadFile(fileName) =>
      val objectKey = fileName
      if (client.doesObjectExist(bucketName, objectKey)) {
        sender() ! Left(ErrorResponse(409, s"File ${fileName} already exists in this bucket"))
        log.info(s"Failed to upload file ${fileName}. It already exists")
      } else {
        val filePath = s"${path1}/${fileName}"
        val file: File = new File(filePath)
        Try(uploadFile2(client, bucketName, objectKey, file)) match {
          case Success(_) =>
            sender() ! Right(SuccessfulResponse(201, s"File ${fileName} downloaded successfully"))
            log.info("File {} uploaded to AWS S3", fileName)
          case Failure(exception) =>
            sender() ! Left(ErrorResponse(500, s"Internal error occurred while uploading a file ${fileName}"))
            log.info(s"Failed to upload ${fileName}. Error message: ${exception.getMessage}")
        }
      }

    case UploadAllFiles =>
      val mainDirectory: File = new File(path3)
      uploadFileOrDirectory(bucketName, mainDirectory.toString, mainDirectory)

      def uploadFileOrDirectory(bucketName: String, dest: String, file: File): Unit = {
        var path = Paths.get(file.getPath)
        if (file.isDirectory)
          file.listFiles.toSeq.foreach { file2 => uploadFileOrDirectory(bucketName, file2.getPath, file2) }
        else {
          uploadFile2(client, bucketName, (path.subpath(5, path.getNameCount).toString.replace('\\', '/')), file)
        }
      }

    case DownloadAllFiles =>
      val objects = client.listObjects(new ListObjectsRequest().withBucketName(bucketName))
      objects.getObjectSummaries.forEach(objectSummary => downloadFile(client, bucketName, objectSummary.getKey, path2 + "/" + objectSummary.getKey))
  }
}
