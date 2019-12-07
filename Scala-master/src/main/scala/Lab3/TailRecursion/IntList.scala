package Lab3.TailRecursion

sealed trait IntList{
  def length(): Int = {
    def nestedLength(node: IntList, n: Int): Int = node match {
      case Node(_, tail) => nestedLength(tail, n + 1)
      case End=> n
    }

    nestedLength(this, 0)
  }

  def product(): Int={
    //@scala.annotation.tailrec
     def nestedProduct(node: IntList, n: Int): Int= node match {
      case Node(head, tail)=> nestedProduct(tail, n*head)
      case End=>n
    }
    nestedProduct(this,1)
  }
  def double(): IntList={
    def nestedDouble(node: IntList): IntList= node match {
      case Node(head, tail)=> Node(head*2, nestedDouble(tail))
      case End => End
    }
    nestedDouble(this)
  }
  def map(f:Int => Int): IntList={
     def nestedMap(node: IntList): IntList= node match {
      case Node(head, tail) => Node(f(head), nestedMap(tail))
      case End => End
    }
    nestedMap(this)
  }
}

case object End extends IntList{
}
case class Node(head: Int, tail: IntList) extends IntList{
}

object IntList extends App{
  val intList= Node(1, Node(2, Node(3, Node(4, End))))
  println(intList.length())
  assert(intList.product == 1 * 2 * 3 * 4)
  assert(intList.tail.product == 2 * 3 * 4)
  assert(End.product == 1)
  println(intList.map(x=>x*3))
  println(intList.map(x=>5-x))
 }