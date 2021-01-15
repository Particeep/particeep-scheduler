package services

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import domain.JobTag.JobId
import domain._
import mockws._
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.test.Helpers._
import play.api.{ Configuration, Logging }
import repository.job._
import repository._
import services.job.JobOperation
import services.job_manager.RunningJobOperation
import test.ZioTestHelper

import zio._
import repository.execution.ExecutionRepositoryInMemory

class JobOperationTest extends PlaySpec with ZioTestHelper with MockWSHelpers with BeforeAndAfterAll with Logging {

  val base_job = Job(
    id          = JobId.from("1"),
    name        = "Test Job",
    start_time  = "",
    frequency   = Frequency("ONCE"),
    credentials = None,
    url         = new Url("https://api.particeep.com/ping"),
    method      = HttpMethod.GET
  )

  val config = Configuration.from(Map(
    "job.request.timeout_ms" -> 500L
  ))

  val clock = zio.clock.Clock.Service.live

  val scheduler_service   = new SchedulerService.Live(Has(clock))
  val running_job_service = RunningJobOperation

  "JobOperationTest" should {

    "run a job in success" in {

      val ws: WSClient                   = MockWS {
        case (GET, "https://api.particeep.com/ping") => Action(Ok("Ok"))
      }
      val exec_repo: ExecutionRepository = new ExecutionRepositoryInMemory()
      val job_repo: JobRepository        = new JobRepositoryInMemory()

      val job                = base_job
      val service_under_test = new JobOperation(
        ws,
        config,
        clock,
        exec_repo,
        scheduler_service,
        running_job_service,
        job_repo
      )
      val result_to_run      = service_under_test.runOnce(job)
      val result             = runInSuccess(result_to_run)

      result.job_id mustBe JobId.from("1")
      result.status mustBe 200
      result.response mustBe "Ok"

      val db_result = Runtime.default.unsafeRun(
        exec_repo.search(ExecutionSearchCriteria(), TableSearch()).map(_.data.headOption)
      )
      db_result.map(_.job_id) mustBe Some(JobId.from("1"))
      db_result.map(_.status) mustBe Some(200)
      db_result.map(_.response) mustBe Some("Ok")
    }

    "run a job in error" in {
      val ws: WSClient                   = MockWS {
        case (GET, "https://api.particeep.com/ping") => Action(Status(500)("Wrong request"))
      }
      val exec_repo: ExecutionRepository = new ExecutionRepositoryInMemory()
      val job_repo: JobRepository        = new JobRepositoryInMemory()

      val service_under_test = new JobOperation(
        ws,
        config,
        clock,
        exec_repo,
        scheduler_service,
        running_job_service,
        job_repo
      )

      val job           = base_job
      val result_to_run = service_under_test.runOnce(job)
      val result        = runInSuccess(result_to_run)

      result.job_id mustBe JobId.from("1")
      result.status mustBe 500
      result.response mustBe "Wrong request"

      val db_result = Runtime.default.unsafeRun(
        exec_repo.search(ExecutionSearchCriteria(), TableSearch()).map(_.data.headOption)
      )
      db_result.map(_.job_id) mustBe Some(JobId.from("1"))
      db_result.map(_.status) mustBe Some(500)
      db_result.map(_.response) mustBe Some("Wrong request")
    }
  }

  override def afterAll(): Unit = {
    shutdownHelpers()
  }
}
