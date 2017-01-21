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
	// can send up to 4 requests, each requires 3 spots
	static int LUMBERJACK_REQUESTS_START = 20;
	static int LUMBERJACK_REQUESTS_END = 35;

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
			//runSoldier();
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
		System.out.println("I'm an archon!");
		while (true) {
			try {
				if (rc.getRoundNum() == 1) {
					MapLocation startingArchon = assignStartingArchon();
					rc.broadcast(0, (int)startingArchon.x);
					rc.broadcast(1, (int)startingArchon.y);
					// If this robot is in the same location as the assigned starting archon's location, save this robot's ID to the array
					int xCoordinate = rc.readBroadcast(0);
					int yCoordinate = rc.readBroadcast(1);
					if ((int)rc.getLocation().x == xCoordinate && (int)rc.getLocation().y == yCoordinate) {
						rc.hireGardener(nextUnoccupiedDirection(RobotType.GARDENER, 0));
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
		int state = 0; // 0-finding location 1-building tree circle
		int count = 0;
		Direction move = randomDirection();
		while (true) {
			try {
				if (rc.getRoundNum() == 2) {
					rc.buildRobot(RobotType.SCOUT, nextUnoccupiedDirection(0));
				}
				MapLocation myLocation = rc.getLocation();
				if (state == 0) {
					if (tryMove(randomDirection(), (float) 20, 5)) {
						count++;
					}
				}
				if (count == 10) {
					state = 1;
				}
				if (state == 1) {
					// gets all neutralTrees that could be in the way
					TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 3, Team.NEUTRAL);
					// request lumberJacks for each
					for (TreeInfo tree : neutralTrees) {
						// request number of lumberjacks based on tree health
						requestLumberJack(tree, 1 + (int) (tree.health / 41));
					}
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
		// 0 search and shake
		int mode = 0;
		Direction move = Direction.getSouth().rotateLeftDegrees(45);
		Direction toTree = Direction.getEast();
		Direction randomDir = randomDirection();
		int treeID = 0;
		while (true) {
			try {
				//
				if (mode == 0) {
					if (!shakeTrees(rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL))) {
						avoidBullets(rc.senseNearbyBullets());
						if (rc.canMove(randomDir)) {
							rc.move(randomDir);
						} else {
							randomDir = randomDirection();
						}
					}
				}
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
	/*
	 * 
	 * static void runSoldier() throws GameActionException {
	 * System.out.println("I'm an soldier!"); Team enemy =
	 * rc.getTeam().opponent(); while (true) { try { MapLocation myLocation =
	 * rc.getLocation(); // See if there are any nearby enemy robots RobotInfo[]
	 * robots = rc.senseNearbyRobots(-1, enemy); // If there are some... if
	 * (robots.length > 0) { // And we have enough bullets, and haven't attacked
	 * yet this // turn... if (rc.canFireSingleShot()) { // ...Then fire a
	 * bullet in the direction of the enemy.
	 * rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location)); }
	 * Clock.yield(); } else { int movementRange[] = new int[2]; //possible
	 * range of movement in degrees //if(tryMoveToLocation())
	 * switch(getMapStats()) { case "bottom": movementRange[0] = 0;
	 * movementRange[1] = 180; break; case "top": movementRange[0] = 180;
	 * movementRange[1] = 360; break; case "left": movementRange[0] = -90;
	 * movementRange[1] = 90; break; case "right": movementRange[0] = 90;
	 * movementRange[1] = 270; break; case "bottomLeft": movementRange[0] = -45;
	 * movementRange[1] = 135; break; case "bottomRight": movementRange[0] = 45;
	 * movementRange[1] = 225; break; case "topLeft": movementRange[0] = -135;
	 * movementRange[1] = 45; break; case "topRight": movementRange[0] = 135;
	 * movementRange[1] = 315; break; } int degreeMove =
	 * parseInt((movementRange[1] - movementRange[0]) * Math.random()) +
	 * movementRange[0]; float radianMove = (float) ((degreeMove/360) * Math.PI
	 * * 2); tryMove(new Direction(radianMove), (float) 10, 5); } } catch
	 * (Exception e) { System.out.println("Soldier Exception");
	 * e.printStackTrace(); } } }
	 */

	static void runLumberjack() throws GameActionException {
		System.out.println("I'm an LumberJack!");
		Team enemy = rc.getTeam().opponent();

		MapLocation tree = null;
		while (true) {
			try {
				TreeInfo[] nearByTrees = rc.senseNearbyTrees();
				// gets what tree it should look for

				if (tree != null) {
					if (1 == fufuilLumberJackRequest(tree, nearByTrees)) {
						tree = null;
					}

				} else {
					tree = getLumberJackRequest();
				}

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
	/**
	 * uses intial archon locations to guess the map size
	 * 
	 * @author John
	 * @return
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
	/**
	 * uses map location of robot to imrove the inital guess
	 * 
	 * @author John
	 * @param myLoc
	 * @throws GameActionException
	 */
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

	/**
	 * maintains a flower tree around a gardner
	 * 
	 * @author John
	 * @throws GameActionException
	 */
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
			// System.out.println(dir);

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

	/**
	 * avoids bullets, the array comes pre sorted by closest, so if it can it
	 * will avoid the closest should be made more effcient by includign return
	 * for avoidBullet to tell if it was succeful and if so end loop
	 */
	static void avoidBullets(BulletInfo[] bullets) {
		for (BulletInfo bullet : bullets) {
			avoidBullet(bullet);
		}
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

	// returns true if move was performed, false if not performed
	static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

		// First, try intended direction
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
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
	 * @author John
	 */
	static MapLocation johnsgetMapCenter() {
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

	/**
	 * Returns a random Direction
	 * 
	 * @return a random Direction
	 */
	static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	/**
	 * writes to Broadcast Array to request a lumberjack
	 * 
	 * @return returns false if fails (no more spots to request, should try
	 *         again next round)
	 * @throws GameActionException
	 * @author John
	 */
	static boolean requestLumberJack(TreeInfo tree, int NumLumberJacks) throws GameActionException {
		for (int i = LUMBERJACK_REQUESTS_START; i <= LUMBERJACK_REQUESTS_END; i += 3) {

			// if empty spot in array, add
			if (rc.readBroadcast(i) == 0) {
				rc.broadcast(i, NumLumberJacks);
				rc.broadcast(i + 1, (int) (tree.getLocation().x * 1000));
				rc.broadcast(i + 2, (int) (tree.getLocation().y * 1000));
				return true;
			}
		}
		return false;
	}

	/**
	 * when a lumberjack is not busy it should run this method to find a task (
	 * a tree to chop down)
	 * 
	 * @return returns map Location of tree if no requests, returns null
	 * @throws GameActionException
	 * @author John
	 */
	static MapLocation getLumberJackRequest() throws GameActionException {
		int x = 0;
		int y = 0;
		int pos = LUMBERJACK_REQUESTS_START;
		double minDist = 1000000000;
		for (int i = LUMBERJACK_REQUESTS_START; i <= LUMBERJACK_REQUESTS_END; i += 3) {
			if (rc.readBroadcast(i) > 0) {
				int treex = rc.readBroadcast(i + 1);
				int treey = rc.readBroadcast(i + 2);
				double dist = Math.pow(rc.getLocation().x - treex, 2) + Math.pow(rc.getLocation().y - treey, 2);
				if (dist < minDist) {
					x = treex;
					y = treey;
					pos = i;
					minDist = dist;
				}
			}
		}
		if (minDist == 1000000000) {
			return null;
		} else {
			rc.broadcast(pos, rc.readBroadcast(pos) - 1); // return new
			return new MapLocation((float) (x / 1000.0), (float) (y / 1000.0));
		}
	}

	/**
	 * moves scout to initial archon location and looks for gardener
	 * 
	 * @param robots
	 * @throws GameActionException
	 * @author John
	 */
	static int scoutLookForGardners(RobotInfo[] robots) throws GameActionException {
		tryMoveToLocation(rc.getInitialArchonLocations(rc.getTeam().opponent())[0], (float) 20, 2);
		int gardnersFound = 0;
		for (RobotInfo r : robots) {
			if (r.getType() == RobotType.GARDENER && r.getTeam() == rc.getTeam().opponent()) {
				gardnersFound++;
			}
		}
		return gardnersFound;
	}

	/**
	 * attacks garder, used for scout to attack in begining preventing other
	 * teams production
	 * 
	 * @param robots
	 * @return
	 * @throws GameActionException
	 * @author John
	 */
	static boolean attackGardner(RobotInfo[] robots) throws GameActionException {
		MapLocation gardnerLocation = null;
		for (RobotInfo r : robots) {
			if (r.getType() == RobotType.GARDENER && r.getTeam() == rc.getTeam().opponent()) {
				gardnerLocation = r.getLocation();
			}
		}
		if (gardnerLocation == null) {
			return false;
		}
		attackEnemy(gardnerLocation);
		return true;
	}

	/**
	 * 
	 * @param tree
	 * @param trees
	 * @return 0 continue choping tree 1 cant find tree 2 cant move
	 * @throws GameActionException
	 * @author John
	 */
	static int fufuilLumberJackRequest(MapLocation tree, TreeInfo[] trees) throws GameActionException {
		boolean found = false;
		for (TreeInfo t : trees) {
			if (Math.round(tree.x * 100) == Math.round(t.getLocation().x)) {
				found = true;
			}
		}
		if (found) {
			if (rc.canChop(tree)) {
				rc.canChop(tree);
				return 0;
			} else if (!tryMoveToLocation(tree, 20, 5)) {
				return 2;
			}
			return 0;
		} else {
			if (tree.distanceTo(rc.getLocation()) < rc.getType().sensorRadius) {
				return 1;
			}
			if (!tryMoveToLocation(tree, 20, 5)) {
				return 2;
			}
			return 0;
		}
	}
	

	/**
	 * sends scouts to find and harvest bullets
	 * 
	 * @author John
	 * @param trees
	 * @throws GameActionException
	 */
	static boolean shakeTrees(TreeInfo[] trees) throws GameActionException {
		// find closet tree with bullets
		TreeInfo closest = null;
		float dist = 1000000000;
		// if there are no trees detected return false
		if (trees.length == 0) {
			return false;
		} else {

		}
		// for each detected tree
		for (TreeInfo tree : trees) {
			// if it contains bullets
			if (tree.containedBullets > 0) {
				// if no tree has yet been found with bullets set closest and
				// dist
				if (closest == null) {
					closest = tree;
					dist = closest.getLocation().distanceTo(rc.getLocation());
				} else {
					// otherwise see if it is closer the chosen one
					float testDist = tree.getLocation().distanceTo(rc.getLocation());
					if (testDist < dist) {
						closest = tree;
						dist = testDist;
					}
				}
			}
		}
		// if no trees with bullets return false
		if (closest == null) {
			return false;
		}
		// try and shake tree
		if (rc.canShake(closest.getLocation())) {
			rc.shake(closest.getLocation());
		} else {
			tryMoveToLocation(closest.getLocation(), 10, 3);
		}
		return true;
	}

	// converts bullets to victory points
	static void convertVictoryPoints() throws GameActionException {
		System.out.println("bullet" + rc.getTeamBullets());
		// if can win spend all bullets
		if (rc.getTeamBullets() / 10 >= GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints()) {
			rc.donate(rc.getTeamBullets());
		}
		if (rc.getTeamBullets() > 500) {
			float teamBullets = rc.getTeamBullets();
			float excessBullets = teamBullets - 500;
			excessBullets = Math.round((excessBullets / 2) / 10) * 10;
			System.out.println(excessBullets);

			rc.donate(excessBullets);
		}
	}
	
	//-------------------------------------------------------------------------------------------------------
		// Below are statements to get the orientation of map, map center, and assign the starting Archon.
		// I think all of these can be called in the robot controller class
		static MapLocation assignStartingArchon() throws GameActionException {
			MapLocation[] archonLocationF = rc.getInitialArchonLocations(rc.getTeam());
			switch (getMapStartingOrientation()) {
			case "bottom":
			case "top":
				// If we start on the top or bottom, we want to return the location of the middle archon
				if (archonLocationF.length == 1) {
					return archonLocationF[0];
				}
				else if (archonLocationF.length == 2) {
					// These statements check if the archon is surrounded by trees
					if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[0], 3)) {
						return archonLocationF[0];
					}
					else {
						return archonLocationF[1];
					}
				}
				else {
					if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[1], 3)) {
						return archonLocationF[1];
					}
					else if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[0], 3)) {
						return archonLocationF[0];
					}
					else {
						return archonLocationF[2];
					}
				}
			case "left":
			case "bottomLeft":
			case "topLeft":
				//If we start on the left side of the map, we want to return the closest archon to a corner
				if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[0], 3))
					return archonLocationF[0];
				else if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[1], 3))
					return archonLocationF[1];
				else 
					return archonLocationF[2];
			case "right":
			case "bottomRight":
			case "topRight":
				//If we start on the right side of the map, we want to return the closest archon to a corner
				if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[archonLocationF.length], 3))
					return archonLocationF[archonLocationF.length];
				else if (rc.isCircleOccupiedExceptByThisRobot(archonLocationF[archonLocationF.length-1], 3))
					return archonLocationF[archonLocationF.length-1];
				else 
					return archonLocationF[archonLocationF.length-2];
			} 
			return null;
		}
		
		static MapLocation getMapCenter()
		{
			float xCoordinate = 0;
			float yCoordinate = 0;
			MapLocation[] archonLocationF = rc.getInitialArchonLocations(rc.getTeam());
			MapLocation[] archonLocationE = rc.getInitialArchonLocations(rc.getTeam().opponent());
			//Switch statement uses the getMapStartingOrientation method to calculate map center
			switch (getMapStartingOrientation()) {
			case "bottom":
			case "top":
				if (archonLocationF.length == 1) {
					// If there is only one archon and the map is symmetric over the x-axis, the center is between them
					xCoordinate = archonLocationF[0].x;
					yCoordinate = (archonLocationE[0].y + archonLocationF[0].y)/2;
					MapLocation mapCenter = new MapLocation(xCoordinate, yCoordinate);
					return mapCenter;
				}
				if (archonLocationF.length == 2) {
					// If there are two archons, take the average x-values and set that equal to the map canter's x coordinate
					xCoordinate = (archonLocationF[0].x + archonLocationF[1].x)/2;
					yCoordinate = (archonLocationE[0].y + archonLocationF[0].y)/2;
					MapLocation mapCenter = new MapLocation(xCoordinate, yCoordinate);
					return mapCenter;
				}
				else {
					// If there are three archons, set the middle one's x-value to the center
					xCoordinate = archonLocationF[1].x;
					yCoordinate = (archonLocationE[0].y + archonLocationF[0].y)/2;
					MapLocation mapCenter = new MapLocation(xCoordinate, yCoordinate);
					return mapCenter;
				}
			case "left":
			case "right":
				if (archonLocationF.length == 1) {
					// If there is only one archon and the map is symmetric over the y-axis, the center is between them
					xCoordinate = (archonLocationE[0].x + archonLocationF[0].x)/2;
					yCoordinate = archonLocationF[0].y;
					MapLocation mapCenter = new MapLocation(xCoordinate, yCoordinate);
					return mapCenter;
				}
				if (archonLocationF.length == 2) {
					// If there are two archons, take the average y-values and set that equal to the map canter's y coordinate
					xCoordinate = (archonLocationE[0].x + archonLocationF[0].x)/2;
					yCoordinate = (archonLocationF[0].y + archonLocationF[1].y)/2;
					MapLocation mapCenter = new MapLocation(xCoordinate, yCoordinate);
					return mapCenter;
				}
				else {
					// If there are three archons, set the middle one's y-value to the center
					xCoordinate = (archonLocationE[0].x + archonLocationF[0].x)/2;
					yCoordinate = archonLocationF[1].y;
					MapLocation mapCenter = new MapLocation(xCoordinate, yCoordinate);
					return mapCenter;
				}
			case "bottomLeft":
			case "topLeft":
			case "bottomRight":
			case "topRight":
				// If we start at the bottom left corner, the archon with the smallest x and y will always be in the first position of the array
				// The archon with the largest x and y on the enemy team will be in the last spot of the array
				MapLocation cornerArchonF = archonLocationF[0];
				MapLocation cornerArchonE = archonLocationE[archonLocationE.length];
				MapLocation mapCenter = new MapLocation((cornerArchonE.x + cornerArchonF.x)/2, (cornerArchonE.y + cornerArchonF.y)/2);
				return mapCenter;
			}
			return null;
		}

		static String getMapStartingOrientation() {
			// Array of Archon location friendly/enemy
			MapLocation[] archonLocationF = rc.getInitialArchonLocations(rc.getTeam());
			MapLocation[] archonLocationE = rc.getInitialArchonLocations(rc.getTeam().opponent());
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
		
//		static String getMapTypes()
		// Close map - Create soldiers instantly
		// Big map, start off slow, don't send scouts
		// Tree map, create many lumber jacks
		// Open map, start making trees

}
