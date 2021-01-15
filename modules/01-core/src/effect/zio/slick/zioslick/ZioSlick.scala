package effect.zio.slick.zioslick

import effect.Fail
import effect.zio.sorus.ZioSorus
import slick.dbio.DBIO

import zio._

trait ZioSlick extends ZioSorus {

  def db_layer: SlickDatabase

  def DBIO2ZioSlick[T](dbio: DBIO[T]): ZioSlickEffect[T] = ZioSlickEffect[T](dbio)

  implicit def dbio2Zio[T](dbio: DBIO[T]): ZIO[Any, Fail, T] = {
    DBIO2ZioSlick(dbio).provide(Has(db_layer))
  }

  implicit def dbio2ZioFail[T](dbio: DBIO[T]): ZioFail[Any, T] = {
    val effect = DBIO2ZioSlick(dbio).provide(Has(db_layer))
    new ZioFail(effect)
  }
}
