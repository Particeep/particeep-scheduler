package repository.models.dao

import play.api.db.slick.HasDatabaseConfig
import slick.jdbc.JdbcProfile

trait EntityWithTableLifecycle[J <: JdbcProfile] { self: HasDatabaseConfig[J] =>

  import profile.api._

  def tables: TableQuery[_]

  def createTable() = db.run(profile.buildTableSchemaDescription(tables.shaped.value.asInstanceOf[Table[_]]).create)
  def dropTable()   = db.run(profile.buildTableSchemaDescription(tables.shaped.value.asInstanceOf[Table[_]]).drop)
}
