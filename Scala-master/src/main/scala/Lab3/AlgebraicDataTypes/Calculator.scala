package Lab3.AlgebraicDataTypes

sealed trait Calculator
case class SuccessCalculator(result: Int) extends Calculator

case class Failure(result: String) extends Calculator{
}


