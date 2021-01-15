package utils

import java.security.SecureRandom

import scala.util.Try

import com.google.common.io.BaseEncoding

object StringUtils {

  def generateUuid(): String = {
    java.util.UUID.randomUUID().toString
  }

  /**
   * Elegant random strign generation in Scala -> http://www.bindschaedler.com/2012/04/07/elegant-random-string-generation-in-scala/
   */
  //Random Generator
  private[this] val random = new SecureRandom()

  // Generate a random string of length n from the given alphabet
  private[this] def randomString(alphabet: String)(n: Int): String = {
    LazyList.continually(random.nextInt(alphabet.size)).map(alphabet).take(n).mkString
  }

  // Generate a random alphabnumeric string of length n
  def randomAlphanumericString(n: Int): String = {
    randomString("abcdefghijklmnopqrstuvwxyz0123456789")(n)
  }

  def randomNumericString(n: Int): String = {
    randomString("0123456789")(n)
  }

  def safeToInt(s: String): Option[Int] = Try {
    s.toInt
  }.toOption

  def toBase64(data: String): String = BaseEncoding.base64().encode(data.getBytes("utf-8"))

  /**
   * String validation via regex is dangerous, cf.
   *
   * Regular expressions (regexs) are frequently subject to Denial of Service (DOS) attacks (called ReDOS).
   * This is due to the fact that regex engines may take a large amount of time when analyzing certain strings, depending on how the regex is defined.
   * For example, for the regex: ^(a+)+$, the input "aaaaaaaaaaaaaaaaX" will cause the regex engine to analyze 65536 different paths.[1] Example taken from OWASP references
   *
   * Therefore, it is possible that a single request may cause a large amount of computation on the server side.
   * The problem with this regex, and others like it, is that there are two different ways the same input character can be accepted
   * by the Regex due to the + (or a *) inside the parenthesis, and the + (or a *) outside the parenthesis.
   * The way this is written, either + could consume the character 'a'. To fix this, the regex should be rewritten to
   * eliminate the ambiguity. For example, this could simply be rewritten as: ^a+$, which is presumably what the author
   * meant anyway (any number of a's). Assuming that's what the original regex meant, this new regex can be evaluated quickly,
   * and is not subject to ReDOS.
   *
   * use OWASP regex to be safe : https://www.owasp.org/index.php/OWASP_Validation_Regex_Repository
   */
  private[this] val emailRegex = """^[a-zA-Z0-9+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$""".r
  private[this] val urlRegex   =
    """^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)([).!';/?:,][[:blank:|:blank:]])?$""".r
  private[this] val uuidRegex  = "(^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$)".r
  private[this] val ipRegex    =
    """^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""".r

  def isEmail(e: String): Boolean = Try {
    !e.trim.isEmpty && emailRegex.findFirstMatchIn(e).isDefined
  }.getOrElse(false)

  def isUrl(e: String): Boolean = Try {
    !e.trim.isEmpty && urlRegex.findFirstMatchIn(e).isDefined
  }.getOrElse(false)

  def isUuid(maybeUuid: String): Boolean = Try {
    !maybeUuid.trim.isEmpty && uuidRegex.findFirstMatchIn(maybeUuid).isDefined
  }.getOrElse(false)

  def isIp(maybeIp: String): Boolean = Try {
    !maybeIp.trim.isEmpty && ipRegex.findFirstMatchIn(maybeIp).isDefined
  }.getOrElse(false)

}
