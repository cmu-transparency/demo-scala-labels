package edu.cmu.spf.lio

import scala.collection.immutable.{Set=>SSet}

import cats.kernel.Eq

// https://github.com/typelevel/algebra/blob/master/core/src/main/scala/algebra/lattice/JoinSemilattice.scala

trait Label[T <: Serializable]
    extends PartiallyOrdered[T]
    with Serializable { self: T =>

  def join(that: T): T
  def meet(that: T): T

  def ⊔(that: T): T = join(that)
  def ⨅(that: T): T = meet(that)

  def isTop: Boolean = false
  def isBot: Boolean = false

  def tryCompareTo[S >: T]
  (that: S)
  (implicit evidence: S => PartiallyOrdered[S]): Option[Int] = {
    val ret =
        if (self == that) { Some(0) }
        else if (self == self.join(that.asInstanceOf[T])) { Some(1) }
        else if (self == self.meet(that.asInstanceOf[T])) { Some(-1) }
        else { None }
    ret
  }
}

case class BoundedLabel[T <: Label[T]](val lower: T, val upper: T)
    extends Label[BoundedLabel[T]]
    with Serializable {

  override def toString = "[" + lower.toString + "," + upper.toString + "]"

  def join(that: BoundedLabel[T]): BoundedLabel[T] =
    new BoundedLabel[T](
      this.lower.join(that.lower),
      this.upper.join(that.upper)
    )

  def meet(that: BoundedLabel[T]): BoundedLabel[T] =
    new BoundedLabel[T](
      this.lower.meet(that.lower),
      this.upper.meet(that.upper)
    )
}

object BoundedLabel {
  def apply[L <: Label[L]](l: L): BoundedLabel[L] =
    new BoundedLabel[L](l, l)
}

sealed abstract class LBoolean extends Label[LBoolean]
case class LTrueAndFalse() extends LBoolean() with DefaultTop[LBoolean]
case class LTrue() extends LBoolean() {
  def join(that: LBoolean): LBoolean = that match {
    case LTrue()
       | LNeitherTrueNorFalse() => LTrue()
    case LFalse()
       | LTrueAndFalse() => LTrueAndFalse()
  }

  def meet(that: LBoolean): LBoolean = that match {
    case LTrue()
       | LTrueAndFalse() => LTrue()
    case LFalse()
       | LNeitherTrueNorFalse() => LNeitherTrueNorFalse()
  }
}
case class LFalse() extends LBoolean() {
  def join(that: LBoolean): LBoolean = that match {
    case LFalse()
       | LNeitherTrueNorFalse() => LFalse()
    case LTrue()
       | LTrueAndFalse() => LTrueAndFalse()
  }

  def meet(that: LBoolean): LBoolean = that match {
    case LFalse()
       | LTrueAndFalse() => LFalse()
    case LTrue()
       | LNeitherTrueNorFalse() => LNeitherTrueNorFalse()
  }
}
case class LNeitherTrueNorFalse()
    extends LBoolean()
    with DefaultBottom[LBoolean]

class LabelTuple2[A <: Label[A], B <: Label[B]](val a: A, val b: B)
    extends Tuple2[A,B](a,b)
    with Label[LabelTuple2[A,B]] {

  def join(that: LabelTuple2[A,B]): LabelTuple2[A,B] = new LabelTuple2(
    this.a.join(that.a),
    this.b.join(that.b)
  )

  def meet(that: LabelTuple2[A,B]): LabelTuple2[A,B] = new LabelTuple2(
    this.a.meet(that.a),
    this.b.meet(that.b)
  )
}

class LabelTuple3[A <: Label[A], B <: Label[B], C <: Label[C]]
  (val a: A, val b: B, val c: C)
    extends Tuple3[A,B,C](a,b,c)
    with Label[LabelTuple3[A,B,C]] {

  def join(that: LabelTuple3[A,B,C]) = new LabelTuple3(
    this.a.join(that.a),
    this.b.join(that.b),
    this.c.join(that.c)
  )

  def meet(that: LabelTuple3[A,B,C]) = new LabelTuple3(
    this.a.meet(that.a),
    this.b.meet(that.b),
    this.c.meet(that.c)
  )
}

/*
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
 */

class LabelTuple4[A <: Label[A], B <: Label[B], C <: Label[C], D <: Label[D]]
  (val a: A, val b: B, val c: C, val d: D)
    extends Tuple4[A,B,C,D](a,b,c,d)
    with Label[LabelTuple4[A,B,C,D]]
{

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
  )

}


trait LabelTuple5Functions[
  A <: Label[A],
  B <: Label[B],
  C <: Label[C],
  D <: Label[D],
  E <: Label[E],
  T <: Tuple5[A,B,C,D,E]
] extends Tuple5[A,B,C,D,E] {

  def join(that: T): T = new Tuple5(
    this._1.join(that._1),
    this._2.join(that._2),
    this._3.join(that._3),
    this._4.join(that._4),
    this._5.join(that._5)
  ).asInstanceOf[T]

  def meet(that: T): T = new Tuple5(
    this._1.meet(that._1),
    this._2.meet(that._2),
    this._3.meet(that._3),
    this._4.meet(that._4),
    this._5.meet(that._5)
  ).asInstanceOf[T]

}


class LabelTuple5[
  +A,// <: Label[A],
  +B,// <: Label[B],
  +C,// <: Label[C],
  +D,// <: Label[D],
  +E// <: Label[E]]
]
  (a: A, b: B, c: C, d: D, e: E)
    extends Tuple5[A,B,C,D,E](a,b,c,d,e)
    //with LabelTuple5Functions[LabelTuple5[A,B,C,D,E]]
{/*
  def join(that: LabelTuple5[A,B,C,D,E]) = new LabelTuple5(
    this.a.join(that.a),
    this.b.join(that.b),
    this.c.join(that.c),
    this.d.join(that.d),
    this.e.join(that.e)
  )

  def meet(that: LabelTuple5[A,B,C,D,E]) = new LabelTuple5(
    this.a.meet(that.a),
    this.b.meet(that.b),
    this.c.meet(that.c),
    this.d.meet(that.d),
    this.e.meet(that.e)
  )
  */
}

trait DefaultTop[T] { self: T =>
  def join(t: T): T = this
  def meet(t: T): T = t
}
trait DefaultBottom[T] { self: T =>
  def join(t: T): T = t
  def meet(t: T): T = this
}

sealed abstract class USet[T]
    extends Label[USet[T]]
    with Serializable

case class AllSet[T]()
    extends USet[T]
    with DefaultTop[USet[T]] {
  override def toString: String = "all"
}
case class NoneSet[T]()
    extends USet[T]
    with DefaultBottom[USet[T]] {
  override def toString: String = "∅"
}
case class ThisSet[T]
  (elements: SSet[T]) extends USet[T] {

  override def toString: String = "{" + (elements.mkString(", ")) + "}"

  def join(that: USet[T]): USet[T] = that match {
    case s2: ThisSet[T] => ThisSet(elements.union(s2.elements))
    case _ => that.join(this)
  }

  def meet(that: USet[T]): USet[T] = that match {
    case s2: ThisSet[T] => ThisSet(elements.intersect(s2.elements))
    case _ => that.meet(this)
  }
/*
  def join[U <: USet[T]](that: U): USet[T] = that match {
    case s2: ThisSet[T] => ThisSet(elements.union(s2.elements))
    case _ => that.join(this)
  }

  def meet[U <: USet[T]](that: U): USet[T] = that match {
    case s2: ThisSet[T] => ThisSet(elements.intersect(s2.elements))
    case _ => that.meet(this)
  }
 */
}

case class Selector[DL, L](
  val select: DL => L,
  val s: String
) {
  def apply(l: DL): L = select(l)
  override def toString = s
}

case class Condition[L](val cond: L => Boolean) {
  def apply(l: L): Boolean = cond(l)

  def and(that: Condition[L]): Condition[L] = {
    val temp = this
    new Condition[L](
      l => apply(l) && that.apply(l)
    ) {
      override def toString: String = temp.toString + " AND " + that.toString
    }
  }
}
