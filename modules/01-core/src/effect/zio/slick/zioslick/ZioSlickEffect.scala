package effect.zio.slick.zioslick

import effect.Fail
import slick.dbio.{ DBIO, StreamingDBIO }

import zio.interop.reactivestreams._
import zio.stream.ZStream
import zio.{ Has, ZIO }

object ZioSlickEffect {
  def apply[T](action: DBIO[T]): ZioSlickEffect[T] = {
    (for {
      slick_db <- ZIO.access[Has[SlickDatabase]](_.get)
      res      <- ZIO.fromFuture(_ => slick_db.database.run(action))
    } yield {
      res
    }).mapError(t => Fail("Error in slick layer").withEx(t))
  }

  def fromStreamingDBIO[T](dbio: StreamingDBIO[_, T])
    : ZIO[Has[SlickDatabase], Throwable, ZStream[Any, Throwable, T]] = {
    for {
      slick_db <- ZIO.access[Has[SlickDatabase]](_.get)
      rr       <- ZIO.effect(slick_db.database.stream(dbio))
    } yield {
      rr.toStream(qSize = 16)
    }
  }
}
