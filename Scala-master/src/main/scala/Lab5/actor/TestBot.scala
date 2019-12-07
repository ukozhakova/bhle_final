package Lab5.actor
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import Lab5.model.{Director, ErrorResponse, Movie, SuccessfulResponse}

object TestBot {

  case object TestCreate

  case object TestConflict

  case object TestRead

  case object TestNotFound

  case object TestUpdate

  case object TestDelete

  def props(manager: ActorRef) = Props(new TestBot(manager))
}

class TestBot(manager: ActorRef) extends Actor with ActorLogging {
  import TestBot._

  override def receive: Receive = {
    case TestCreate =>
      manager ! MovieManager.CreateMovie(Movie("1", "Joker", Director("dir-1", "Todd", "Philips"), 2019))

    case TestConflict =>
      manager ! MovieManager.CreateMovie(Movie("2", "Charlie's Angels", Director("dir-2", "Ivan", "Ivanov"), 2019))
      manager ! MovieManager.UpdateMovie(Movie("8", "Test Test", Director("dir-2", "Ivan", "Ivanov"), 2019))

    case TestRead =>
      manager ! MovieManager.ReadMovie("1")

    case TestNotFound =>
      manager ! MovieManager.ReadMovie("6")
      manager ! MovieManager.DeleteMovie("7")

    case TestUpdate =>
      manager ! MovieManager.UpdateMovie(Movie("1", "K2", Director("dir-2", "nugu", "nugu"), 2019))

    case TestDelete =>
      manager ! MovieManager.DeleteMovie("2")
    case SuccessfulResponse(status, msg) =>
      log.info("Received Successful Response with status: {} and message: {}", status, msg)

    case ErrorResponse(status, msg) =>
      log.warning("Received Error Response with status: {} and message: {}", status, msg)

    case movie: Movie =>
      log.info("Received movie: [{}]", movie)
  }
}