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
import cats.Applicative

import cats.mtl.FunctorEmpty
import cats.mtl.TraverseEmpty
import cats.mtl.implicits._

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

    /* Given a sensor_id, find its physical location. */
    def locate_sensor(sensor_id: Int): LIO[CoreTypes.Location] =
      for {
        sensor <- Core.unlabel(sensors(sensor_id))
      } yield sensor.location
  }

  object Aggregator {
    import DemoTypes._

    val sensors = Data.Sensors.sensors
    val readings = Data.Readings.raw_readings

    import cats.Functor

    /* Filter the given readings to those at a given location. */
    def readingsAtLocation(
      readings: Seq[Data.RawReading],
      location: CoreTypes.Location
    ): LIO[List[Data.RawReading]] =
      Core.LIO.filterM(readings.toList) {
        reading => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
        } yield loc == location
      }

    /* Look up how many readings there are at a particular location. */
    def occupancy(location: CoreTypes.Location): LIO[Int] = {
      Core.LIO.foldM(readings.toList, 0) {
        case (tot, reading) => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
          if loc == location
        } yield 1 + tot
      }
     }

    /* Produce an occupancy table for every location. */
    def aggregate(readings: Seq[Data.RawReading]):
        Map[CoreTypes.Location, LIO[Int]] =

      Data.Locations.raw_locations
        .foldLeft(Map[CoreTypes.Location, LIO[Int]]()) {
          case (m, loc) => m + (loc -> occupancy(loc))
        }

  }
}
