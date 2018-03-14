package edu.cmu.spf.lio

/* Labeled data comes with both a lower and upper bound on the label
   that is attributed to it. */

case class Labeled
  [L <: Label[L], T <: Serializable]
  (label: L, element: T)
    extends Serializable {

  //def flatMap[S <: Serializable](k: T => Labeled[L, S]): Labeled[L, S] = ???
  //def map[S <: Serializable](k: T => S): Labeled[L, S] = ???

  override def toString: String = element.toString + "[" + label.toString + "]"

}
