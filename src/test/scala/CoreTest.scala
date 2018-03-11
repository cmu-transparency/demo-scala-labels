package edu.cmu.spf.lio.demo

import collection.mutable.Stack
import org.scalatest._

import edu.cmu.spf.lio._

import scala.Predef.String

class CoreSpec extends FlatSpec {
  import Core._
  import HiLow._

  type LBase = HiLow
  type L = BoundedLabel[HiLow]

  val lowPC: L = BoundedLabel[LBase](Low)
  val hiPC: L  = BoundedLabel[LBase](Hi)

  val lowExec = State[L](lowPC, Policy.AllowAll)
  val hiExec = State[L](hiPC, Policy.AllowAll)

  "join with Hi" should "return Hi" in {
    assert(Hi === Hi.join(Low))
    assert(Hi === Hi.join(Hi))
  }
  "meet with Low" should "return Low" in {
    assert(Low === Low.meet(Low))
    assert(Low === Low.meet(Hi))
  }

  /*
  "labeling a password" should "return a labeled password" in {
    val secret = label[L, String](BoundedLabel[LBase](Hi), "password")
      .evalLIO(lowExec)

    assert(secret === Labeled[L, String](BoundedLabel(Hi), "password"))
  }

  it should "fail if have pc tainted too high" in {
    assertThrows[IFCException] {
      label[L, String](BoundedLabel[LBase](Low), "password").evalLIO(hiExec)
    }
  }

  val nonsecret_labeler: LIO[L, Labeled[L, String]] =
    Core.label[L, String](BoundedLabel[LBase](Low), "42")

  val nonsecret_labeled: Labeled[L, String] =
    nonsecret_labeler.evalLIO(lowExec)

  val nonsecret: LIO[L, String] = LIO.unit[L, String]("42")

  val secret: LIO[L, String] = LIO.unit[L, String]("attack at dawn")

  val secret_labeler: LIO[L, Labeled[L, String]] =
    Core.label[L, String](BoundedLabel[LBase](Hi), "attack at dawn")

  val secret_labeled: Labeled[L, String] =
    secret_labeler.evalLIO(lowExec)

  val combiner: LIO[L, Labeled[L, String]] =
    for {
      ns <- unlabel(nonsecret_labeled)
      ss <- unlabel(secret_labeled)
      ls <- label(ns ++ ss)
    } yield ls

  val combined_label = combiner.evalLIO(lowExec).label

  "combining low and high" should "be labeled high" in {
    assert(BoundedLabel[LBase](Hi) == combined_label)
  }

  val combiner_low: LIO[L, Labeled[L, String]] =
    for {
      ns <- unlabel(nonsecret_labeled)
      ss <- unlabel(nonsecret_labeled)
      ls <- label(ns ++ ss)
    } yield ls

  val combined_low_label = combiner_low.evalLIO(lowExec).label

  "combining low and low" should "be labeled low" in {
    assert(BoundedLabel[LBase](Low) == combined_low_label)
  }
   */
}
