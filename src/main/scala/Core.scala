/* LIO.Core and related */

package edu.cmu.spf.lio

import Policy._

import cats.Monad
import cats.Functor
import cats.Applicative
import cats.Foldable
import cats.Traverse
import cats.implicits._

import cats.mtl.FunctorEmpty
import cats.mtl.TraverseEmpty
import cats.mtl.implicits._

import scala.annotation.tailrec

import org.apache.spark.rdd.RDD

object Core {
  sealed case class State[L <: Label[L]]
    (val pc: L, val policy: Policy[L]) {

    def canLabel(l: L): Boolean = //true
      {
        policy(pc.join(l)) match {
          case Some(true) => true
          case _ => false
            //println(pc.toString + " ≤ " + l.toString + " = " + ret.toString)
        }
      }

    override def toString = pc.toString + " under " + policy.toString
  }

/* Labeled data comes with both a lower and upper bound on the label
   that is attributed to it. */

  private [lio] case class Labeled
    [L <: Label[L], T <: Serializable]
    (label: L, element: T)
      extends Serializable {

    override def toString: String = element.toString + "[" + label.toString + "]"

  }


  private[lio] case class LIO[L <: Label[L], T] (f: State[L] => (T, State[L])) {

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

    def TCBeval(context: L, policy: Policy[L]): T =
      Core.evalLIO(this, new State(context, policy))
  }

  object LIO {
    /* for Monad */
    def unit[L <: Label[L], T](x: T): LIO[L, T] = LIO[L, T](s => (x,s))

    @tailrec
    def tailRecM[L <: Label[L], T, S]
      (a: T, f: T => LIO[L, Either[T, S]], s: State[L]): (S, State[L]) = {
      val (retval, retstate) = f(a)(s)
      retval match {
        case Left(nextA) => tailRecM(nextA, f, retstate)
        case Right(b)    => (b, retstate)
      }
    }

    /* for Applicative */
    def ap[L <: Label[L], A,B]
      (ff: LIO[L, A => B])
      (fa: LIO[L, A]): LIO[L, B] = LIO( s => {
        val (fa2b, s2) = ff(s) // unsure if state needs to be threaded in here
        val (a, s3) = fa(s2)
        val b = fa2b(a)
        (b, s3)
      }
    )


    /* traversals */
    def foldM[L <: Label[L], F[_], A, B]
    (fa: F[A], b: B)
    (fab: (B, A) => LIO[L, B])
    (implicit FF: Foldable[F]): LIO[L, B] =
      FF.foldM[({type l[T] = LIO[L,T]})#l, A, B](fa, b)(fab)

    def filterM[L <: Label[L], F[_], A]
      (fa: F[A])(f: A => LIO[L, Boolean])
      (implicit T: TraverseEmpty[F]): LIO[L, F[A]] =
      T.filterA[({type l[T] = LIO[L,T]})#l, A](fa)(f)

    def mapRDD[L <: Label[L], A, B](it: RDD[A])(fab: A => LIO[L,B]):
        RDD[LIO[L,B]] = it.map(fab)

  }

  implicit def numericForLIO[T: Numeric, L <: Label[L]] = 
    new Numeric[LIO[L,T]] {
      def fromInt(x: Int): LIO[L,T] =
        throw IFCException("only mapping operations available for Numeric[LIO[L,T]]")
      def toDouble(x: LIO[L,T]): Double =
        throw IFCException("only mapping operations available for Numeric[LIO[L,T]]")
      def toFloat(x: LIO[L,T]): Float =
        throw IFCException("only mapping operations available for Numeric[LIO[L,T]]")
      def toInt(x: LIO[L,T]): Int =
        throw IFCException("only mapping operations available for Numeric[LIO[L,T]]")
      def toLong(x: LIO[L,T]): Long =
        throw IFCException("only mapping operations available for Numeric[LIO[L,T]]")
      def compare(x: LIO[L,T], y: LIO[L,T]): Int =
        throw IFCException("only mapping operations available for Numeric[LIO[L,T]]")

      def minus(x: LIO[L,T],y: LIO[L,T]): LIO[L,T] = for {
        vx <- x
        vy <- y
      } yield implicitly[Numeric[T]].minus(vx,vy)
      def negate(x: LIO[L,T]): LIO[L,T] = for {
        vx <- x
      } yield implicitly[Numeric[T]].negate(vx)
      def plus(x: LIO[L,T],y: LIO[L,T]): LIO[L,T] = for {
        vx <- x
        vy <- y
      } yield implicitly[Numeric[T]].plus(vx, vy)
      def times(x: LIO[L,T],y: LIO[L,T]): LIO[L,T] = for {
        vx <- x
        vy <- y
      } yield implicitly[Numeric[T]].times(vx, vy)
  }

  implicit def applicativeForLIO[L <: Label[L]]
      : Applicative[({type l[T] = LIO[L,T]})#l] =
    new Applicative[({type l[T] = LIO[L,T]})#l] {

      def ap[A,B](ff: LIO[L, A => B])(fa: LIO[L, A]): LIO[L, B] = LIO.ap(ff)(fa)
      def pure[T](a: T): LIO[L,T] = LIO.unit(a)
  }

  implicit def monadForLIO[L <: Label[L]]
      : Monad[({type l[T] = LIO[L,T]})#l] =
    new Monad[({type l[T] = LIO[L,T]})#l] {

      def flatMap[T, S](fa : LIO[L,T])
        (f : T => LIO[L, S]) : LIO[L, S] = fa.flatMap(f)

      def pure[T](a : T) : LIO[L,T] = LIO.unit(a)

      def tailRecM[T, S](a : T)(f : T => LIO[L, Either[T, S]]) : LIO[L, S] =
        LIO(s => LIO.tailRecM(a, f, s))
  }

  /** Example showing how to use the Monad instance of LIO */
  def foldM_example[L <: Label[L]](a : List[Int], f : Int => LIO[L, Int]) : LIO[L, Int] =
    Foldable[List].foldM[({type l[T] = LIO[L,T]})#l, Int, Int](a, 0) { (acc, i) =>
      for {
        x <- f(i)
      } yield acc + x
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
  def label[L <: Label[L], T <: Serializable](l: L, x: T)
      : LIO[L, Labeled[L,T]] =
    new LIO(s =>
      if (s.canLabel(l)) (Labeled(l, x), s)
      else throw IFCException(s.toString + " cannot label " + l.toString)
    )

  def label[L <: Label[L], T <: Serializable](x: T): LIO[L, Labeled[L,T]] =
    new LIO(
      s => (Labeled(s.pc, x), s)
    )

  def unlabel[L <: Label[L], T <: Serializable](lt: Labeled[L, T]): LIO[L, T] =
    LIO[L, T](s => {
      val newPc = s.pc.join(lt.label);
      if (s.pc <= newPc) { //TODO: Fix  && newPc <= s.upper) {
        val news = new State(newPc, s.policy)
        (lt.element, news)
      } else throw IFCException("")
    }
    )
}
