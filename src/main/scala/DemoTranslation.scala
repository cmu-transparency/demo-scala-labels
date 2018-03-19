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

/* Basic example of how to use the monadic style label tracking. */
object TranslationDemo {
  import Core._
  import HiLow._

  type LBase = HiLow
  type L = BoundedLabel[HiLow]

  val lowPC: L = BoundedLabel[LBase](Low)
  val hiPC: L  = BoundedLabel[LBase](Hi)

  val lowExec = State[L](lowPC, Policy.AllowAll)
  val hiExec = State[L](hiPC, Policy.AllowAll)

  /* Example without monadic style. */
  object Original {
    val ourStrength = 150
    val enemySightings = List(42, 10, 54, 1)
    val doAttackOrder = enemySightings.sum < ourStrength
  }

  /* Same example but using labels to annotate ourStrength with Hi
   * Security and individual enemySightings as Low security. */
  object Translated {
    val ourStrength = Labeled(hiPC, 150: BigInt)
    val enemySightings: List[Labeled[L, BigInt]] =
      List(42, 10, 54, 1).map{n => Labeled[L, BigInt](lowPC, n)}

    val doAttackOrder = for {
      us <- unlabel(ourStrength)
      them <- enemySightings.map{_.unlabel}.reduce(
        implicitly[Numeric[LIO[L, BigInt]]].plus(_,_)
      )
    } yield us > them
  }

}