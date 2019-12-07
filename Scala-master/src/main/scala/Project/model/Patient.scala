package Project.model

import Project.model.Gender.Gender

case class Patient(id: String, firstName: String, lastName: String, diagnosis: String, gender: Gender, doctor: Doctor) {

}
