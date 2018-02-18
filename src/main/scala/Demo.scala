package edu.cmu.spf.lio.demo

import edu.cmu.spf.lio._

import org.apache.spark.sql.ForeachWriter
import org.apache.spark.sql._

import Core._
import HiLow._

object Demo extends App {

  //println(StringUtil.prettyPrint(Data.Users.users))

  import DemoTypes._

  import System.Aggregator._

  Data.Users.users.foreach { case (k, v) =>
    println(s"$k -> $v")
  }

  aggregate(readings).foreach { case (k, v) =>
    println(s"$k -> $v")
  }

  object HVAC {
    val readings = Data.Readings.raw_readings

    /* Compute activation of the HVAC system for each room. HVAC is
     * activated if there is at least one person in the room. */
    val activity: Map[CoreTypes.Location, LIO[Boolean]] =
      System.Aggregator.aggregate(readings).mapValues { occupancyM =>
        for {
          occupancy <- occupancyM
        } yield occupancy > 0
      }
/*
    val policy = (new Legalese()
      deny ()
      except (
        Origin.Location ⊑ Origin.Location.⊤ and
        Purpose ⊑ Purpose.climate_control
      )
 */
  }

  //val sensors = System.Sensors
  //val timer = System.Timer

  /*
  timer.stream.writeStream.foreach { new ForeachWriter[Row] () {
    def process(r: Row): Unit = {
      println(r)
    }
    def open(partitionId: Long, version: Long): Boolean = true
    def close(errorOrNull: Throwable): Unit = ()
  } }.start.awaitTermination
   */
  //.foreach{ row =>
  //    println(row)
  //  }

}
