package battlecode2;

import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	static int TREE_POS_ARR_START = 50; // the position in the broadcast array
										// where the tree positions are first
										// stored and continues till end
	static int ARCHON_COUNT_ARR = 237; // the position in the broadcast array
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
	// can send up to 6? requests, each requires 3 spots
	static int LUMBERJACK_REQUESTS_START = 20;
	static int LUMBERJACK_REQUESTS_END = 41;// uses 42 and 43 also i think
	static int TREE_REMOVED_START = 50;
	static int TREE_REMOVED_END = 71;
	static int TOP_OF_GRID_ARR = 80;
	static int BOTTOM_OF_GRID_ARR = 81;
	static int LEFT_OF_GRID_ARR = 82;
	static int RIGHT_OF_GRID_ARR = 83;
	static int BASE_TREE_X = 84;
	static int BASE_TREE_Y = 85;

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
	// ----------------------------------------------------------------------------------------------
	// ARCHON PLAYER & METHODS

	static void runArchon() throws GameActionException {
		System.out.println("I'm an archon!");
		while (true) {
			try {
				roundOneCommands();
				roundTwoCommands();
				if (rc.readBroadcast(1) == rc.getID()) {

				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	// Methods specifically for the archon method. Returns true if the archon is
	// not surrounded
	static boolean isNotSurrounded() {
		Direction test = Direction.getEast();
		for (int angle = 0; angle <= 360; angle = angle + 45) {
			test = test.rotateLeftDegrees(angle);
			if (rc.canMove(test)) {
				return true;
			}
		}
		return false;
	}

	// This method will find the archon farthest away from the center. If this
	// archon is not surrounded by trees, it will be set as the default archon
	static void roundOneCommands() {
		if (rc.getRoundNum() == 1) {
			MapLocation[] archonLocationF = rc.getInitialArchonLocations(rc.getTeam());
			MapLocation mapCenter = getMapCenter();
			MapLocation farthestArchonLocation = mapCenter;
			float largestDistance = 0;
			for (MapLocation archonLocation : archonLocationF) {
				if (archonLocation.distanceTo(mapCenter) > largestDistance) {
					largestDistance = archonLocation.distanceTo(mapCenter);
					farthestArchonLocation = archonLocation;
				}
			}
			if (rc.getLocation() == farthestArchonLocation && isNotSurrounded()) {
				try {
					rc.broadcast(1, rc.getID());
					tryBuildRobot(farthestArchonLocation.directionTo(mapCenter), 1, 180, RobotType.GARDENER);
				} catch (GameActionException e) {
					System.out.println("Round one archon exception!");
					e.printStackTrace();
				}
			}
		}
	}

	// If no default archon has been set in round one, set a different archon as
	// the default then build a gardener
	static void roundTwoCommands() throws GameActionException {
		if (rc.getRoundNum() == 2) {
			if (rc.readBroadcast(1) == 0 && isNotSurrounded()) {
				try {
					rc.broadcast(1, rc.getID());
					MapLocation myLocation = rc.getLocation();
					tryBuildRobot(randomDirection(), 1, 180, RobotType.GARDENER);
				} catch (GameActionException e) {
					System.out.println("Roudn two archon exception!");
					e.printStackTrace();
				}
			}
		}
	}

	// ------------------------------------------------------------------------------
	// GARDENER METHODS

	static void runGardener() throws GameActionException {
		boolean aboutToDie = false;
		System.out.println("I'm a Gardner!");

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
				if (rc.readBroadcast(SOLDIER_COUNT_ARR) < 3) {
					tryBuildRobot(Direction.getNorth(), 10, 18, RobotType.SOLDIER);
				}
				maintainTreeGrid(rc.senseNearbyTrees());
				MapLocation myLocation = rc.getLocation();
				while (!tryMove(move, (float) 10, 5)) {
					move = randomDirection();
				}
				/*
				 * if (state == 0) { if (tryMove(randomDirection(), (float) 20,
				 * 5)) { count++; } } if (count == 10) { state = 1; } if (state
				 * == 1) { // gets all neutralTrees that could be in the way
				 * TreeInfo[] neutralTrees =
				 * rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 3,
				 * Team.NEUTRAL); // request lumberJacks for each
				 * 
				 * for (TreeInfo tree : neutralTrees) {
				 * System.out.println("requesting tree id:" + tree.ID); //
				 * request number of lumberjacks based on tree health
				 * requestLumberJack(tree, 1 + (int) (tree.health / 41)); }
				 * maintainTreeRing(); }
				 */

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

	/**
	 * moves and maintains a tree grid
	 * 
	 * @throws GameActionException
	 */
	static void maintainTreeGrid(TreeInfo[] trees) throws GameActionException {
		// direction is stored in gardener, grid should have edge constraints in
		// message array

		// get 4 surrounding spots
		float spacing = (float) 4.2;
		MapLocation myLocation = rc.getLocation();
		MapLocation baseLocation = new MapLocation(rc.readBroadcast(BASE_TREE_X / 10000000),
			rc.readBroadcast(BASE_TREE_Y / 10000000));
		MapLocation[] nearbySpots = new MapLocation[4];
		nearbySpots[0] = new MapLocation(myLocation.x + ((baseLocation.x - myLocation.x) % spacing),
				myLocation.y + ((baseLocation.y - myLocation.y) % spacing));
		nearbySpots[1] = new MapLocation(myLocation.x + ((baseLocation.x - myLocation.x) % spacing) + spacing,
				myLocation.y + ((baseLocation.y - myLocation.y) % spacing));
		nearbySpots[2] = new MapLocation(myLocation.x + ((baseLocation.x - myLocation.x) % spacing),
				myLocation.y + ((baseLocation.y - myLocation.y) % spacing) + spacing);
		nearbySpots[3] = new MapLocation(myLocation.x + ((baseLocation.x - myLocation.x) % spacing) + spacing,
				myLocation.y + ((baseLocation.y - myLocation.y) % spacing) + spacing);
		boolean[] doesNotNeedTree = new boolean[4];

		// if find spot without tree plant if doesen't need to move plant tree,
		// else
		// move to that spot, check 4 surounding spots
		for (int i = 0; i < nearbySpots.length; i++) {
			MapLocation loc = nearbySpots[i];
			rc.setIndicatorDot(loc, 1, 1, 1);
			for (TreeInfo tree : trees) {
				if ((int) (tree.getLocation().x * 1000) == (int) (loc.x * 1000)
						&& (int) (tree.getLocation().x * 1000) == (int) (loc.x * 1000)) {
					doesNotNeedTree[i] = true;
					break;
				}
			}
		}
		for (int i = 0; i < nearbySpots.length; i++) {
			if (!doesNotNeedTree[i]) {
				if (Math.round(rc.getLocation().distanceTo(nearbySpots[i])*10)/10 == 2.1) {
					rc.plantTree(rc.getLocation().directionTo(nearbySpots[i]));
				} else {
					//try to move to location 2.1 away
					
				}
			}
		}

		// if didn't move to align move in grid, go forward or turn right

		// water weakest tree that can water

	}

	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		rc.broadcast(SCOUT_COUNT_ARR, rc.readBroadcast(SCOUT_COUNT_ARR) + 1);
		
		Direction moveDirection = randomDirection();
		boolean combatMode = false;
		while (true) {
			try {
				// If a scout does not see an enemy, it will run this code
				if (!combatMode) {
					TreeInfo[] treeLocation = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
					for (TreeInfo tree: treeLocation) {
						if (tree.getContainedBullets() > 0) {
							shakeTree(treeLocation);
						}
					}
					if (rc.canMove(moveDirection) && !rc.hasMoved()) {
						rc.move(moveDirection);
					}
					else {
						moveDirection = randomDirection();
						if (!rc.canMove(moveDirection)) {
							tryMove(moveDirection, 10, 20);
							//Work on this not being in a random direction
						}
					}
					// Check every turn if there is an enemy nearby
					/*RobotInfo[] enemyLocation = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, rc.getTeam().opponent());
					for (RobotInfo enemy: enemyLocation) {
						if (enemy.getType() != RobotType.SCOUT) {
							combatMode = true;
							break;
						}
					}*/
				}
				else if (combatMode)
				{
					
				}
				// rc.fireSingleShot(Direction.SOUTH);
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
		System.out.println("I'm a soldier!");

		Direction tempMoveDirection;
		Direction moveDirection;
		// where did we start from? -> where we should initially move
		float radianMove;
		switch (getMapStats()) {
		case "bottom":
			radianMove = (float) Math.PI / 2;
			break;
		case "top":
			radianMove = (float) Math.PI * 3 / 2;
			break;
		case "left":
			radianMove = (float) 0;
			break;
		case "right":
			radianMove = (float) Math.PI;
			break;
		case "bottomRight":
			radianMove = (float) Math.PI * 3 / 4;
			break;
		case "bottomLeft":
			radianMove = (float) Math.PI * 1 / 4;
			break;
		case "topLeft":
			radianMove = (float) Math.PI * 7 / 4;
			break;
		case "topRight":
			radianMove = (float) Math.PI * 5 / 4;
			break;
		default:
			System.out.println("DEFAULT");
			radianMove = (float) Math.random() * 2 * (float) Math.PI;
			break;
		}
		moveDirection = new Direction(radianMove);

		while (true) {
			try {
				if (rc.canMove(moveDirection) && !rc.hasMoved()) {
					rc.move(moveDirection);
				} else {
					tempMoveDirection = randomDirection();
					if (!rc.canMove(moveDirection)) {
						if (!rc.hasMoved()) {
							tryMove(tempMoveDirection, 10, 20);
						}
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
		boolean aboutToDie = false;
		System.out.println("I'm an LumberJack!");
		rc.broadcast(LUMBERJACK_COUNT_ARR, rc.readBroadcast(LUMBERJACK_COUNT_ARR) + 1);
		Team enemy = rc.getTeam().opponent();
		int stuckCount = 0;
		Direction randDir = randomDirection();

		MapLocation tree = null;
		while (true) {
			try {

				System.out.println(tree);
				TreeInfo[] nearByTrees = rc.senseNearbyTrees();
				tryUseStrike();
				tryShake(nearByTrees);
				// gets what tree it should look for
				/*
				 * if (stuckCount > 0) { stuckCount--; int x = 0; if
				 * (rc.canMove(randDir)) { rc.move(randDir); } else { while
				 * (!rc.canMove(randDir) && x < 20) { randDir =
				 * randomDirection(); x++; } if (rc.canMove(randDir)) {
				 * rc.move(randDir); } } }
				 */
				if (tree != null) {

					int result = fufuilLumberJackRequest(tree, nearByTrees);
					if (result == 1) {
						tree = null;
					} else if (result == 2) {
						if (!chopNearestTrees(nearByTrees)) {
							System.out.println("Stuck");
							stuckCount = 5;
							// destination = tree;
							// nextPathLocation =
							// smartMoveToLocation(nextPathLocation,
							// destination, nearByTrees,
							// rc.senseNearbyRobots());
						}
					}

				} else {
					// tree = getLumberJackRequest();
					if (tree == null) {
						tree = getNextTreeLocation(nearByTrees);
					}
					if (tree == null) {
						if (!chopNearestTrees(nearByTrees)) {
							if (!rc.hasMoved()) {
								int x = 0;
								if (tryMove(randDir, 2, 15)) {

								} else {
									while (!rc.canMove(randDir) && x < 20) {
										randDir = randomDirection();
										x++;
									}
								}

							}
						}
					}
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
		if (rc.hasMoved()) {
			System.out.print("already moved");
			return false;
		}
		boolean result = tryMove(dirTo, degreeOffset, checksPerSide);
		return result;
	}

	static boolean tryShoot() throws GameActionException {
		// shoots with correct number of bullets in correct distance. Use as a
		// conditional to check if there is an enemy in range and call this
		// function

		// 0-2 distance shoot pent, 3-5 distance shoot triad, >6 distance shoot
		// single
		RobotInfo[] enemies;
		int shotType;
		boolean didShoot = true;
		Direction dir;

		// decide which shot to shoot
		if (rc.senseNearbyRobots((float) 2)[0] != null) {
			shotType = 5;
			enemies = rc.senseNearbyRobots((float) 2);
		} else if (rc.senseNearbyRobots((float) 5)[0] != null) {
			shotType = 3;
			enemies = rc.senseNearbyRobots((float) 5);
		} else if (rc.senseNearbyRobots((float) -1)[0] != null) {
			shotType = 1;
			enemies = rc.senseNearbyRobots((float) -1);
		} else {
			shotType = -1;
			enemies = new RobotInfo[0];
		}

		// determine direction of shooting
		dir = shotType == 1 ? rc.getLocation().directionTo(enemies[0].getLocation()) : null;

		switch (shotType) {
		case 5:
			if (rc.canFirePentadShot()) {
				rc.firePentadShot(dir);
			}
			break;
		case 3:
			if (rc.canFireTriadShot()) {
				rc.fireTriadShot(dir);
			}
			break;
		case 1:
			if (rc.canFireSingleShot()) {
				rc.fireSingleShot(dir);
			}
			break;
		default:
			didShoot = false;
			System.out.println("Did not shoot :(");
			break;
		}
		return didShoot;
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
		if (minDist == 1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.0) {
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
			if (Math.round(tree.x) == Math.round(t.getLocation().x)
					&& Math.round(tree.y) == Math.round(t.getLocation().y)) {
				id = t.getID();
				tree = t.getLocation();
				found = true;
			}
		}
		if (found) {
			System.out.println(id);
			if (rc.canChop(id)) {
				rc.chop(id);
				tryMoveToLocation(tree, 1, 20);
				System.out.println("Choping Request id: " + id);
				return 0;
			} else if (!tryMoveToLocation(tree, 1, 70)) {
				return 2;
			}
			return 0;
		} else {
			if (tree.distanceTo(rc.getLocation()) < rc.getType().sensorRadius) {
				return 1;
			}
			if (!tryMoveToLocation(tree, 1, 70)) {
				return 2;
			}
			return 0;
		}
	}

	static boolean chopNearestTrees(TreeInfo[] trees) throws GameActionException {
		TreeInfo closest = null;
		float dist = (float) 0;
		// if there are no trees detected return false
		if (trees.length == 0) {
			return false;
		}
		// for each detected tree
		for (TreeInfo tree : trees) {

			if (tree.getTeam() == Team.NEUTRAL || tree.getTeam() == rc.getTeam().opponent()) {
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
			return tryMoveToLocation(closest.getLocation(), 1, 30);
		}

		return false;
	}

	static MapLocation getNextTreeLocation(TreeInfo[] trees) {
		if (trees.length == 0) {
			return null;
		}
		// prioritize ones with robots
		for (TreeInfo tree : trees) {
			if (tree.containedRobot != null) {
				return tree.getLocation();
			}
		}
		// return closest eneym or neutral tree
		for (TreeInfo tree : trees) {
			if (tree.getTeam() == rc.getTeam().opponent() || tree.getTeam() == Team.NEUTRAL) {
				return tree.getLocation();
			}
		}
		return null;

	}

	static boolean tryUseStrike() throws GameActionException {
		for (TreeInfo tree : rc.senseNearbyTrees(3, rc.getTeam())) {
			return false;
		}
		for (TreeInfo tree : rc.senseNearbyTrees(3, Team.NEUTRAL)) {
			if (tree.getHealth() < 6)
				return false;
		}
		for (RobotInfo robot : rc.senseNearbyRobots(3, rc.getTeam())) {
			return false;
		}
		rc.strike();
		return true;
	}

	static boolean tryShake(TreeInfo[] trees) throws GameActionException {
		for (TreeInfo tree : trees) {
			if (tree.getContainedBullets() > 0 && rc.canShake(tree.ID)) {
				rc.shake(tree.ID);
				return true;
			}
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

	static MapLocation getNextPathLocation(TreeInfo[] treeList, RobotInfo[] robotList, MapLocation destination) {
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
		// for each group
		for (int x = 0; x < listOfConnectedTreeGroups.size(); x++) {
			// for each pair
			for (int r = 0; r < listOfConnectedRobotGroups.get(x).size(); r++) {
				RobotInfo robot = listOfConnectedRobotGroups.get(x).get(r);
				for (int r2 = r; r2 < listOfConnectedRobotGroups.get(x).size(); r2++) {
					RobotInfo robot2 = listOfConnectedRobotGroups.get(x).get(r2);
					if (robot != robot2) {
						// check for objects inside and outside, if all on same
						// side, then these are the droids we are looking for
						// convert to polar but backwards cuz thats how it
						// worked
						// out :P
						double robotAngle = Math.atan2(robot.getLocation().y - rc.getLocation().y,
								robot.getLocation().x - rc.getLocation().x) + Math.PI;
						double robot2Angle = Math.atan2(robot2.getLocation().y - rc.getLocation().y,
								robot2.getLocation().x - rc.getLocation().x) + Math.PI;
						double angleOffset;
						double secondAngle;
						// use which ever one is smaller as starting location
						if (robotAngle <= robot2Angle) {
							secondAngle = robot2Angle - robotAngle;
							angleOffset = robotAngle;
						} else {
							secondAngle = robotAngle - robot2Angle;
							angleOffset = robot2Angle;
						}
						// check if there are between and or outside
						boolean between = false;
						boolean outside = false;
						// if touching, between is true
						if (robot.getLocation().distanceTo(robot2.getLocation()) < robot.getType().bodyRadius
								+ robot2.getType().bodyRadius + rc.getType().bodyRadius) {
							between = true;
						}
						// check robots
						for (int r3 = 0; r3 < listOfConnectedRobotGroups.get(x).size(); r3++) {
							RobotInfo robot3 = listOfConnectedRobotGroups.get(x).get(r3);
							// make sure
							// get angle relative to starting angle
							double robot3Angle = Math.atan2(robot3.getLocation().y - rc.getLocation().y,
									robot3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
							if (robot3Angle < secondAngle) {
								between = true;
							} else {
								outside = true;
							}
						}
						// check trees
						for (int t3 = 0; t3 < listOfConnectedTreeGroups.get(x).size(); t3++) {
							TreeInfo tree3 = listOfConnectedTreeGroups.get(x).get(t3);
							// make sure
							// get angle relative to starting angle
							double tree3Angle = Math.atan2(tree3.getLocation().y - rc.getLocation().y,
									tree3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
							if (tree3Angle < secondAngle) {
								between = true;
							} else {
								outside = true;
							}
						}
						// if both between and outside, these are not the droids
						// we
						// are looking for if
						if ((!between && outside) || (!outside && between)) {
							edges.add(robot.getLocation());
							edges.add(robot2.getLocation());
							// need something to stop looking for pairs if this
							// part
							// runs
						}
					}
				}
				for (int t = 0; t < listOfConnectedTreeGroups.get(x).size(); t++) {
					TreeInfo tree2 = listOfConnectedTreeGroups.get(x).get(t);
					// check for objects inside and outside, if all on same
					// side, then these are the droids we are looking for
					// convert to polar but backwards cuz thats how it worked
					// out :P
					double robotAngle = Math.atan2(robot.getLocation().y - rc.getLocation().y,
							robot.getLocation().x - rc.getLocation().x) + Math.PI;
					double tree2Angle = Math.atan2(tree2.getLocation().y - rc.getLocation().y,
							tree2.getLocation().x - rc.getLocation().x) + Math.PI;
					double angleOffset;
					double secondAngle;
					// use which ever one is smaller as starting location
					if (robotAngle <= tree2Angle) {
						secondAngle = tree2Angle - robotAngle;
						angleOffset = robotAngle;
					} else {
						secondAngle = robotAngle - tree2Angle;
						angleOffset = tree2Angle;
					}
					// check if there are between and or outside
					boolean between = false;
					boolean outside = false;
					// if touching, between is true
					if (robot.getLocation().distanceTo(tree2.getLocation()) < robot.getType().bodyRadius + tree2.radius
							+ rc.getType().bodyRadius) {
						between = true;
					}
					// check robots
					for (int r3 = 0; r3 < listOfConnectedRobotGroups.get(x).size(); r3++) {
						RobotInfo robot3 = listOfConnectedRobotGroups.get(x).get(r3);
						// make sure
						// get angle relative to starting angle
						double robot3Angle = Math.atan2(robot3.getLocation().y - rc.getLocation().y,
								robot3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
						if (robot3Angle < secondAngle) {
							between = true;
						} else {
							outside = true;
						}
					}
					// check trees
					for (int t3 = 0; t3 < listOfConnectedTreeGroups.get(x).size(); t3++) {
						TreeInfo tree3 = listOfConnectedTreeGroups.get(x).get(t3);
						// make sure
						// get angle relative to starting angle
						double tree3Angle = Math.atan2(tree3.getLocation().y - rc.getLocation().y,
								tree3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
						if (tree3Angle < secondAngle) {
							between = true;
						} else {
							outside = true;
						}
					}
					// if both between and outside, these are not the droids we
					// are looking for if
					if ((!between && outside) || (!outside && between)) {
						edges.add(robot.getLocation());
						edges.add(tree2.getLocation());
						// need something to stop looking for pairs if this part
						// runs
					}
				}
			}

			for (int t = 0; t < listOfConnectedTreeGroups.get(x).size(); t++) {
				TreeInfo tree = listOfConnectedTreeGroups.get(x).get(t);
				for (int t2 = 0; t2 < listOfConnectedTreeGroups.get(x).size(); t2++) {
					TreeInfo tree2 = listOfConnectedTreeGroups.get(x).get(t2);
					if (tree != tree2) {
						// check for objects inside and outside, if all on same
						// side, then these are the droids we are looking for
						// convert to polar but backwards cuz thats how it
						// worked
						// out :P
						double treeAngle = Math.atan2(tree.getLocation().y - rc.getLocation().y,
								tree.getLocation().x - rc.getLocation().x) + Math.PI;
						double tree2Angle = Math.atan2(tree2.getLocation().y - rc.getLocation().y,
								tree2.getLocation().x - rc.getLocation().x) + Math.PI;
						double angleOffset;
						double secondAngle;
						// use which ever one is smaller as starting location
						if (treeAngle <= tree2Angle) {
							secondAngle = tree2Angle - treeAngle;
							angleOffset = treeAngle;
						} else {
							secondAngle = treeAngle - tree2Angle;
							angleOffset = tree2Angle;
						}
						// check if there are between and or outside
						boolean between = false;
						boolean outside = false;
						// if touching, between is true
						if (tree.getLocation().distanceTo(tree2.getLocation()) < tree.radius + tree2.radius
								+ rc.getType().bodyRadius) {
							between = true;
						}
						// check robots
						for (int r3 = 0; r3 < listOfConnectedRobotGroups.get(x).size(); r3++) {
							RobotInfo robot3 = listOfConnectedRobotGroups.get(x).get(r3);
							// make sure
							// get angle relative to starting angle
							double robot3Angle = Math.atan2(robot3.getLocation().y - rc.getLocation().y,
									robot3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
							if (robot3Angle < secondAngle) {
								between = true;
							} else {
								outside = true;
							}
						}
						// check trees
						for (int t3 = 0; t3 < listOfConnectedTreeGroups.get(x).size(); t3++) {
							TreeInfo tree3 = listOfConnectedTreeGroups.get(x).get(t3);
							// make sure
							// get angle relative to starting angle
							double tree3Angle = Math.atan2(tree3.getLocation().y - rc.getLocation().y,
									tree3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
							if (tree3Angle < secondAngle) {
								between = true;
							} else {
								outside = true;
							}
						}
						// if both between and outside, these are not the droids
						// we
						// are looking for if
						if ((!between && outside) || (!outside && between)) {
							edges.add(tree.getLocation());
							edges.add(tree2.getLocation());
							// need something to stop looking for pairs if this
							// part
							// runs
						}
					}
				}
			}
		}
		// get the edge closest to destination
		MapLocation closestEdge = edges.get(0);
		Direction dirToDest = rc.getLocation().directionTo(destination);
		for (int x = 0; x < edges.size(); x++) {
			if (Math.abs(dirToDest.radians - rc.getLocation().directionTo(edges.get(x)).radians) > Math
					.abs(dirToDest.radians - rc.getLocation().directionTo(closestEdge).radians)) {
				closestEdge = edges.get(x);
			}
		}
		location = closestEdge;
		System.out.println("used " + (Clock.getBytecodeNum() - bytesBefore) + "Byte Codes");
		return location;
	}

	static MapLocation simplerGetNextPathLocation(TreeInfo[] treeList, RobotInfo[] robotList, MapLocation destination) {
		MapLocation location = null;
		ArrayList<TreeInfo> treeGroup = new ArrayList<TreeInfo>();
		ArrayList<RobotInfo> robotGroup = new ArrayList<RobotInfo>();
		ArrayList<MapLocation> edges = new ArrayList<MapLocation>();
		float degreesBetweenClosest;
		TreeInfo treeInWay;
		// find /robot/ or tree directly in way
		if (treeList.length != 0) {
			treeInWay = treeList[0];
			degreesBetweenClosest = rc.getLocation().directionTo(destination)
					.degreesBetween(rc.getLocation().directionTo(treeList[0].getLocation()));
			for (int t = 0; t < treeList.length; t++) {
				TreeInfo tree = treeList[t];
				float degreesBetweenThisTree = rc.getLocation().directionTo(destination)
						.degreesBetween(rc.getLocation().directionTo(tree.getLocation()));
				if (degreesBetweenThisTree < degreesBetweenClosest) {
					treeInWay = tree;
					degreesBetweenClosest = degreesBetweenThisTree;
				}
			}
			// find all trees "connected" to the one in the way, add to
			if (treeList.length != 0) {
				for (int t = 0; t < treeList.length; t++) {
					TreeInfo tree = treeList[t];
					if (tree.getLocation().distanceTo(treeInWay.getLocation()) < tree.radius + treeInWay.radius
							+ rc.getType().bodyRadius) {
						treeGroup.add(tree);
					}
				}
			}
			// find edges
			for (int t = 0; t < treeGroup.size(); t++) {
				TreeInfo tree = treeGroup.get(t);
				for (int t2 = 0; t2 < treeGroup.size(); t2++) {
					TreeInfo tree2 = treeGroup.get(t2);
					if (tree != tree2) {
						// check for objects inside and outside, if all on same
						// side, then these are the droids we are looking for
						// convert to polar but backwards cuz thats how it
						// worked
						// out :P
						double treeAngle = Math.atan2(tree.getLocation().y - rc.getLocation().y,
								tree.getLocation().x - rc.getLocation().x) + Math.PI;
						double tree2Angle = Math.atan2(tree2.getLocation().y - rc.getLocation().y,
								tree2.getLocation().x - rc.getLocation().x) + Math.PI;
						double angleOffset;
						double secondAngle;
						// use which ever one is smaller as starting location
						if (treeAngle <= tree2Angle) {
							secondAngle = tree2Angle - treeAngle;
							angleOffset = treeAngle;
						} else {
							secondAngle = treeAngle - tree2Angle;
							angleOffset = tree2Angle;
						}
						// check if there are between and or outside
						boolean between = false;
						boolean outside = false;
						// if touching, between is true
						if (tree.getLocation().distanceTo(tree2.getLocation()) < tree.radius + tree2.radius
								+ rc.getType().bodyRadius) {
							between = true;
						}
						// check trees
						for (int t3 = 0; t3 < treeGroup.size(); t3++) {
							TreeInfo tree3 = treeGroup.get(t3);
							// make sure
							// get angle relative to starting angle
							double tree3Angle = Math.atan2(tree3.getLocation().y - rc.getLocation().y,
									tree3.getLocation().x - rc.getLocation().x) + Math.PI - angleOffset;
							if (tree3Angle < secondAngle) {
								between = true;
							} else {
								outside = true;
							}
						}
						// if both between and outside, these are not the droids
						// we
						// are looking for if
						if (!between && outside) {
							// edges.add(tree.getLocation());
							// edges.add(tree2.getLocation());
							// need something to stop looking for pairs if this
							// part
							// runs
						} else if (!outside && between) {

						}

					}
				}
			}

		}
		return location;
	}

	static MapLocation smartMoveToLocation(MapLocation nextPathLocation, MapLocation destination, TreeInfo[] trees,
			RobotInfo[] robots) throws GameActionException {
		if (nextPathLocation == null) {
			nextPathLocation = destination;
		}
		if (rc.getLocation().distanceTo(destination) < rc.getType().bodyRadius + 2) {
			nextPathLocation = destination;
		} else if (rc.getLocation().distanceTo(nextPathLocation) < rc.getType().bodyRadius + 2) {
			nextPathLocation = getNextPathLocation(trees, robots, destination);
		}
		if (!tryMoveToLocation(nextPathLocation, 3, 30)) {
			nextPathLocation = getNextPathLocation(trees, robots, destination);
		}
		return nextPathLocation;
	}

	static boolean willCollideWithMe(BulletInfo bullet) throws GameActionException {
		MapLocation myLocation = rc.getLocation();

		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		float perpendicularDist = (float) (distToRobot * Math.sin(theta));
		perpendicularDist = Math.round(perpendicularDist * 1000) / 1000;
		float bodyRadius = Math.round((rc.getType().bodyRadius) * 1000) / 1000;
		System.out.println("Perpendicular Distance: " + perpendicularDist);
		System.out.println("Body radius: " + bodyRadius);

		if (Math.abs(perpendicularDist) <= bodyRadius) {
			if (perpendicularDist <= 0) {
				if (rc.canMove(propagationDirection.rotateRightRads((float) Math.PI / 2))) {
					System.out.println("Perpendicular Distance: " + perpendicularDist);
					System.out.println("Moved Right!");
					rc.move(propagationDirection.rotateRightRads((float) Math.PI / 2));
				} else {

				}
			} else {
				if (rc.canMove(propagationDirection.rotateLeftRads((float) Math.PI / 2))) {
					System.out.println("Perpendicular Distance: " + perpendicularDist);
					System.out.println("Moved Left!");
					rc.move(propagationDirection.rotateLeftRads((float) Math.PI / 2));
				} else {

				}
			}
			return true;
		}
		return false;
	}

	static void avoidBullet() throws GameActionException {
		BulletInfo[] allNearbyBullets = rc.senseNearbyBullets();
		// System.out.println("Number of bullets detected "+
		// allNearbyBullets.length);
		if (allNearbyBullets.length > 0) {
			for (BulletInfo bullet : allNearbyBullets) {
				if (willCollideWithMe(bullet)) {
					System.out.println("Dodged bullet!");
					return;
				}
			}
		}

	}

	/**
	 * gets the Map Type based on size and nubmer of trees nearby
	 * 
	 * @return
	 */
	static int getMapType() {
		boolean enclosed = false;
		boolean small = true;
		// count number of trees nearby
		TreeInfo[] trees = rc.senseNearbyTrees();
		// may need to adjust number
		if (trees.length > 10) {
			enclosed = true;
		}
		float[] size = guessMapSize();
		// if greater than a certain area
		if (size[0] * size[1] > 1000) {
			small = false;
		}
		if (small && enclosed) {
			return 1;
		}
		if (small && !enclosed) {
			return 2;
		}
		if (!small && enclosed) {
			return 3;
		}
		if (!small && !enclosed) {
			return 4;
		}
		// should never return 5;
		return 5;
	}

}

// -------------------------------------------------------------------------------------------------------
// Below are statements to get the orientation of map, map center, and
// assign the starting Archon.
// I think all of these can be called in the robot controller class

// static String getMapTypes()
// Close map - Create soldiers instantly
// Big map, start off slow, don't send scouts
// Tree map, create many lumber jacks
// Open map, start making trees
