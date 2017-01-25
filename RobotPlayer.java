package battlecode2;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	// The indexes of the player array where the data is stored
	static int ARCHON_COUNT_ARR = 10;
	static int GARDENER_COUNT_ARR = 11;
	static int SOLDIER_COUNT_ARR = 12;
	static int LUMBERJACK_COUNT_ARR = 13;
	static int SCOUT_COUNT_ARR = 14;
	static int TANK_COUNT_ARR = 15;

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
				avoidBullet();
				runAway();
				System.out.println("Map type: " + rc.readBroadcast(0));
				if (rc.readBroadcast(1) == rc.getID() || rc.getRoundNum() > 400) {
					if (rc.readBroadcast(GARDENER_COUNT_ARR) <= 20) {
						tryBuildRobot(randomDirection(), 5, 10, RobotType.GARDENER);
					}

				}
				convertVictoryPoints(500);
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
					if (rc.readBroadcast(0) == 0) {
						int mapType = getMapType();
						System.out.println(mapType);
						rc.broadcast(0, mapType);
					}
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
					if (rc.readBroadcast(0) == 0) {
						int mapType = getMapType();
						System.out.println(mapType);
						rc.broadcast(0, mapType);
					}
					tryBuildRobot(randomDirection(), 1, 180, RobotType.GARDENER);
				} catch (GameActionException e) {
					System.out.println("Roudn two archon exception!");
					e.printStackTrace();
				}
			}
		}
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
		MapLocation start = rc.getInitialArchonLocations(rc.getTeam())[0];
		int count = 20;
		boolean dontTree = false;
		if(rc.getRoundNum()<3) {
			dontTree = true;
		}
		System.out.println("I'm a gardener!");
		rc.broadcast(GARDENER_COUNT_ARR, rc.readBroadcast(GARDENER_COUNT_ARR) + 1);
		int mapData = rc.readBroadcast(0);
		Direction awayDir = rc.getLocation().directionTo(start).opposite();
		if (rc.readBroadcast(SCOUT_COUNT_ARR) == 0) {
			tryBuildRobot(randomDirection(), 5, 10, RobotType.SCOUT);
			rc.broadcast(SCOUT_COUNT_ARR, rc.readBroadcast(SCOUT_COUNT_ARR) + 1);
		}

		while (true) {
			try {
				avoidBullet();
				if (count > 0) {
					count--;

					if (!tryMove(awayDir, 1, 90)) {
						awayDir = rc.getLocation().directionTo(start).opposite();

					}
					if (mapData == 1) {
						// Make lumberjacks then have a balance between
						// attacking
						// and farming
						mapTypeOneGardener();
					} else if (mapData == 2) {
						// Make soldiers then send them to attack
						tryBuildRobot(randomDirection(), 10, 9, RobotType.SOLDIER);
					} else if (mapData == 3) {
						// Make lumberjacks then do tree stuff
						mapTypeOneGardener();
						// maintainTreeRing();
					} else if (mapData == 4) {
						// Do tree stuff and make gardeners, maybe periodically
						// make
						// a soldier
						if (rc.readBroadcast(SOLDIER_COUNT_ARR) <= 3) {
							tryBuildRobot(randomDirection(), 10, 9, RobotType.SOLDIER);
						} else {
							// Tree stuff
							// maintainTreeRing();
						}
					}
				} else {
					if (rc.readBroadcast(LUMBERJACK_COUNT_ARR) <= 3) {
						tryBuildRobot(randomDirection(), 10, 9, RobotType.LUMBERJACK);
					}
					if (rc.readBroadcast(SOLDIER_COUNT_ARR) <= 3) {
						tryBuildRobot(randomDirection(), 10, 9, RobotType.SOLDIER);
					}
					if (rc.readBroadcast(TANK_COUNT_ARR) <= 3) {
						tryBuildRobot(randomDirection(), 10, 9, RobotType.TANK);
					}
					if(!dontTree) {
						maintainTreeRing();
					} else {
						if (!tryMove(awayDir, 1, 20)) {
							awayDir = rc.getLocation().directionTo(start).opposite();

						}
					}
					

				}
				convertVictoryPoints(500);
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	static void mapTypeOneGardener() throws GameActionException {
		if (rc.readBroadcast(GARDENER_COUNT_ARR) < 20) {
			tryBuildRobot(randomDirection(), 10, 9, RobotType.LUMBERJACK);
		} else {
			tryBuildRobot(randomDirection(), 10, 9, RobotType.SOLDIER);
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
			// System.out.println(dir);
		}
	}

	// -------------------------------------------------------------------------------------------------------------
	// SOLDIER PLAYER & METHODS

	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
		rc.broadcast(SOLDIER_COUNT_ARR, rc.readBroadcast(SOLDIER_COUNT_ARR) + 1);
		int mapData = rc.readBroadcast(0);
		while (true) {
			try {
				avoidBullet();
				moveToNearestEnemy();
				tryShoot();
				if (mapData == 1) {
					// Somewat aggro
					aggroAttack();
				} else if (mapData == 2) {
					// Aggro
					aggroAttack();
				} else if (mapData == 3) {
					// Likely no soldiers will be made
				} else if (mapData == 4) {
					// Defense
					if (!rc.hasMoved()) {
						rc.move(randomDirection());
					}
				}
				convertVictoryPoints(500);
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	static void mapTypeOneSoldier() {

	}

	static void aggroAttack() throws GameActionException {
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
		Direction moveDirection = new Direction(radianMove);
		if (!rc.hasMoved()) {
			tryMove(moveDirection, 10, 9);
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
				convertVictoryPoints(500);
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
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

	// -------------------------------------------------------------------------------------------------------------
	// SCOUT PLAYER & METHODS

	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		rc.broadcast(SCOUT_COUNT_ARR, rc.readBroadcast(SCOUT_COUNT_ARR) + 1);
		Direction moveDirection = randomDirection();
		boolean combatMode = false;
		while (true) {
			try {
				avoidBullet();
				// If a scout does not see an enemy, it will run this code
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
							tryMove(moveDirection, 10, 9);
							// Work on this not being in a random direction
						}
					}
					// Check every turn if there is an enemy nearby
					/*
					 * RobotInfo[] enemyLocation =
					 * rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius,
					 * rc.getTeam().opponent()); for (RobotInfo enemy:
					 * enemyLocation) { if (enemy.getType() != RobotType.SCOUT)
					 * { combatMode = true; break; } }
					 */
				} else if (combatMode) {

				}
				convertVictoryPoints(500);
				Clock.yield();
				
			} catch (Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}

	// This method shakes the nearby trees returned in the parameter. It will
	// shake all trees before sensing trees again.
	// This method will end once there are no more trees to shake.
	static void shakeTree(TreeInfo[] treeList) throws GameActionException {
		TreeInfo[] treeLocation = treeList;
		for (TreeInfo tree : treeLocation) {
			// This while loop will keep on trying to move the scout to the
			// nearest tree
			while (tree.getContainedBullets() > 0) {
				MapLocation treeLoc = tree.getLocation();
				Direction moveDirection = rc.getLocation().directionTo(treeLoc);
				if (rc.canShake(treeLoc)) {
					rc.shake(treeLoc);
					// If the nearest tree has been shook, break out of the
					// while loop and move on to the next tree in the for loop
					break;
				} else if (rc.canMove(moveDirection)) {
					rc.move(moveDirection);
				} else {
					tryMove(moveDirection, 10, 9);
				}
			}
		}
	}

	// static void combatMode() {

	// --------------------------------------------------------------------------------------------------------------
	// TANK METHODS

	static void runTank() throws GameActionException {
		System.out.println("I'm a Tank!");
		rc.broadcast(TANK_COUNT_ARR, rc.readBroadcast(TANK_COUNT_ARR) + 1);
		int mapData = rc.readBroadcast(0);
		while (true) {
			try {
				avoidBullet();
				tryShoot();
				convertVictoryPoints(500);
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

	// Starts at east then rotates counter clockwise to find the next available
	// space at increments of 30 degrees.
	static Direction nextUnoccupiedDirection(RobotType robot, int degrees) {
		Direction testDirection = Direction.getEast().rotateLeftDegrees(degrees);
		while (rc.canMove(testDirection) == false) {
			testDirection = testDirection.rotateLeftDegrees(90);
		}
		return testDirection;
	}

	// Method is called if an enemy is sensed
	static void attackEnemy(MapLocation enemyLocation) throws GameActionException {
		System.out.println("Attacking!");
		MapLocation myLocation = rc.getLocation();
		Direction directionToEnemy = myLocation.directionTo(enemyLocation);
		rc.fireSingleShot(directionToEnemy);
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

	static void moveToNearestEnemy() throws GameActionException {
		if (!rc.hasMoved()) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
			if (enemyRobots.length > 0) {
				MapLocation enemyLocation = enemyRobots[0].getLocation();
				Direction toEnemy = rc.getLocation().directionTo(enemyLocation);
				tryMove(toEnemy, 5, 10);
			}
		}
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
		// shoots with correct number of bullets in correct distance. Use as a
		// conditional to check if there is an enemy in range and call this
		// function

		// 0-2 distance shoot pent, 3-5 distance shoot triad, >6 distance shoot
		// single
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			MapLocation myLocation = rc.getLocation();
			// Loops through all the enemies nearby starting with the closest
			// one
			for (int counter = 0; counter < enemyRobots.length; counter++) {
				Direction directionToEnemy = myLocation.directionTo(enemyRobots[counter].getLocation());
				// Checks if it will hit a friendly or not
				if (!willHitFriendly(directionToEnemy)) {
					float distanceToEnemy = myLocation.distanceTo(enemyRobots[counter].getLocation());
					if (distanceToEnemy < 4 && rc.canFirePentadShot()) {
						rc.firePentadShot(directionToEnemy);
						return;
					} else if (distanceToEnemy < 7 && rc.canFireTriadShot()) {
						rc.fireTriadShot(directionToEnemy);
						return;
					}
				}
			}
		} else {
			System.out.println("No enemies detected!");
		}
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
		// System.out.println(midPoints.get(maxCount));
		return midPoints.get(maxCount);
	}

	static int getMapType() {
		boolean enclosed = false;
		boolean small = true;
		// count number of trees nearby
		TreeInfo[] trees = rc.senseNearbyTrees();
		if (trees.length >= 10) {
			enclosed = true;
		}
		float[] size = guessMapSize();
		// if greater than a certain area
		if (Math.sqrt(size[0] * size[0] + size[1] * size[1]) > 30) {
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
