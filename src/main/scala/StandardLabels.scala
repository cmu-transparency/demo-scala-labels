/* A collection of standard label types. */

package edu.cmu.spf.lio

sealed abstract class HiLow
    extends Label[HiLow]
    with Serializable

object HiLow {
  case object Hi extends HiLow {
    def join(l2: HiLow): HiLow = this
    def meet(l2: HiLow): HiLow = l2
  }
  case object Low extends HiLow {
    def join(l2: HiLow): HiLow = l2
    def meet(l2: HiLow): HiLow = this
  }
}
