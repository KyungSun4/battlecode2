package battlecode2;

import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	static int TREE_POS_ARR_START = 50; // the position in the broadcast array
										// where the tree positions are first
										// stored and continues till end
	static int ARCHON_COUNT_ARR = 0; // the position in the broadcast array
										// where the count of archon is stored

	static int GARDNER_COUNT_ARR = 1; // the position in the broadcast array
										// where the count of gardener is stored
	static int SOLDIER_COUNT_ARR = 2; // the position in the broadcast array
										// where the count of gardener is stored

	static int LUMBERJACK_COUNT_ARR = 3; // the position in the broadcast array
											// where the count of lumber-jack is
											// stored
	static int SCOUT_COUNT_ARR = 4; // the position in the broadcast array
									// where the count of scout is stored
	static int TANK_COUNT_ARR = 5; // the position in the broadcast array where
									// the count of tank is stored
	static int MIN_MAP_WIDTH_ARR = 6; // since float, multiply by 1000
	static int MIN_MAP_HEIGHT_ARR = 7; // since float, multiply by 1000
	static int ORIGIN_X_ARR = 8; // since float, multiply by 1000
	static int ORIGIN_Y_ARR = 9; // since float, multiply by 1000

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
		case TANK:
			runTank();
			break;
		}
	}

	static void runArchon() throws GameActionException {
		System.out.println("I'm an Archon!");
		while (true) {
			try {
				// This code is special and will only be run during round one.
				if (rc.getRoundNum() == 1) {
					MapLocation mapCenter = getMapCenter();
					MapLocation archonLocation[] = rc.getInitialArchonLocations(rc.getTeam());
					float compareDistance = 0;
					MapLocation maxDistanceArchonLocation = rc.getLocation();
					// Searches for the location that has the greatest distance
					// away
					for (MapLocation locations : archonLocation) {
						if (locations.distanceTo(mapCenter) > compareDistance) {
							compareDistance = locations.distanceTo(mapCenter);
							maxDistanceArchonLocation = locations;
						}
					}
					// If this archons location matches the location with the
					// greatest distance away, make a gardener
					if (rc.getLocation() == maxDistanceArchonLocation) {
						Direction makeRobot = nextUnoccupiedDirection(0);
						if (rc.canHireGardener(makeRobot))
							rc.hireGardener(makeRobot);
					}
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	static void runGardener() throws GameActionException {
		System.out.println("I'm an Gardner!");
		Team enemy = rc.getTeam().opponent();
		// 0-finding location 1-building tree circle
		int state = 0;
		int count = 0;
		while (true) {
			try {
				if (rc.getRoundNum() == 2) {
					rc.buildRobot(RobotType.SCOUT, nextUnoccupiedDirection(0));
				}
				MapLocation myLocation = rc.getLocation();
				if (state == 0) {
					if (tryMove(Direction.getEast(), (float) 20, 3)) {
						count++;
					}
				}
				if (count == 10) {
					state = 1;
				}
				if (state == 1) {
					maintainTreeRing();
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Gardern Exception");
				e.printStackTrace();
			}
		}
	}

	// Scout fuckery
	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		boolean busy = false;
		Direction move = Direction.getSouth().rotateLeftDegrees(45);
		Direction toTree = Direction.getEast();
		int treeID = 0;
		while (true) {
			try {
				if (!busy) {
					if (rc.canMove(move)) {
						rc.move(move);
					} else {
						move.rotateLeftDegrees(90);
						if (rc.canMove(move)) {
							rc.move(move);
						}
					}
					TreeInfo trees[] = rc.senseNearbyTrees();
					for (TreeInfo tree : trees) {
						if (tree.getContainedBullets() > 0) {
							MapLocation myLocation = rc.getLocation();
							MapLocation treeLocation = tree.getLocation();
							treeID = tree.getID();
							busy = true;
						}
						break;
						// why is is this break here, only the first tree get
						// checked then it breaks
					}
				} else if (busy) {
					if (rc.canMove(toTree)) {
						rc.move(toTree);
					} else {
						// why is a scout trying to shake??????
						Direction nextMove = nextUnoccupiedDirection(rc.getType(), (int) toTree.getAngleDegrees());
						rc.move(nextMove);
					}
					if (rc.canShake(treeID)) {
						rc.shake(treeID);
						busy = false;
					}
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}

	static void runTank() throws GameActionException {
		System.out.println("I'm an Tank!");
		Team enemy = rc.getTeam().opponent();
		while (true) {
			try {
				MapLocation myLocation = rc.getLocation();
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Tank Exception");
				e.printStackTrace();
			}
		}

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
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	static void runLumberjack() throws GameActionException {
		System.out.println("I'm an LumberJack!");
		Team enemy = rc.getTeam().opponent();
		while (true) {
			try {
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

	/*
	 * *************************************************************************
	 * *****************************************************************
	 */

	static float[] guessMapSize() {

		float w = 0;// max distnace between arcons width
		float h = 0;// max distance between arcons height
		MapLocation[] ALocs = rc.getInitialArchonLocations(Team.A);
		MapLocation[] BLocs = rc.getInitialArchonLocations(Team.B);
		// gets distances between arcons and sets w and h to the maximum
		// distances found
		for (MapLocation MLA1 : ALocs) {
			for (MapLocation MLB1 : BLocs) {
				if (Math.abs(MLA1.x - MLB1.x) > w) {
					w = Math.abs(MLA1.x - MLB1.x);
				}
				if (Math.abs(MLA1.y - MLB1.y) > h) {
					h = Math.abs(MLA1.y - MLB1.y);
				}
			}
			for (MapLocation MLA2 : ALocs) {
				if (Math.abs(MLA1.x - MLA2.x) > w) {
					w = Math.abs(MLA1.x - MLA2.x);
				}
				if (Math.abs(MLA1.y - MLA2.y) > h) {
					h = Math.abs(MLA1.y - MLA2.y);
				}
			}
		}
		float[] max = { w + RobotType.ARCHON.bodyRadius * 2, h + RobotType.ARCHON.bodyRadius * 2 };
		return max;
	}

	// Needs improvements.
	static void improveMapGuesses(MapLocation myLoc) throws GameActionException {
		int myx = (int) (myLoc.x * 1000);
		int myy = (int) (myLoc.y * 1000);
		int originx = rc.readBroadcast(ORIGIN_X_ARR);
		int originy = rc.readBroadcast(ORIGIN_X_ARR);
		int width = rc.readBroadcast(MIN_MAP_WIDTH_ARR);
		int height = rc.readBroadcast(MIN_MAP_WIDTH_ARR);
		// check if bellow origin
		if (myx < originx) {
			rc.broadcast(ORIGIN_X_ARR, myx);
		}
		// check if above height plus origin
		else if (myx > myx + width) {
			rc.broadcast(MIN_MAP_WIDTH_ARR, myx - originx);
		}
		// check if to the left of origin
		if (myy < rc.readBroadcast(ORIGIN_Y_ARR)) {
			rc.broadcast(ORIGIN_Y_ARR, myy);
		}
		// check if to the right of origin plus height
		else if (myy > myy + height) {
			rc.broadcast(MIN_MAP_HEIGHT_ARR, myy - originy);
		}
	}

	static void maintainTreeRing() throws GameActionException {
		TreeInfo[] sensedTrees = rc.senseNearbyTrees(3, rc.getTeam());
		// all trees it can water are within 3 away
		/*
		 * ArrayList<TreeInfo> mySensedTrees = new ArrayList<TreeInfo>(); //
		 * check if any of my trees died get rid of dead ones and put all living
		 * // ones in mySensedTrees for (int i = 0; i < myTrees.size(); i++) {
		 * boolean notFound = true; for (TreeInfo sensedTree : sensedTrees) { if
		 * (sensedTree.getTeam() == rc.getTeam()) { if
		 * (sensedTree.location.equals(myTrees.get(i))) {
		 * mySensedTrees.add(sensedTree); notFound = false; } } } if (notFound)
		 * { // tree must have died so remove it myTrees.remove(i); } }
		 */
		// water weakest tree
		TreeInfo weakest = null;
		for (int i = 0; i < sensedTrees.length; i++) {
			if (weakest == null || sensedTrees[i].health < weakest.health && rc.canWater(sensedTrees[i].ID)) {
				weakest = sensedTrees[i];
			}
		}
		if (weakest != null) {
			if (rc.canWater(weakest.ID)) {
				rc.water(weakest.ID);
			}
		}
		// plants 6 trees around to start. 30 degree offsets

		Direction dir = new Direction(Direction.getNorth().radians);
		for (int x = 0; x < 7; x++) {
			if (rc.canPlantTree(dir)) {
				rc.plantTree(dir);
				x = 7;
			}
			dir = dir.rotateRightRads((float) (Math.PI / 3));
			System.out.println(dir);

		}

	}

	static boolean willCollideWithMe(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();

		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and
		// we can break early
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to
		// know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and
		// intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our
		// location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return (perpendicularDist <= rc.getType().bodyRadius);
	}

	static String getMapStats() {
		// Array of archon location friendly/enemy
		MapLocation archonLocationF[] = rc.getInitialArchonLocations(rc.getTeam());
		MapLocation archonLocationE[] = rc.getInitialArchonLocations(rc.getTeam().opponent());
		for (MapLocation locationsF : archonLocationF) {
			// This means it is symmetrical across the X axis
			if (locationsF.x == archonLocationE[0].x) {
				// If it is symmetrical across X, need to find if we start on
				// the bottom or top
				if (locationsF.y < archonLocationE[0].y) {
					// Start on bottom
					System.out.println("Bottom");
					return "bottom";
				} else {
					// Start on top
					System.out.println("Top");
					return "top";
				}
			}
			// This means it is symmetrical across the Y axis
			else if (locationsF.y == archonLocationE[0].y) {
				// If it is symmetrical across Y, need to find if we start on
				// the left or right
				if (locationsF.x < archonLocationE[0].x) {
					// Start on left
					System.out.println("Left");
					return "left";
				} else {
					// Start on right
					System.out.println("Right");
					return "right";
				}
			}
		}
		// If not symmetrical on X or Y axis, assume symmetrical across the XY
		// Need to find which corner we start in
		float smallestX = archonLocationF[0].x;
		float smallestY = archonLocationF[0].y;
		// This for loop finds the smallest X and Y values for comparison
		for (MapLocation locations : archonLocationF) {
			if (locations.x < smallestX)
				smallestX = locations.x;
			if (locations.y < smallestY)
				smallestY = locations.y;
		}
		// If an enemy archon's location is smaller than our smallest location,
		// we know we are not in the bottom/left corner ect.
		boolean isBottom = true;
		boolean isLeft = true;
		for (MapLocation locations : archonLocationE) {
			if (smallestY > locations.y)
				isBottom = false;
			if (smallestX > locations.x)
				isLeft = false;
		}
		if (isBottom == true && isLeft == true)
			return "bottomLeft";
		else if (isBottom == true && isLeft == false)
			return "bottomRight";
		else if (isBottom == false && isLeft == true)
			return "topLeft";
		else if (isBottom == false && isLeft == false)
			return "topRight";
		else
			return "ERROR";
	}

	// Starts at east then rotates counter clockwise to find the next available
	// space at increments of 30 degrees.
	static Direction nextUnoccupiedDirection(int degrees) {
		Direction testDirection = Direction.getEast().rotateLeftDegrees(degrees);
		while (rc.canMove(testDirection) == false) {
			testDirection = testDirection.rotateLeftDegrees(30);
		}
		return testDirection;
	}

	static Direction nextUnoccupiedDirection(RobotType robot, int degrees) {
		Direction testDirection = Direction.getEast().rotateLeftDegrees(degrees);
		while (rc.canMove(testDirection) == false) {
			testDirection = testDirection.rotateLeftDegrees(30);
		}
		return testDirection;
	}

	static boolean tryMoveToLocation(MapLocation loc, float degreeOffset, int checksPerSide)
			throws GameActionException {
		Direction dirTo = rc.getLocation().directionTo(loc);
		return tryMove(dirTo, degreeOffset, checksPerSide);
	}

	// Method is called if an enemy is sensed
	static void attackEnemy(MapLocation enemyLocation) throws GameActionException {
		System.out.println("Attacking!");
		MapLocation myLocation = rc.getLocation();
		Direction directionToEnemy = myLocation.directionTo(enemyLocation);
		rc.fireSingleShot(directionToEnemy);
	}

	// Called if a bullet is sensed within the robots. A robot will sense nearby
	// bullets and will run this code for each one of the bullets
	static boolean ifNeedToAvoidBullet(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();
		MapLocation bulletlocation = bullet.getLocation();
		float distanceToBullet = myLocation.distanceTo(bulletlocation);
		float bulletSpeed = bullet.getSpeed();
		if (willCollideWithMe(bullet) && bulletSpeed / (distanceToBullet + rc.getType().bodyRadius) >= .5) {
			System.out.print("Need to evade bullet!");
			return true;
		}
		return false;
	}

	static void avoidBullet(BulletInfo bullet) {
		// These statements simply get info about the orientation and angles
		// between the robot and bullet
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;
		MapLocation myLocation = rc.getLocation();
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		try {
			Direction moveToAvoid;
			// Sets the direction it wants to move based on what portion of the
			// robot the bullet will hit
			if ((directionToRobot.getAngleDegrees() - propagationDirection.getAngleDegrees()) >= 0) {
				// Sets moveToAvoid a direction perpendicular to the direction
				// of the bullet
				moveToAvoid = propagationDirection.rotateLeftRads((float) Math.PI / 2);
			} else {
				moveToAvoid = propagationDirection.rotateRightRads((float) Math.PI / 2);
			}
			// Checks if the direction it wants to move is clear before moving.
			if (rc.canMove(moveToAvoid)) {
				rc.move(moveToAvoid);
			}
			// If the space it wants to move to is occupied it will use the
			// nextUnoccupiedDirection method to move
			else {
				// This if statement is to check the direction the bullet is
				// coming. You dont want to move into the path of the bullet
				if (directionToRobot.getAngleDegrees() > 180) {
					rc.move(nextUnoccupiedDirection(rc.getType(), 180));
				} else {
					rc.move(nextUnoccupiedDirection(rc.getType(), 0));
				}
			}
			System.out.println("Avoided successfully?");
		} catch (Exception e) {
			System.out.println("FAILED TO AVOID BULLET!");
			e.printStackTrace();
		}
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

	/**
	 * Gets the center of map based on inital Archon locations
	 * 
	 * @return MapLocation of center
	 */
	static MapLocation getMapCenter() {
		// gets initial locations of both teams
		MapLocation[] teamALocations = rc.getInitialArchonLocations(rc.getTeam());
		MapLocation[] teamBLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
		// stores midpoints btwn
		ArrayList<MapLocation> midPoints = new ArrayList<MapLocation>();
		// store the number of times the midpoint is found
		ArrayList<Integer> midPointCounts = new ArrayList<Integer>();
		// compares each archon from one team with the archons from the other
		// team
		for (int i = 0; i < teamALocations.length; i++) {
			for (int j = 0; j < teamBLocations.length; j++) {
				// get midpoint btwn the two archons
				MapLocation midPoint = new MapLocation(
						(teamBLocations[j].x - teamALocations[i].x) / 2 + teamALocations[i].x,
						(teamBLocations[j].y - teamALocations[i].y) / 2 + teamALocations[i].y);
				// checks if this midpoint was already recorded, if yes, add to
				// its count
				boolean alreadyFound = false;
				for (int k = 0; k < midPoints.size(); k++) {
					if ((int) (midPoint.x) == (int) (midPoints.get(k).x)
							&& (int) (midPoint.y) == (int) (midPoints.get(k).y)) {
						midPointCounts.set(k, midPointCounts.get(k) + 1);
						alreadyFound = true;
					}
				}
				// if not already found add it
				if (!alreadyFound) {
					midPoints.add(midPoint);
				}
				midPointCounts.add(1);
			}
		}
		// if there's only one midpoint then return it
		if (midPoints.size() == 1) {
			return midPoints.get(0);
		}
		// finds the midpoint with the most counts
		int withMaxCount = 0;
		for (int i = 0; i < midPoints.size(); i++) {
			if (midPointCounts.get(i) > midPointCounts.get(withMaxCount)) {
				withMaxCount = i;
			}
		}
		System.out.println(midPoints);
		System.out.println(midPoints.get(withMaxCount));
		return midPoints.get(withMaxCount);
	}
}
