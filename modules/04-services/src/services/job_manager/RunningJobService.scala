package services
import domain.RunningJobTag.RunningJobId
import domain._

import repository.ExecutionRepository

import services.job_manager.RunningJobOperation

import effect.Fail
import play.api.Configuration
import play.api.libs.ws.WSClient

import zio._
import zio.clock.Clock

trait RunningJobService {

  def add(job: RunningJob): ZIO[Any, Fail, List[RunningJob]]

  def stop(id: RunningJobId): ZIO[Any, Fail, RunningJob]

  def list(): IO[Nothing, List[RunningJob]]

  def clean(status: Fiber.Status): ZIO[Any, Nothing, Unit]
}

object RunningJobService {

  val live: ZLayer[
    Has[WSClient] with Has[Configuration] with Clock with Has[ExecutionRepository],
    Nothing,
    Has[RunningJobService]
  ] = {
    ZLayer.fromFunction(_ => RunningJobOperation)
  }

  def add(job: RunningJob): ZIO[Has[RunningJobService], Fail, List[RunningJob]] = {
    ZIO.accessM(_.get[RunningJobService].add(job))
  }

  def stop(id: RunningJobId): ZIO[Has[RunningJobService], Fail, RunningJob] = {
    ZIO.accessM(_.get[RunningJobService].stop(id))
  }

  def list(): ZIO[Has[RunningJobService], Nothing, List[RunningJob]] = {
    ZIO.accessM(_.get[RunningJobService].list())
  }

  def clean(status: Fiber.Status): ZIO[Has[RunningJobService], Nothing, Unit] = {
    ZIO.accessM(_.get[RunningJobService].clean(status))
  }

  def formatForDislay(jobs: List[RunningJob]): ZIO[Any, Nothing, List[RunningJobDisplay]] = {
    IO.collectAll(jobs.map(_.format()))
  }
}
