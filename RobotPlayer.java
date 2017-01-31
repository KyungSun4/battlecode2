package battlecode2;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	// The indexes of the player array where the data is stored
	static int MAP_TYPE = 0;
	static int MAIN_ARCHON_ID = 1;
	static int MAIN_ARCHON_LOCATION_X = 2;
	static int MAIN_ARCHON_LOCATION_Y = 3;
	static int INITIAL_ENEMY_LOCATION_X = 4;
	static int INITIAL_ENEMY_LOCATION_Y = 5;
	static int REINFORCEMENT_NEEDED_X = 6;
	static int REINFORCEMENT_NEEDED_Y = 7;

	static int ARCHON_COUNT_ARR = 10;
	static int GARDENER_COUNT_ARR = 11;
	static int SOLDIER_COUNT_ARR = 12;
	static int LUMBERJACK_COUNT_ARR = 13;
	static int SCOUT_COUNT_ARR = 14;
	static int TANK_COUNT_ARR = 15;
	static int TOP_OF_GRID_ARR = 80;
	static int BOTTOM_OF_GRID_ARR = 81;
	static int LEFT_OF_GRID_ARR = 82;
	static int RIGHT_OF_GRID_ARR = 83;
	static int BASE_TREE_X = 84;
	static int BASE_TREE_Y = 85;
	static int ENEMY_X = 86;
	static int ENEMY_Y = 87;

	static Direction randomDir = randomDirection();

	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		// System.out.print(rc.getTeam());
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
		}
	}

	// -------------------------------------------------------------------------------------------------------------------------
	// ARCHON PLAYER & METHODS

	static void runArchon() throws GameActionException {
		System.out.println("I'm an archon!");
		rc.broadcast(ARCHON_COUNT_ARR, rc.readBroadcast(ARCHON_COUNT_ARR) + 1);
		while (true) {
			try {
				roundOneCommands();
				roundTwoCommands();
				if (rc.readBroadcast(MAIN_ARCHON_ID) == rc.getID() || rc.getRoundNum() > 400) {
					if (rc.readBroadcast(GARDENER_COUNT_ARR) <= (int) (3.0 * rc.getRoundNum() / 300)) {
						// this donest work: &&
						// rc.readBroadcast(SOLDIER_COUNT_ARR) >= 2

						tryBuildRobot(randomDirection(), 10, 9, RobotType.GARDENER);
					}
				}
				runAway();
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
			if (rc.canBuildRobot(RobotType.GARDENER, test)) {
				return true;
			}
		}
		return false;
	}

	// This method will find the archon farthest away from the center. If this
	// archon is not surrounded by trees, it will be set as the default archon
	static void roundOneCommands() throws GameActionException {
		if (rc.getRoundNum() == 1) {
			// These following statements are to find the location of the
			// farthest archon from the center
			System.out.print("maptype:" + getMapType());
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
			// If this archon is the farthest and is not surrounded, it will
			// save its ID into the array and make a gardener. It will also save
			// what type of map it is
			if (rc.getLocation() == farthestArchonLocation && isNotSurrounded()) {
				saveInitialData();
				rc.broadcast(BASE_TREE_X, (int) (rc.getLocation().x * 100000));
				rc.broadcast(BASE_TREE_Y, (int) (rc.getLocation().y * 100000));
				tryBuildRobot(farthestArchonLocation.directionTo(mapCenter), 10, 18, RobotType.GARDENER);
				System.out.println("Im the farthest archon!");
			}
		}
	}

	// If no default archon has been set in round one, set a different archon as
	// the default then build a gardener
	static void roundTwoCommands() throws GameActionException {
		if (rc.getRoundNum() == 2) {
			// If there is no ID saved to the 2nd position of the array, it will
			// set a different archon
			if (rc.readBroadcast(MAIN_ARCHON_ID) == 0 && isNotSurrounded()) {
				saveInitialData();
				rc.broadcast(BASE_TREE_X, (int) (rc.getLocation().x * 100000));
				rc.broadcast(BASE_TREE_Y, (int) (rc.getLocation().y * 100000));
				tryBuildRobot(randomDirection(), 10, 18, RobotType.GARDENER);
				System.out.println("Eh!?");
			}

		}
	}

	// Saves data into the array, made for the first two rounds
	static void saveInitialData() throws GameActionException {
		rc.broadcast(MAP_TYPE, getMapType());
		rc.broadcast(MAIN_ARCHON_ID, rc.getID());
		rc.broadcast(MAIN_ARCHON_LOCATION_X, (int) rc.getLocation().x);
		rc.broadcast(MAIN_ARCHON_LOCATION_Y, (int) rc.getLocation().y);
		enemyLocation();
	}

	static void enemyLocation() throws GameActionException {
		// Cheese statements get the necessary information to calculate the
		// location of the enemy
		// We cannot use the arhconLocaion method because
		MapLocation mapCenter = getMapCenter();
		float mapCenterX = mapCenter.x;
		float mapCenterY = mapCenter.y;
		MapLocation mainArchonLocation = new MapLocation(rc.readBroadcast(MAIN_ARCHON_LOCATION_X),
				rc.readBroadcast(MAIN_ARCHON_LOCATION_Y));
		float archonX = mainArchonLocation.x;
		float archonY = mainArchonLocation.y;

		float differenceX = (mapCenterX - archonX) * 2;
		float differenceY = (mapCenterY - archonY) * 2;

		rc.broadcast(INITIAL_ENEMY_LOCATION_X, (int) (archonX + differenceX));
		rc.broadcast(INITIAL_ENEMY_LOCATION_Y, (int) (archonY + differenceY));
	}

	
	static void runAway() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadius, rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			Direction awayFromEnemy = enemyRobots[0].getLocation().directionTo(rc.getLocation());
			if (!rc.hasMoved()) {
				tryMove(awayFromEnemy, 10, 9);
			}
		}
	}
	 

	// --------------------------------------------------------------------------------------------------------
	// GARDENER PLAYER & METHODS

	static void runGardener() throws GameActionException {
		System.out.println("I'm a gardener!");
		rc.broadcast(GARDENER_COUNT_ARR, rc.readBroadcast(GARDENER_COUNT_ARR) + 1);

		MapLocation start = rc.getInitialArchonLocations(rc.getTeam())[0];
		Direction awayDir = rc.getLocation().directionTo(start).opposite();
		int count = 10;
		int mapData = rc.readBroadcast(MAP_TYPE);
		boolean set = false;
		boolean leaveSpace = (Math.random() > .5);
		boolean aboutToDie = false;

		while (true) {
			try {
				// Small and enclosed
				if (mapData == 1) {
					if (rc.readBroadcast(SCOUT_COUNT_ARR) == 0) {
						tryBuildRobot(randomDirection(), 10, 18, RobotType.SCOUT);
					}
					
					if (rc.readBroadcast(LUMBERJACK_COUNT_ARR) <= 10) {
						tryBuildRobot(randomDir, 10, 18, RobotType.LUMBERJACK);
					}
					else if (rc.readBroadcast(SOLDIER_COUNT_ARR) <= 20) {
						tryBuildRobot(randomDir, 10, 18, RobotType.SOLDIER);

					}
					else if (rc.readBroadcast(TANK_COUNT_ARR) < 5) {
						tryBuildRobot(randomDir, 10, 18, RobotType.TANK);
					}
				}
				// Small and open
				else if (mapData == 2) {
					if (rc.readBroadcast(SCOUT_COUNT_ARR) == 0) {
						tryBuildRobot(randomDirection(), 10, 18, RobotType.SCOUT);
					}
					
					if (rc.readBroadcast(LUMBERJACK_COUNT_ARR) <= 10) {
						tryBuildRobot(randomDir, 10, 18, RobotType.LUMBERJACK);
					}
					else if (rc.readBroadcast(SOLDIER_COUNT_ARR) <= 20) {
						tryBuildRobot(randomDir, 10, 18, RobotType.SOLDIER);

					}
					else if (rc.readBroadcast(TANK_COUNT_ARR) < 5) {
						tryBuildRobot(randomDir, 10, 18, RobotType.TANK);
					}
				}
				// Big and enclosed
				else if (mapData == 3) {
					if (rc.readBroadcast(SCOUT_COUNT_ARR) == 0) {
						tryBuildRobot(randomDirection(), 10, 18, RobotType.SCOUT);
					}
					
					if (rc.readBroadcast(LUMBERJACK_COUNT_ARR) <= 10) {
						tryBuildRobot(randomDir, 10, 18, RobotType.LUMBERJACK);
					}
					else if (rc.readBroadcast(SOLDIER_COUNT_ARR) <= 20) {
						tryBuildRobot(randomDir, 10, 18, RobotType.SOLDIER);

					}
					else if (rc.readBroadcast(TANK_COUNT_ARR) < 5) {
						tryBuildRobot(randomDir, 10, 18, RobotType.TANK);
					}
				}
				// Big and open
				else if (mapData == 4) {
					if (rc.readBroadcast(SCOUT_COUNT_ARR) == 0) {
						tryBuildRobot(randomDirection(), 10, 18, RobotType.SCOUT);
					}
					
					if (rc.readBroadcast(LUMBERJACK_COUNT_ARR) <= 10) {
						tryBuildRobot(randomDir, 10, 18, RobotType.LUMBERJACK);
					}
					else if (rc.readBroadcast(SOLDIER_COUNT_ARR) <= 20) {
						tryBuildRobot(randomDir, 10, 18, RobotType.SOLDIER);

					}
					else if (rc.readBroadcast(TANK_COUNT_ARR) < 5) {
						tryBuildRobot(randomDir, 10, 18, RobotType.TANK);
					}
				}
				
				set = maintainTreeGridOfFlowers(set, rc.senseNearbyRobots(), leaveSpace);
				TreeInfo[] sensedTrees = rc.senseNearbyTrees();
				alwaysWater(sensedTrees);
				convertVictoryPoints(1000);
				
				if (rc.getHealth() <= 10 && !aboutToDie) {
					aboutToDie = true;
					rc.broadcast(SOLDIER_COUNT_ARR, rc.readBroadcast(SOLDIER_COUNT_ARR) - 1);
				}
				
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	static boolean maintainTreeGridOfFlowers(boolean set, RobotInfo[] robots, boolean leaveSpace)
			throws GameActionException {
		// will provide space for a radius one robot to fit through
		// Maybe later make a hexagonal if that can allow denser arangment

		MapLocation myLocation = rc.getLocation();

		// set means that it is already maintaining a flower, so, it doesnt need
		// to align to grid
		// if false it needs to find and get to a grid location
		System.out.println("set is " + set);
		if (set == false) {
			// if no other garders in existence, can find whatever open spot and
			// make that the baseLocation
			// if (rc.readBroadcast(GARDENER_COUNT_ARR) == 1) {

			// } else {
			// otherwise find unocupied spot on grid
			// once this is working, make more complex grid arangement
			float spacing = (float) 8.3;
			// MapLocation baseLocation = new MapLocation(spacing * 200 + 500,
			// spacing * 200 + 500);

			MapLocation baseLocation = new MapLocation(spacing * 200 + rc.readBroadcast(BASE_TREE_X) / (float) 100000,
					spacing * 200 + rc.readBroadcast(BASE_TREE_Y) / (float) 100000);

			MapLocation offsetLocation = new MapLocation(spacing * 200 + baseLocation.x,
					spacing * 200 + baseLocation.y);
			System.out.println("base is " + baseLocation);
			MapLocation[] nearbySpots = new MapLocation[16];
			int x = -2;
			int y = -2;
			for (int i = 0; i < nearbySpots.length; i++) {
				MapLocation loc = new MapLocation(
						myLocation.x + ((offsetLocation.x - myLocation.x) % spacing) + spacing * x,
						myLocation.y + ((offsetLocation.y - myLocation.y) % spacing) + spacing * y);
				rc.setIndicatorDot(loc, 0, 0, 0);
				if (rc.canSenseLocation(loc)) {
					nearbySpots[i] = loc;
				}
				x++;
				if (i == 3) {
					x = -2;
					y++;
				}
				if (i == 7) {
					x = -2;
					y++;
				}
				if (i == 11) {
					x = -2;
					y++;
				}
			}
			MapLocation closest = null;
			// boolean[] hasGardener = new boolean[16];
			// go to nearest spot
			for (int i = 0; i < nearbySpots.length; i++) {
				MapLocation loc = nearbySpots[i];
				boolean notPosible = false;
				if (loc != null) {
					// only check if its closer than the already found one
					if (closest == null || loc.distanceTo(rc.getLocation()) < closest.distanceTo(rc.getLocation())) {
						rc.setIndicatorDot(loc, 200, 200, 200);
						if (rc.canSenseAllOfCircle(loc, 1)) {
							if (!rc.onTheMap(loc, 1)) {
								notPosible = true;
								rc.setIndicatorDot(loc, 0, 200, 0);
							}
							if (rc.isCircleOccupiedExceptByThisRobot(loc, 1)) {
								// should request lumberjack to clear spot
								notPosible = true;
								rc.setIndicatorDot(loc, 0, 200, 0);
							}
						} else {
							notPosible = true;
						}
						for (RobotInfo robot : robots) {
							// check if there already is a gardener there
							if ((int) (robot.getLocation().x) == (int) (loc.x)
									&& (int) (robot.getLocation().y) == (int) (loc.y)
									&& robot.getType() == RobotType.GARDENER && robot.getTeam() == rc.getTeam()) {
								notPosible = true;
								rc.setIndicatorDot(loc, 0, 200, 0);
								System.out.println("Found robot in spot" + loc);
							}
						}
						if (!notPosible) {
							closest = loc;
						}
					}
				}
			}
			System.out.println(closest);
			if (closest != null) {
				rc.setIndicatorLine(rc.getLocation(), closest, 1, 0, 0);
				System.out.println("dist to" + myLocation.distanceTo(closest));
				if (myLocation.distanceTo(closest) > rc.getType().strideRadius) {
					tryMoveToLocation(closest, 1, 90);
					System.out.println("moving to" + closest);
				} else if (myLocation.distanceTo(closest) < .001) {
					set = true;
				} else if (rc.canMove(myLocation.directionTo(closest), myLocation.distanceTo(closest))) {
					rc.move(myLocation.directionTo(closest), myLocation.distanceTo(closest));
					System.out.println("moving to" + closest);
				}

			} else {
				System.out.print("i dont see spots");
				if (!rc.hasMoved()) {
					if (!tryMove(randomDir, 1, 90)) {
						randomDir = randomDirection();
					}
				}

			}
		} else {
			maintainTreeRing(leaveSpace);
		}
		return set;
	}

	static void maintainTreeGrid(TreeInfo[] trees) throws GameActionException {
		// direction is stored in gardener, grid should have edge constraints in
		// message array

		// get 16 surrounding spots
		float spacing = (float) 4.2;
		MapLocation myLocation = rc.getLocation();
		MapLocation baseLocation = new MapLocation(spacing * 200 + rc.readBroadcast(BASE_TREE_X) / (float) 100000,
				spacing * 200 + rc.readBroadcast(BASE_TREE_Y) / (float) 100000);
		MapLocation offsetLocation = new MapLocation(spacing * 200 + baseLocation.x, spacing * 200 + baseLocation.y);
		System.out.println("base is " + offsetLocation);
		MapLocation[] nearbySpots = new MapLocation[16];
		int x = -2;
		int y = -2;
		for (int i = 0; i < nearbySpots.length; i++) {
			MapLocation loc = new MapLocation(
					myLocation.x + ((offsetLocation.x - myLocation.x) % spacing) + spacing * x,
					myLocation.y + ((offsetLocation.y - myLocation.y) % spacing) + spacing * y);
			rc.setIndicatorDot(loc, 0, 0, 0);
			if (rc.canSenseLocation(loc)) {
				nearbySpots[i] = loc;
			}
			x++;
			if (i == 3) {
				x = -2;
				y++;
			}
			if (i == 7) {
				x = -2;
				y++;
			}
			if (i == 11) {
				x = -2;
				y++;
			}
		}

	boolean[] doesNotNeedTree = new boolean[16];
		// if find spot without tree plant if doesen't need to move plant tree,
		// else
		// move to that spot, check 4 surounding spots
		for (int i = 0; i < nearbySpots.length; i++) {
			MapLocation loc = nearbySpots[i];
			if (loc != null) {
				rc.setIndicatorDot(loc, 200, 200, 200);
				if (rc.canSenseAllOfCircle(loc, 1)) {
					if (!rc.onTheMap(loc, 1)) {
						doesNotNeedTree[i] = true;
						rc.setIndicatorDot(loc, 0, 200, 0);
					}

					if (rc.isCircleOccupiedExceptByThisRobot(loc, 1)) {
						doesNotNeedTree[i] = true;
						rc.setIndicatorDot(loc, 0, 200, 0);
					}
				} else {
					doesNotNeedTree[i] = true;
				}

				for (TreeInfo tree : trees) {
					if ((int) (tree.getLocation().x) == (int) (loc.x)
							&& (int) (tree.getLocation().y) == (int) (loc.y)) {
						doesNotNeedTree[i] = true;
						rc.setIndicatorDot(loc, 0, 200, 0);
						System.out.println("Found tree in spot" + loc);
					}
				}
			} else {
				doesNotNeedTree[i] = true;
			}
		}
		boolean tryingToPlant = false;
		MapLocation closest = null;
		MapLocation emptySpot = null;
		MapLocation closestEmptySpot = null;
		for (int i = 0; i < nearbySpots.length; i++) {
			if (nearbySpots[i] != null) {
				if (!doesNotNeedTree[i]) {
					emptySpot = nearbySpots[i];
					System.out.println("dist to" + rc.getLocation().distanceTo(emptySpot));
					System.out.println(emptySpot);
					if (Math.round(rc.getLocation().distanceTo(emptySpot) * 10) / 10 == 2.1) {
						rc.plantTree(rc.getLocation().directionTo(emptySpot));
					} else {
						tryingToPlant = true;
						// try to move to location 2.1 away
						// find which of 4 positions is closest
						MapLocation[] pointsAround = new MapLocation[4];
						pointsAround[0] = new MapLocation(emptySpot.x + spacing / 2, emptySpot.y);
						pointsAround[1] = new MapLocation(emptySpot.x - spacing / 2, emptySpot.y);
						pointsAround[2] = new MapLocation(emptySpot.x, emptySpot.y + spacing / 2);
						pointsAround[3] = new MapLocation(emptySpot.x, emptySpot.y - spacing / 2);

						for (int p = 0; p < pointsAround.length; p++) {
							if (rc.canSenseLocation(pointsAround[p])) {
								if (closest == null) {
									closestEmptySpot = emptySpot;
									closest = pointsAround[p];
								} else if (myLocation.distanceTo(pointsAround[p]) < myLocation.distanceTo(closest)) {
									closestEmptySpot = emptySpot;
									closest = pointsAround[p];
								}
							}
						}
					}

				}
			}
		}
		System.out.println(closest);
		if (closest != null && closestEmptySpot != null) {
			rc.setIndicatorLine(rc.getLocation(), closest, 1, 0, 0);
			if (myLocation.distanceTo(closest) > rc.getType().strideRadius) {

				tryMoveToLocation(closest, 1, 90);
				System.out.println("moving to " + closest);

			} else if (myLocation.distanceTo(closest) < .001) {
				if (rc.canPlantTree(myLocation.directionTo(closestEmptySpot))) {
					rc.plantTree(myLocation.directionTo(closestEmptySpot));
				}
			} else if (rc.canMove(myLocation.directionTo(closest), myLocation.distanceTo(closest))) {
				rc.move(myLocation.directionTo(closest), myLocation.distanceTo(closest));
				System.out.println("moving to " + closest);
			}

		} else {
			System.out.print("Cant move there");
		}
		// if didn't move to align move in grid try moving to tree that needs
		// water

		if (!tryingToPlant) {
			System.out.println("going to water wekeast");
			TreeInfo weakest = null;
			for (TreeInfo tree : trees) {
				if (tree.team == rc.getTeam()) {
					if (weakest == null) {
						weakest = tree;
					} else if (tree.getHealth() < weakest.getHealth()) {
						weakest = tree;
					}
				}
			}
			if (weakest != null) {
				if (myLocation.distanceTo(weakest.location) > 3) {
					tryMoveToLocation(weakest.getLocation(), 1, 90);
				}
			}
		}

		// water weakest tree that can water
		if (rc.canWater()) {
			TreeInfo weakest = null;
			for (TreeInfo tree : trees) {
				if (tree.team == rc.getTeam() && myLocation.distanceTo(tree.getLocation()) <= 3) {
					if (weakest == null) {
						weakest = tree;
					} else if (tree.getHealth() < weakest.getHealth()) {
						weakest = tree;
					}
				}
			}
			if (weakest != null && rc.canWater(weakest.ID)) {
				rc.water(weakest.ID);
			} else {
				for (TreeInfo tree : trees) {
					if (rc.canWater(tree.ID)) {
						rc.water(tree.ID);
					}
				}
			}
		}
	}

	static void maintainTreeRing(boolean leaveSpace) throws GameActionException {
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
		int numTrees = 6;
		if (leaveSpace) {
			numTrees = 5;
		}
		Direction dir = new Direction(Direction.getNorth().radians);
		for (int x = 1; x <= numTrees; x++) {
			if (rc.canPlantTree(dir)) {
				rc.plantTree(dir);
				x = numTrees + 1;
			}
			dir = dir.rotateRightRads((float) (Math.PI / 3));
			// System.out.println(dir);
		}
	}

	// Does stuff
	static void alwaysWater(TreeInfo[] sensedTrees) throws GameActionException {
		TreeInfo weakest = null;
		for (int i = 0; i < sensedTrees.length; i++) {
			if (sensedTrees[i].getTeam() == rc.getTeam()) {
				if (weakest == null || sensedTrees[i].health < weakest.health && rc.canWater(sensedTrees[i].ID)) {
					weakest = sensedTrees[i];
				}
			}
		}
		if (weakest != null) {
			if (rc.canWater(weakest.ID)) {
				rc.water(weakest.ID);
			}
		}
	}
	// -------------------------------------------------------------------------------------------------------------
	// SOLDIER PLAYER & METHODS

	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
		rc.broadcast(SOLDIER_COUNT_ARR, rc.readBroadcast(SOLDIER_COUNT_ARR) + 1);
		// int mapData = rc.readBroadcast(MAP_TYPE);

		MapLocation targetLocation = rc.getLocation();
		Direction wanderDirection = Direction.NORTH;
		float randomDistance = 0;
		boolean aboutToDie = false;
		boolean hasReinforced = true;
		boolean hasCheckedInitial = false;
		boolean setNewWanderLocation = true;
		MapLocation moveReference = new MapLocation(rc.readBroadcast(INITIAL_ENEMY_LOCATION_X),
				rc.readBroadcast(INITIAL_ENEMY_LOCATION_Y));
		MapLocation wanderReference = new MapLocation(rc.readBroadcast(INITIAL_ENEMY_LOCATION_X),
				rc.readBroadcast(INITIAL_ENEMY_LOCATION_Y));
		MapLocation reinforcementLocation = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
				rc.readBroadcast(REINFORCEMENT_NEEDED_Y));

		while (true) {
			try {
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());

				// Check if it can go into combat or if it has a combat request
				if (enemyRobots.length > 0) {
					rc.broadcast(REINFORCEMENT_NEEDED_X, (int) enemyRobots[0].location.x);
					rc.broadcast(REINFORCEMENT_NEEDED_Y, (int) enemyRobots[0].location.y);
					reinforcementLocation = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
							rc.readBroadcast(REINFORCEMENT_NEEDED_Y));
					while (enemyRobots.length > 0) {
						System.out.println("Initiating attack sequence!");
						// avoidBullet();
						// It will only move to the nearest enemy if it has not
						// moved yet
						moveToNearestEnemy(enemyRobots);
						// Will shoot the nearest robot
						tryShoot();
						if (rc.getHealth() <= 10 && !aboutToDie) {
							aboutToDie = true;
							rc.broadcast(SOLDIER_COUNT_ARR, rc.readBroadcast(SOLDIER_COUNT_ARR) - 1);
						}
						Clock.yield();
						enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
					}
				}
				// Check if the robots reinforcement position is different from
				// the one in the array. If the robot is not currently going to
				// reinforce, it will
				// Set a new reinforcement position
				else if ((reinforcementLocation.x != rc.readBroadcast(REINFORCEMENT_NEEDED_X)
						|| reinforcementLocation.y != rc.readBroadcast(REINFORCEMENT_NEEDED_Y)) && hasReinforced) {
					System.out.println("New reinforcement location detected!");
					// If true, the robot will try to move to the location given
					reinforcementLocation = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
							rc.readBroadcast(REINFORCEMENT_NEEDED_Y));
					wanderReference = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
							rc.readBroadcast(REINFORCEMENT_NEEDED_Y));
					hasReinforced = false;
				}

				// It acts on the previous if statements. If there is a combat
				// request, it will move towards it. If none, it will wander
				if (!hasReinforced) {
					System.out.println("Moving to reinforce!");
					hasReinforced = smartMovement(reinforcementLocation);
					// If statement for debugging purposes
					if (hasReinforced) {
						System.out.println("Moved to reinforcement location!");
					}
				}
				// If it has no where to reinforce and there are no enemies in
				// sight, go to the initial desired location
				else if (!hasCheckedInitial) {
					System.out.println("Moving to enemy archon location!");
					hasCheckedInitial = smartMovement(moveReference);
					// If statement for debugging purposes
					if (hasCheckedInitial) {
						System.out.println("Initial enemy archon location checked!");
					}
				}
				// If there is nothing else to do, the soldier will wander
				else {
					// Will keep on setting new locations until one that is one
					// the map is found
					while (setNewWanderLocation) {
						System.out.println("Picking new location!");
						Direction directionToReference = rc.getLocation().directionTo(wanderReference);
						// These generate a random direction in relation to the
						// direction to the reference point
						int randomDegree = (int) (Math.random() * 271);
						wanderDirection = directionToReference.rotateLeftDegrees(135);
						wanderDirection = wanderDirection.rotateRightDegrees(randomDegree);

						// Creates a random distance to travel in-between 5-15
						// units
						randomDistance = (int) (Math.random() * 11 + 5);

						if (rc.onTheMap(rc.getLocation().add(wanderDirection, 7))) {
							System.out.println("Location is on the map!");
							setNewWanderLocation = false;
							targetLocation = rc.getLocation().add(wanderDirection, randomDistance);
						}
					}
					if (!rc.onTheMap(rc.getLocation(), 4)) {
						setNewWanderLocation = true;
					}

					if (smartMovement(targetLocation)) {
						setNewWanderLocation = true;
					}
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	// -------------------------------------------------------------------------------------------------------------
	// LUMBERJACK PLAYER & METHODS

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

					int result = fulfillLumberjackRequest(tree, nearByTrees);
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
								if (tryMove(randDir, 10, 9)) {

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
				convertVictoryPoints(1000);
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

	static void tryUseStrike() throws GameActionException {
		if (rc.canStrike()) {
			TreeInfo[] friendlyTrees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);
			int count = 0;
			Team myTeam = rc.getTeam();
			for (TreeInfo tree : friendlyTrees) {
				if (tree.getTeam() == myTeam || (tree.getHealth() < 5 && tree.containedRobot != null)) {
					System.out.println("Lumberjack: " + rc.getID() + " could not use strike()!");
					return;
				}
			}
			RobotInfo[] friendlyRobots = rc.senseNearbyRobots(3, rc.getTeam());
			if (friendlyTrees.length > 0 || friendlyRobots.length > 0) {
				System.out.println("Lumberjack: " + rc.getID() + " could not use strike()!");
				return;
			}
			rc.strike();
		}
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

	static int fulfillLumberjackRequest(MapLocation tree, TreeInfo[] trees) throws GameActionException {
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

	// -------------------------------------------------------------------------------------------------------------
	// SCOUT PLAYER & METHODS

	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		rc.broadcast(SCOUT_COUNT_ARR, rc.readBroadcast(SCOUT_COUNT_ARR) + 1);

		Direction randomDirection = randomDirection();
		boolean aboutToDie = false;
		boolean hasCheckedInitial = false;
		MapLocation moveReference = new MapLocation(rc.readBroadcast(INITIAL_ENEMY_LOCATION_X), rc.readBroadcast(INITIAL_ENEMY_LOCATION_Y));
		
		while (true) {
			try {
				
				TreeInfo[] treeLocation = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
				if (!hasCheckedInitial) {
					System.out.println("Moving to enemy archon location!");
					hasCheckedInitial = smartMovement(moveReference);
					// If statement for debugging purposes
					if (hasCheckedInitial) {
						System.out.println("Initial enemy archon location checked!");
					}
				}
				else if (seeGardener()) {
					System.out.println("Finding gardener");
					findGardener();
					scoutAttack();
					System.out.println("Moved!");
				}
				else if (hasBullets()) {
					for (TreeInfo tree: treeLocation) {
						while (tree.containedBullets > 0) {
							if (rc.canShake(tree.location)) {
								rc.shake(tree.location);
								break;
							}
							else {
								Direction moveDirection = rc.getLocation().directionTo(tree.location);
								tryMove(moveDirection);
								Clock.yield();
							}
						}
					}
				}
				else {
					if (rc.canMove(randomDirection) && !rc.hasMoved()) {
						rc.move(randomDirection);
					}
					else {
						while (!rc.canMove(randomDirection)) {
							randomDirection = randomDirection();
						}
						rc.move(randomDirection);
					}
				}
				
				if (rc.getHealth() <= 10 && !aboutToDie) {
					aboutToDie = true;
					rc.broadcast(SCOUT_COUNT_ARR, rc.readBroadcast(SCOUT_COUNT_ARR) - 1);
				}
				
			} catch (Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}
	// This method shakes the nearby trees returned in the parameter. It will
	// shake all trees before sensing trees again.
	static void shakeTree(TreeInfo tree) throws GameActionException {
		MapLocation treeLoc = tree.getLocation();
		Direction moveDirection = rc.getLocation().directionTo(treeLoc);
		while (tree.getContainedBullets() > 0) {
			if (rc.canShake(treeLoc)) {
				rc.shake(treeLoc);
				return;
			} else {
				tryMove(moveDirection);
			}
			Clock.yield();
		}
	}

	static void findGardener() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, rc.getTeam().opponent());
		for (RobotInfo robot : enemyRobots) {
			if (robot.getType() == RobotType.GARDENER && rc.getLocation().distanceTo(robot.location) > 4.5) {
				tryMove(rc.getLocation().directionTo(robot.location));
				return;
			} else if (robot.getType() == RobotType.GARDENER) {
				float distance = (float) ((rc.getLocation().distanceTo(robot.location)) - 2.01);
				if (rc.canMove(rc.getLocation().directionTo(robot.location), distance)) {
					rc.move(rc.getLocation().directionTo(robot.location), distance);
					return;
				}
			}
		}
	}

	static void scoutAttack() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, rc.getTeam().opponent());
		for (RobotInfo robot : enemyRobots) {
			if (robot.getType() == RobotType.GARDENER && !rc.hasAttacked() && rc.canFireSingleShot()
					&& rc.getLocation().distanceTo(robot.getLocation()) < 2.5) {
				rc.fireSingleShot(rc.getLocation().directionTo(robot.location));
			}
		}
	}

	static boolean hasBullets() {
		TreeInfo[] treeLocation = rc.senseNearbyTrees(RobotType.SCOUT.sensorRadius, Team.NEUTRAL);
		for (TreeInfo tree : treeLocation) {
			if (tree.containedBullets > 0) {
				return true;
			}
		}
		return false;
	}

	static boolean seeGardener() {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, rc.getTeam().opponent());
		for (RobotInfo robot : enemyRobots) {
			if (robot.getType() == RobotType.GARDENER) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------------------------
	// TANK METHODS

	static void runTank() throws GameActionException {
		System.out.println("I'm a Tank!");
		rc.broadcast(TANK_COUNT_ARR, rc.readBroadcast(TANK_COUNT_ARR) + 1);
		int mapData = rc.readBroadcast(MAP_TYPE);

		Direction wanderDirection = Direction.NORTH;
		float randomDistance = 0;
		boolean aboutToDie = false;
		boolean hasReinforced = true;
		boolean hasCheckedInitial = false;
		boolean setNewWanderLocation = true;
		MapLocation moveReference = new MapLocation(rc.readBroadcast(INITIAL_ENEMY_LOCATION_X),
				rc.readBroadcast(INITIAL_ENEMY_LOCATION_Y));
		MapLocation wanderReference = new MapLocation(rc.readBroadcast(INITIAL_ENEMY_LOCATION_X),
				rc.readBroadcast(INITIAL_ENEMY_LOCATION_Y));
		MapLocation reinforcementLocation = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
				rc.readBroadcast(REINFORCEMENT_NEEDED_Y));

		while (true) {
			try {
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());

				// Check if it can go into combat or if it has a combat request
				if (enemyRobots.length > 0) {
					rc.broadcast(REINFORCEMENT_NEEDED_X, (int) enemyRobots[0].location.x);
					rc.broadcast(REINFORCEMENT_NEEDED_Y, (int) enemyRobots[0].location.y);
					reinforcementLocation = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
							rc.readBroadcast(REINFORCEMENT_NEEDED_Y));
					while (enemyRobots.length > 0) {
						System.out.println("Initiating attack sequence!");
						// avoidBullet();
						// It will only move to the nearest enemy if it has not
						// moved yet
						moveToNearestEnemy(enemyRobots);
						// Will shoot the nearest robot
						tryShoot();
						if (rc.getHealth() <= 10 && !aboutToDie) {
							aboutToDie = true;
							rc.broadcast(TANK_COUNT_ARR, rc.readBroadcast(TANK_COUNT_ARR) - 1);
						}
						Clock.yield();
						enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
					}
				}
				// Check if the robots reinforcement position is different from
				// the one in the array. If the robot is not currently going to
				// reinforce, it will
				// Set a new reinforcement position
				else if ((reinforcementLocation.x != rc.readBroadcast(REINFORCEMENT_NEEDED_X)
						|| reinforcementLocation.y != rc.readBroadcast(REINFORCEMENT_NEEDED_Y)) && hasReinforced) {
					System.out.println("New reinforcement location detected!");
					// If true, the robot will try to move to the location given
					reinforcementLocation = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
							rc.readBroadcast(REINFORCEMENT_NEEDED_Y));
					wanderReference = new MapLocation(rc.readBroadcast(REINFORCEMENT_NEEDED_X),
							rc.readBroadcast(REINFORCEMENT_NEEDED_Y));
					hasReinforced = false;
				}

				// It acts on the previous if statements. If there is a combat
				// request, it will move towards it. If none, it will wander
				if (!hasReinforced) {
					System.out.println("Moving to reinforce!");
					hasReinforced = smartMovement(reinforcementLocation);
					// If statement for debugging purposes
					if (hasReinforced) {
						System.out.println("Moved to reinforcement location!");
					}
				}
				// If it has no where to reinforce and there are no enemies in
				// sight, go to the initial desired location
				else if (!hasCheckedInitial) {
					System.out.println("Moving to enemy archon location!");
					hasCheckedInitial = smartMovement(moveReference);
					// If statement for debugging purposes
					if (hasCheckedInitial) {
						System.out.println("Initial enemy archon location checked!");
					}
				}
				// If there is nothing else to do, the soldier will wander
				else {
					// Will keep on setting new locations until one that is one
					// the map is found
					while (setNewWanderLocation) {
						System.out.println("Picking new location!");
						Direction directionToReference = rc.getLocation().directionTo(wanderReference);
						// These generate a random direction in relation to the
						// direction to the reference point
						int randomDegree = (int) (Math.random() * 271);
						wanderDirection = directionToReference.rotateLeftDegrees(135);
						wanderDirection = wanderDirection.rotateRightDegrees(randomDegree);

						// Creates a random distance to travel in-between 5-15
						// units
						randomDistance = (int) (Math.random() * 11 + 5);

						if (rc.onTheMap(rc.getLocation().add(wanderDirection, 5))) {
							System.out.println("Location is on the map!");
							setNewWanderLocation = false;
						}
					}
					MapLocation targetLocation = rc.getLocation().add(wanderDirection, randomDistance);
					smartMovement(targetLocation);
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Tank Exception");
				e.printStackTrace();
			}
		}
	}

	// --------------------------------------------------------------------------------------------------------------
	// MOVING / DIRECTION METHODS

	static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
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

	static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	static boolean tryMoveToLocation(MapLocation loc, float degreeOffset, int checksPerSide)
			throws GameActionException {
		Direction dirTo = rc.getLocation().directionTo(loc);
		return tryMove(dirTo, degreeOffset, checksPerSide);
	}

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

	// Will try to move to the prioritized enemy. Needed to make this kind of
	// long because of the different cases with robots
	static void moveToNearestEnemy(RobotInfo[] enemyLocations) throws GameActionException {
		if (!rc.hasMoved()) {
			RobotInfo[] enemyRobots = enemyLocations;

			// If the robot type of the enemy is a soldier or tank, move towards
			// it and return early
			for (RobotInfo enemy : enemyRobots) {
				if (enemy.getType() == RobotType.SOLDIER || enemy.getType() == RobotType.TANK) {
					MapLocation enemyLocation = enemy.getLocation();
					Direction toEnemy = rc.getLocation().directionTo(enemyLocation);
					tryMove(toEnemy);
					System.out.println("Ran moveToNearestEnemy!");
					return;
				} else if (enemy.getType() == RobotType.LUMBERJACK) {
					MapLocation enemyLocation = enemy.getLocation();
					Direction toEnemy = rc.getLocation().directionTo(enemyLocation);
					if (rc.getLocation().distanceTo(enemyLocation) > 4) {
						tryMove(toEnemy);
					} else {
						tryMove(toEnemy.opposite());
					}
					System.out.println("Ran moveToNearestEnemy!");
					return;
				}
			}

			// If no tanks or soldiers are detected, look for a gardener or
			// scout first before attacking the archon
			for (RobotInfo enemy : enemyRobots) {
				if (enemy.getType() == RobotType.GARDENER) {
					MapLocation enemyLocation = enemy.getLocation();
					Direction toEnemy = rc.getLocation().directionTo(enemyLocation);
					tryMove(toEnemy);
					System.out.println("Ran moveToNearestEnemy!");
					return;
				}
				// If the robot is an archon, there must be no other enemies in
				// range
				else if (enemy.getType() == RobotType.ARCHON && enemyRobots.length == 1) {
					MapLocation enemyLocation = enemyRobots[0].getLocation();
					Direction toEnemy = rc.getLocation().directionTo(enemyLocation);
					tryMove(toEnemy);
					System.out.println("Ran moveToNearestEnemy!");
					return;
				}
			}
		}
	}

	// Smart path finding. Need a while statement, while no enemies are sensed,
	// run this code. Will stop running this method if an enemy is detected
	static boolean smartMovement(MapLocation destination) throws GameActionException {
		Direction directionToDestination = rc.getLocation().directionTo(destination);

		// Returns true if the robot is within stride radius of its destination.
		// If the robot can move in the direction to the destination, move
		if (rc.canMove(directionToDestination)) {
			System.out.println("Moved normally!");
			rc.move(directionToDestination);
			if (rc.isLocationOccupiedByTree(rc.getLocation().add(directionToDestination, 2))) {
				rc.firePentadShot(directionToDestination);
			}
		}
		// If the robot cannot move in the direction to its destination, find
		// the next possible position to move to
		else {
			System.out.println("Trying to find another direction!");
			Direction smartMove = nextAvaliableDirection(directionToDestination);
			if (rc.canMove(smartMove)) {
				System.out.println("Using nextAvaliableDirection to move!");
				rc.move(smartMove);
			}
		}
		rc.setIndicatorDot(destination, 0, 0, 0);
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 0);

		// If a move has gotten it close enough to its destination, it has moved
		// to its destination and will return true
		if (rc.getType().strideRadius + 2 >= rc.getLocation().distanceTo(destination)) {
			System.out.println("Reached Destination!");
			return true;
		}
		return false;
	}

	// This is needed because the robot will always keep the wall to its left.
	// Therefore, it can only rotate one direction
	static Direction nextAvaliableDirection(Direction destination) {
		for (int angle = 0; angle < 360; angle = angle + 1) {
			if (rc.canMove(destination.rotateRightDegrees(angle))) {
				System.out.println("Rotated right: " + angle + " degrees!");
				return destination.rotateRightDegrees(angle);
			}
		}
		System.out.println("ERROR in smart pathfinding!");
		return null;
	}

	// -----------------------------------------------------------------------------------------------------------------------
	// BULLET METHODS

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
					// System.out.println("Perpendicular Distance: " +
					// perpendicularDist);
					// System.out.println("Moved Right!");
					rc.move(propagationDirection.rotateRightRads((float) Math.PI / 2));
				} else {
					tryMove((propagationDirection.rotateRightRads((float) Math.PI / 2)), 10, 3);
				}
			} else {
				if (rc.canMove(propagationDirection.rotateLeftRads((float) Math.PI / 2))) {
					// System.out.println("Perpendicular Distance: " +
					// perpendicularDist);
					// System.out.println("Moved Left!");
					rc.move(propagationDirection.rotateLeftRads((float) Math.PI / 2));
				} else {
					tryMove((propagationDirection.rotateLeftRads((float) Math.PI / 2)), 10, 3);
				}
			}
			return true;
		}
		return false;
	}

	static boolean willHitFriendly(Direction dir) {
		RobotInfo[] friendlyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam());
		for (RobotInfo robot : friendlyRobots) {
			// Looks through an array of all the friendly robots near you
			Direction directionToRobot = rc.getLocation().directionTo(robot.getLocation());
			float distanceToRobot = rc.getLocation().distanceTo(robot.getLocation());
			float theta = Math.abs(directionToRobot.radiansBetween(dir));
			float perpendicularDistance = (float) (Math.sin((double) theta) * distanceToRobot);
			// If the perpendicular distance is less than or equal to the robots
			// radius, the bullet will hit it
			if (perpendicularDistance <= robot.getRadius()) {
				// Immediately break return true if it will hit a friendly
				return true;
			}
		}
		return false;
	}

	static void tryShoot() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			MapLocation myLocation = rc.getLocation();
			// Loops through all the enemies nearby starting with the closest
			// one
			for (int counter = 0; counter < enemyRobots.length; counter++) {
				Direction directionToEnemy = myLocation.directionTo(enemyRobots[counter].getLocation());
				// Checks if it will hit a friendly or not
				if (!willHitFriendly(directionToEnemy) && !treeInWay(directionToEnemy)) {
					float distanceToEnemy = myLocation.distanceTo(enemyRobots[counter].getLocation())
							+ rc.getType().bodyRadius;
					System.out.println("Distance to enemy: " + distanceToEnemy);
					if (distanceToEnemy < 6 && rc.canFirePentadShot()) {
						rc.firePentadShot(directionToEnemy);
						System.out.println("Pentad shot!");
						return;
					} else if (distanceToEnemy < 9 && rc.canFireTriadShot()) {
						rc.fireTriadShot(directionToEnemy);
						System.out.println("Triad shot!");
						return;
					}
				}
			}
		} else {
			System.out.println("No enemies detected!");
		}
	}

	static boolean treeInWay(Direction dir) {
		TreeInfo[] allTrees = rc.senseNearbyTrees(4, Team.NEUTRAL);
		for (TreeInfo tree : allTrees) {
			// Looks through an array of all the trees near you
			Direction directionToTree = rc.getLocation().directionTo(tree.getLocation());
			float distanceToTree = rc.getLocation().distanceTo(tree.getLocation());
			float theta = Math.abs(directionToTree.radiansBetween(dir));
			float perpendicularDistance = (float) (Math.sin((double) theta) * distanceToTree);
			// If the perpendicular distance is less than or equal to the trees
			// radius, the bullet will hit it
			if (perpendicularDistance <= tree.getRadius() && distanceToTree <= 4) {
				// Immediately break return true if it will hit a tree
				return true;
			}
		}
		return false;
	}

	static boolean tryMove(Direction dir) throws GameActionException {
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		for (int angle = 5; angle <= 180; angle = angle + 5) {
			// Try the offset of the left side
			if (rc.canMove(dir.rotateLeftDegrees(angle))) {
				rc.move(dir.rotateLeftDegrees(angle));
				return true;
			}
			// Try the offset on the right side
			if (rc.canMove(dir.rotateRightDegrees(angle))) {
				rc.move(dir.rotateRightDegrees(angle));
				return true;
			}
		}
		// A move never happened, so return false.
		System.out.println("Robot: " + rc.getID() + " Could not move!");
		return false;
	}

	// --------------------------------------------------------------------------------------------------------------
	// RUNTIME METHODS

	static void avoidBullet() throws GameActionException {
		BulletInfo[] allNearbyBullets = rc.senseNearbyBullets();
		// System.out.println("Number of bullets detected "+
		// allNearbyBullets.length);
		if (allNearbyBullets.length > 0) {
			for (BulletInfo bullet : allNearbyBullets) {
				if (willCollideWithMe(bullet)) {
					// System.out.println("Dodged bullet!");
					return;
				}
			}
		}
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

	/**
	 * 
	 * @return
	 * @throws GameActionException
	 */
	static void updateEnemyLocation(RobotInfo[] robots) throws GameActionException {
		// if at the Enemy Location, and no enemy robots are found
		if (rc.getLocation()
				.distanceTo(new MapLocation(rc.readBroadcast(ENEMY_X) / 100000, rc.readBroadcast(ENEMY_Y) / 100000)) < 3
				&& rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0) {

		}
	}

	// --------------------------------------------------------------------------------------------------------------
	// STARTING ORIENTATION / CENTER / MAP INFO

	static MapLocation getMapCenter() {
		// gets initial locations of both teams
		MapLocation[] archonLocationF = rc.getInitialArchonLocations(rc.getTeam());
		MapLocation[] archonLocationE = rc.getInitialArchonLocations(rc.getTeam().opponent());
		ArrayList<MapLocation> midPoints = new ArrayList<MapLocation>();
		// store the number of times the midpoint is found
		ArrayList<Integer> midPointCounts = new ArrayList<Integer>();
		// compares each archon from one team with the archons from the other
		// team
		for (int i = 0; i < archonLocationF.length; i++) {
			for (int j = 0; j < archonLocationE.length; j++) {
				// get midpoint between the two archons
				MapLocation midPoint = new MapLocation(
						(archonLocationE[j].x - archonLocationF[i].x) / 2 + archonLocationF[i].x,
						(archonLocationE[j].y - archonLocationF[i].y) / 2 + archonLocationF[i].y);
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
		int maxCount = 0;
		for (int i = 0; i < midPoints.size(); i++) {
			if (midPointCounts.get(i) > midPointCounts.get(maxCount)) {
				maxCount = i;
			}
		}
		// System.out.println(midPoints);
		System.out.println(midPoints.get(maxCount));
		return midPoints.get(maxCount);
	}

	static int getMapType() {
		boolean enclosed = false;
		boolean small = true;
		// count number of trees nearby
		TreeInfo[] trees = rc.senseNearbyTrees();
		int treeHealth = 0;
		for (TreeInfo tree : trees) {
			treeHealth += tree.getHealth();
		}
		System.out.println("messured TreeHealth:" + treeHealth);
		if (treeHealth > 3000) {
			enclosed = true;
		}
		float[] size = guessMapSize();
		// if greater than a certain area
		System.out.println("max Dist:" + Math.sqrt(size[0] * size[0] + size[1] * size[1]));
		if (Math.sqrt(size[0] * size[0] + size[1] * size[1]) > 55) {
			small = false;
		}
		if (small && enclosed) {
			// 1 = Small and Enclosed
			return 1;
		}
		if (small && !enclosed) {
			// 2 Small not enclosed
			return 2;
		}
		if (!small && enclosed) {
			// 3 = big and Enclosed
			return 3;
		}
		if (!small && !enclosed) {
			// 4 = Big and Open
			return 4;
		}
		// should never return 5;
		return 5;
	}

	static float[] guessMapSize() {

		float w = 0;// max distance between archons width
		float h = 0;// max distance between archons height
		MapLocation[] ALocs = rc.getInitialArchonLocations(Team.A);
		MapLocation[] BLocs = rc.getInitialArchonLocations(Team.B);
		// gets distances between archons and sets w and h to the maximum
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
}
