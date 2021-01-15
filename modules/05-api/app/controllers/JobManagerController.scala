package controllers.api

import domain.JobTag.JobId
import domain.RunningJobTag.RunningJobId
import domain._

import repository.JobRepository

import services._
import services.schedule.SchedulerOperation

import javax.inject._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import utils.TimeUtils

import zio._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class JobManagerController extends MainController with JsonParser {

  private[this] lazy val running_jobs = runtime.unsafeRun(Ref.make(List[RunningJob]()))

  def clean() = Action.zio { implicit request =>
    val effect = for {
      _ <- RunningJobService.clean(Fiber.Status.Done)
    } yield {
      Redirect(routes.JobManagerController.list())
    }

    effect.provideLayer(layer)
  }

  def stop(running_job_id: String) = Action.zio { implicit request =>
    val effect = for {
      _ <- RunningJobService.stop(RunningJobId.from(running_job_id))
    } yield {
      Redirect(routes.JobManagerController.list())
    }

    effect.provideLayer(layer)
  }

  def run(job_id: String) = Action.zio { implicit request =>
    val effect = for {
      job          <- JobRepository.load(JobId.from(job_id))
      policy        = Schedule.recurs(1) //SchedulerOperation.computeSchedule(job, TimeUtils.now())
      effect_to_run = JobService.runOnce(job).provideLayer(layer)
      fiber        <- SchedulerService.scheduleAsDeamon(effect_to_run, policy)
      new_job       = RunningJob(
                        job        = job,
                        started_at = TimeUtils.now(),
                        fiber      = fiber
                      )
      _            <- RunningJobService.add(new_job)
    } yield {
      Redirect(routes.JobManagerController.list())
    }

    effect.provideLayer(layer)
  }

  def schedule(job_id: String) = Action.zio { implicit request =>
    val effect = for {
      job          <- JobRepository.load(JobId.from(job_id))
      policy       <- SchedulerOperation.computeSchedule(job, TimeUtils.now())
      effect_to_run = JobService.runOnce(job).provideLayer(layer)
      fiber        <- SchedulerService.scheduleAsDeamon(effect_to_run, policy)
      new_job       = RunningJob(
                        job        = job,
                        started_at = TimeUtils.now(),
                        fiber      = fiber
                      )
      _            <- RunningJobService.add(new_job)
    } yield {
      Redirect(routes.JobManagerController.list())
    }

    effect.provideLayer(layer)
  }

  def list() = Action.zio { implicit request =>
    val effect = for {
      tableSearch <- tableSearchForm.bindFromRequest()    ?| ()
      criteria    <- criteriaSearchForm.bindFromRequest() ?| ()
      job_list    <- JobRepository.search(criteria, tableSearch)
    } yield {
      Ok(Json.toJson(job_list))
    }

    effect.provideLayer(layer)
  }

  def list_running() = Action.zio { implicit request =>
    val effect = for {
      tableSearch      <- tableSearchForm.bindFromRequest() ?| ()
      all_running_jobs <- RunningJobService.list()
      running_job_list <- RunningJobService.formatForDislay(all_running_jobs)
    } yield {
      val result = SearchWithTotalSize(
        data       = running_job_list,
        total_size = running_job_list.length
      )
      Ok(Json.toJson(result))
    }

    effect.provideLayer(layer)
  }

  protected val criteriaSearchForm = Form(
    mapping(
      "method" -> optional(text).transform[Option[HttpMethod]](
        _.flatMap(HttpMethod.parse(_)),
        _.map(_.productPrefix)
      )
    )(JobSearchCriteria.apply)(JobSearchCriteria.unapply)
  )
}
