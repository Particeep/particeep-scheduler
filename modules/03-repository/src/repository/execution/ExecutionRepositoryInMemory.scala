package repository.execution

import domain.ExecutionTag.ExecutionId
import domain._

import repository.ExecutionRepository

import effect.Fail
import effect.zio.sorus.ZioSorus

import scala.collection.mutable

import zio._

class ExecutionRepositoryInMemory extends ExecutionRepository with ZioSorus {
  private[this] var in_memory_repository: mutable.Map[ExecutionId, Execution] = mutable.Map()

  def load(id: ExecutionId): ZIO[Any, Fail, Execution] = {
    in_memory_repository.get(id) ?| "error.execution.not_found"
  }

  def store(execution: Execution): ZIO[Any, Fail, Int] = {
    ZIO.succeed(in_memory_repository.put(execution.id, execution)).map(_ => 1)
  }

  def search(
    criteria:    ExecutionSearchCriteria,
    tableSearch: TableSearch
  ): ZIO[Any, Fail, SearchWithTotalSize[Execution]] = {
    val result = SearchWithTotalSize(
      total_size = in_memory_repository.values.toList.length,
      data       = in_memory_repository.values.toList
    )

    ZIO.succeed(result)
  }
}
