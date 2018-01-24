package edu.cmu.spf.lio.demo

import edu.cmu.spf.lio._
//import edu.cmu.spf.lio.demo._
import edu.cmu.spf.lio.demo.System._

object DemoPolicy {
  type L = DemoLabel.T
  type T = Policy[L]

  implicit class LabelCompare[L <: Label[L]](select: Selector[L]) {
    def ⊑(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Boolean = select(dl) <= l
    }
    def ⊏(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Boolean = select(dl) < l
    }
    def ⊒(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Boolean = select(dl) >= l
    }
    def ⊐(l: L): Policy[DemoLabel] = new Policy[DemoLabel] {
      def apply(dl: DemoLabel): Boolean = select(dl) > l
    }

    def ≤(l: L): Policy[DemoLabel] = ⊑(l)
    def <=(l: L): Policy[DemoLabel] = ⊑(l)
    def <(l: L): Policy[DemoLabel] = ⊏(l)
    def ≥(l: L): Policy[DemoLabel] = ⊒(l)
    def >=(l: L): Policy[DemoLabel] = ⊒(l)
    def >(l: L): Policy[DemoLabel] = ⊐(l)


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

/* Positive norms and negative norms. Policy[L] applies if one of the
 * positive norms is satisfied, and none of the negative ones is. */
  case class Legalese[L <: Label[L]](
    val positives: Iterable[Policy[L]],
    val negatives: Iterable[Policy[L]]
  ) extends Policy[L] {

    def this() = this(Seq(), Seq())

    def apply(l: L): Boolean =
      positives.exists(_(l)) && negatives.forall(! _(l))

    def allow(some: Iterable[Policy[L]]): Legalese[L] = {
      Legalese(positives ++ some, negatives)
    }
    def allow(some: Policy[L]): Legalese[L] = {
      Legalese(positives ++ Seq(some), negatives)
    }
    def except(some: Iterable[Policy[L]]): Legalese[L] = {
      Legalese(positives, negatives ++ some)
    }
    def except(some: Policy[L]): Legalese[L] = {
      Legalese(positives, negatives ++ Seq(some))
    }
  }
 }

object Examples {
  import Policy._
  import DemoPolicy._
  import DemoLabel.Implicits._

  val publicRooms: Origin.Location =
    CoreTypes.Location("100")

  val allowPublicRooms = (new Legalese()
    allow(Origin.Location ⊑ publicRooms)
  )

  val allowLocationForHVAC = (new Legalese()
    allow (Purpose ⊑ Purpose.climate_control)
    except (Origin.Person ⊐ Origin.Person.bot)
  )
}
