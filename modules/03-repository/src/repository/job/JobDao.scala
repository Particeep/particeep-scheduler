package repository.job

import domain.JobTag.JobId
import domain._

import repository.models.dao._

import play.api.db.slick.HasDatabaseConfig

import java.time.OffsetDateTime

trait JobDao extends DomainMapping {
  self: HasDatabaseConfig[EnhancedPostgresDriver] =>

  import profile.api._

  class JobTable(table_tag: Tag) extends Table[Job](table_tag, "jobs") {

    val id: Rep[JobId] = column[JobId]("id", O.PrimaryKey)
    val created_at     = column[OffsetDateTime]("created_at")
    val name           = column[String]("name")
    val start_time     = column[String]("start_time")
    val frequency      = column[Frequency]("frequency")
    val credentials    = column[Option[HmacCredential]]("credentials")
    val url            = column[Url]("url")
    val method         = column[HttpMethod]("method")
    val tag            = column[List[String]]("tag")

    def * =
      (
        id,
        created_at,
        name,
        start_time,
        frequency,
        credentials,
        url,
        method,
        tag
      ).<>(Job.tupled, Job.unapply _)
  }

  /** This is usefull for typesafe dynamic sorting */
  implicit val columns: Map[String, JobTable => Rep[_]] = Map(
    "id"          -> { t => t.id },
    "created_at"  -> { t => t.created_at },
    "start_time"  -> { t => t.start_time },
    "frequency"   -> { t => t.frequency },
    "credentials" -> { t => t.credentials },
    "url"         -> { t => t.url },
    "method"      -> { t => t.method }
  )
}
