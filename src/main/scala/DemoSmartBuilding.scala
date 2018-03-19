package edu.cmu.spf.lio.demo

import java.time.Instant
import java.sql.Timestamp

import java.io.Serializable

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

import Aliases._
import Core.unlabel
import Core.label

object SmartBuilding {

  object Data {
    import DemoLabel.Implicits._
    import DemoPolicy._
    import Policy._
    import Legalese._
    import SerialUtil._

    val dataIngressPolicy = deny.except(Seq(
      allow(Role ⊒ Role.Administrator
        and Purpose ⊒ Purpose.Storage)
    ))
    val simulatedContext: DL = (Purpose.Storage: DL) ⊔ Role.Administrator

    val rawUsers: Seq[DT.Person] = Seq(
      DT.Person(1001, "Piotr Mardziel"),
      DT.Person(1002, "Anupam Datta"),
      DT.Person(1003, "Michael Tschantz"),
      DT.Person(1004, "Sebastian Benthall"),
      DT.Person(1005, "Helen Nissenbaum")
    )
    val labeledUsers: Map[DT.Id, Ld[DT.Person]] =
      rawUsers.map { u =>
        (u.device_id, Core.label(u: DL, u).TCBeval(
          simulatedContext,
          dataIngressPolicy
        )) }.toMap
        write("users.data", labeledUsers)

    val secretRoom = DT.Location(105, "Super Secret Room 105")

    val rawLocations: Seq[DT.Location] = Seq(
      DT.Location(101, "Room 101"),
      DT.Location(102, "Room 102"),
      DT.Location(103, "Room 103"),
      DT.Location(104, "Room 104"),
      secretRoom
    )
    write("locations.data", rawLocations)

    val rawReadings: Seq[DT.SensorReading] = Seq(
      DT.SensorReading(2001, 1001, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2002, 1002, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2003, 1003, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2001, 1004, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2005, 1005, 1.0, Timestamp.from(Instant.now()))
    )
    write("readings.data", rawReadings)

    val rawSensors: Array[DT.Sensor] = Array[DT.Sensor](
      DT.Sensor(2001, rawLocations(0)),
      DT.Sensor(2002, rawLocations(1)),
      DT.Sensor(2003, rawLocations(2)),
      DT.Sensor(2004, rawLocations(3)),
      DT.Sensor(2005, secretRoom)
    )
    val labeledSensors: Map[DT.Id, Ld[DT.Sensor]] =
      rawSensors.map { s =>
        (s.sensor_id, Core.label(s.location: DL, s)
          .TCBeval(simulatedContext, dataIngressPolicy))
      }.toMap
    write("sensors.data", labeledSensors)

    val labeledSensorsByLocation: Map[DT.Location, Ld[Array[DT.Sensor]]] =
      rawLocations.map { loc =>
        val sensors: Array[DT.Sensor] =
          rawSensors.filter{_.location == loc}.toArray.asInstanceOf[Array[DT.Sensor]]
        val temp1 = label[DL with Label[DL], Array[DT.Sensor]](loc: DemoLabel, sensors)
        val temp2 = temp1.TCBeval(simulatedContext, dataIngressPolicy)
        (loc, temp2)
      }.toMap[DT.Location, Ld[Array[DT.Sensor]]]

    write("sensorsByLocation.data", labeledSensorsByLocation)

    def time: Ld[DT.Time] = {
      val now = DT.Time(Timestamp.from(Instant.now()))
      Core.label(now: DL, now)
        .TCBeval(simulatedContext, dataIngressPolicy)
    }

    lazy val users:     Map[DT.Id, Ld[DT.Person]] = load("users.data")
    lazy val locations: Seq[DT.Location]          = load("locations.data")
    lazy val readings:  RDD[DT.SensorReading] =
      SparkUtil.rdd(load("readings.data"))
    lazy val sensors:   Map[DT.Id, Ld[DT.Sensor]] = load("sensors.data")
    lazy val sensorsByLocation: Map[DT.Location, Core.Labeled[DemoLabel, Array[DemoTypes.Sensor] with Serializable]] =
      load("sensorsByLocation.data")
  }

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
      location: DT.Location): LIO[BigInt] = {

      Core.LIO.mapRDD(readings) {
        case reading => for {
          loc <- Locator.locate_sensor(reading.sensor_id)
        } yield if (loc == location) { 1: BigInt } else { 0: BigInt }
      }.reduce(implicitly[Numeric[LIO[BigInt]]].plus)
    }

    def occupancy_better(
      readings: RDD[DT.SensorReading],
      location: DT.Location): LIO[BigInt] = {

      val sensorsLIO = Data.sensorsByLocation(location)

      Core.LIO.mapRDD[DL, DT.SensorReading, BigInt](readings) {
        reading: DT.SensorReading => for {
          sensors: Array[DT.Sensor] <- unlabel(sensorsLIO)
          temp = sensors.filter{s => s.sensor_id == reading.sensor_id}.length > 0 
        } yield if (temp) { 1 } else { 0 }
      }.reduce(implicitly[Numeric[LIO[BigInt]]].plus(_,_))
    }

    /* Produce an occupancy table for every location. */
    def aggregate(readings: RDD[DT.SensorReading]):
        Map[DT.Location, LIO[BigInt]] =
      Data.locations.foldLeft(Map[DT.Location, LIO[BigInt]]()) {
        case (m, loc) => m + (loc -> occupancy_better(readings, loc))
      }
  }
}

/* Smart building demo. */
object SmartBuildingDemo extends App {
  import Policy._
  import Legalese._
  import DemoPolicy._
  import DemoLabel.Implicits._

  import SmartBuilding._
  import Aggregator._

  val debugPolicy: Legalese[DL] = allow
  val debugContext = DemoLabel.bot

  println("### Location occupancy ###")
  aggregate(Data.readings).foreach { case (k, comp) =>
    val num = (comp >>= label[DL,BigInt]).TCBeval(debugContext, debugPolicy)
    println(s"$k -> $num")
  }

  println("### Policy over smart building systems ###")
  val buildingPolicy = deny.except(Seq(
    allow (Origin.Location ⊐ Origin.Location.bot
      and Purpose ⊑ Purpose.ClimateControl).except(
        deny(Origin.Location ⊒ Origin.Location(Data.secretRoom))
    ),
    allow (Purpose ⊐ Purpose.Legal)
  ))
  println(buildingPolicy.toString)

  /* Controller of HVAC systems in building. */
  object HVAC {
    val purpose = Purpose.ClimateControl

    def run {
      println("### Running HVAC status update for each location ###")

      /*
       Compute the number of users at each location.
       */

      val isOccupied: Map[DT.Location,LIO[Boolean]] =
      aggregate(Data.readings).mapValues{lnum =>
        for {
          num <- lnum
        } yield num > 0
      }

      /*
       Determine whether the HVAC system should be on in each
       location. It is turned on if that location has non-zero
       occupancy. Policy might not allow checking this for all
       locations.
       */

      isOccupied.foreach { case (l, locc) =>
        try {
          val location_occupied = locc.TCBeval(purpose, buildingPolicy)
          println(s"  room $l, HVAC ON = $location_occupied")
        } catch {
          case e: Core.IFCException =>
            println(s"  room $l, cannot access occupancy due to policy")
        }
      }
    }
  }

  HVAC.run

  SparkUtil.shutdown
}
