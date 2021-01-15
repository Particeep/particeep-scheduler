package services.job_manager
import domain.RunningJobTag.RunningJobId
import domain._

import services.RunningJobService

import effect.Fail
import effect.zio.sorus.ZioSorus

import zio._

object RunningJobOperation extends RunningJobService with ZioSorus {

  private[this] lazy val running_jobs = Runtime.default.unsafeRun(Ref.make(List[RunningJob]()))

  override def add(job: RunningJob): ZIO[Any, Fail, List[RunningJob]] = {
    for {
      _        <- running_jobs.update(_ ++ List(job))
      all_jobs <- running_jobs.get
    } yield {
      all_jobs
    }
  }

  override def stop(id: RunningJobId): ZIO[Any, Fail, RunningJob] = {
    for {
      running_jobs_list <- running_jobs.get
      current_job       <-
        running_jobs_list.filter(_.id == id).headOption ?| s"No running job with id $id"
      _                 <- current_job.fiber.interrupt
    } yield {
      current_job
    }
  }

  override def list(): IO[Nothing, List[RunningJob]] = running_jobs.get

  def clean(status_to_clean: Fiber.Status): ZIO[Any, Nothing, Unit] = {
    job_with_status()
      .map { list =>
        list.filter(_._2 != status_to_clean).map(_._1)
      }
      .flatMap(running_jobs.set)
  }

  private[this] def job_with_status(): ZIO[Any, Nothing, List[(RunningJob, Fiber.Status)]] = {
    val nested_result = running_jobs.get.map { running_jobs_list =>
      running_jobs_list.map { running_job =>
        for {
          status <- running_job.fiber.status
        } yield {
          (running_job, status)
        }
      }
    }

    nested_result.flatMap(list => ZIO.collect(list)(x => x))
  }
}
