package services

import effect.Fail
import play.api.Logging

import zio._
import zio.clock._

trait SchedulerService {

  def scheduleAsDeamon[A](
    effect: ZIO[Any, Fail, A],
    policy: Schedule[Any, Any, Long]
  ): ZIO[Any, Nothing, Fiber.Runtime[Fail, Long]]
}

object SchedulerService extends Logging {

  class Live(clock: Clock) extends SchedulerService {

    /**
     * https://fr.slideshare.net/jdegoes/zio-schedule-conquering-flakiness-recurrence-with-pure-functional-programming-119932802
     */
    def scheduleAsDeamon[A](
      effect: ZIO[Any, Fail, A],
      policy: Schedule[Any, Any, Long]
    ): ZIO[Any, Nothing, Fiber.Runtime[Fail, Long]] = {
      schedule(effect, policy)
    }

    private[this] def schedule[A](
      effect: ZIO[Any, Fail, A],
      policy: Schedule[Any, Any, Long]
    ): IO[Nothing, Fiber.Runtime[Fail, Long]] = {
      effect
        .schedule(policy)
        .forkDaemon
        .provide(clock)
    }

    private[this] def onScheduleError[A](fail: Fail, maybe_count: Option[Long]): UIO[Long] = {
      val count = maybe_count.getOrElse(0L)

      ZIO
        .effectTotal(logger.error(s"Error running schedule after ${count} count with error ${fail.techMessages()}"))
        .map(_ => count)
    }
  }

  val live: ZLayer[Clock, Nothing, Has[SchedulerService]] = {
    ZLayer.fromFunction[Clock, SchedulerService](env => new Live(env))
  }

  def scheduleAsDeamon[A](
    effect: ZIO[Any, Fail, A],
    policy: Schedule[Any, Any, Long]
  ): ZIO[Has[SchedulerService], Nothing, Fiber.Runtime[Fail, Long]] = {
    ZIO.accessM(_.get.scheduleAsDeamon(effect, policy))
  }
}
