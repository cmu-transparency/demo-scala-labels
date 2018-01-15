A purely dynamic version of the label tracking for scala.

Tracks labels to sinks, enforcing policies relating which labels can
flow into which sinks. The system makes use of two core elements: data
labels (of type Label), and computation labels (monads of type LIO).
Protected data is tracked along with its label while computations that
make use of labeled data work in the LIO context.

# Labeling data

val secret_labeler: LIO[Labeled[String]] =
  "attack at dawn".label(HiLow.Hi, secret)

val secret_labeled: Labeled[String] =
   secret_labeler.evalLIO(LIO.initialState())

# Using labeled data

val user: LIO[Unit] = new LIO { s =>
           
  }         
}

# Data egress

# Policies

