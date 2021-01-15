package repository.models.dao

import domain.TableSearch

import play.api.db.slick.HasDatabaseConfig
import slick.lifted.ColumnOrdered

import scala.reflect.runtime.universe.TypeTag

/**
 * usage  :
 *
 *  - extends Sorting
 *  - filter : users.sortDynamic("first_name.desc,id.asc") or query.sort_it(criteria.sort_by, criteria.order_by)
 *
 * inspired by https://www.slideshare.net/skillsmatter/patterns-for-slick-database-applications
 */
trait Sorting { self: HasDatabaseConfig[EnhancedPostgresDriver] =>

  import profile.api._

  /**
   * The dynamic sorting doesn't work in some case. Mainly because of a left join that make column optional
   * The type become [Table, Option[Rep[Table2]]] wich doesn't match the <: Table constraint
   *
   * This sort require a Map define in the Dao.
   * Here is an example
   *
   * implicit val columns: Map[String, ProductAccessSearchTable => Rep[_]] = Map(
   *   "id" -> { t => t.id },
   *   "created_at" -> { t => t.created_at },
   *   "updated_at" -> { t => t.updated_at },
   *   "name" -> { t => t.name },
   *   "tag" -> { t => t.tag }
   * )
   */
  def sort[T, E: TypeTag, C[_]](
    query:          Query[T, E, C],
    table_criteria: TableSearch,
    columns:        Map[String, T => Rep[_]]
  ): Query[T, E, C] = {
    val aux        = columns
      .get(table_criteria.sort_by.getOrElse("created_at"))
      .getOrElse(columns.get("id").get)
    val orderedRep = table_criteria.order_by.map(_.toLowerCase) match {
      case Some("asc") => (t: T) => ColumnOrdered(aux(t), slick.ast.Ordering(slick.ast.Ordering.Asc))
      case _           => (t: T) => ColumnOrdered(aux(t), slick.ast.Ordering(slick.ast.Ordering.Desc))
    }

    query.sortBy(orderedRep)
  }

  implicit class QuerySortingExtensions[T <: Table[_], E: TypeTag, C[_]](val query: Query[T, E, C]) {

    def sort_it(table_criteria: TableSearch)(implicit columns: Map[String, T => Rep[_]]): Query[T, E, C] = {
      sort(query, table_criteria, columns)
    }
  }
}
