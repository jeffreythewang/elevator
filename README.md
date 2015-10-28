# Elevator Control System

I decided to use Akka actors to build the elevator system.

## Build Instructions
* run `sbt` in the root of the project directory - make sure you have sbt installed
* `run <numElevators>` - start up the system with numElevators amount of elevators
* The simulation (for sake of exercise) is run in the main function in the ElevatorInterface object. You can modify the sequence of events there to test the output (using status checks and print statements to see how things run), or build an HTTP request handler, or write unit tests.

## Implementation

My 2 actors are:
* ElevatorController
* Elevator

The system takes the following requests from a client:
* PickupRequest(floor, direction): given a floor (integer value) and direction (up/down)
* ClientDropoffRequest(elevatorId, floor): in a full implementation, the client will receive an id when s/he gets on the elevator. The client would send the destination floor request, along with the id for the elevator s/he is on, to tell the elevator which floor s/he wants to go to.
* ElevatorStatusRequest(elevatorId): the client can request for the status of an elevator, which returns the current floor, goal floor, and number of stops in the current direction
* ElevatorStatusRequestAll: returns the status (as described above) for all elevators
* Tick: perform a time step - this is considered a movement of 1 floor for all active elevators (non-zero floor requests outstanding) or 0 floors for an idle elevator (zero floor requests outstanding)

What I did not implement:
* receiving an update about the status of an elevator - this functionality did not quite make sense for me to implement. I implemented it so that elevators are constantly updating their status with the controller, and the client can receive that update whenever s/he sends a status request for a certain elevator

### Elevator Controller
This actor spawns up a number of elevators on start up, based on the number of elevators specified when the system starts. It keeps a handle for each of the elevator actors and responds to client requests. It also takes pick up requests based on a floor and direction given by a client.

When a pickup is requested, the controller iterates through all the elevator handles and delegates the closest elevator to the desired pickup floor (based on the elevator's current floor) to pick up the client. I decided to use this delegation strategy because it exploits the benefit of locale. To improve this, I would include an elevator's number of stops to calculate a busy-ness value into the equation. Also I would check the stops an elevator is going to make, and increase the delegation rating if the requested floor is in that set. However, for this implementation, it does not matter because every tick is at most 1 floor of movement.

### Elevator
This actor is spawned by the controller and consists of 3 states - idle, going up, and going down. The elevator keeps track of all the requests for floors above and below its current floor, in two OrderedSets. I used a mutable OrderedSet because floor requests are unique within the same elevator, and floors are served in some order. The aboveRequests set is in ascending order, and the belowRequests set is in descending order, because they are served in those respective orders.

If a request is made to go down when it is in the up state, that floor will be added as a below floor, and it should not move towards that floor until it switches states. The same is true vice-versa. The elevator will not change directions until all the requests towards its current direction are satisfied. Therefore, customers are served on a closest-floor-in-direction basis, and not first-come-first-serve.

States:
* idle - during a tick, the elevator will do nothing and remain on its current floor. If a pickup is requested, the elevator will go to the up/down state depending on the direction of the request. The floor in that request will be added.
* going up/down - during a tick, the elevator will go 1 floor up/down. If its current floor is equal to the head of the request set for the current direction, the floor value is removed from the set. If the set is empty, the elevator changes states. If both sets are empty, the elevator becomes idle.

Whenever either of the ordered sets, or the current floor is updated, the elevator sends a status update to the controller containing the current floor, goal floor (the last item of the respective ordered set), and the number of stops it has to make (the number of items in the respective ordered set). For this specific implementation, number of stops does not mean anything, since it stops at every floor at every tick.
