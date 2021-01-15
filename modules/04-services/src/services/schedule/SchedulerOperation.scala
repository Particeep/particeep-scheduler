package services.schedule

import domain.Job

import effect.Fail
import effect.zio.sorus.ZioSorus
import utils.StringUtils
import utils.TimeUtils

import java.time.Duration
import java.time.OffsetDateTime

import zio._

/**
 * doc : https://fr.slideshare.net/jdegoes/zio-schedule-conquering-flakiness-recurrence-with-pure-functional-programming-119932802
 */
object SchedulerOperation extends ZioSorus {

  case class Cron(hh: Int, mm: Int, ss: Int)
  case class JobScheduleInput(initial_delay: Duration, frequency: Option[Duration] = None)

  private[this] val time_regex = """([0-2][0-9]):?([0-6][0-9])?:?([0-6][0-9])?""".r
  private[this] def parseCron(input: String): Either[Fail, Cron] = {
    val now = TimeUtils.now()

    input match {
      case "now"                  => Right(Cron(now.getHour(), now.getMinute(), now.getSecond()))
      case time_regex(hh, mm, ss) => Right(
          Cron(
            StringUtils.safeToInt(hh).getOrElse(0),
            StringUtils.safeToInt(mm).getOrElse(0),
            StringUtils.safeToInt(ss).getOrElse(0)
          )
        )
      case _                      => Left(Fail(s"Cron format '${input}' is not allowed"))
    }
  }

  def computeScheduleInput[R, E, A](job: Job, now: OffsetDateTime): ZIO[Any, Fail, JobScheduleInput] = {
    for {
      cron <- parseCron(job.start_time) ?| s"Wrong Cron format for job ${job.id}"
    } yield {
      val base_start_date = now.withHour(cron.hh).withMinute(cron.mm).withSecond(cron.ss)
      val start_date      = if(base_start_date.isBefore(now)) base_start_date.plusDays(1) else base_start_date
      val initial_delay   = Duration.between(now, start_date)
      val frequency       = job.frequency.toDuration().map(f => Duration.ofNanos(f.toNanos))

      JobScheduleInput(initial_delay, frequency)
    }
  }

  def computeSchedule[R, E, A](job: Job, now: OffsetDateTime): ZIO[Any, Fail, Schedule[Any, Any, Long]] = {
    for {
      input <- computeScheduleInput(job, now)
    } yield {

      val initial_delay_schedule = Schedule.recurs(1).addDelay(_ => input.initial_delay)

      input.frequency match {
        case Some(frequency) => initial_delay_schedule andThen Schedule.fixed(frequency)
        case None            => initial_delay_schedule
      }
    }
  }
}
