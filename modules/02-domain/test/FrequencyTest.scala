package services

import org.scalatestplus.play.PlaySpec
import domain._
import scala.concurrent.duration._

class FrequencyTest extends PlaySpec {

  "Frequency" should {

    "compute duration based on string" in {

      val r1 = new Frequency("1 days")
      r1.toDuration() mustBe Some(1 days)

      val r2 = new Frequency("5 minutes")
      r2.toDuration() mustBe Some(5 minutes)

      val r3 = new Frequency("once")
      r3.toDuration() mustBe None
    }
  }
}
