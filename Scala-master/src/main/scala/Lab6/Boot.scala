package Lab6

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import Lab6.actor.{MovieManager, TestBot}
import Lab6.model.{ErrorResponse, Movie, Response, SuccessfulResponse}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

object Boot extends App with SprayJsonSerializer {

  implicit val system: ActorSystem = ActorSystem("movie-service")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  implicit val timeout: Timeout = Timeout(10.seconds)

  val movieManager = system.actorOf(MovieManager.props(), "movie-manager")

  val route =
    path("healthcheck") {
      get {
       complete(StatusCodes.Accepted ->"hello")
      }
    }~
      pathPrefix("kbtu-cinema") {
        path("movie" / Segment) { movieId =>
          get {
            val res= (movieManager ? MovieManager.ReadMovie(movieId)).mapTo[Either[ErrorResponse, Movie]]
            onSuccess(res) {
              case Left(error) => complete(error.status, error)
              case Right(movie) => complete(200, movie)

            }
          }
        } ~
          path("movie") {
            post {
              entity(as[Movie]) { movie =>
                val res = (movieManager ? MovieManager.CreateMovie(movie)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
                onSuccess(res) {
                  case Left(error) =>complete(error.status, error)
                  case Right(success) =>complete(201, success)
                }
              }
            }
          } ~
        path("movie"){
          put{
            entity(as[Movie]) { movie =>
              val res=  (movieManager ? MovieManager.UpdateMovie(movie)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
              onSuccess(res){
                case Left(error) =>complete(error.status, error)
                case Right(success) =>complete(success.status, success)
              }
            }
          }
        }~
        path("movie"/Segment){movieID =>
          delete {
              val res= (movieManager ? MovieManager.DeleteMovie(movieID)).mapTo[Either[ErrorResponse, SuccessfulResponse]]
            onSuccess(res){
              case Left(error) =>complete(error.status, error)
              case Right(success) =>complete(success.status, success)
            }
          }
        }
      }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8000)

}