package repository.execution

import domain.ExecutionTag.ExecutionId
import domain._

import repository.ExecutionRepository
import repository.models.dao._

import effect.Fail
import effect.zio.slick.zioslick._
import effect.zio.sorus.ZioSorus
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider

import zio._

class ExecutionRepositoryPersistent(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends ExecutionRepository
    with HasDatabaseConfigProvider[EnhancedPostgresDriver]
    with ExecutionDao
    with ZioSorus
    with ZioSlick
    with SlickUtils {

  import profile.api._

  val tables        = TableQuery[ExecutionTable]
  lazy val db_layer = SlickDatabase.Live(db)

  def load(id: ExecutionId): ZIO[Any, Fail, Execution] = {
    val query = tables.filter(_.id === id)
    for {
      maybe_execution <- query.result.headOption ?| "Error on db request"
      execution       <- maybe_execution         ?| s"No execution with id $id"
    } yield {
      execution
    }
  }

  def store(execution: Execution): ZIO[Any, Fail, Int] = {
    tables += execution
  }

  def search(
    criteria:    ExecutionSearchCriteria,
    tableSearch: TableSearch
  ): ZIO[Any, Fail, SearchWithTotalSize[Execution]] = {
    val q = tables
      .filterOpt(criteria.runned_after)(_.executed_at.? >= _)
      .filterOpt(criteria.runned_before)(_.executed_at.? <= _)
      .filterOpt(criteria.status)(_.status === _)
      .filterOpt(tableSearch.global_search.filter(_.nonEmpty))((t, global_search) => {
        (t.response.? ilike s"%${global_search}%")
      })
      .sort_it(tableSearch)

    toPaginate(q, tableSearch.offset, tableSearch.limit)
  }
}
