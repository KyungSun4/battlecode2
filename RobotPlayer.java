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
			// runSoldier();
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
		boolean aboutToDie = false;
		System.out.println("I'm an archon!");
		rc.broadcast(ARCHON_COUNT_ARR, rc.readBroadcast(ARCHON_COUNT_ARR) + 1);
		while (true) {
			try {
				if (rc.getRoundNum() == 1) {
					// MapLocation startingArchon = assignStartingArchon();
					// rc.broadcast(0, (int) startingArchon.x);
					// rc.broadcast(1, (int) startingArchon.y);
					// If this robot is in the same location as the assigned
					// starting archon's location, save this robot's ID to the
					// array
					int xCoordinate = rc.readBroadcast(0);
					int yCoordinate = rc.readBroadcast(1);
					if ((int) rc.getLocation().x == xCoordinate && (int) rc.getLocation().y == yCoordinate) {
						rc.hireGardener(nextUnoccupiedDirection(RobotType.GARDENER, 0));
					}
				}
				// getNextPathLocation(rc.senseNearbyTrees(),
				// rc.senseNearbyRobots());
				tryHireGardner(Direction.getNorth(), 10, 18);

				convertVictoryPoints(500);
				if (rc.getHealth() <= 5 && aboutToDie) {
					aboutToDie = true;
					rc.broadcast(ARCHON_COUNT_ARR, rc.readBroadcast(ARCHON_COUNT_ARR) - 1);
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	static void runGardener() throws GameActionException {
		boolean aboutToDie = false;
		System.out.println("I'm an Gardner!");

		rc.broadcast(GARDNER_COUNT_ARR, rc.readBroadcast(GARDNER_COUNT_ARR) + 1);
		ArrayList<TreeInfo> requestedTrees = new ArrayList<TreeInfo>();
		Team enemy = rc.getTeam().opponent();
		int state = 0; // 0-finding location 1-building tree circle
		int count = 0;
		Direction move = randomDirection();
		while (true) {
			try {
				if (rc.readBroadcast(LUMBERJACK_COUNT_ARR) < 3) {
					tryBuildRobot(Direction.getNorth(), 10, 18, RobotType.LUMBERJACK);
				}
				System.out.print(rc.readBroadcast(SCOUT_COUNT_ARR));
				if (rc.readBroadcast(SCOUT_COUNT_ARR) < 3) {
					tryBuildRobot(Direction.getNorth(), 10, 18, RobotType.SCOUT);
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
				// update robot count
				if (rc.getHealth() <= 5 && aboutToDie) {
					aboutToDie = true;
					rc.broadcast(GARDNER_COUNT_ARR, rc.readBroadcast(GARDNER_COUNT_ARR) - 1);
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Gardern Exception");
				e.printStackTrace();
			}
		}
	}

	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		Direction moveDirection = randomDirection();
		boolean combatMode = false;
		while (true) {
			try {
				if (!combatMode) {
					TreeInfo[] treeLocation = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
					for (TreeInfo tree : treeLocation) {
						if (tree.getContainedBullets() > 0) {
							shakeTree(treeLocation);
						}
					}
					if (rc.canMove(moveDirection) && !rc.hasMoved()) {
						rc.move(moveDirection);
					} else {
						moveDirection = randomDirection();
						if (!rc.canMove(moveDirection)) {
							tryMove(moveDirection, 10, 20);
							// Work on this not being in a random direction
						}
					}
					RobotInfo[] enemyLocation = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius,
							rc.getTeam().opponent());
					for (RobotInfo enemy : enemyLocation) {
						if (enemy.getType() != RobotType.SCOUT) {
							combatMode = true;
							break;
						}
					}
				} else if (combatMode) {

				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}

	static void scoutMove() {

	}

	static void shakeTree(TreeInfo[] treeList) throws GameActionException {
		TreeInfo[] treeLocation = treeList;
		for (TreeInfo tree : treeLocation) {
			while (tree.getContainedBullets() > 0) {
				MapLocation treeLoc = tree.getLocation();
				Direction moveDirection = rc.getLocation().directionTo(treeLoc);
				if (rc.canShake(treeLoc)) {
					rc.shake(treeLoc);
					break;
				} else if (rc.canMove(moveDirection)) {
					rc.move(moveDirection);
				} else {
					tryMove(moveDirection, 10, 5);
				}
			}
		}
	}

	static void runTank() throws GameActionException {
		boolean aboutToDie = false;
		System.out.println("I'm an Tank!");
		rc.broadcast(TANK_COUNT_ARR, rc.readBroadcast(TANK_COUNT_ARR) + 1);
		Team enemy = rc.getTeam().opponent();
		while (true) {
			try {
				MapLocation myLocation = rc.getLocation();
				if (rc.getHealth() <= 5 && aboutToDie) {
					aboutToDie = true;
					rc.broadcast(TANK_COUNT_ARR, rc.readBroadcast(TANK_COUNT_ARR) - 1);
				}
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Tank Exception");
				e.printStackTrace();
			}
		}

	}
	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
		while (true) {
			try {
				RobotInfo[] enemies = rc.senseNearbyRobots(7, rc.getTeam().opponent());
				if (rc.getTeam() == Team.B) {
					avoidBullet(rc.getLocation().directionTo(enemies[0].getLocation()));
				}
				if (rc.getTeam() == Team.A)
				{
					Direction toEnemy = rc.getLocation().directionTo(enemies[0].getLocation());
					rc.fireSingleShot(toEnemy);
					System.out.println("SHOT");
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	static void runLumberjack() throws GameActionException {
		boolean aboutToDie = false;
		System.out.println("I'm an LumberJack!");
		rc.broadcast(LUMBERJACK_COUNT_ARR, rc.readBroadcast(LUMBERJACK_COUNT_ARR) + 1);
		Team enemy = rc.getTeam().opponent();

		MapLocation tree = null;
		while (true) {
			try {
				System.out.println(tree);
				TreeInfo[] nearByTrees = rc.senseNearbyTrees();
				// gets what tree it should look for

				if (tree != null) {
					int result = fufuilLumberJackRequest(tree, nearByTrees);
					if (result == 1) {
						tree = null;
					} else if (result == 2) {
						chopNearestTrees(nearByTrees);
					}

				} else {
					tree = getLumberJackRequest();
				}
				if (rc.getHealth() <= 5 && aboutToDie) {
					aboutToDie = true;
					rc.broadcast(LUMBERJACK_COUNT_ARR, rc.readBroadcast(LUMBERJACK_COUNT_ARR) - 1);
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
	 * maintains a flower tree around a gardener
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

	/**
	 * @param start
	 *            first diretion to check
	 * @param degreeOffset
	 *            degrees btwn checks
	 * @param checksPerSide
	 *            number of times on each side to check
	 * @param robotType
	 *            type of robot
	 * @return if succeful
	 * @throws GameActionException
	 */
	static boolean tryBuildRobot(Direction start, float degreeOffset, int checksPerSide, RobotType robotType)
			throws GameActionException {
		Direction ans = start;
		if (rc.canBuildRobot(robotType, start)) {
			rc.buildRobot(robotType, start);
			return true;
		}
		// Now try a bunch of similar angles
		int currentCheck = 1;
		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canBuildRobot(robotType, ans.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.buildRobot(robotType, ans.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canBuildRobot(robotType, ans.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.buildRobot(robotType, ans.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}
		return false;
	}

	/**
	 * same thing as tryBuildRobot but for try Hire Gardener
	 *
	 * @param start
	 *            first diretion to check
	 * @param degreeOffset
	 *            degrees btwn checks
	 * @param checksPerSide
	 *            number of times on each side to check
	 * @param robotType
	 *            type of robot
	 * @return if succeful
	 * @throws GameActionException
	 */
	static boolean tryHireGardner(Direction start, float degreeOffset, int checksPerSide) throws GameActionException {
		Direction ans = start;
		if (rc.canHireGardener(start)) {
			rc.hireGardener(start);
			return true;
		}
		// Now try a bunch of similar angles
		int currentCheck = 1;
		while (currentCheck <= checksPerSide) {
			// Try the offset of the left side
			if (rc.canHireGardener(ans.rotateLeftDegrees(degreeOffset * currentCheck))) {
				rc.hireGardener(ans.rotateLeftDegrees(degreeOffset * currentCheck));
				return true;
			}
			// Try the offset on the right side
			if (rc.canHireGardener(ans.rotateRightDegrees(degreeOffset * currentCheck))) {
				rc.hireGardener(ans.rotateRightDegrees(degreeOffset * currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}
		return false;
	}

	static boolean tryMoveToLocation(MapLocation loc, float degreeOffset, int checksPerSide)
			throws GameActionException {
		Direction dirTo = rc.getLocation().directionTo(loc);
		return tryMove(dirTo, degreeOffset, checksPerSide);
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
		double minDist = 1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.0;
		for (int i = LUMBERJACK_REQUESTS_START; i <= LUMBERJACK_REQUESTS_END; i += 3) {
			if (rc.readBroadcast(i) > 0) {
				System.out.println("gotrequest");
				int treex = rc.readBroadcast(i + 1);
				int treey = rc.readBroadcast(i + 2);
				double dist = Math.pow(rc.getLocation().x - treex, 2) + Math.pow(rc.getLocation().y - treey, 2);
				System.out.println(dist);
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
			MapLocation requestLocation = new MapLocation((float) (x / 1000.0), (float) (y / 1000.0));
			System.out.println("request Loc" + requestLocation);
			return requestLocation;
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
		int id = 0;
		boolean found = false;
		for (TreeInfo t : trees) {
			if (Math.round(tree.x) == Math.round(t.getLocation().x)) {
				id = t.getID();
				tree = t.getLocation();
				found = true;
			}
		}
		if (found) {
			System.out.println(id);
			if (rc.canChop(id)) {
				rc.canChop(id);
				return 0;
			} else if (!tryMoveToLocation(tree, 1, 80)) {
				return 2;
			}
			return 0;
		} else {
			if (tree.distanceTo(rc.getLocation()) < rc.getType().sensorRadius) {
				return 1;
			}
			if (!tryMoveToLocation(tree, 1, 80)) {
				return 2;
			}
			return 0;
		}
	}

	static boolean chopNearestTrees(TreeInfo[] trees) throws GameActionException {
		TreeInfo closest = null;
		float dist = (float) 1000000000000000000000000000.0;
		// if there are no trees detected return false
		if (trees.length == 0) {
			return false;
		}
		// for each detected tree
		for (TreeInfo tree : trees) {
			// if it contains bullets
			if (tree.getTeam() == Team.NEUTRAL || tree.getTeam() == rc.getTeam().opponent()) {
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
		if (closest == null) {
			return false;
		}
		// try and chop tree
		if (rc.canChop(closest.getLocation())) {
			rc.chop(closest.getLocation());
		} else {
			tryMoveToLocation(closest.getLocation(), 10, 3);
		}

		return false;
	}

	// Converts bullets to victory points
	static void convertVictoryPoints(int overflowRange) throws GameActionException {
		if (rc.getTeamBullets() > overflowRange) {
			float teamBullets = rc.getTeamBullets();
			double excessBullets = teamBullets - overflowRange;
			excessBullets = ((int) (excessBullets / (7.5 + (rc.getRoundNum() * 12.5 / 3000))))
					* (7.5 + rc.getRoundNum() * 12.5 / 3000);
			System.out.println(excessBullets);
			rc.donate((float) (excessBullets));
		}
		// If we can win spend all bullets
		if (rc.getTeamBullets() / (7.5 + (rc.getRoundNum()) * 12.5 / 3000) >= GameConstants.VICTORY_POINTS_TO_WIN
				- rc.getTeamVictoryPoints())
			rc.donate(rc.getTeamBullets());
		// If we're on the last round, donate all bullets
		if (rc.getRoundNum() == 2999)
			rc.donate(rc.getTeamBullets());
	}

	static MapLocation getNextPathLocation(TreeInfo[] treeList, RobotInfo[] robotList) {
		int bytesBefore = Clock.getBytecodeNum();
		MapLocation location = null;
		ArrayList<ArrayList<RobotInfo>> listOfConnectedRobotGroups = new ArrayList<ArrayList<RobotInfo>>();
		ArrayList<ArrayList<TreeInfo>> listOfConnectedTreeGroups = new ArrayList<ArrayList<TreeInfo>>();
		for (TreeInfo tree : treeList) {
			// check if already in group
			ArrayList<TreeInfo> myTreeGroup = null;
			ArrayList<RobotInfo> myRobotGroup = null;
			for (int i = 0; i < listOfConnectedTreeGroups.size(); i++) {
				for (Object object : listOfConnectedTreeGroups.get(i)) {
					if (object == tree) {
						myTreeGroup = listOfConnectedTreeGroups.get(i);
						myRobotGroup = listOfConnectedRobotGroups.get(i);
					}
				}
			}
			if (myTreeGroup == null) {
				ArrayList<TreeInfo> tempTreeList = new ArrayList<TreeInfo>();
				ArrayList<RobotInfo> tempRobotList = new ArrayList<RobotInfo>();
				tempTreeList.add(tree);
				listOfConnectedTreeGroups.add(tempTreeList);
				listOfConnectedRobotGroups.add(tempRobotList);
				myTreeGroup = tempTreeList;
				myRobotGroup = tempRobotList;

			}
			for (TreeInfo tree2 : treeList) {
				if (tree2 != tree) {
					if (tree2.getLocation().distanceTo(tree.getLocation()) < rc.getType().bodyRadius + tree2.radius
							+ tree.radius) {
						// if already has group, merge groups else add to group
						ArrayList<TreeInfo> tree2Group = null;
						ArrayList<RobotInfo> tree2RobotGroup = null;
						for (int i = 0; i < listOfConnectedTreeGroups.size(); i++) {
							for (Object object : listOfConnectedTreeGroups.get(i)) {
								if (object == tree2) {
									tree2Group = listOfConnectedTreeGroups.get(i);
									tree2RobotGroup = listOfConnectedRobotGroups.get(i);
								}
							}
						}
						if (tree2Group != myTreeGroup) {
							if (tree2Group == null) {
								myTreeGroup.add(tree2);
							} else {
								myTreeGroup.addAll(tree2Group);
								myRobotGroup.addAll(tree2RobotGroup);
								listOfConnectedTreeGroups.remove(tree2Group);
								listOfConnectedRobotGroups.remove(tree2RobotGroup);
							}
						}
					}
				}
			}
			for (RobotInfo robot : robotList) {

				if (robot.getLocation().distanceTo(tree.getLocation()) < rc.getType().bodyRadius
						+ robot.getType().bodyRadius + tree.radius) {
					// if already has group, merge groups else add to group
					ArrayList<TreeInfo> robotTreeGroup = null;
					ArrayList<RobotInfo> robotRobotGroup = null;
					for (int i = 0; i < listOfConnectedRobotGroups.size(); i++) {
						for (Object object : listOfConnectedRobotGroups.get(i)) {
							if (object == robot) {
								robotTreeGroup = listOfConnectedTreeGroups.get(i);
								robotRobotGroup = listOfConnectedRobotGroups.get(i);
							}
						}
					}
					if (robotTreeGroup != myTreeGroup) {
						if (robotRobotGroup == null) {
							myRobotGroup.add(robot);
						} else {
							myTreeGroup.addAll(robotTreeGroup);
							myRobotGroup.addAll(robotRobotGroup);
							listOfConnectedTreeGroups.remove(robotTreeGroup);
							listOfConnectedRobotGroups.remove(robotRobotGroup);
						}
					}
				}

			}
		}
		for (RobotInfo robot : robotList) {
			// check if already in group
			ArrayList<TreeInfo> myTreeGroup = null;
			ArrayList<RobotInfo> myRobotGroup = null;
			for (int i = 0; i < listOfConnectedRobotGroups.size(); i++) {
				for (Object object : listOfConnectedRobotGroups.get(i)) {
					if (object == robot) {
						myTreeGroup = listOfConnectedTreeGroups.get(i);
						myRobotGroup = listOfConnectedRobotGroups.get(i);
					}
				}
			}
			if (myTreeGroup == null) {
				ArrayList<TreeInfo> tempTreeList = new ArrayList<TreeInfo>();
				ArrayList<RobotInfo> tempRobotList = new ArrayList<RobotInfo>();
				tempRobotList.add(robot);
				listOfConnectedTreeGroups.add(tempTreeList);
				listOfConnectedRobotGroups.add(tempRobotList);
				myTreeGroup = tempTreeList;
				myRobotGroup = tempRobotList;

			}
			for (RobotInfo robot2 : robotList) {
				if (robot2 != robot) {
					if (robot2.getLocation().distanceTo(robot.getLocation()) < rc.getType().bodyRadius
							+ robot2.getType().bodyRadius + robot.getType().bodyRadius) {
						// if already has group, merge groups else add to group
						ArrayList<TreeInfo> robot2TreeGroup = null;
						ArrayList<RobotInfo> robot2RobotGroup = null;
						for (int i = 0; i < listOfConnectedRobotGroups.size(); i++) {
							for (Object object : listOfConnectedRobotGroups.get(i)) {
								if (object == robot2) {
									robot2TreeGroup = listOfConnectedTreeGroups.get(i);
									robot2RobotGroup = listOfConnectedRobotGroups.get(i);
								}
							}
						}
						if (robot2RobotGroup != myRobotGroup) {
							if (robot2RobotGroup == null) {
								myRobotGroup.add(robot2);
							} else {
								myTreeGroup.addAll(robot2TreeGroup);
								myRobotGroup.addAll(robot2RobotGroup);
								listOfConnectedTreeGroups.remove(robot2TreeGroup);
								listOfConnectedRobotGroups.remove(robot2RobotGroup);
							}
						}
					}
				}
			}
		}
		// find group edges
		ArrayList<MapLocation> edges = new ArrayList<MapLocation>();

		for (int x = 0; x < listOfConnectedTreeGroups.size(); x++) {
			MapLocation leftEdge;
			float leftDegrees = 0;
			float rightDegrees = 0;
			MapLocation rightEdge;
			boolean firstFound = false;
			// for each pair
			for (int r = 0; r < listOfConnectedRobotGroups.get(x).size(); r++) {
				for (int r2 = 0; r < listOfConnectedRobotGroups.get(x).size(); r2++) {
					//check for objects inside and outside, if all on same side, then these are the droids we are looking for
					
				}
				for (int t = 0; t < listOfConnectedTreeGroups.get(x).size(); t++) {

				}
			}
			for (int t = 0; t < listOfConnectedTreeGroups.get(x).size(); t++) {
				for (int t2 = 0; t < listOfConnectedTreeGroups.get(x).size(); t2++) {

				}
			}
		}
		System.out.println("used " + (Clock.getBytecodeNum() - bytesBefore) + "Byte Codes");
		return location;

	}

	static void smartMoveToLocation() {

	}
	
	static boolean willCollideWithMe(BulletInfo bullet, MapLocation robotLocation, float extraRadius) {
		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(robotLocation);
		float distToRobot = bulletLocation.distanceTo(robotLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return (perpendicularDist <= (rc.getType().bodyRadius + extraRadius));
	}

	
	static void avoidBullet(Direction optimalDirection) throws GameActionException {
		BulletInfo[] allNearbyBullets = rc.senseNearbyBullets();
		System.out.println("Number of bullets detected "+ allNearbyBullets.length);
		ArrayList<BulletInfo> incomingBullets = new ArrayList<BulletInfo>();
		for (BulletInfo checkBullet: allNearbyBullets) {
			if (willCollideWithMe(checkBullet, rc.getLocation(), rc.getType().strideRadius)) {
				incomingBullets.add(checkBullet);
			}
		}
		System.out.println(Clock.getBytecodeNum());
		
		//EVERYTHING BELOW THIS HAS TO BE FIXED
		
		System.out.println("Number Of bullets that will potentially hit me is " + incomingBullets.size());
		if (incomingBullets.size() > 0) {
			// This for loop computes the optimal direction that the robot can travel in to avoid all bullets
			// The following two statements will dictate where the robot will move if there is not spot that has 0 collisions
			
			Direction bestDirection = optimalDirection;
			int lowestNumberOfTimesHit = incomingBullets.size();
			int numberOfTimesHit = 0;

			for (int angle = 0; angle < 360; angle = angle + 45) {
				Direction newDirection = optimalDirection.rotateLeftDegrees(angle);
				// Generate new location using the old location then adding the stride radius and new direction
				MapLocation newLocation = rc.getLocation().add(newDirection, rc.getType().strideRadius);
				// Searches through the list of potential collisions and see which ones still collide
				for (BulletInfo checkBullet: incomingBullets) {
					MapLocation bulletLocation = checkBullet.getLocation();
					float distanceToBullet = newLocation.distanceTo(bulletLocation) - rc.getType().bodyRadius;
					float bulletSpeed = checkBullet.getSpeed();
					if ((distanceToBullet / bulletSpeed) <= 1) {
						numberOfTimesHit++;
					}
				}
				System.out.println("I will be hit by: " + numberOfTimesHit + "bullets next round");
				if (numberOfTimesHit == 0) {
					if (rc.canMove(newDirection)) {
						rc.move(newDirection);
						System.out.println("I am not going to be hit next turn!!!!!!!!!");
						return;
					}
				}
				if (numberOfTimesHit < lowestNumberOfTimesHit) {
					lowestNumberOfTimesHit = numberOfTimesHit;
					bestDirection = newDirection;
					System.out.println("Changed!");
				}
			}
			if (rc.canMove(bestDirection)) {
				rc.move(bestDirection);
				System.out.println(lowestNumberOfTimesHit);
			}
			Clock.yield();
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// Below are statements to get the orientation of map, map center, and
	// assign the starting Archon.
	// I think all of these can be called in the robot controller class

}

// static String getMapTypes()
// Close map - Create soldiers instantly
// Big map, start off slow, don't send scouts
// Tree map, create many lumber jacks
// Open map, start making trees
