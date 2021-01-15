package services

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import domain.JobTag.JobId
import domain._
import mockws._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.test.Helpers._
import repository._
import services.job.JobOperation
import services.job_manager.RunningJobOperation
import test.ZioTestHelper

import zio._
import repository.execution.ExecutionRepositoryInMemory
import repository.job.JobRepositoryInMemory

class JobOperationTimeoutTest extends PlaySpec with ZioTestHelper with MockWSHelpers with BeforeAndAfterAll {

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

  "JobOperationTimeoutTest" should {

    "run a job in timeout" in {
      val ws: WSClient                   = MockWS {
        case (GET, "https://api.particeep.com/ping") => {
          Thread.sleep(650L)
          Action(Ok("Ok"))
        }
      }
      val exec_repo: ExecutionRepository = new ExecutionRepositoryInMemory()
      val scheduler_service              = new SchedulerService.Live(Has(clock))
      val running_job_service            = RunningJobOperation
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
      val result_to_run      = service_under_test.runOnce(base_job)
      val result             = run(result_to_run)

      result.isRight mustBe true
      result.map { exec =>
        exec.status mustBe 0
        exec.response mustBe "Timeout for GET request on https://api.particeep.com/ping"
      }
    }
  }

  override def afterAll(): Unit = {
    shutdownHelpers()
  }
}
