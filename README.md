Label tracking and label-based policy enforcement demo for the origin
privacy project.

# Basic types, as defined for the demo in DemoTypes.

* L - label that tracks purpose, and three types of origin: person, location, time.
* Ld[T] - labeled data of type T
* LIO[T] - a label-manipulating computation that returns T


# Policies

'''
  val publicRooms: DemoLabel =
    new DemoLabel(location = CoreTypes.Location("100"))

  val allowPublicRooms = (new Legalese()
    allow ⊑(publicRooms)
  )

  val allowLocationForHVAC = (new Legalese()
    allow ⊑(purpose = Purpose.climate_control)
    except ⊐(person = Origin.Person.bot)
  )
'''

# Labeling data

# Using labeled data

# Data egress

# Policies

