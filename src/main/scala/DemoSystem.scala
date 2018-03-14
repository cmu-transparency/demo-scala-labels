package edu.cmu.spf.lio.demo

import java.time.Instant
import java.sql.Timestamp

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.rdd.RDD

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
  import Aliases._
  import Core.unlabel
  import Core.label

  object Identifier {
    val users = Data.users

    def identify(device_id: DT.Id): LIO[DT.Person] =
      unlabel(users(device_id))
  }

  object Locator {
    val sensors = Data.sensors

    /* Given a sensor_id, find its physical location. */
    def locate_sensor(sensor_id: DT.Id): LIO[DT.Location] = for {
      sensor <- unlabel(sensors(sensor_id))
    } yield sensor.location

    /* Filter the given readings to those at a given location. */
    def readingsAtLocation(
      readings: Seq[DT.SensorReading],
      location: DT.Location
    ): LIO[List[DT.SensorReading]] =
      Core.LIO.filterM(readings.toList) {
        reading => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
        } yield loc == location
      }
  }

  object Aggregator {
    import DT._

    //val sensors = Data.sensors
    //val readings = Data.readings

    /* Look up how many readings there are at a particular location. */
    def occupancyCollect(
      readings: RDD[DT.SensorReading],
      location: DT.Location): LIO[Int] = {

      Core.LIO.foldM(readings.collect.toList, 0) {
        case (tot, reading) => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
          if loc == location
        } yield 1 + tot
      }
    }


    def occupancy(
      readings: RDD[DT.SensorReading],
      location: DT.Location): LIO[Int] = {

      Core.LIO.mapRDD(readings) {
        case reading => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
        } yield if (loc == location) { 1 } else { 0 }
      }.reduce(implicitly[Numeric[LIO[Int]]].plus)
    }

    /* Produce an occupancy table for every location. */
    def aggregate(readings: RDD[DT.SensorReading]):
        Map[DT.Location, LIO[Int]] =

      Data.locations
        .foldLeft(Map[DT.Location, LIO[Int]]()) {
          case (m, loc) => m + (loc -> occupancy(readings, loc))
        }
  }
}
