package edu.cmu.spf.lio.demo

import java.time.Instant
import java.sql.Timestamp

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql._

import edu.cmu.spf.lio._

import SparkUtil._

object Data {
  import Aliases._
  import DemoLabel.Implicits._
  import DemoPolicy._
  import Policy._
  import Legalese._
  import SerialUtil._

  private object TCB_PopulateData {
    def ping: Unit = ()

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

    val rawLocations: Seq[DT.Location] = Seq(
      DT.Location(101, "Room 101"),
      DT.Location(102, "Room 102"),
      DT.Location(103, "Room 103"),
      DT.Location(104, "Room 104")
    )
    write("locations.data", rawLocations)

    val rawReadings: Seq[DT.SensorReading] = Seq(
      DT.SensorReading(2001, 1001, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2002, 1002, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2003, 1003, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2001, 1004, 1.0, Timestamp.from(Instant.now())),
      DT.SensorReading(2002, 1005, 1.0, Timestamp.from(Instant.now()))
    )
    write("readings.data", rawReadings)

    val rawSensors: Seq[DT.Sensor] = Seq(
      DT.Sensor(2001, rawLocations(0)),
      DT.Sensor(2002, rawLocations(1)),
      DT.Sensor(2003, rawLocations(2))
    )
    val labeledSensors: Map[DT.Id, Ld[DT.Sensor]] =
      rawSensors.map { s =>
        (s.sensor_id, Core.label(s.location: DL, s)
          .TCBeval(simulatedContext, dataIngressPolicy))
      }.toMap
    write("sensors.data", labeledSensors)

    def time: Ld[DT.Time] = {
      val now = DT.Time(Timestamp.from(Instant.now()))
      Core.label(now: DL, now)
        .TCBeval(simulatedContext, dataIngressPolicy)
    }
  }

  TCB_PopulateData // make sure the data files are created

  def time: Ld[DT.Time] = TCB_PopulateData.time

  import org.apache.spark.rdd.RDD

  lazy val users:     Map[DT.Id, Ld[DT.Person]] = load("users.data")
  lazy val locations: Seq[DT.Location]          = load("locations.data")
  lazy val readings:  RDD[DT.SensorReading]     = SparkUtil.rdd(load("readings.data"))
  lazy val sensors:   Map[DT.Id, Ld[DT.Sensor]] = load("sensors.data")

}
