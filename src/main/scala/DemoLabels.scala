package edu.cmu.spf.lio.demo

import java.time.Instant

import scala.collection.immutable.{Set=>SSet}

import edu.cmu.spf.lio._
import edu.cmu.spf.lio.demo.System._

case class Selector[L](val select: DemoLabel => L, val s: String) {
  def apply(l: DemoLabel): L = select(l)
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

class DemoLabel(
  val person: Origin.Person.T = Origin.Person.bot,
  val location: Origin.Location.T = Origin.Location.bot,
  val time: Origin.Time.T = Origin.Time.bot,
  val purpose: Purpose.T = Purpose.bot,
  val role: Role.T = Role.bot
)
    extends LabelTuple5[
      Origin.Person.T,
      Origin.Location.T,
      Origin.Time.T,
      Purpose.T,
      Role.T](person, location, time, purpose, role)
    with LabelTuple5Functions[
      Origin.Person.T,
      Origin.Location.T,
      Origin.Time.T,
      Purpose.T,
      Role.T,
      DemoLabel]
    with Label[DemoLabel]
    with Serializable

object DemoLabel {
  type T = DemoLabel

  val bot: T = new T(
    Origin.Person.bot,
    Origin.Location.bot,
    Origin.Time.bot,
    Purpose.bot,
    Role.bot)

  object Implicits {
    implicit def personToPersonLabel(p: CoreTypes.Person):
        Origin.Person = Origin.Person(p)

    implicit def personToDemoLabel(p: CoreTypes.Person):
        DemoLabel = new DemoLabel(person = p)

    implicit def timeToTimeLabel(t: CoreTypes.Time):
        Origin.Time = Origin.Time(t)
    implicit def timeToDemoLabel(t: CoreTypes.Time):
        DemoLabel = new DemoLabel(time = t)

    implicit def locationToLocationLabel(l: CoreTypes.Location):
        Origin.Location.T = Origin.Location(l)
    implicit def locationToDemoLabel(l: CoreTypes.Location):
        DemoLabel = new DemoLabel(location = l)

    implicit def purposeToPurposeLabel(p: CoreTypes.Purpose):
        Purpose.T = Purpose(p)
    implicit def purposeToDemoLabel(p: CoreTypes.Purpose):
        DemoLabel = new DemoLabel(purpose = p)

  }
}

object Purpose extends Selector[USet[CoreTypes.Purpose]](_._4, "Purpose") {
  type T = USet[CoreTypes.Purpose]

  object Nothing extends NoneSet
  object Everything extends AllSet

  val bot: T = Nothing.asInstanceOf[T]
  val top: T = Everything.asInstanceOf[T]

  val Legal = Purpose(CoreTypes.Purpose("Legal"))
  val ClimateControl = Purpose(CoreTypes.Purpose("ClimateControl"))
  val Sharing = Purpose(CoreTypes.Purpose("Sharing"))

  def apply(p: CoreTypes.Purpose): T = ThisSet(Seq(p).toSet)
}

object Role extends Selector[USet[CoreTypes.Role]](_._5, "Role") {
  type T = USet[CoreTypes.Role]

  object Nothing extends NoneSet
  object Everything extends AllSet

  val bot: T = Nothing.asInstanceOf[T]
  val top: T = Everything.asInstanceOf[T]

  val Affiliate = Role(CoreTypes.Role("Affiliate"))

  def apply(p: CoreTypes.Role): T = ThisSet(Seq(p).toSet)
}

object Origin {
  type Person = USet[CoreTypes.Person]
  object Person extends Selector[Person](_._1, "Person") {
    type T = Person
    val top: Person = AllSet()
    val bot: Person = NoneSet()
    def apply(p: CoreTypes.Person): Person = ThisSet(Seq(p).toSet)
  }

  sealed abstract class Time extends Label[Time]
  object Time extends Selector[Time](_._3, "Time") {
    import CoreTypes.Implicits
    type T = Time

    /* Time. Will be used to keep track of the temporal origin of data. */
    case object Never extends Time with DefaultBottom[Time]
    case object Always extends Time with DefaultTop[Time]

    val top: Time = Always
    val bot: Time = Never

    def apply(t: CoreTypes.Time): Time = AtTime(t)

    case class AtTime(at: CoreTypes.Time) extends Time {
      def join(l2: Time): Time =
        l2 match {
          case Never => this
          case Always => Always
          case AtTime(t) => {
            if (at < t) new Between(at, t)
            else if (at == t) this
            else new Between(t, at)
          }
          case Between(t1, t2) => {
            if (t1 <= at && at <= t2) l2
            else Always
          }
        }

      def meet(l2: Time): Time =
        l2 match {
          case Never => Never
          case Always => this
          case AtTime(t) => if (at == t) this else Never
          case Between(t1, t2) => {
            if (t1 <= at && at <= t2) this
            else Never
          }
        }
    }

    /* Invariant: after <= before */
    case class Between(
      after: CoreTypes.Time,
      before: CoreTypes.Time
    ) extends Time {

      def join(l2: Time): Time = {
        l2 match {
          case Never => this
          case Always => Always
          case AtTime(at) => {
            if (after <= at && at <= before) this
            else if (at < after) new Between(at, before)
            else new Between(after, at)
          }
          case Between(after2, before2) => {
            new Between(after min after2, before max before2)
          }
        }
      }

      def meet(l2: Time): Time = {
        l2 match {
          case Never => Never
          case Always => this
          case AtTime(at) => {
            if (after <= at && at <= before) l2 else Never
          }
          case Between(after2, before2) => {
            if (before < after2 || before2 < after) Never
            else new Between(after max after2, before min before2)
          }
        }
      }
    }
  }

  type Location = USet[CoreTypes.Location]
  object Location extends Selector[Location](_._2, "Location") {
    type T = Location

    object Nowhere extends NoneSet
    object Everywhere extends AllSet

    val top: T = Everywhere.asInstanceOf[T]
    val bot: T = Nowhere.asInstanceOf[T]

    def apply(l: CoreTypes.Location): T = ThisSet(Seq(l).toSet)

  }
}
