package edu.cmu.spf.lio.demo

import edu.cmu.spf.lio._
//import edu.cmu.spf.lio.demo._
import edu.cmu.spf.lio.demo.System._

/* Policies that accept or reject (depending on positive) when on one of
 * the ins and none of the outs. */

//  type LL = Legalese

object Legalse {
  def allow[L <: Label[L]]: Legalese[L] = new Legalese(true)
  def deny[L <: Label[L]]: Legalese[L] = new Legalese(false)
}

case class PolicySyntaxException(s: String) extends Exception

case class Legalese[L <: Label[L]](
  val positive: Boolean, // True means ins are positives
  val ins: Iterable[Policy[L]],
  val outs: Iterable[Policy[L]]
) extends Policy[L] {

  def this() = this(true, Seq(), Seq())
  def this(pos: Boolean) = this(pos, Seq(), Seq())

/*
 allow A except deny B, deny C
 deny A except allow B, allow C
 */

  def apply(l: L): Boolean = if (positive) {
    ins.exists(_(l)) && outs.forall(! _(l))
  } else {
    ! (ins.exists(_(l)) && outs.forall(! _(l)))
  }

  def _when_positive[A](f: => A): A = if (positive) { f } else {
    throw PolicySyntaxException("cannot add sub-policy to a positive legalese policy")
  }

  def _when_negative[A](f: => A): A = if (! positive) { f } else {
    throw PolicySyntaxException("cannot add sub-policy to a negative legalese policy")
  }

  def allow(some: Iterable[Policy[L]]): Legalese[L] = _when_positive {
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

  def except(some: Iterable[Policy[L]]): Legalese[L] = {
      Legalese(positive, ins, outs ++ some)
    }
  def except(some: Policy[L]): Legalese[L] = {
      Legalese(positive, ins, outs ++ Seq(some))
    }
}
