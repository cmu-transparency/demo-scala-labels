package edu.cmu.spf.lio

import scala.collection.immutable.{Set=>SSet}

import cats.kernel.Eq

// https://github.com/typelevel/algebra/blob/master/core/src/main/scala/algebra/lattice/JoinSemilattice.scala

trait Lattice[T] extends Serializable {

  def top: T
  def bot: T
  def isTop(l: T)(implicit ev: Eq[T]): Boolean = ev.eqv(l, top)
  def isBot(l: T)(implicit ev: Eq[T]): Boolean = ev.eqv(l, bot)
  def join(l1: T, l2: T): T
  def meet(l1: T, l2: T): T
}

/*
trait LatticeFunctions[L[T] <: Lattice[T]] {
  def top[T](implicit ev: L[T]): T = ev.top
  def bot[T](implicit ev: L[T]): T = ev.bot

  def join[T](l1: T, l2: T)(implicit ev: L[T]): T = ev.join(l1, l2)
  def meet[T](l1: T, l2: T)(implicit ev: L[T]): T = ev.meet(l1, l2)
}

object Lattice extends LatticeFunctions[Lattice] {
  def apply[T](implicit ev: Lattice[T]): Lattice[T] = ev
}
 */

trait LatticeElement[T] extends PartiallyOrdered[T] { self: T =>
  def join(that: T): T
  def meet(that: T): T

  def isTop: Boolean = false
  def isBot: Boolean = false

  def tryCompareTo[S >: T]
  (that: S)
  (implicit evidence: S => PartiallyOrdered[S]): Option[Int] = {
    val ret = //_that match {
      //case that =>
        if (self == that) { Some(0) }
        else if (self == self.join(that.asInstanceOf[T])) { Some(1) }
        else if (self == self.meet(that.asInstanceOf[T])) { Some(-1) }
        else { None }
   //   case _ => None
//    }
//    println("tryCompareTo(" + this.toString + ", " + that.toString + ") = " + ret.toString)
    ret
  }
}

trait Label[T] extends LatticeElement[T] { self: T => }

abstract class LabelLattice[T] extends Lattice[T]

case class BoundedLabel[T <: Label[T]](val lower: T, val upper: T)
    extends Label[BoundedLabel[T]] {

  override def toString = "[" + lower.toString + "," + upper.toString + "]"

  def join(that: BoundedLabel[T]): BoundedLabel[T] =
    new BoundedLabel(
      this.lower.join(that.lower),
      this.upper.join(that.upper)
    )

  def meet(that: BoundedLabel[T]): BoundedLabel[T] =
    new BoundedLabel(
      this.lower.meet(that.lower),
      this.upper.meet(that.upper)
    )
}

object BoundedLabel {
  def apply[L <: Label[L]](l: L): BoundedLabel[L] =
    new BoundedLabel[L](l, l)
}

//abstract class BoundedLabelLattice[T] extends Lattice[T] {
//}

class LabelTuple2[A <: Label[A], B <: Label[B]](val a: A, val b: B)
    extends Tuple2[A,B](a,b) with Label[LabelTuple2[A,B]] {

  def join(that: LabelTuple2[A,B]) = new LabelTuple2(
    this.a.join(that.a),
    this.b.join(that.b)
  )

  def meet(that: LabelTuple2[A,B]) = new LabelTuple2(
    this.a.meet(that.a),
    this.b.meet(that.b)
  )
}

class LabelTuple3[A <: Label[A], B <: Label[B], C <: Label[C]]
  (val a: A, val b: B, val c: C)
    extends Tuple3[A,B,C](a,b,c) with Label[LabelTuple3[A,B,C]] {

  def join(that: LabelTuple3[A,B,C]) = new LabelTuple3(
    this.a.join(that.a),
    this.b.join(that.b),
    this.c.join(that.c)
  )

  def meet(that: LabelTuple3[A,B,C]) = new LabelTuple3(
    this.a.meet(that.a),
    this.b.meet(that.b),
    this.c.join(that.c)
  )
}

trait LabelTuple4Functions[
  A <: Label[A],
  B <: Label[B],
  C <: Label[C],
  D <: Label[D],
  T <: Tuple4[A,B,C,D]
] extends Tuple4[A,B,C,D] { self: T =>

  def join(that: T) = (
    this._1.join(that._1),
    this._2.join(that._2),
    this._3.join(that._3),
    this._4.join(that._4)
  ).asInstanceOf[T]

  def meet(that: T) = (
    this._1.meet(that._1),
    this._2.meet(that._2),
    this._3.meet(that._3),
    this._4.meet(that._4)
  ).asInstanceOf[T]
}

class LabelTuple4[A <: Label[A], B <: Label[B], C <: Label[C], D <: Label[D]]
  (val a: A, val b: B, val c: C, val d: D)
    extends Tuple4[A,B,C,D](a,b,c,d) {// with Label[LabelTuple4[A,B,C,D]] {

  /*
  def join(that: LabelTuple4[A,B,C,D]) = new LabelTuple4(
    this.a.join(that.a),
    this.b.join(that.b),
    this.c.join(that.c),
    this.d.join(that.d)
  )

  def meet(that: LabelTuple4[A,B,C,D]) = new LabelTuple4(
    this.a.meet(that.a),
    this.b.meet(that.b),
    this.c.meet(that.c),
    this.d.meet(that.d)
  )*/
}

trait DefaultTop[T] { self: T =>
  def join(t: T): T = this
  def meet(t: T): T = t
}
trait DefaultBottom[T] { self: T =>
  def join(t: T): T = t
  def meet(t: T): T = this
}

sealed abstract class USet[T] extends Label[USet[T]]
case class AllSet[T]() extends USet[T] with DefaultTop[USet[T]] {
  override def toString: String = "all"
}
case class NoneSet[T]() extends USet[T] with DefaultBottom[USet[T]] {
  override def toString: String = "∅"
}
case class ThisSet[T]
  (elements: SSet[T]) extends USet[T] {

  override def toString: String = "{" + (elements.mkString(", ")) + "}"

  def join(_s2: USet[T]): USet[T] = _s2 match {
    case s2: ThisSet[T] => ThisSet(elements.union(s2.elements))
    case _ => this.join(_s2)
  }

  def meet(_s2: USet[T]): USet[T] = _s2 match {
    case s2: ThisSet[T] => ThisSet(elements.intersect(s2.elements))
    case _ => this.meet(_s2)
  }
}
