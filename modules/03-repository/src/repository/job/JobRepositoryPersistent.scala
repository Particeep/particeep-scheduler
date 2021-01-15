package repository.job

import domain.JobTag.JobId
import domain._

import repository.JobRepository
import repository.models.dao._

import effect.Fail
import effect.zio.slick.zioslick._
import effect.zio.sorus.ZioSorus
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider

import zio._

class JobRepositoryPersistent(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends JobRepository
    with HasDatabaseConfigProvider[EnhancedPostgresDriver]
    with JobDao
    with ZioSorus
    with ZioSlick
    with SlickUtils {

  import profile.api._

  val tables        = TableQuery[JobTable]
  lazy val db_layer = SlickDatabase.Live(db)

  def load(id: JobId): ZIO[Any, Fail, Job] = {
    val query = tables.filter(_.id === id)
    for {
      maybe_job <- query.result.headOption ?| "Error on db request"
      job       <- maybe_job               ?| s"No job with id $id"
    } yield {
      job
    }
  }

  def store(job: Job): ZIO[Any, Fail, Int] = {
    tables += job
  }

  def search(criteria: JobSearchCriteria, tableSearch: TableSearch): ZIO[Any, Fail, SearchWithTotalSize[Job]] = {
    val q = tables
      .filterOpt(criteria.method)(_.method === _)
      .filterOpt(tableSearch.global_search.filter(_.nonEmpty))((t, global_search) => {
        (t.url.asColumnOf[String].? ilike s"%${global_search}%") ||
          (t.name.? ilike s"%${global_search}%")
      })
      .sort_it(tableSearch)

    toPaginate(q, tableSearch.offset, tableSearch.limit)
  }
}
