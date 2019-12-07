package Lab4


case class Film( name: String,
                 yearOfRelease: Int,
                 imdbRating: Double){
  override def equals(that: Any): Boolean ={
  val film= that.asInstanceOf[Film]
      film.name==this.name && this.yearOfRelease==film.yearOfRelease && this.imdbRating==film.imdbRating
}
}
case class Director( firstName: String,
                     lastName: String,
                     yearOfBirth: Int,
                     films: Seq[Film]){
  override def equals(that: Any): Boolean = {
    val dir = that.asInstanceOf[Director]
   dir.firstName==this.firstName && dir.lastName==this.lastName && dir.yearOfBirth==this.yearOfBirth && dir.yearOfBirth==this.yearOfBirth && dir.films==this.films
  }
}

object Movies extends App {
  val memento = new Film("Memento", 2000, 8.5)
  val darkKnight = new Film("Dark Knight", 2008, 9.0)
 // println(memento.equals(darkKnight))
  val inception = new Film("Inception", 2010, 8.8)
  val highPlainsDrifter = new Film("High Plains Drifter", 1973, 7.7)
  val outlawJoseyWales = new Film("The Outlaw Josey Wales", 1976, 7.9)
  val unforgiven = new Film("Unforgiven", 1992, 8.3)
  val granTorino = new Film("Gran Torino", 2008, 8.2)
  val invictus = new Film("Invictus", 2009, 7.4)
  val predator = new Film("Predator", 1987, 7.9)
  val dieHard = new Film("Die Hard", 1988, 8.3)
  val huntForRedOctober = new Film("The Hunt for Red October", 1990, 7.6)
  val thomasCrownAffair = new Film("The Thomas Crown Affair", 1999, 6.8)
  //val films = Seq(memento, darkKnight, inception, highPlainsDrifter, outlawJoseyWales, unforgiven, granTorino, invictus, predator, dieHard, huntForRedOctober, thomasCrownAffair)
  val eastwood = new Director("Clint", "Eastwood", 1930,
    Seq(highPlainsDrifter, outlawJoseyWales, unforgiven, granTorino, invictus))
  /*val eastwood2 = new Director("Clint", "Eastwood", 1930,
    Seq(highPlainsDrifter, outlawJoseyWales, unforgiven, granTorino, invictus))
*/  //println(eastwood.equals(eastwood2))
  val mcTiernan = new Director("John", "McTiernan", 1951,
    Seq(predator, dieHard, huntForRedOctober, thomasCrownAffair))
  val nolan = new Director("Christopher", "Nolan", 1970,
    Seq(memento, darkKnight, inception))
  val someGuy = new Director("Just", "Some Guy", 1990,
    Seq())
  val directors = Seq(eastwood, mcTiernan, nolan, someGuy)

//Task1

  def DirectorsWIthMoreThanNFilm(N:Int): Seq[Director]={
    directors.filter(d=>d.films.length>N)
  }
  //println(DirectorsWIthMoreThanNFilm(4))

//Task2
  def directorsWereBornBeforeThisYear(year:Int): Seq[Director]={
    directors.filter(d=>d.yearOfBirth<year)
  }
  //println(DirectorsWereBornBeforeThisYear(1952))


  //Task3
def BeforeYearAndMoreThanN(year:Int, N:Int):Seq[Director]={
    directors.filter(d=>d.yearOfBirth<year).filter(d=>d.films.length>N)
  }
//println(BeforeYearAndMoreThanN(1952,3))

  // Task4
  def SortByAge(ascending: Boolean): Seq[Director]={
    directors.sortWith((d1,d2)=>if(ascending) d1.yearOfBirth<d2.yearOfBirth else d1.yearOfBirth>d2.yearOfBirth)
  }
  //println(SortByAge(true))
  //Task5
  def NolanFilms():Seq[String]={
    nolan.films.map(f=>f.name)
  }
  //println(NolanFilms())

  //Task6
  var filmNames:Seq[String]=Seq()
  def Cinephile():Seq[String]={
    for(d<-directors){
      for(f<-d.films) {
         filmNames= filmNames :+ f.name
      }
    }
    filmNames

    // TODO:
    directors.flatMap(d => d.films).map(_.name)
  }
 // println(Cinephile())

  //Task7
  def VintageMcTiernan(): Seq[Film]={
     mcTiernan.films.sortWith((f1,f2)=>f1.yearOfRelease<f2.yearOfRelease)

  }
  //println(VintageMcTiernan()(0).yearOfRelease)

  //Task8
  def HighScoreTable(): Seq[Film]={
    directors.map(d=>d.films.sortWith((f1,f2)=>f1.imdbRating>f2.imdbRating)).flatten
  }
 // println(HighScoreTable())

  //Task9
  // TODO: fold, foldLeft
  def avgScore(): Double={
    var Scores= directors.map(d=>d.films.map(f=>f.imdbRating)).flatten
    var sum: Double=0
    for(s<-Scores){
      sum=sum+ s
    }
    sum/Scores.length
  }
  //println(avgScore())
  //Task10
  def TonightsListings()={
    directors.foreach(director=>director.films.foreach(film=>{
      println(s"Tonight only! ${film.name} by ${director.firstName}")
    }))
  }
  TonightsListings()

  //Task11
  def FromTheArchives(director: Director)={
    directors.find(d=>d.firstName==director.firstName).get.films.minBy(f=>f.yearOfRelease)
  }
  //println(FromTheArchives(nolan))
}
