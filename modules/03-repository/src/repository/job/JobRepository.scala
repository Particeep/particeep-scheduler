package repository

import domain.JobTag.JobId
import domain._

import repository.job.JobRepositoryInMemory
import repository.job.JobRepositoryPersistent

import effect.Fail
import play.api.db.slick.DatabaseConfigProvider

import zio._

trait JobRepository {
  def load(id:         JobId): ZIO[Any, Fail, Job]
  def store(job:       Job): ZIO[Any, Fail, Int]
  def search(criteria: JobSearchCriteria, tableSearch: TableSearch): ZIO[Any, Fail, SearchWithTotalSize[Job]]
}

object JobRepository {

  val in_memory: ZLayer[Any, Nothing, Has[JobRepository]] = {
    ZLayer.succeed(new JobRepositoryInMemory())
  }

  val in_db: ZLayer[Has[DatabaseConfigProvider], Nothing, Has[JobRepository]] = {
    ZLayer.fromService[
      DatabaseConfigProvider,
      JobRepository
    ](
      new JobRepositoryPersistent(_)
    )
  }

  def search(criteria: JobSearchCriteria, tableSearch: TableSearch) = {
    ZIO.accessM[Has[JobRepository]](_.get.search(criteria, tableSearch))
  }

  def load(id: JobId) = {
    ZIO.accessM[Has[JobRepository]](_.get.load(id))
  }

  def store(job: Job) = {
    ZIO.accessM[Has[JobRepository]](_.get.store(job))
  }
}
