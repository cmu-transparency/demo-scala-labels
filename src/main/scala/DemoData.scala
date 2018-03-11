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

  import java.io._
  import java.nio.file.{Paths, Files}

  /* https://gist.github.com/ramn/5566596 */
  class ObjectInputStreamWithCustomClassLoader(
    fileInputStream: FileInputStream
  ) extends ObjectInputStream(fileInputStream) {
    override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
      try { Class.forName(desc.getName, false, getClass.getClassLoader) }
      catch { case ex: ClassNotFoundException => super.resolveClass(desc) }
    }
  }

  def loadOrMake[T](filename: String)(f: => T): T = {
    if (Files.exists(Paths.get(filename))) {
      println(s"$filename exists, loading")
      load(filename)
    } else {
      println(s"$filename does not exist, computing")
      val temp:T = f
      write(filename, temp)
      temp
    }
  }

  def load[T](filename: String): T = {
    val ois = new ObjectInputStreamWithCustomClassLoader(new FileInputStream(filename))
    val e = ois.readObject.asInstanceOf[T]
    ois.close
    e
  }

  def write[T](filename: String, o: T): Unit = {
    val oos = new ObjectOutputStream(new FileOutputStream(filename))
    oos.writeObject(o)
    oos.close
  }

  val labelingContext: State =
    new State(DemoLabel.bot, Policy.AllowAll)

  object Users {
    import DemoLabel.Implicits._

    val raw_users: Seq[CoreTypes.Person] = Seq(
      CoreTypes.Person(1001, "Piotr Mardziel"),
      CoreTypes.Person(1002, "Anupam Datta"),
      CoreTypes.Person(1003, "Michael Tschantz"),
      CoreTypes.Person(1004, "Sebastian Benthall"),
      CoreTypes.Person(1005, "Helen Nissenbaum")
    )

    val users: Map[Int, Labeled[L, CoreTypes.Person]] =
      loadOrMake("users.serialized") {
      raw_users.map { u =>
        (u.device_id, Core.label(u: DemoLabel, u).evalLIO(labelingContext))
      }.toMap
      }
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
