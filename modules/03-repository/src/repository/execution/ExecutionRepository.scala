package repository

import domain.ExecutionTag.ExecutionId
import domain._

import repository.execution.{ ExecutionRepositoryInMemory, ExecutionRepositoryPersistent }

import effect.Fail
import play.api.db.slick.DatabaseConfigProvider

import zio._

trait ExecutionRepository {
  def load(id:         ExecutionId): ZIO[Any, Fail, Execution]
  def store(execution: Execution): ZIO[Any, Fail, Int]
  def search(
    criteria:          ExecutionSearchCriteria,
    tableSearch:       TableSearch
  ): ZIO[Any, Fail, SearchWithTotalSize[Execution]]
}

object ExecutionRepository {

  val in_memory: ZLayer[Any, Nothing, Has[ExecutionRepository]] = {
    ZLayer.succeed(new ExecutionRepositoryInMemory())
  }

  val in_db: ZLayer[Has[DatabaseConfigProvider], Nothing, Has[ExecutionRepository]] = {
    ZLayer.fromService[
      DatabaseConfigProvider,
      ExecutionRepository
    ](
      new ExecutionRepositoryPersistent(_)
    )
  }

  def search(criteria: ExecutionSearchCriteria, tableSearch: TableSearch) = {
    ZIO.accessM[Has[ExecutionRepository]](_.get.search(criteria, tableSearch))
  }
}
