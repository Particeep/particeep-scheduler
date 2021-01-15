package services

import domain.{ Execution, Job }

import repository.ExecutionRepository
import repository.JobRepository

import services.job.JobOperation

import effect.Fail
import play.api.Configuration
import play.api.libs.ws.WSClient

import zio._
import zio.clock.Clock

trait JobService {
  def runOnce(job:         Job): ZIO[Any, Fail, Execution]
  def runPeriodically(job: Job): ZIO[Any, Fail, Fiber.Runtime[Fail, Long]]
  def startAllJobs(): ZIO[Any, Fail, List[Fiber.Runtime[Fail, Long]]]
}

object JobService {

  val live: ZLayer[
    Has[WSClient] with Has[Configuration] with Clock with Has[ExecutionRepository] with Has[SchedulerService] with Has[
      RunningJobService
    ] with Has[JobRepository],
    Nothing,
    Has[JobService]
  ] = {
    ZLayer.fromServices[
      WSClient,
      Configuration,
      Clock.Service,
      ExecutionRepository,
      SchedulerService,
      RunningJobService,
      JobRepository,
      JobService
    ](
      new JobOperation(_, _, _, _, _, _, _)
    )
  }

  def runOnce(job: Job): ZIO[Clock with Has[JobService], Fail, Execution] = {
    ZIO.accessM(_.get[JobService].runOnce(job))
  }
  def runPeriodically(job: Job): ZIO[Has[JobService], Fail, Fiber.Runtime[Fail, Long]] = {
    ZIO.accessM(_.get[JobService].runPeriodically(job))
  }
  def startAllJobs(): ZIO[Has[JobService], Fail, List[Fiber.Runtime[Fail, Long]]] = {
    ZIO.accessM(_.get[JobService].startAllJobs())
  }
}
