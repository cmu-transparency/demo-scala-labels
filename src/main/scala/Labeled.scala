package edu.cmu.spf.lio

/* Labeled data comes with both a lower and upper bound on the label
   that is attributed to it. */

case class Labeled[L <: Label[L], T](label: L, element: T) {
  def flatMap[S](k: T => Labeled[L, S]): Labeled[L, S] = ???
  def map[S](k: T => S): Labeled[L, S] = ???

  override def toString: String = element.toString + "[" + label.toString + "]"

}

//object Labeled {
//  def apply[T, L <: Label[L]](l: L, e: T): Labeled[T, L] =
//    Labeled(l, e)
//}
