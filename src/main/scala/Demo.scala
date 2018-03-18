package edu.cmu.spf.lio.demo

import edu.cmu.spf.lio._

import org.apache.spark.sql.ForeachWriter
import org.apache.spark.sql._

import Core._
import HiLow._

object Demo extends App {

  //println(StringUtil.prettyPrint(Data.Users.users))

  import Policy._
  import Legalese._

  import DemoPolicy._
  import DemoLabel.Implicits._
  import Aliases._

  import System.Aggregator._

  Data.users.foreach { case (k, v) =>
    println(s"$k -> $v")
  }

  aggregate(Data.readings).foreach { case (k, v) =>
    println(s"$k -> $v")
  }

  val demoPolicy = deny.except(Seq(
    allow (Origin.Location ⊐ Origin.Location.bot
      and Purpose ⊑ Purpose.ClimateControl),
    allow (Purpose ⊐ Purpose.Legal)
  ))



  val specExample = allow.except(
    deny(Origin.Person ⊐ Origin.Person.bot and Purpose ⊒ Purpose.Sharing)
      .except(Seq(
        allow(Role ⊒ Role.Affiliate),
        allow(Purpose ⊒ Purpose.Legal)
      ))
  )


  println(specExample.toString)

  SparkUtil.shutdown
}
