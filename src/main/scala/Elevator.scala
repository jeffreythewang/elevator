package elevator

import akka.actor._

trait ElevatorControlSystem {
  def status(): Seq[(Int, Int, Int)]
  def update(elevatorId: Int, curFloor: Int, goalFloor: Int)
  def pickup(floor: Int, direction: Int)
  def step()
}

case object Tick

class ElevatorController(numElevators: Int) extends Actor {

  val elevators = Vector.fill(numElevators)(context.actorOf(Props(new Elevator(self))))

  def receive = {
    case Tick => elevators.foreach{_ ! Tick}
  }
}

sealed trait Direction
case object Up extends Direction
case object Down extends Direction

case class PickupRequest(floor: Int, direction: Direction)
case class DropoffRequest(floor: Int)

object Elevator {
  sealed trait State
  case object Idle extends State
  case object GoingUp extends State
  case object GoingDown extends State
}

class Elevator(elevatorController: ActorRef) extends Actor {

  var currentFloor : Int = 0

  val descendingIntValue = Ordering[Int].reverse
  val belowRequests = scala.collection.mutable.SortedSet[Int]()(descendingIntValue)
  val aboveRequests = scala.collection.mutable.SortedSet[Int]()

  // Going Up state
  def goingUp : Receive = {
    case Tick => upTick()
    case PickupRequest(floor, direction) => receiveActivePickup(floor, direction)
    case DropoffRequest(floor) => receiveActiveDropoff(floor)
  }

  // Going Down state
  def goingDown : Receive = {
    case Tick => downTick()
    case PickupRequest(floor, direction) => receiveActivePickup(floor, direction)
    case DropoffRequest(floor) => receiveActiveDropoff(floor)

  }

  // Idle State
  def receive = {
    case Tick => // do nothing / stay on the same floor if no requests
    case PickupRequest(floor, direction) => idleRequest(floor)
    case DropoffRequest(floor) => idleRequest(floor) // when someone doesn't get off the elevator when they are supposed to
  }

  def idleRequest(floor: Int) = {
    if (floor >= currentFloor) {
      aboveRequests += floor
      context.become(goingUp)
    } else {
      belowRequests += floor
      context.become(goingDown)
    }
  }

  def receiveActivePickup(floor: Int, direction: Direction) = direction match {
      case Up => aboveRequests += floor
      case Down => belowRequests += floor
  }

  def receiveActiveDropoff(floor: Int) = {
    if (floor >= currentFloor) {
      aboveRequests += floor
    }
    else {
      belowRequests += floor
    }
  }

  def upTick() = {
    if (aboveRequests.nonEmpty) {
      if (currentFloor == aboveRequests.head) {

      } else {
        currentFloor += 1
      }
    }
  }

  def downTick() = {
    if (belowRequests.nonEmpty) {
      if (currentFloor == belowRequests.head) {

      } else {
        currentFloor -= 1
      }
    }
  }
}