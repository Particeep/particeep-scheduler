package effect.zio.play

import effect.Fail
import play.api.data.Form
import play.api.libs.json.{ JsPath, JsValue, JsonValidationError, Reads }
import play.api.mvc.Result

import zio.{ IO, ZIO }

case class FailWithResult(
  override val message: String,
  val result:           Result,
  override val cause:   Option[Either[Throwable, Fail]] = None
) extends Fail(message, cause) {
  override def withEx(fail: Fail): FailWithResult = new FailWithResult(this.message, result, Some(Right(fail)))
}

/**
 * Convert json validator / Form / etc... to Fail in ZIO effect
 */
trait ZioPlayHelper {

  type JsErrorContent = scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]

  def defaultJsonError2Fail(err: JsErrorContent): Fail = {
    val msg = err.map(x => x._1.toString() + " : " + x._2.mkString(", ")).mkString("\n")
    new Fail(msg)
  }

  protected def jsonValidation[A](jsValue: JsValue)(implicit reads: Reads[A]): ZIO[Any, Fail, A] =
    IO.fromEither(jsValue.validate[A].asEither).mapError(defaultJsonError2Fail)

  implicit def form2Zio[A](form: Form[A]) = new ZioForm(form)

  /**
   * Allow this kind of mapping with result on the left
   *
   * criteria <- eventSearchForm.bindFromRequest ?| (formWithErrors => Ok(views.html.analyzer.index(formWithErrors)))
   */
  implicit def result2Fail(result: Result): FailWithResult = {
    FailWithResult("result from ctrl", result)
  }
}

class ZioForm[T](form: Form[T]) {
  def ?|(failureHandler: (Form[T]) => Fail): ZIO[Any, Fail, T] = {
    ZIO.fromEither(
      form.fold(failureHandler andThen Left.apply, Right.apply)
    )
  }

  def ?|(unit: Unit): ZIO[Any, Fail, T] = ?|(f => default_failure_handler(f))
  private[this] def default_failure_handler[A](formWithErrors: Form[A]): Fail = {
    val msg = formWithErrors.errors.map(_.message).mkString("\n")
    Fail(s"Error in your input data : $msg")
  }
}
