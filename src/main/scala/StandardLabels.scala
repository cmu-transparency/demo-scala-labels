/* A collection of standard label types. */

package edu.cmu.spf.lio

sealed abstract class HiLow extends Label[HiLow]

object HiLow {
/*  implicit val hiLowLabeling: Labeling[HiLow] = new Labeling[HiLow] {
    val bot: HiLow = Low
    val top: HiLow = Hi
  }*/
  case object Hi extends HiLow {
    //  override def isTop = true

    def join(l2: HiLow): HiLow = this
    def meet(l2: HiLow): HiLow = l2

/*    override def join(_l2: Label): Label = _l2 match {
      case l2: HiLow => this.join(l2)
      case _ => super.join(_l2)
    }
    /*
    def join(_l2: Label): Label = {
      val temp = if (_l2.isBot) {
        this.asInstanceOf[Label]
      } else {
        _l2 match {
          case l2: HiLow => this.join(l2).asInstanceOf[Label]
          case _ => LabelLattice.top
        }
      }
      println(this.toString + " join " + _l2.toString + "=" + temp.toString)
      temp
     }*/

    def meet(_l2: Label): Label = {
      val temp = if (_l2.isTop) {
        this.asInstanceOf[Label]
      } else {
        _l2 match {
          case l2: HiLow => this.meet(l2).asInstanceOf[Label]
          case _ => LabelLattice.bot
        }
      }
      println(this.toString + " meet " + _l2.toString + "=" + temp.toString)
      temp
    }
 */

//    def join(l2: HiLow): HiLow = Hi
//    def meet(l2: HiLow): HiLow = l2
/*    def tryCompareTo[L >: Label <% PartiallyOrdered[L]](that: L)
        : Option[Int] = {
      println(that.getClass)
      that match {
        case Low => Some(1)
        case Hi  => Some(0)
        case Label => Some(0)
        case _   => None
      }
    }*/
  }
  case object Low extends HiLow {
    //    override def isBot = true

    def join(l2: HiLow): HiLow = l2
    def meet(l2: HiLow): HiLow = this

    /*
    def join(_l2: Label): Label = {
      val temp = if (_l2.isBot) {
        this.asInstanceOf[Label]
      } else {
        _l2 match {
          case l2: HiLow => this.join(l2).asInstanceOf[Label]
          case _ => LabelLattice.top
        }
      }
        println(this.toString + " join " + _l2.toString + "=" + temp.toString)
        temp
    }
    def meet(_l2: Label): Label = {
      val temp = if (_l2.isTop) {
        this.asInstanceOf[Label]
      } else {
        _l2 match {
          case l2: HiLow => this.meet(l2).asInstanceOf[Label]
          case _ => LabelLattice.bot
        }
      }
      println(this.toString + " meet " + _l2.toString + "=" + temp.toString)
      temp
    }
     */

/*    def join(l2: Label): Label = l2
    def meet(l2: Label): Label = Low.asInstanceOf[Label]*/

//    def join(l2: HiLow): HiLow = l2
//    def meet(l2: HiLow): HiLow = Low
/*    def tryCompareTo[L >: Label <% PartiallyOrdered[L]](that: L)
        : Option[Int] = that match {
      case Hi  => Some(-1)
      case Low => Some(0)
      case Label => Some(0)
      case _   => None
    }*/
  }
}
