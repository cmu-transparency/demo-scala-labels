/* A collection of standard label types. */

package edu.cmu.spf.lio

sealed abstract class HiLow
    extends Label[HiLow]
    with Serializable

object HiLow {
  case object Hi  extends HiLow with DefaultTop[HiLow]
  case object Low extends HiLow with DefaultBottom [HiLow]
}
