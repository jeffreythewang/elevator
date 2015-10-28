package elevator

import akka.actor._
import ElevatorController._
import Elevator._

object Elevator {

  sealed trait Direction
  case object Up extends Direction
  case object Down extends Direction

  case class PickupRequest(floor: Int, direction: Direction)
  case class DropoffRequest(floor: Int)

}

class Elevator(elevatorId: Int, elevatorController: ActorRef) extends Actor {

  var currentFloor : Int = 0

  val descendingIntValue = Ordering[Int].reverse
  val belowRequests = scala.collection.mutable.SortedSet[Int]()(descendingIntValue)
  val aboveRequests = scala.collection.mutable.SortedSet[Int]()

  // Going Up state
  def goingUp : Receive = {
    case Tick => doTick(Up)
    case PickupRequest(floor, direction) => receiveActivePickup(floor, direction)
    case DropoffRequest(floor) => receiveActiveDropoff(floor)
  }

  // Going Down state
  def goingDown : Receive = {
    case Tick => doTick(Down)
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
    if (floor > currentFloor) {
      aboveRequests += floor
      elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, aboveRequests.lastOption.getOrElse(currentFloor), aboveRequests.size)
      context.become(goingUp)
    } else if (floor < currentFloor) {
      belowRequests += floor
      elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, belowRequests.lastOption.getOrElse(currentFloor), belowRequests.size)
      context.become(goingDown)
    }
    // if the floor requested is the current floor, the elevator does not move
  }

  def receiveActivePickup(floor: Int, direction: Direction) = direction match {
      case Up =>
        aboveRequests += floor
        elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, aboveRequests.lastOption.getOrElse(currentFloor), aboveRequests.size)
      case Down =>
        belowRequests += floor
        elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, belowRequests.lastOption.getOrElse(currentFloor), belowRequests.size)
      case _ => throw new Exception("Unknown direction")
  }

  def receiveActiveDropoff(floor: Int) = {
    if (floor > currentFloor) {
      aboveRequests += floor
      elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, aboveRequests.lastOption.getOrElse(currentFloor), aboveRequests.size)
    }
    else if (floor < currentFloor) {
      belowRequests += floor
      elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, belowRequests.lastOption.getOrElse(currentFloor), belowRequests.size)
    }
    // if the floor requested is the current floor, the elevator does not queue up any requests
  }

  def doTick(direction: Direction) = {
    val requestBuffer = direction match {
      case Up => aboveRequests
      case Down => belowRequests
      case _ => throw new Exception("Unknown direction")
    }

    lazy val otherBuffer = direction match {
      case Up => belowRequests
      case Down => aboveRequests
      case _ => throw new Exception("Unknown direction")
    }

    lazy val otherActionState = direction match {
      case Up => goingDown
      case Down => goingUp
      case _ => throw new Exception("Unknown direction")
    }

    if (requestBuffer.nonEmpty) {
      direction match {
        case Up => currentFloor += 1
        case Down => currentFloor -= 1
        case _ => throw new Exception("Unknown direction")
      }

      val head = requestBuffer.head
      if (currentFloor == head) {
        println(s"Opening on floor $currentFloor")
        requestBuffer -= head
        if (requestBuffer.isEmpty) {
          elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, otherBuffer.lastOption.getOrElse(currentFloor), otherBuffer.size)
          if (otherBuffer.isEmpty) {
            context.unbecome()
          } else {
            context.become(otherActionState)
          }
        } else {
          elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, requestBuffer.lastOption.getOrElse(currentFloor), requestBuffer.size)
        }
      } else {
        elevatorController ! ElevatorStatusUpdate(elevatorId, currentFloor, requestBuffer.lastOption.getOrElse(currentFloor), requestBuffer.size)
      }
    }
  }

}
