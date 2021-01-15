package repository.models.dao

import domain._

import repository.models.dao.EnhancedPostgresDriver

import pl.iterators.kebs.tagged.slick.SlickSupport
import play.api.Logging
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.json._
import utils.json.JsonErrorFormat

trait DomainMapping extends SlickSupport with JsonParser with Logging {
  self: HasDatabaseConfig[EnhancedPostgresDriver] =>

  import profile.api._

  implicit def credentialsMapper = MappedColumnType.base[HmacCredential, JsValue](
    creds => Json.toJson(creds),
    json =>
      json.validate[HmacCredential] match {
        case JsSuccess(result, _) => result
        case e: JsError           => {
          logger.error(s"Can't parse HmacCredential from json: ${JsonErrorFormat.format(e.errors)}")
          HmacCredential("--", "--")
        }
      }
  )

  implicit def urlMapper    = MappedColumnType.base[Url, String](url => url.underlying, str => new Url(str))
  implicit def urlFrequency = MappedColumnType.base[Frequency, String](f => f.underlying, str => new Frequency(str))

  implicit def httpMethodMapper = MappedColumnType.base[HttpMethod, String](
    x => x.productPrefix,
    d => HttpMethod.parse(d).getOrElse(HttpMethod.GET)
  )
}
