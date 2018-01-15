/* LIO.Core and related */

package edu.cmu.spf.lio

import Policy._

import cats.Monad
import cats.Functor

object Core {
  sealed case class State[L <: Label[L]]
    (val pc: L, val policy: Policy[L]) {

    def canLabel(l: L): Boolean = {
      val ret = pc <= l
      //println(pc.toString + " ≤ " + l.toString + " = " + ret.toString)
      ret
    }

    override def toString = pc.toString + " under " + policy.toString
  }

  //type T[L, A] = LIO[L, A]

  private[lio] case class LIO[L <: Label[L], T]
    (f: State[L] => (T, State[L])) {
//      extends Monad[({ type LIOL[T] = LIO[L,T] })#LIOL] {

//  type LIOL[T] = LIO[L,T]

  def apply(x: State[L]): (T, State[L]) = f(x)

  def flatMap[S](k: T => LIO[L, S]): LIO[L, S] =
    LIO(s => {
      val (retval, retstate) = f(s)
      k(retval)(retstate)
    } )

  def map[S](k: T => S): LIO[L, S] =
    LIO(s => {
      val (retval, retstate) = f(s)
      (k(retval), retstate)
    } )

  def filter(cond: T => Boolean): LIO[L, T] =
    LIO(s => {
      val (res, retstate) = f(s)
      if (cond(res)) (res,retstate) else throw new MatchError(res)
    })

  def runLIO(s: State[L]): (T, State[L]) = Core.runLIO(this, s)
  def evalLIO(s: State[L]): T = Core.evalLIO(this, s)
}

object LIO {
  def unit[L <: Label[L], T](x: T): LIO[L, T] = LIO[L, T](s => (x,s))
}

  case class IFCException(s: String) extends Throwable {
    override def toString: String = s
  }

  /** Execute an IFC computation k given initial values for its label
    * and its clearance. Return a triple (res, l, c) combining the
    * result of the computation and the final values of the label and
    * the clearance. This method differs from its Haskell analog in
    * that any exceptions thrown during the execution of k are
    * propagated to runLIO; in the Haskell version, the exception is
    * only rethrown if we try to force the result, while the final
    * label and clearance are still accessible. */
  def runLIO[L <: Label[L], T]
    (k: LIO[L, T], s: State[L]): (T, State[L]) = {
    val (res, resstate) = k(s);
    (res, resstate)
  }

  /** Similar to runLIO, but only returns the result of the computation,
    * discarding the final label and clearance. */
  def evalLIO[L <: Label[L], T]
    (k: LIO[L, T], s: State[L]): T = {
    val (res, _) = runLIO(k, s);
    res
  }

  // AAA: We probably need to handle IFC exceptions differently
  def label[L <: Label[L], T](l: L, x: T): LIO[L, Labeled[L,T]] =
    new LIO(s =>
      if (s.canLabel(l)) (Labeled(l, x), s)
      else throw IFCException(s.toString + " cannot label " + l.toString)
    )

  def label[L <: Label[L], T](x: T): LIO[L, Labeled[L,T]] =
    new LIO(
      s => (Labeled(s.pc, x), s)
    )

  def unlabel[L <: Label[L], T](lt: Labeled[L, T]): LIO[L, T] =
    LIO[L, T](s => {
      val newPc = s.pc.join(lt.label);
      if (s.pc <= newPc) { //TODO: Fix  && newPc <= s.upper) {
        val news = new State(newPc, s.policy)
        (lt.element, news)
      } else throw IFCException("")
    }
    )
}
