package edu.cmu.spf.lio

import edu.cmu.spf.lio._
//import edu.cmu.spf.lio.demo.System._

object Legalese {
  def appliesToAll[L <: Label[L]]: Condition[L] = new Condition[L](_ => true) {
    override def toString: String = ""
  }

  def allow[L <: Label[L]]: Legalese[L] = new Legalese(true, appliesToAll)
  def deny[L <: Label[L]]: Legalese[L] = new Legalese(false, appliesToAll)

  def allow[L <: Label[L]](cond: Condition[L]): Legalese[L] =
    new Legalese(true, cond)
  def deny[L <: Label[L]](cond: Condition[L]): Legalese[L] =
    new Legalese(false, cond)
}

case class PolicySyntaxException(s: String) extends Exception

case class Legalese[L <: Label[L]](
  val isAllow: Boolean,          // true = Allow, false = Deny
  val cond: Condition[L],
  val except: Iterable[Legalese[L]]
) extends Policy[L] {

  def this(cond: Condition[L]) = this(true, cond, Seq())
  def this(pos: Boolean, cond: Condition[L]) = this(pos, cond, Seq())

/*

(ALLOW EXCEPT
       (DENY (DataType PII) (UseForPurpose Sharing) EXCEPT
             (ALLOW (DataType PII:OptIn))
             (ALLOW (AccessByRole Affiliates))
             (ALLOW (UseForPurpose Legal))))

(ALLOW EXCEPT
       (DENY (Origin users) (UseForPurpose Sharing) EXCEPT
             (ALLOW (Origin users:OptIn))
             (ALLOW (AccessByRole Affiliates))
             (ALLOW (UseForPurpose Legal))))

 (ALLOW t) (r) = allow   if r ⊑ t
 (ALLOW t) (r) = na      if not r ⊑ t
 (DENY t) (r) = deny     if r ⊑ t
 (DENY t) (r) = na       if not r ⊑ t
 (ALLOW t EXCEPT D₁ ··· Dₙ) (r) = allow   if r ⊑ t and Dᵢ (r) ≠ deny for all i
 (ALLOW t EXCEPT D₁ ··· Dₙ) (r) = deny    if r ⊑ t and Dᵢ (r) = deny for some i
 (ALLOW t EXCEPT D₁ ··· Dₙ) (r) = na      if not r ⊑ t
 (DENY t EXCEPT A₁ ··· Aₙ) (r) = deny     if r ⊑ t and Aᵢ (r) ≠ allow for all i
 (DENY t EXCEPT A₁ ··· Aₙ) (r) = allow    if r ⊑ t and Aᵢ (r) = allow for some i 
 (DENY t EXCEPT A₁ ··· Aₙ) (r) = na       if not r ⊑ t

 */

  /*
   allow A except deny B, deny C
   deny A except allow B, allow C
   */

  def apply(l: L): Option[Boolean] = {
    val outs_app: Iterable[Boolean] =
      except.map{_(l)}.filter { ! _.isEmpty }.map{_.get}

    (cond(l), outs_app.exists(a => a ^ isAllow)) match {
      case (true, true) => Some(! isAllow)
      case (true, false) => Some(isAllow)
      case (false, false)
         | (false,true) => None
    }
  }

  def _when_allow[A](f: => A): A = if (isAllow) { f } else {
    throw PolicySyntaxException("cannot add sub-policy to a positive legalese policy")
  }

  def _when_deny[A](f: => A): A = if (! isAllow) { f } else {
    throw PolicySyntaxException("cannot add sub-policy to a negative legalese policy")
  }

/*  def allow(some: Iterable[Policy[L]]): Legalese[L] = _when_positive {
    Legalese(positive, ins ++ some, outs)
  }
  def allow(some: Policy[L]): Legalese[L] = _when_positive {
    Legalese(positive, ins ++ Seq(some), outs)
  }

  def deny(some: Iterable[Policy[L]]): Legalese[L] = _when_negative {
    Legalese(positive, ins ++ some, outs)
  }
  def deny(some: Policy[L]): Legalese[L] = _when_negative {
    Legalese(positive, ins ++ Seq(some), outs)
  }
 */

  override def toString: String = {
    val condString = cond.toString
    "(" ++
    {if (isAllow) { "ALLOW" } else { "DENY" }} ++
    {if (condString == "") { "" } else {" " ++ condString}} ++
    " " ++
    { if (except.size > 0) {
      "EXCEPT " ++ except.map(_.toString).mkString("\n")
    } else {
      ""
    }
    } ++
    ")"
  }

  def except(some: Iterable[Legalese[L]]): Legalese[L] = {
      Legalese(isAllow, cond, except ++ some)
    }
  def except(some: Legalese[L]): Legalese[L] = {
      Legalese(isAllow, cond, except ++ Seq(some))
    }
}
