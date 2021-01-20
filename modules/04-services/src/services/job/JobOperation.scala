package services.job

import domain._

import repository.ExecutionRepository
import repository.JobRepository

import services.JobService
import services.RunningJobService
import services.SchedulerService
import services.schedule.SchedulerOperation

import effect.Fail
import effect.zio.sorus.ZioSorus
import operation.CredentialOperation
import play.api.Configuration
import play.api.libs.ws.{ EmptyBody, WSClient, WSResponse }
import utils.TimeUtils

import zio.Fiber
import zio.clock.Clock
import zio.{ Has, ZIO }

class JobOperation(
  ws_client:           WSClient,
  config:              Configuration,
  clock:               Clock.Service,
  executions:          ExecutionRepository,
  scheduler_service:   SchedulerService,
  running_job_service: RunningJobService,
  job_repo:            JobRepository
) extends JobService
    with ZioSorus {

  def startAllJobs(): ZIO[Any, Fail, List[Fiber.Runtime[Fail, Long]]] = {
    for {
      all_job <- job_repo.search(JobSearchCriteria(), TableSearch(limit = Some(9999)))
      result  <- ZIO.foreach(all_job.data)(job => runPeriodically(job))
    } yield {
      result
    }
  }

  def runPeriodically(job: Job): ZIO[Any, Fail, Fiber.Runtime[Fail, Long]] = {
    val effect = for {
      policy       <- SchedulerOperation.computeSchedule(job, TimeUtils.now())
      effect_to_run = runOnce(job)
      fiber        <- SchedulerService.scheduleAsDeamon(effect_to_run, policy)
      new_job       = RunningJob(
                        job        = job,
                        started_at = TimeUtils.now(),
                        fiber      = fiber
                      )
      _            <- RunningJobService.add(new_job)
    } yield {
      fiber
    }

    effect
  }.provide(Has(scheduler_service) ++ Has(running_job_service) ++ Has(clock))

  def runOnce(job: Job): ZIO[Any, Fail, Execution] = {
    val http_req = buildHttpRequest(job)
    val timeout  = config.get[Long]("job.request.timeout_ms")

    for {
      execution <- runHttpRequest(ws_client, http_req, timeout).either.map(parseResponse(job))
      _         <- executions.store(execution)
    } yield {
      execution
    }
  }.provide(Has(clock))

  private[this] def runHttpRequest(
    ws:            WSClient,
    req:           HttpRequest,
    timeout_in_ms: Long
  ): ZIO[Clock, Fail, WSResponse] = {
    val ws_req = ws
      .url(req.url.underlying)
      .withHttpHeaders(req.headers.toList: _*)

    val result: ZIO[Any, Fail, WSResponse] = req.method match {
      case HttpMethod.GET  => ws_req.get()           ?| s"Error for GET request on ${req.url}"
      case HttpMethod.POST => ws_req.post(EmptyBody) ?| s"Error for POST request on ${req.url.underlying}"
      case _               => ZIO.fail(Fail(s"${req.method} is not supported"))
    }

    for {
      maybe_response <- result.timeout(zio.duration.Duration.fromMillis(timeout_in_ms))
      response       <- maybe_response ?| s"Timeout for ${req.method} request on ${req.url.underlying}"
    } yield {
      response
    }
  }

  private[this] def buildHttpRequest(job: Job): HttpRequest = HttpRequest(
    method  = job.method,
    url     = job.url,
    headers = buildSecurityHeader(job)
  )

  private[this] def buildSecurityHeader(job: Job): Map[String, String] = {
    CredentialOperation.security_headers(job)
  }

  private[this] def parseResponse(job: Job)(response: Either[Fail, WSResponse]): Execution = {
    Execution(
      job_id      = job.id,
      executed_at = TimeUtils.now(),
      status      = response.fold(
        _ => 0,
        _.status
      ),
      response    = response.fold(
        _.userMessage(),
        _.body
      )
    )
  }
}
