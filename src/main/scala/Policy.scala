package edu.cmu.spf.lio

import edu.cmu.spf.lio._

import cats.Monad
import cats.implicits._

/* Policies, given a label, return Option[Boolean], indicating
 *   None        - the policy does not apply,
 *   Some(True)  - the policy applies and in makes a positive decision, or
 *   Some(False) - the policy applies but makes a negative decision.
 */
abstract class Policy[L <: Label[L]] {
  def apply(l: L): Option[Boolean]

  def >>(f: Boolean => Boolean): Policy[L] = new Policy[L] {
    def apply(l: L): Option[Boolean] = this.apply(l) >>= (r => Some(f(r)))
  }

  def not(): Policy[L] = this >> (! _)

  def and(that: Policy[L]): Policy[L] = new Policy[L] {
    def apply(l: L): Option[Boolean] =
      this.apply(l) >>= (a => that.apply(l) >>= (b => Some(a && b)))
  }

  /*
  def or(that: Policy[L]): Policy[L] = new Policy[L] {
    def apply(l: L): Boolean = this.apply(l) || that.apply(l)
  }

  def xor(that: Policy[L]): Policy[L] = new Policy[L] {
    def apply(l: L): Boolean = this.apply(l) ^ that.apply(l)
  }
   */
}

object Policy {
  /*
  def all[L <: Label[L]](these: Iterable[Policy[L]]): Policy[L] =
    new Policy[L] {
      def apply(l: L): Boolean = these.forall(_(l))
    }

  def any[L <: Label[L]](these: Iterable[Policy[L]]): Policy[L] =
    new Policy[L] {
      def apply(l: L): Boolean = these.exists(_(l))
  }
   */
  def AllowAll[L <: Label[L]] = new Policy[L] {
    def apply(l: L): Option[Boolean] = Some(true)
    override def toString = "Allow"
  }
  def DenyAll[L <: Label[L]] = new Policy[L] {
    def apply(l: L): Option[Boolean] = Some(false)
    override def toString = "Deny"
  }
  def NotApplicableAll[L <: Label[L]] = new Policy[L] {
    def apply(l: L): Option[Boolean] = None
    override def toString = "N/A"
  }

  /*
  /* Like PositiveNegative below except nested policies inherit the
   * label requirements from parent policies. */
  case class Legalese(
    val allow: Boolean,
    val positives: Iterable[Legalese],
    val negatives: Iterable[Legalese]
  ) extends Policy[L] {

    def apply(lower: DL, upper: DL): Boolean = {
      positives.exists{ p =>
        p(lower, upper)


      && negatives.forall(! _(lower, upper))
    }
  }*/
}

/*
import scala.util.parsing.combinator._
import scala.util.parsing.combinator.lexical._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.input._

trait Parsing[T] extends StandardTokenParsers with PackratParsers {
  type PT[E] = PackratParser[E]

  def getRes[T](r: ParseResult[T]): T = r match {
    case Success(res, _) => res
    case fail: NoSuccess => scala.sys.error(fail.msg)
  }

  def int: PT[Int] = numericLit ^^ { _.toInt }

  def optIter[T](lopts: List[PT[T]]): PT[T] = lopts match {
    case Nil => failure("")
    case a :: Nil => a
    case a :: rest => a ||| optIter[T](rest)
  }

  val parser: PT[T]

  def parseString(instring: String): T = {
    val tokens = new lexical.Scanner(instring)
    getRes(phrase(parser)(new PackratReader(tokens)))
  }

  def castp[T](p: Parsing[T]): PT[T] = p.parser.asInstanceOf[PT[T]]
}

object Parsing {
  // http://enear.github.io/2016/03/31/parser-combinators/

  sealed trait PolicyToken

  case class LABEL(n: String) extends PolicyToken

  case object ACCEPT extends PolicyToken
  case object DENY extends PolicyToken
  case object EXCEPT extends PolicyToken

  case object COLON extends PolicyToken

  object PolicyLexer extends RegexParsers {
    override def skipWhitespace = true
    override val whiteSpace = "[ \t\r\f]+".r

    def label: Parser[LABEL] =
      "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { str => LABEL(str) }

    def accept = "accept" ^^ (_ => ACCEPT)
    def deny = "deny" ^^ (_ => DENY)
    def except = "except" ^^ (_ => EXCEPT)

    def colon = ":" ^^ (_ => COLON)
  }

}
 */