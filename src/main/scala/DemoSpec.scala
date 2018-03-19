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

object SpecDemo extends App {
  import Policy._
  import Legalese._
  import DemoPolicy._
  import DemoLabel.Implicits._

  val specExample = allow.except(
    deny(Origin.Person ⊐ Origin.Person.bot and Purpose ⊒ Purpose.Sharing)
      .except(Seq(
        allow(Role ⊒ Role.Affiliate),
        allow(Purpose ⊒ Purpose.Legal)
      ))
  )

  println("### Policy ###")
  println(specExample.toString)

}