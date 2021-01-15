package repository.models.dao

import repository.models.dao.EnhancedPostgresDriver.api._

import pl.iterators.kebs.tagged.slick.SlickSupport
import play.api.db.slick.HasDatabaseConfig
import slick.ast._
import slick.lifted.{ Rep => _ }

class GlobalOptionsColumnExtensionMethods[A: BaseTypedType](val c: Rep[Option[A]]) {

  /**
   * We need to pass options to get the custom values null / notnull
   * Otherwise we get type error, event if A is map on VARCHAR-like type in DB
   */
  def inSetWithNull(values: Seq[A], options: Seq[String])(implicit x: TypedType[A]): Rep[Option[Boolean]] = {
    if(options.contains("null")) {
      c.isEmpty || (c inSet values)
    } else if(options.contains("notnull")) {
      !c.isEmpty.?
    } else {
      c inSet values
    }
  }

  def ===?(value: Option[A]): Rep[Option[Boolean]] =
    value.map { v =>
      c === v
    }.getOrElse {
      c.isEmpty.?
    }
}

class StringOptionsColumnExtensionMethods2(val c: Rep[Option[String]]) {

  /**
   * If the string value startWith ! then do a non equals operation : =!=
   * Otherwise do an equals operation : ===
   */
  def =/=(value: String): Rep[Option[Boolean]] = {
    if(value.startsWith("!")) {
      c =!= value.substring(1)
    } else {
      c === value
    }
  }

  def ===?(value: String): Rep[Option[Boolean]] =
    if(value == "null" || value.contains("null")) {
      c.isEmpty.?
    } else if(value == "not null" || value == "notnull") {
      !c.isEmpty.?
    } else {
      c === value
    }

  def like_null(value: String): Rep[Option[Boolean]] =
    if(value == "null") {
      c.isEmpty.?
    } else {
      c like value
    }

  def ilike(value: String): Rep[Option[Boolean]] = c.toLowerCase like value.toLowerCase

  def inSetWithNull(values: Seq[String]): Rep[Option[Boolean]] = {
    if(values.contains("null")) {
      c.isEmpty || (c inSet values)
    } else if(values.contains("notnull")) {
      !c.isEmpty.?
    } else {
      c inSet values
    }
  }
}

class BooleanOptionsColumnExtensionMethods2(val c: Rep[Option[Boolean]]) {

  def ===?(value: Boolean): Rep[Option[Boolean]] =
    if(value) {
      c === value
    } else {
      c === value || c.isEmpty
    }

}

trait SlickOperator {
  implicit def stringOptionsColumnExtensionMethodsConverter(c:  Rep[Option[String]])  =
    new StringOptionsColumnExtensionMethods2(c)
  implicit def booleanOptionsColumnExtensionMethodsConverter(c: Rep[Option[Boolean]]) =
    new BooleanOptionsColumnExtensionMethods2(c)

  implicit def globalOptionsColumnExtensionMethods[B1: BaseTypedType](c: Rep[Option[B1]])
    : GlobalOptionsColumnExtensionMethods[B1] = new GlobalOptionsColumnExtensionMethods[B1](c)
}

trait PostgresFunction {
  def replace_regex(origin: Rep[String], to_replace: Rep[String], regex: Rep[String], flag: Rep[String]) =
    SimpleFunction[String]("REGEXP_REPLACE").apply(Seq(
      origin,
      to_replace,
      regex,
      flag
    ))
}

trait LTreeOperator {

  /**
   * LTree allow only [A-Za-z0-9_] separated by '.' to specify a path
   *
   * doc : https://www.postgresql.org/docs/current/ltree.html
   */
  def formatForLTree(uuid: String): String = uuid.replaceAll("[^A-Za-z0-9_.]", "_")
}

trait SlickUtils
  extends Sorting
    with LTreeOperator
    with SlickOperator
    with PostgresFunction
    with SlickSupport
    with RepositoryHelper {
  self: HasDatabaseConfig[EnhancedPostgresDriver] =>
}
