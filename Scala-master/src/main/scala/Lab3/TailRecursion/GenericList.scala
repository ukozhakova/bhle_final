package Lab3.TailRecursion

sealed trait GenericList[A] {
  def length(): Int = {
    def nestedLength(node: GenericList[A], n: Int): Int = node match {
      case GenericNode(_, tail) => nestedLength(tail, n + 1)
      case GenericEnd() => n
    }

    nestedLength(this, 0)
  }

  def map[B](f: A => B): GenericList[B] = {
    def nestedMap(node: GenericList[A]): GenericList[B]=node match {
      case GenericNode(head, tail) => GenericNode(f(head), nestedMap(tail))
      case GenericEnd() => GenericEnd()
  }
    nestedMap(this)
  }

}
  case class GenericEnd[A]() extends GenericList[A]{
  }
  case class GenericNode[A](head: A, tail: GenericList[A]) extends GenericList[A]{

  }
object GenList extends App{
  val genericList: GenericList[Int] = GenericNode(1, GenericNode(2, GenericNode(3, GenericEnd())))
  println(genericList.map(x=>x+8))
  println(genericList.map(x=>x.toString()))
}
