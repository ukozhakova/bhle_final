package Project.serializers
import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import Project.model.Patient
import spray.json._
import scala.util.Try
trait ElasticSerializer extends SprayJsonSerializer {
  // object -> JSON string
  implicit object PatientIndexable extends Indexable[Patient] {
    override def json(patient: Patient): String = patient.toJson.compactPrint
  }

  // JSON string -> object
  // parseJson is a spray method
  implicit object patientHitReader extends HitReader[Patient] {
    override def read(hit: Hit): Either[Throwable, Patient] = {
      Try {
        val jsonAst = hit.sourceAsString.parseJson
        jsonAst.convertTo[Patient]
      }.toEither
    }
  }
}
