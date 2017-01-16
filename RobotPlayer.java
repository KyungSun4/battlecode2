package battlecode2;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static int TREE_POS_ARR_START = 50;
	static RobotController rc;

	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		switch (rc.getType()) {
		case ARCHON:
			runArchon();
			break;
		case GARDENER:
			runGardener();
			break;
		case SOLDIER:
			runSoldier();
			break;
		case LUMBERJACK:
			runLumberjack();
			break;
		case SCOUT:
			runScout();
			break;
		}
	}

	static void runArchon() throws GameActionException {
		System.out.println("I'm an Archon!");
		int numOfGardeners = rc.readBroadcast(1);
		while (true) {
			try {
				if (rc.readBroadcast(1) == 0) {
					rc.hireGardener(Direction.getNorth());
					rc.broadcast(1, numOfGardeners++);
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	static void runGardener() throws GameActionException {
		System.out.println("I'm a gardener!");
		while (true) {
			try {

				Clock.yield();
			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	static void runScout() throws GameActionException {
		System.out.println("I'm an Scout!");
	}

	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
		Team enemy = rc.getTeam().opponent();
		while (true) {
			try {
				MapLocation myLocation = rc.getLocation();
				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
				// If there are some...
				if (robots.length > 0) {
					// And we have enough bullets, and haven't attacked yet this
					// turn...
					if (rc.canFireSingleShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
					}
				}

				// Move randomly
				tryMove(randomDirection());

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	static void runLumberjack() throws GameActionException {
		System.out.println("I'm a lumberjack!");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this
		// loop
		while (true) {
			// Try/catch blocks stop unhandled exceptions, which cause your
			// robot to explode
			try {
				// See if there are any enemy robots within striking range
				// (distance 1 from lumberjack's radius)
				RobotInfo[] robots = rc.senseNearbyRobots(
						RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

				if (robots.length > 0 && !rc.hasAttacked()) {
					// Use strike() to hit all nearby robots!
					rc.strike();
				} else {
					// No close robots, so search for robots within sight radius
					robots = rc.senseNearbyRobots(-1, enemy);

					// If there is a robot, move towards it
					if (robots.length > 0) {
						MapLocation myLocation = rc.getLocation();
						MapLocation enemyLocation = robots[0].getLocation();
						Direction toEnemy = myLocation.directionTo(enemyLocation);

						tryMove(toEnemy);
					} else {
						// Move Randomly
						tryMove(randomDirection());
					}
				}

				// Clock.yield() makes the robot wait until the next turn, then
				// it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

	static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

		// First, try intended direction
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
		boolean moved = false;
		int currentCheck = 1;

		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}

	static boolean willCollideWithMe(BulletInfo bullet) {
		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		MapLocation myLocation = rc.getLocation();
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and
		// we can break early
		if (Math.abs(theta) > Math.PI / 2)
			return false;

		// distToRobot is our hypotenuse, theta is our angle, and we want to
		// know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and
		// intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our
		// location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh
																					// cah
																					// toa
																					// :)

		return (perpendicularDist <= rc.getType().bodyRadius);
	}

	// Rough outline of avoiding bullets. Does not account for collisions.
	static void avoidBullet(BulletInfo bullet) {
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;
		MapLocation myLocation = rc.getLocation();
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		try {
			Direction moveToAvoid;
			// Sets the direction it wants to move based on the orientation to
			// the bullet
			if ((directionToRobot.getAngleDegrees() - propagationDirection.getAngleDegrees()) >= 0) {
				moveToAvoid = propagationDirection.rotateLeftRads((float) Math.PI / 2);
			} else {
				moveToAvoid = propagationDirection.rotateRightRads((float) Math.PI / 2);
			}
			// Checks if the direction it wants to move is clear before moving.
			// If false, it will check if it can move backwards
			// If that is also false it will move towards the bullet in attempt
			// to make room to move away next turn
			if (rc.canMove(moveToAvoid)) {
				rc.move(moveToAvoid);
			}
			// In the future make a method to detect the next available
			// rc.move() direction to replace these.
			else if (rc.canMove(propagationDirection)) {
				rc.move(propagationDirection);
			} else {
				rc.move(propagationDirection.rotateLeftRads((float) Math.PI));
			}
			// Possibly replace with the tryMove method?^^
		} catch (Exception e) {
			System.out.println("bulletAvoid Exception");
			e.printStackTrace();
		}
	}

	// Returns true if the coast is clear
	static boolean friendlyFire() {
		return true;
	}

	// Called if an enemy robot sensed
	// A couple of ways to do this, you could ask for the id of the robot as a
	// param rather than location
	// Create an algorithm to take into account robots trying to dodge bullets?
	static void attackEnemy(MapLocation enemyLocation) throws GameActionException {
		MapLocation myLocation = rc.getLocation();
		Direction directionToEnemy = myLocation.directionTo(enemyLocation);
		rc.fireSingleShot(directionToEnemy);
	}

	static boolean enemyDetected() {
		return false;
	}

	static Direction directionToClosestEnemy() {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots();
		MapLocation myLocation = rc.getLocation();
		MapLocation enemyLocation = nearbyEnemies[0].location;
		// Sets the initial shortest distance to the first spot in the array
		Float shortestDistance = myLocation.distanceTo(enemyLocation);
		Direction directionToEnemy = myLocation.directionTo(enemyLocation);
		for (int i = 1; 1 < nearbyEnemies.length; i++) {
			enemyLocation = nearbyEnemies[i].location;
			Float tempDistance = myLocation.distanceTo(enemyLocation);
			if (tempDistance < shortestDistance) {
				shortestDistance = tempDistance;
				directionToEnemy = myLocation.directionTo(nearbyEnemies[i].location);
			}
		}
		return directionToEnemy;
	}

	/**
	 * the scouts move method
	 * 
	 */
	static void scoutMove() {
		// if nessesary, evade
		// keep view distnace away from other scouts
		// scan scouts in range
		// scan map and update info
	}

	/**
	 * All tree locations will be stored in main message array, this method will
	 * takein all the trees within the sight and if a tree in the ist that was
	 * in that radius is no longer there, remove it. if a new tree is found, it
	 * will add it to the list
	 */
	static void updateTreesInMessArr() {
		MapLocation myLoc = rc.getLocation();
		TreeInfo[] trees = rc.senseNearbyTrees();
		for (TreeInfo t : trees) {
			
		}
	}

	/**
	 * adds tree to Message array
	 * @throws GameActionException 
	 * 
	 */
	static boolean addTreeToList(float x,float y)  {
		for(int i = TREE_POS_ARR_START; i<997; i+=3) {
			
			if(rc.readBroadcast(i)==0) {
				rc.broadcast(i, (int)(x*100));
				return true;
			}
		}
		return false;
	}

	/**
	 * removes tree from Message array
	 * 
	 */
	static void removeTreeToList() {
	}

	/**
	 * uses Initial arcon locations to guess map size
	 * 
	 */
}
