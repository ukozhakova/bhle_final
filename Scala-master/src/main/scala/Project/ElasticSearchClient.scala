package Project
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.http.index.CreateIndexResponse
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object ElasticSearchClient {
  private val port = 9200
  private val host = "localhost"

  val client = HttpClient(ElasticsearchClientUri(host, port))

  def createEsIndex(indexName: String) = {
    val cmd: Future[Either[RequestFailure, RequestSuccess[CreateIndexResponse]]] =
      client.execute { createIndex(indexName) }


    cmd.onComplete {
      case Success(value) =>
        value.foreach {requestSuccess =>
          println(requestSuccess)}

      case Failure(exception) =>
        println(exception.getMessage)
    }
  }
}
