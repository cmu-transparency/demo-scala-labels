package edu.cmu.spf.lio.demo

//import java.time.Instant
import java.sql.Timestamp

import edu.cmu.spf.lio._

object CoreTypes {

  object Implicits {
    implicit def <(t1: Timestamp, t2: Timestamp): Boolean = t1.compareTo(t2) < 0

    implicit def toTimestamp(t: Time): Timestamp = t.i
    implicit def toTime(i: Timestamp): Time = Time(i)

//    implicit def toLong(t: Time): Long = 0
//    implicit def toTime(t: Long): Time = Time(Instant.now())
  }

  implicit class Time(val i: Timestamp) extends Serializable {
    def <(that: Time): Boolean = this.i.compareTo(that.i) < 0
    def <=(that: Time): Boolean = this.i.compareTo(that.i) <= 0
    def min(that: Time): Time = if (this <= that) this else that
    def max(that: Time): Time = if (this <= that) that else this
  }

  /*
  abstract class Time(val when: Timestamp) extends Label[Time] {
    // FIXME: Use standard Java Time class

    def compare(that: Time): Int = {
      this.when.compare(that.when)
    }
    // FIXME: Are min and max defined somewhere in the standard library?
    def min(that: Time): Time = {
      if (this.when <= that.when) this else that
    }

    def max(that: Time): Time = {
      if (this.when <= that.when) that else this
    }
  }
   */

  case class Person(val device_id: Int, val name: String)
      extends Serializable
  case class Location(val name: String)
      extends Serializable
  case class Purpose(val name: String)
      extends Serializable
  case class Role(val name: String)
      extends Serializable
  case class Sensor(val sensor_id: Int, val location: Location)
      extends Serializable

}

import CoreTypes._
import edu.cmu.spf.lio._

import cats.Monad

object DemoTypes {
  type L = DemoLabel.T
  type Ld[T <: Serializable] = Labeled[L, T]
  type LIO[T] = Core.LIO[L, T]
  type State = Core.State[L]

  sealed abstract class Purpose extends Serializable
  case object Policing extends Purpose
  case object Auditing extends Purpose
  case object ClimateControl extends Purpose

  type LocalizedRow = (
    Person,
    Time,
    Location
  ) // labeled will by applied to the whole thing
}
