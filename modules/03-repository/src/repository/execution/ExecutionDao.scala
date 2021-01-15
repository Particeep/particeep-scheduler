package repository.execution

import domain.ExecutionTag.ExecutionId
import domain.JobTag.JobId
import domain._

import repository.models.dao._

import play.api.db.slick.HasDatabaseConfig

import java.time.OffsetDateTime

trait ExecutionDao extends DomainMapping {
  self: HasDatabaseConfig[EnhancedPostgresDriver] =>

  import profile.api._

  class ExecutionTable(table_tag: Tag) extends Table[Execution](table_tag, "executions") {

    val id: Rep[ExecutionId] = column[ExecutionId]("id", O.PrimaryKey)
    val job_id               = column[JobId]("job_id")
    val executed_at          = column[OffsetDateTime]("executed_at")
    val status               = column[Int]("status")
    val response             = column[String]("response")

    def * =
      (
        id,
        job_id,
        executed_at,
        status,
        response
      ).<>(Execution.tupled, Execution.unapply _)
  }

  /** This is usefull for typesafe dynamic sorting */
  implicit val columns: Map[String, ExecutionTable => Rep[_]] = Map(
    "id"          -> { t => t.id },
    "job_id"      -> { t => t.job_id },
    "executed_at" -> { t => t.executed_at },
    "status"      -> { t => t.status },
    "response"    -> { t => t.response }
  )
}
