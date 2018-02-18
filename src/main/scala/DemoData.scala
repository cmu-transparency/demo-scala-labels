package edu.cmu.spf.lio.demo

import java.time.Instant
import java.sql.Timestamp

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql._

import edu.cmu.spf.lio._

import SparkUtil._

object Data {
  import DemoTypes._
  import DemoLabel.Implicits._

  val labelingContext: State =
    new State(DemoLabel.bot, Policy.Allow)

  object Users {
    import DemoLabel.Implicits._

    val raw_users: Seq[CoreTypes.Person] = Seq(
      CoreTypes.Person(1001, "Piotr Mardziel"),
      CoreTypes.Person(1002, "Anupam Datta"),
      CoreTypes.Person(1003, "Michael Tschantz"),
      CoreTypes.Person(1004, "Sebastian Benthall"),
      CoreTypes.Person(1005, "Helen Nissenbaum")
    )

    val users: Map[Int, Labeled[L, CoreTypes.Person]] = raw_users.map { u =>
      (u.device_id, Core.label(u: DemoLabel, u).evalLIO(labelingContext))
    }.toMap
  }

  object Locations {
    val raw_locations: Seq[CoreTypes.Location] = Seq(
      CoreTypes.Location("101"),
      CoreTypes.Location("102"),
      CoreTypes.Location("103"),
      CoreTypes.Location("103")
    )
  }

  case class RawReading(val sensor_id: Int, device_id: Int)
  object Readings {
    val raw_readings: Seq[RawReading] = Seq(
      RawReading(1, 1001),
      RawReading(2, 1002),
      RawReading(3, 1003),
      RawReading(4, 1004),
      RawReading(5, 1005)
    )
  }

  object Sensors {
    /* Sensor information including labeled location, labeled sensor
     * type.*/

    val raw_sensors: Seq[CoreTypes.Sensor] = Seq(
      CoreTypes.Sensor(1, CoreTypes.Location("101")),
      CoreTypes.Sensor(2, CoreTypes.Location("102")),
      CoreTypes.Sensor(3, CoreTypes.Location("103"))
    )

    val sensors: Map[Int, Labeled[L, CoreTypes.Sensor]] = raw_sensors.map { s =>
      (s.sensor_id, Core.label(s.location: DemoLabel, s).evalLIO(labelingContext))
    }.toMap

  }

  object Clock {
    /* Read the time and label it with a time origin. */
    def time: Labeled[L, CoreTypes.Time] = {
      val now = CoreTypes.Time(Timestamp.from(Instant.now()))
      Core.label(now: DemoLabel, now).evalLIO(labelingContext)
    }
  }
}
