package edu.cmu.spf.lio.demo

import edu.cmu.spf.lio._
import edu.cmu.spf.lio.demo.System._

object DemoPolicy {
  type L = DemoLabel
  type T = Policy[L]

  implicit def policyOfCond[L <: Label[L]](
    allow: Boolean,
    cond: Condition[L]
  ): Policy[L] =
    new Policy[L] {
      def apply(l: L): Option[Boolean] =
        if (cond(l)) { Some(allow) } else { None }
    }

  implicit class LabelCondition[L <: Label[L]](select: Selector[L]) {
    def ⊑(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) <= l) {
      override def toString = select.toString ++ " ⊑ " ++ l.toString
    }
    def ⊏(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) < l) {
      override def toString = select.toString ++ " ⊏ " ++ l.toString
    }
    def ⊒(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) >= l) {
      override def toString = select.toString ++ " ⊒ " ++ l.toString
    }
    def ⊐(l: L): Condition[DemoLabel] = new Condition[DemoLabel](select(_) > l) {
      override def toString = select.toString ++ " ⊐ " ++ l.toString
    }

    def ≤ (l: L): Condition[DemoLabel] = ⊑(l)
    def <=(l: L): Condition[DemoLabel] = ⊑(l)
    def < (l: L): Condition[DemoLabel] = ⊏(l)
    def ≥ (l: L): Condition[DemoLabel] = ⊒(l)
    def >=(l: L): Condition[DemoLabel] = ⊒(l)
    def > (l: L): Condition[DemoLabel] = ⊐(l)
  }

}

