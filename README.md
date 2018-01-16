# Lightweight Labels for Scala

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

All labels in this system are approximations of sets of concrete
values modeled as lattices with least upper bound (join or ⊔) and
greatest lower bound (meet or ⨅) operations as well as special top (⊤)
and bottom (⊥) elements that approximate every concrete value or no
concrete value, respectively. As an example, the Time label is
designed to represent moments of time. The model in the demo is able
to represent single time instances, all time instances between two
moments, all instances, and no instances:

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
labels that arise inside of label-manipulating computations.

## Basic types, as defined for the demo in DemoTypes.

* `Label` - label that tracks purpose, and three types of origin:
  person, location, time.

* `Labeled[L, T]` - labeled data of type `T`

* `LIO[L, T]` - a label-manipulating computation that returns `T`

## Policies

```scala
  val publicRooms: DemoLabel =
    new DemoLabel(location = Location("100"))

  val allowPublicRooms = (new Legalese()
    allow ⊑(publicRooms)
  )

  val allowLocationForHVAC = (new Legalese()
    allow ⊑(purpose = Purpose.climate_control)
    except ⊐(person = Origin.Person.bot)
  )
```
