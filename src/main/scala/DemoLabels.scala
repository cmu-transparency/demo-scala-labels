package edu.cmu.spf.lio.demo

import java.time.Instant

import scala.collection.immutable.{Set=>SSet}

import edu.cmu.spf.lio._

import Aliases._

class DemoLabel(
  val person: Origin.Person = Origin.Person.bot,
  val location: Origin.Location = Origin.Location.bot,
  val time: Origin.Time = Origin.Time.bot,
  val purpose: Purpose = Purpose.bot,
  val role: Role = Role.bot)
    extends Tuple5[
      Origin.Person,
      Origin.Location,
      Origin.Time,
      Purpose,
      Role](person, location, time, purpose, role)
/*    with LabelTuple5Functions[
      Origin.Person.T,
      Origin.Location.T,
      Origin.Time.T,
      Purpose.T,
      Role.T,
      L]*/
    with Label[DemoLabel]
    with Serializable {

  def join(that: DemoLabel): DemoLabel = new DemoLabel(
    this._1.join(that._1),
    this._2.join(that._2),
    this._3.join(that._3),
    this._4.join(that._4),
    this._5.join(that._5)
  )

  def meet(that: DemoLabel): DemoLabel = new DemoLabel(
    this._1.meet(that._1),
    this._2.meet(that._2),
    this._3.meet(that._3),
    this._4.meet(that._4),
    this._5.meet(that._5)
  )

  //def this(r: Role.T) = this(role = r)
}

object DemoLabel {
  val bot: DL = new DL(
    Origin.Person.bot,
    Origin.Location.bot,
    Origin.Time.bot,
    Purpose.bot,
    Role.bot)

  object Implicits {
    implicit def personToPersonLabel(p: DT.Person):
        Origin.Person = Origin.Person(p)
    implicit def personToL(p: DT.Person): DL = new DL(person = p)

    implicit def timeToTimeLabel(t: DT.Time): Origin.Time = Origin.Time(t)
    implicit def timeToL(t: DT.Time):
        DL = new DL(time = t)

    implicit def locationToLocationLabel(l: DT.Location):
        Origin.Location.T = Origin.Location(l)
    implicit def locationToL(l: DT.Location):
        DL = new DL(location = l)

    implicit def purposeToPurposeLabel(p: DT.Purpose):
        Purpose.T = Purpose(p)
    implicit def purposeToL(p: DT.Purpose):
        DL = new DL(purpose = p)
    implicit def purposeLabelToL(p: Purpose.T):
        DL = new DL(purpose = p)

    implicit def roleToRoleLabel(p: DT.Role):
        Role.T = Role(p)
    implicit def roleToL(p: DT.Role):
        DL = new DL(role = p)
    implicit def roleLabelToL(p: Role.T):
        DL = new DL(role = p)

  }
}

object Purpose extends Selector[DemoLabel, USet[DT.Purpose]](_._4, "Purpose") {
  type T = USet[DT.Purpose]

  object Nothing extends NoneSet
  object Everything extends AllSet

  val bot: T = Nothing.asInstanceOf[T]
  val top: T = Everything.asInstanceOf[T]

  val Legal = Purpose(DT.Purpose("Legal"))
  val ClimateControl = Purpose(DT.Purpose("ClimateControl"))
  val Sharing = Purpose(DT.Purpose("Sharing"))
  val Storage = Purpose(DT.Purpose("Storage"))

  def apply(p: DT.Purpose): T = ThisSet(Seq(p).toSet)
}

object Role extends Selector[DemoLabel, USet[DT.Role]](_._5, "Role") {
  type T = USet[DT.Role]

  object Nothing extends NoneSet
  object Everything extends AllSet

  val bot: T = Nothing.asInstanceOf[T]
  val top: T = Everything.asInstanceOf[T]

  val Affiliate = Role(DT.Role("Affiliate"))
  val Administrator = Role(DT.Role("Administrator"))

  def apply(p: DT.Role): T = ThisSet(Seq(p).toSet)
}

object Origin {
  type Person = USet[DT.Person]
  object Person extends Selector[DL, Person](_._1, "Person") {
    type T = Person
    val top: Person = AllSet()
    val bot: Person = NoneSet()
    def apply(p: DT.Person): Person = ThisSet(Seq(p).toSet)
  }

  sealed abstract class Time extends Label[Time]
  object Time extends Selector[DL, Time](_._3, "Time") {
    import DT.Implicits
    type T = Time

    /* Time. Will be used to keep track of the temporal origin of data. */
    case object Never extends Time with DefaultBottom[Time]
    case object Always extends Time with DefaultTop[Time]

    val top: Time = Always
    val bot: Time = Never

    def apply(t: DT.Time): Time = AtTime(t)

    case class AtTime(at: DT.Time) extends Time {
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
      after: DT.Time,
      before: DT.Time
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

  type Location = USet[DT.Location]
  object Location extends Selector[DL, Location](_._2, "Location") {
    type T = Location

    object Nowhere extends NoneSet
    object Everywhere extends AllSet

    val top: T = Everywhere.asInstanceOf[T]
    val bot: T = Nowhere.asInstanceOf[T]

    def apply(l: DT.Location): T = ThisSet(Seq(l).toSet)

  }
}