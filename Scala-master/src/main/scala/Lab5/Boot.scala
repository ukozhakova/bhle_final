package Lab5
import akka.actor.ActorSystem;
import Lab5.actor._
import Lab5.model._
object Boot extends App{
  val system = ActorSystem("movie-service")
  val movieManager = system.actorOf(MovieManager.props(), "movie-manager")
  val testBot = system.actorOf(TestBot.props(movieManager), "test-bot")

  testBot ! TestBot.TestCreate
  testBot ! TestBot.TestConflict
  testBot ! TestBot.TestNotFound
  testBot ! TestBot.TestUpdate
  testBot ! TestBot.TestRead
  testBot ! TestBot.TestDelete


}
