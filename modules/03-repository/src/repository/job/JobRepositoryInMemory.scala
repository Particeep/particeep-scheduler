package repository.job

import domain.JobTag.JobId
import domain._

import repository.JobRepository

import effect.Fail
import effect.zio.sorus.ZioSorus

import scala.collection.mutable

import zio._

class JobRepositoryInMemory extends JobRepository with ZioSorus {

  private[this] var in_memory_repository: mutable.Map[JobId, Job] = mutable.Map(
    JobId.from("1") -> Job(
      id          = JobId.from("1"),
      name        = "test job 1",
      start_time  = "now",
      frequency   = Frequency("1 day"),
      credentials = None,
      url         = new Url("https://www.particeep.com"),
      method      = HttpMethod.GET
    ),
    JobId.from("2") -> Job(
      id          = JobId.from("2"),
      name        = "test job 2",
      start_time  = "17:03:00",
      frequency   = Frequency("10 seconds"),
      credentials = None,
      url         = new Url("https://www.particeep.com"),
      method      = HttpMethod.POST
    ),
    JobId.from("3") -> Job(
      id          = JobId.from("3"),
      name        = "test job 3",
      start_time  = "12:00:00",
      frequency   = Frequency("1 days"),
      credentials = None,
      url         = new Url("https://www.particeep.com"),
      method      = HttpMethod.POST
    )
  )

  def load(id: JobId): ZIO[Any, Fail, Job] = {
    in_memory_repository.get(id) ?| "error.job.not_found"
  }

  def store(job: Job): ZIO[Any, Fail, Int] = {
    ZIO.succeed(in_memory_repository.put(job.id, job)).map(_ => 1)
  }

  def search(criteria: JobSearchCriteria, tableSearch: TableSearch): ZIO[Any, Fail, SearchWithTotalSize[Job]] = {
    val result = SearchWithTotalSize(
      total_size = in_memory_repository.values.toList.length,
      data       = in_memory_repository.values.toList
    )

    ZIO.succeed(result)
  }
}
