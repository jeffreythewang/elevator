package elevator

import akka.actor._
import ElevatorController._
import Elevator._

case object Tick

object ElevatorController {

  case class ClientDropoffRequest(elevatorId: Int, floor: Int)
  case class ElevatorHandle(elevatorId: Int, actorRef: ActorRef, curFloor: Int, goalFloor: Int, numStops: Int)
  case class ElevatorStatusRequest(elevatorId: Int)
  case object ElevatorStatusRequestAll
  case class ElevatorStatusUpdate(elevatorId: Int, curFloor: Int, goalFloor: Int, numStops: Int)

}

class ElevatorController(numElevators: Int) extends Actor {

  val elevators = scala.collection.mutable.Map[Int, ElevatorHandle]()

  override def preStart() = for (elevatorId <- 0 until numElevators) {
    elevators += elevatorId -> ElevatorHandle(elevatorId, context.actorOf(Props(new Elevator(elevatorId, self))), 0, 0, 0)
  }

  def receive = {
    case Tick => elevators.values.foreach{_.actorRef ! Tick}
    case PickupRequest(floor, direction) => delegatePickup(floor, direction)
    case ClientDropoffRequest(elevatorId, floor) => elevators(elevatorId).actorRef ! DropoffRequest(floor)
    case ElevatorStatusUpdate(elevatorId, curFloor, goalFloor, numStops) => updateStatus(elevatorId, curFloor, goalFloor, numStops)
    case ElevatorStatusRequestAll =>
      for (elevatorId <- 0 until numElevators) {
        self ! ElevatorStatusRequest(elevatorId)
      }
    case ElevatorStatusRequest(elevatorId) =>
      val elevator = elevators(elevatorId)
      println(s"Elevator: $elevatorId - currently on floor ${elevator.curFloor} - heading towards floor ${elevator.goalFloor} - with ${elevator.numStops} stops")
  }


  // Selects the elevator to be sent to pick up a client
  // This determines which elevator is the closest to the floor at which the request is made (based on current floor in a given direction)
  def delegatePickup(floor: Int, direction: Direction) = {
    var closestElevator = elevators.getOrElse(0, ElevatorHandle(0, null, 0, 0, 0))
    elevators.values.foreach { elevator =>
      direction match {
        case Up =>
          if (elevator.curFloor - elevator.goalFloor <= 0) {
            // For elevators currently going up
            if (elevator.curFloor <= floor && elevator.curFloor > closestElevator.curFloor) {
              closestElevator = elevator
            }
          }
        case Down =>
          if (elevator.curFloor - elevator.goalFloor >= 0) {
            // For elevators currently going down
            if (elevator.curFloor >= floor && elevator.curFloor < closestElevator.curFloor) {
              closestElevator = elevator
            }
          }
      }
    }
    closestElevator.actorRef ! PickupRequest(floor, direction)
  }

  def updateStatus(elevatorId: Int, curFloor: Int, goalFloor: Int, numStops: Int) = {
    val newElevator = elevators(elevatorId).copy(curFloor = curFloor, goalFloor = goalFloor, numStops = numStops)
    elevators += (elevatorId -> newElevator)
  }
}

object ElevatorInterface {

  def main(args: Array[String]) = {
    if (args(0).toInt < 1 || args(0).toInt > 16) {
      throw new Exception("Please choose between 1 and 16 actors")
    }
    val numElevators = args(0).toInt
    val system = ActorSystem("main")
    val elevatorController = system.actorOf(Props(new ElevatorController(numElevators)), name = "controller")

    statusAll(elevatorController)
    pickup(elevatorController, 5, Up)
    tick(elevatorController)
    pickup(elevatorController, 3, Up)
    tick(elevatorController)
    pickup(elevatorController, 1, Down)
    tick(elevatorController)
    statusAll(elevatorController)
    tick(elevatorController)
  }

  def tick(elevatorController: ActorRef) = {
    Thread.sleep(50)
    elevatorController ! Tick
  }

  def statusAll(elevatorController: ActorRef) = {
    elevatorController ! ElevatorStatusRequestAll
  }

  def statusById(elevatorController: ActorRef, elevatorId: Int) = {
    elevatorController ! ElevatorStatusRequest(elevatorId)
  }

  def pickup(elevatorController: ActorRef, floor: Int, direction: Direction) = {
    elevatorController ! PickupRequest(floor, direction)
  }

  def dropoff(elevatorController: ActorRef, elevatorId: Int, floor: Int) = {
    elevatorController ! ClientDropoffRequest(elevatorId, floor)
  }
}