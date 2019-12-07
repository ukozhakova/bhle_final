package Lab3.Shapes

sealed trait Shape{
  def sides: Int
  def perimeter: Double
  def area: Double
}

case class Circle(radius: Double) extends Shape{
  override def sides: Int = 0

  override def perimeter: Double = 2*math.Pi*radius

  override def area: Double = math.Pi*radius*radius

}
case class Rectangle(side1: Double, side2: Double) extends Shape with Rectangular{
  override def sides: Int = 4

  override def perimeter: Double = 2*(side1+side2)

  override def area: Double = side2*side1
}
case class Square(side: Double) extends Shape with Rectangular{
  override def sides: Int = 4

  override def perimeter: Double = 4*side

  override def area: Double = side*side

}
sealed trait Rectangular {
  this: Shape=>
}
object Draw  {
  def apply(shape: Shape): Unit = {
    shape match {
      case Circle(radius) => println(s"A circle of radius $radius cm")
      case Rectangle(width,height) => println(s"A rectangle of width $width cm and height $height cm")
      case Square(length) => println(s"A square of length: $length cm")
    }
  }
}

object Shape extends App{
  Draw(Circle(4))
  Draw(Rectangle(3,4))
}

