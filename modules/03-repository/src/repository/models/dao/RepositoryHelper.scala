package repository.models.dao

import domain.SearchWithTotalSize

import effect.Fail
import effect.zio.slick.zioslick._
import play.api.Logging
import play.api.db.slick.HasDatabaseConfig

import zio._

trait RepositoryHelper extends Logging with ZioSlick { self: HasDatabaseConfig[EnhancedPostgresDriver] =>

  import profile.api._

  def toPaginate[A, B](
    q:      Query[A, B, Seq],
    offset: Option[Int] = None,
    limit:  Option[Int] = None
  ): ZIO[Any, Fail, SearchWithTotalSize[B]] = {

    val search_query = q.to[List]
      .drop(offset.getOrElse(0))
      .take(limit.getOrElse(30))
      .result

    val count_query = q.length.result

    for {
      data       <- search_query ?| s"""Error for query ${q.to[List]
                      .drop(offset.getOrElse(0))
                      .take(limit.getOrElse(30)).result.statements}"""
      total_size <- count_query  ?| s"Error for query ${q.length.result.statements}"
    } yield {
      SearchWithTotalSize(total_size, data)
    }
  }
}
