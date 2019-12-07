package Lab6.actor

import Lab6.model.{ErrorResponse, Movie, SuccessfulResponse}
import akka.actor.{Actor, ActorLogging, Props}

// props
// messages
object MovieManager {

  // Create
  case class CreateMovie(movie: Movie)

  // Read
  case class ReadMovie(id: String)

  // Update
  case class UpdateMovie(movie: Movie)

  // Delete
  case class DeleteMovie(id: String)

  def props() = Props(new MovieManager)
}

// know about existing movies
// can create a movie
// can manage movie
class MovieManager extends Actor with ActorLogging {

  // import companion OBJECT
  import MovieManager._

  var movies: Map[String, Movie] = Map()

  override def receive: Receive = {

    case CreateMovie(movie) =>
      movies.get(movie.id) match {
        case Some(existingMovie) =>
          log.warning(s"Could not create a movie with ID: ${movie.id} because it already exists.")
          sender() ! Left(ErrorResponse(409, s"Movie with ID: ${movie.id} already exists."))

        case None =>
          movies = movies + (movie.id -> movie)
          log.info("Movie with ID: {} created.", movie.id)
          sender() ! Right(SuccessfulResponse(201, s"Movie with ID: ${movie.id} created."))
      }

    case ReadMovie(id) =>
      movies.get(id) match {
        case Some(existingMovie) =>
          log.info(s"Movie with ID: ${id} is:")
          sender() ! Right(existingMovie)

        case None =>
          log.warning("Movie with ID: {} does not exist.", id)
          sender() ! Left(ErrorResponse(404, s"Movie with ID: ${id} not found."))
      }

    case UpdateMovie(movie) =>
      movies.get(movie.id) match {
        case Some(existingMovie) =>
          movies = movies- existingMovie.id+ (movie.id->movie)
          log.info(s"Movie with ID ${movie.id} updated. ")
          sender() ! Right(SuccessfulResponse(200, s"Movie with ID ${movie.id} updated successfully. "))

        case None =>
          log.warning("Movie with ID: {} can not be updated.", movie.id)
          sender() ! Left(ErrorResponse(404, s"Movie with ID: ${movie.id} does not exist."))
      }

    case DeleteMovie(id) =>
      movies.get(id) match {
        case Some(existingMovie) =>
          movies = movies - id
          log.info(s"Movie with ID ${id} deleted. ")
          sender() ! Right(SuccessfulResponse(204, s"Movie with ID ${id} deleted successfully. "))
        case None =>
          log.warning("Movie with ID: {} can not be deleted.", id)
          sender() ! Left(ErrorResponse(404, s"Movie with ID: ${id} does not exist."))
      }
  }

}