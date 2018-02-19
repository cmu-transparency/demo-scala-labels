package edu.cmu.spf.lio.demo

import edu.cmu.spf.lio._
//import edu.cmu.spf.lio.demo._
import edu.cmu.spf.lio.demo.System._

object DemoPolicy {
  type L = DemoLabel.T
  type T = Policy[L]

  implicit def policyOfCond[L <: Label[L]](
    allow: Boolean,
    cond: Condition[L]
  ): Policy[L] =
    new Policy[L] {
      def apply(l: L): Option[Boolean] =
        if (cond(l)) { Some(allow) } else { None }
    }

  /*
  implicit class LabelPolicy[L <: Label[L]](
    condition: Condition,
    select: Selector[L]
  ) {
    def ⊑(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Option[Boolean] = Some(select(dl) <= l)
    }
    def ⊏(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Option[Boolean] = Some(select(dl) < l)
    }
    def ⊒(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Option[Boolean] = Some(select(dl) >= l)
    }
    def ⊐(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Option[Boolean] = Some(select(dl) > l)
    }

    def ≤(l: L): Policy[DemoLabel] = ⊑(l)
    def <=(l: L): Policy[DemoLabel] = ⊑(l)
    def <(l: L): Policy[DemoLabel] = ⊏(l)
    def ≥(l: L): Policy[DemoLabel] = ⊒(l)
    def >=(l: L): Policy[DemoLabel] = ⊒(l)
    def >(l: L): Policy[DemoLabel] = ⊐(l)
  }
  */

  implicit class LabelCondition[L <: Label[L]](select: Selector[L]) {
    def ⊑(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) <= l)
    def ⊏(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) < l)
    def ⊒(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) >= l)
    def ⊐(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) > l)

    def ≤ (l: L): Condition[DemoLabel] = ⊑(l)
    def <=(l: L): Condition[DemoLabel] = ⊑(l)
    def < (l: L): Condition[DemoLabel] = ⊏(l)
    def ≥ (l: L): Condition[DemoLabel] = ⊒(l)
    def >=(l: L): Condition[DemoLabel] = ⊒(l)
    def > (l: L): Condition[DemoLabel] = ⊐(l)
  }

/*
  implicit def ⊑(dl: DemoLabel) = LabelLeq(dl)
  implicit def ⊒(dl: DemoLabel) = LabelGeq(dl)

  implicit def ⊑(
    person: Origin.Person.T = Origin.Person.top,
    location: Origin.Location.T = Origin.Location.top,
    time: Origin.Time.T = Origin.Time.top,
    purpose: Purpose.T = Purpose.top) =
    LabelLeq(new DemoLabel(person, location, time, purpose))

  implicit def ⊒(
    person: Origin.Person.T = Origin.Person.top,
    location: Origin.Location.T = Origin.Location.top,
    time: Origin.Time.T = Origin.Time.top,
    purpose: Purpose.T = Purpose.top) =
    LabelGeq(new DemoLabel(person, location, time, purpose))

  implicit def ⊏(
    person: Origin.Person.T = Origin.Person.top,
    location: Origin.Location.T = Origin.Location.top,
    time: Origin.Time.T = Origin.Time.top,
    purpose: Purpose.T = Purpose.top) =
    LabelLt(new DemoLabel(person, location, time, purpose))

  implicit def ⊐(
    person: Origin.Person.T = Origin.Person.top,
    location: Origin.Location.T = Origin.Location.top,
    time: Origin.Time.T = Origin.Time.top,
    purpose: Purpose.T = Purpose.top) =
    LabelGt(new DemoLabel(person, location, time, purpose))

  case class LabelLeq[L <: Label[L]](l1: L) extends Policy[L] {
    def apply(l2: L): Boolean = l1 <= l2
  }

  case class LabelGeq[L <: Label[L]](l1: L) extends Policy[L] {
    def apply(l2: L): Boolean = l1 >= l2
  }

  case class LabelLt[L <: Label[L]](l1: L) extends Policy[L] {
    def apply(l2: L): Boolean = l1 < l2
  }

  case class LabelGt[L <: Label[L]](l1: L) extends Policy[L] {
    def apply(l2: L): Boolean = l1 > l2
  }
 */
}

object Examples {
  import Policy._
  import DemoPolicy._
  import DemoLabel.Implicits._

  val publicRooms: Origin.Location =
    CoreTypes.Location("100")

  val allowPublicRooms = Legalese
    .allow(Origin.Location ⊑ publicRooms)

  val allowLocationForHVAC = Legalese
    .allow (Purpose ⊑ Purpose.climate_control)
    .except (Legalese
      .deny(Origin.Person ⊐ Origin.Person.bot)
    )
}
