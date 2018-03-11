# Shallow Label Computations for Scala

This library and accompanied examples demonstrate a system for label
tracking within the Scala programming language as well as flow policy
enforcement based on tracked labels. This document serves as an
overview of the system and a description of the demonstration.

## The basics

The implementation presented here is a shallow label tracking, meaning
that no changes to the Scala language are necessary; all of the
functionality is provided by a set of classes and methods implemented
in standard Scala itself.

Labels serve as the basis of the enforcement system. Labels are
special bits of data attached to standard values (and computations)
and accompany them throughout the execution of a program. A string
labeled with label type `L`, is given a type `Labeled[L, String]`.

```scala
  val secret: Labeled[L, String] = ???
```

A value that has been labeled can only be inspected in the context of
a label-aware computation modeled as a monad named here `LIO`. Though
the shallow embedding of labeled programming requires special effort
on behalf of the programmer, Scala does provide some convenient syntax
for working with monadic computations such as the `for` notation.

```scala
   val ready: LIO[L, Boolean] = for {
      actual_secret <- unlabel secret
   } yield (actual_secret == "attack at dawn")
```

The above example is a label-manipulating computation that inspects a
labeled string to check whether it is equal to "attack at dawn". It is
important to remember that defining such computations does not execute
them. Executing a label manipulating computation is performed using a
special method of the `LIO` monad.

```scala
   val actually_ready: Boolean = ready.TCBeval(...)
```

Methods such as `TCBeval` and others starting with TCB refer to trusted
invocations to be restricted to a trusted computing base (TCB). These
methods must be used correctly in order to provide the protections of
this system. The arguments to TCBeval include a policy to be
discussed further in this document.

The significance of a label varies and can include any annotation
relevant to an intended policy. The demonstration here includes labels
to represent information about purpose and several origin annotations
including person, location, and time.

```scala
   abstract class Purpose extends Label ...
   
   abstract class Origin extends Label ...
   
   abstract class Person extends Origin ...
   abstract class Location extends Origin ...
   abstract class Time extends Origin ...
```

## Labels and approximations

A data value might need to have associated with it multiple
annotations. For example an aggregation from a table whose rows are
annotated with different time instances need to indicate that the
origin of the aggregate includes all of those time instances. Tracking
distinct labels could become cumbersome if the number of such labels
grows. For this reason, labels are designed with sound approximation
in mind. 

Labels form lattices with an approximate union operation (least upper
bound, join, or ⊔) and an approximate intersection operation (greatest
lower bound, meet, or ⨅) as well as special top (⊤) and bottom (⊥)
elements that approximate universal or empty sets respectively. As an
example, the Time label is designed to represent moments of time. The
model in the demo is able to represent single time instances, all time
instances between two moments, all instances, and no instances:

```scala
  sealed abstract class Time extends Label {
    def join(b: That): Time = ...
    def meet(b: That): Time = ...
  }

  case class AtTime(t: Timestamp) extends Time ...
  case class Between(after: Timestamp, before: Timestamp) extends Time ...
  case class Always() extends Time ...
  case class Never() extends Time ...

  val ⊤: Time = Always()
  val ⊥: Time = Never()
```

The join operation over-approximates two labels into one that has to
represent at least all of the instances represented by both inputs.
The meet operation under-approximates two labels into one that
represents at most instances represented by both input labels.

Labels also come with an ordering operations ⊑ that determines whether
instances represented on the left are wholly covered by instances
represented on the right.

```scala
  def ⊑(a: Label, b: Label): Boolean = a ⊔ b == b
  def ⊒(a: Label, b: Label): Boolean = a ⨅ b == b

  def ⊏(a: Label, b: Label): Boolean = a ⊑ b && a != b
  def ⊐(a: Label, b: Label): Boolean = a ⊒ b && a != b
```

This and related ordering operations are the basis of policy
specification. Policy restrict the labels or rather the order of
labels that arise inside of label-manipulating computations (more on
policies later).

The approximation scheme is fairly general and allows designs in
support of various policy goals. For example, if business hours need
to be accurately represented for policy purposes, this can be made
precise in a Time label by including an approximate boolean indicating
such:

```scala
   case class BusinessTime(time: Time, is_business_hours: ABoolean) extends Time ...
```

The approximate boolean `ABoolean` indicates true or false as is
normal, but also both or neither. Policies that require origins to be
completely within business hours can then make use of
`is_business_hours` to make sure no non-business-hours origin has
tainted a value.

## Policies

Policies indicate the conditions under which data protected by the LIO
monad can become available outside of it. Evaluation of a protected
computation and the policy check is performed using the TCBeval
operation on an `LIO[L, T]` computation.

```scala
   def TCBeval(context: Label, policy: Policy): T
```

The method returns `T` or fails with a policy violation if the given
policy does not allow for the release of the data. The additional
`context` label provided here is intended to convey contextual
annotations such as purpose of the given release.

Policies are composed of a basic components which are just functions
that, given a label of a computation, return true to indicate the
release of that information is allowed.

```scala
  trait Policy ... {
    def apply(p: Label): Boolean = ???
  }
```

### Upper and Lower bounds

This definition above fairly general and allow one to check whether
the inferred label of a piece of data is or is not of a particular
type or has some particular feature. The goal, however, is for
policies to check whether the inferred label contains a label of
particular interest (contains in terms of an approximation that
includes it). The most convenient way of checking inclusion is using
the lattice ordering operations mentioned earlier:

```scala
  def ⊑(a: Label, b: Label): Boolean = a ⊔ b == b
  def ⊒(a: Label, b: Label): Boolean = a ⨅ b == b

  def ⊏(a: Label, b: Label): Boolean = a ⊑ b && a != b
  def ⊐(a: Label, b: Label): Boolean = a ⊒ b && a != b
```

For example, if we have an abstract boolean indicating business hours
as part, we can check that it is true, or at least not false,
`business_hours ⊒ ATrue`. This stipulates that a label assigned to a
protected computation or data needs to be assigned origins that are at
least during business hours.

Notice that in order to be sound in determining the statement above,
the system needs to track an under-approximation of the labels
assigned to a computation. That is, if we over-approximate a boolean
indicating business hours to be both true and false (and
over-approximation), the test `business_hours ⊒ ATrue` will be unsound
in that it will return true even though non-business hour origin could
have been involved. On the other hand, checking `business_hours ⊑
ATrue` soundly requires an over-approximation. Thus the system tracks
both over and under approximations of labels using both the meet and
join operations noted earlier in this document.

### Label comparison policies

The system provided a convenient way of writing policies that compare
labels. These are written using a selector that access some part of a
larger label, a comparison, and a label to compare to. In the demo,
there are many components in the overall label (type `DemoLabel`),
requiring selectors to pick out the necessary component.

```scala
implicit class LabelSelector(val select: DemoLabel => Label) {
  def ⊑(sublabel: L): Policy[...] = ???
}
```

Together with accessor methods to get at the components of labels, we
can write simply `Origin.Time.BusinessHours ⊒ ATrue` to designate a
policy describe above.

### Compound Policies

Base policies can be combined into larger compound policies that are
more convenient at specifying a real-world policy. The tool for this
provided is the Legalese policy that is composed of a set of positive
policies and a set of negative policies, with the interpretation that
the composed policy allows a release if at least one of the positives
allows it, and none of the negatives do:

```scala
class Legalese[...](
  positives: Iterable[Policy[...]],
  negatives: Iterable[Policy[...]]
  ) {

def apply(l: Label): Boolean =
   positives.exists(_(l)) && negatives.forall(! _(l))
```

Such policies can be further composed, with positives and negatives
themselves composed of further Legalese policies.

# Example Uses

```scala
  val publicRooms: Origin.Location =
      Location(Seq("100", "101", "102"))
      // TODO: add this implicit

  val allowPublicRooms = (new Legalese()
    allow (Origin.Location ⊑ publicRooms)
  )

  val allowLocationForHVAC = (new Legalese()
    allow (Purpose ⊒ Purpose.climate_control)
    except (Origin.Person ⊐ Origin.Person.bot)
  )

  val specExample = allow.except(
    deny(Origin.Person ⊐ Origin.Person.bot and Purpose ⊒ Purpose.Sharing)
      .except(Seq(
        allow(Role ⊒ Role.Affiliate),
        allow(Purpose ⊒ Purpose.Legal)
      ))
  )
```

TODO

# Reference

## Basic types, as defined for the demo in DemoTypes.

* `Label` - label that tracks purpose, and three types of origin:
  person, location, time.

* `Labeled[L, T]` - labeled data of type `T`

* `LIO[L, T]` - a label-manipulating computation that returns `T`

* `DemoLabel` - the complete label composed of various sub-labels used
  in the demo.

# TODO

* The real wifi-data is not included. Piotr is worried about including
  it in a demo since the data might be sensitive.

* The upper bound and lower bound tracking at the same time is not tested.

* The use of spark is not yet done.
