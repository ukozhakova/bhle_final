package Lab3.Shapes.AlgebraicDataTypes

sealed trait Source

class Well extends Source
class Spring extends Source
class Tap extends Source

class Water(size: Int, source: Source, carbonated: Boolean)
