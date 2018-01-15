package edu.cmu.spf.lio.demo

import java.time.Instant
import java.sql.Timestamp

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql._

import edu.cmu.spf.lio._

import SparkUtil._

import cats.implicits._
import cats.Foldable
import cats.Monad

object System {
  import DemoTypes._

//  val datadir = "/Users/piotrm/Dropbox/repos/github/spfoundations/data-wifi/"

  object Identifier {
    val users = Data.Users.users

    def identify(device_id: Int): LIO[CoreTypes.Person] =
      for {
        person <- Core.unlabel(users(device_id))
      } yield person
  }

  object Locator {
    val sensors = Data.Sensors.sensors

    def locate_sensor(sensor_id: Int): LIO[CoreTypes.Location] =
      for {
        sensor <- Core.unlabel(sensors(sensor_id))
      } yield sensor.location
  }

  object Aggregator {
//    import Core.LIOMonad

    import DemoTypes._

    val sensors = Data.Sensors.sensors
    val readings = Data.Readings.raw_readings

    /*
    def occupancy(location: CoreTypes.Location): LIO[Int] = {
      Foldable[List].foldM[LIO, Data.RawReading, Int](readings.toList, 0) {
        case (tot, reading) => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
          if loc == location
        } yield 1 + tot
      }
     }*/

//    def occupancy(location: CoreTypes.Location): LIO[Int] = {
//      val readings = 
//    }

    /*
    def aggregate(readings: Seq[Data.RawReading]):
        LIO[Map[CoreTypes.Location,Int]] = {
      Foldable[List].foldM(Data.Locations.raw_locations, Map()) { case (m, loc) =>
        val local_readings = readings
          .filter{ r => r.location == loc }
          .map{_ => 1}
          .sum
        for {


        }
      }*/
//      for {
//        reading <- readings
//        location <- Locator.locate_sensor(reading.sensor_id)
//      } yield (location -> 1)

  }

}
