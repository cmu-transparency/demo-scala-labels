package edu.cmu.spf.lio.demo

//import java.time.Instant
import java.sql.Timestamp

import edu.cmu.spf.lio._

import cats.Monad

import java.io.Serializable

package object Aliases {
  type DL = DemoLabel
  type Ld[T <: Serializable] = Core.Labeled[DL, T with Serializable]
  type LIO[T] = Core.LIO[DL, T]
  type State = Core.State[DL]
  val DT = DemoTypes

  type Purpose = Purpose.T
  type Role = Role.T
}

object DemoTypes {
  type LocalizedRow = (
    Person,
    Time,
    Location
  )

  object Implicits {
    implicit def <(t1: Timestamp, t2: Timestamp): Boolean = t1.compareTo(t2) < 0

    implicit def toTimestamp(t: Time): Timestamp = t.i
    implicit def toTime(i: Timestamp): Time = Time(i)
  }

  implicit class Time(val i: Timestamp) extends Serializable {
    def <(that: Time): Boolean = this.i.compareTo(that.i) < 0
    def <=(that: Time): Boolean = this.i.compareTo(that.i) <= 0
    def min(that: Time): Time = if (this <= that) this else that
    def max(that: Time): Time = if (this <= that) that else this
  }

  type Id = BigInt
  type Signal = Double
  type Name = String

  case class Person(val device_id: Id, val name: Name)
      extends Serializable

  case class Location(val location_id: Id, val name: Name)
      extends Serializable

  case class Purpose(val name: Name)
      extends Serializable

  case class Role(val name: Name)
      extends Serializable

  @SerialVersionUID(1000L)
  case class Sensor(val sensor_id: Id, val location: Location)
      extends java.io.Serializable

  case class SensorReading(
    val sensor_id: Id,
    val device_id: Id,
    val signal_strength: Signal,
    val timestamp: Time
  )
      extends Serializable
}
