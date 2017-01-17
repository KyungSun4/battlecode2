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
		System.out.println(guessMapSize()[0] + ", " + guessMapSize()[1]);
		while (true) {
			try {
				if (rc.readBroadcast(1) == 0) {
					// rc.hireGardener(Direction.getNorth());
					int tempGardener = rc.readBroadcast(1);
					tempGardener++;
					rc.broadcast(1, tempGardener);
					System.out.println(rc.readBroadcast(1));
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
		int treeCounter = 0;
		int lumberjack = 0;
		Direction treeDir = Direction.getEast().rotateLeftRads((float)(Math.PI/2));
		ArrayList<MapLocation> myTrees = new ArrayList<MapLocation>();
		while (true) {
			try {
				if (lumberjack == 0)
				{
					rc.buildRobot(RobotType.LUMBERJACK, Direction.getNorth());
					lumberjack++;
				}
					
				/*if (treeCounter <= 4 && rc.canPlantTree(treeDir)) {
					rc.plantTree(treeDir);
					treeCounter++;
					treeDir = treeDir.rotateLeftRads((float)(Math.PI/2));
				}*/
				
				
				
				
				
				
				
				
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	static void runScout() throws GameActionException {
		System.out.println("I'm an Scout!");
		Team enemy = rc.getTeam().opponent();
		while (true) {
			try {
				MapLocation myLocation = rc.getLocation();

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

				// Move randomly
				// tryMove(randomDirection());

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
		boolean busy = false;
		int treeID = 0;
		while (true) {
			try {
				if (!busy)
				{
					searchTree:
					{
						TreeInfo arr[] = rc.senseNearbyTrees();
						for (TreeInfo x: arr){
							RobotType tree = x.getContainedRobot();
							if (tree != null)
							{
								treeID = x.getID();
								System.out.println(x.getContainedRobot());
								busy = true;
								break searchTree;
							}
						}
					}
				}
				Direction temp = Direction((float)(Math.PI)*(3/4));
				rc.move(temp);
				
				Clock.yield();
			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

	private static Direction Direction(float f) {
		// TODO Auto-generated method stub
		return null;
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
		// if nescesary, evade
		// get bullets that will hit soon ad avoid them
		BulletInfo[] twoStrideBullets = rc.senseNearbyBullets(RobotType.SCOUT.strideRadius * 2);
		for (BulletInfo i : twoStrideBullets) {
			avoidBullet(i);
		}
		// keep view distance away from other scouts or if alone, try and scan
		// map
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
	 * checks if tree in message array list
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	static boolean checkForTreeInList(float x, float y) {
		return true;
	}

	/**
	 * adds tree to Message array
	 * 
	 * @throws GameActionException
	 * 
	 */

	static boolean addTreeToList(int type, float x, float y) throws GameActionException {
		// starts in broadcast array at TREE_POS_ARR_START, currently goes till
		// end of array i increments by 3 skiping over positons
		for (int i = TREE_POS_ARR_START; i < 997; i += 3) {
			// if empty spot in array, add
			if (rc.readBroadcast(i) == 0) {
				rc.broadcast(i, 1);
				rc.broadcast(i + 1, (int) (x * 100));
				rc.broadcast(i + 2, (int) (y * 100));
				return true;
			}
		}
		return false;
	}

	/**
	 * removes tree from Message array
	 * 
	 */
	static void removeTreeFromList() {
	}

	/**
	 * uses Initial arcon locations to guess map size
	 * 
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

	/**
	 * use robot's position to try and improve guesses for map size and origin.
	 * 
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
	 * uses inital arcon locations to find symetry
	 * 
	 * @return 0-failed 1-horizontal -- 2-vertical | 3-diagonal positive /
	 *         4-diagonal Negative \
	 */
	static int findMapSymetry() {
		return 0;
	}

	/**
	 * Gardners method to build tight ring of trees around itself and water,
	 * farm, and plant those trees.
	 * 
	 * @param myTrees
	 *            the trees that this gardner is maintaining
	 * @throws GameActionException 
	 */
	static void maintainTreeRing(ArrayList<MapLocation> myTrees) throws GameActionException {
		TreeInfo[] sensedTrees = rc.senseNearbyTrees(3); // all trees it can
															// water are within
															// 3 away
		ArrayList<TreeInfo> mySensedTrees = new ArrayList<TreeInfo>();
		// check if any of my trees died get rid of dead ones and put all living ones in mySensedTrees
		for (int i = 0;i<myTrees.size(); i++) {
			boolean notFound = true;
			for (TreeInfo sensedTree : sensedTrees) {
				if (sensedTree.getTeam() == rc.getTeam()) {
					if (sensedTree.location.equals(myTrees.get(i))) {
						mySensedTrees.add(sensedTree);
						notFound = false;
					}
				}
			}
			if(notFound) {
				//tree must have died so remove it
				myTrees.remove(i);
			}
		}
		//plants 4 trees around to start.
		if(mySensedTrees.size()<4) {
			if(rc.canPlantTree(Direction.getNorth())) {
				rc.plantTree(Direction.getNorth());
			}
			else if(rc.canPlantTree(Direction.getSouth())) {
				rc.plantTree(Direction.getSouth());
			}
			else if(rc.canPlantTree(Direction.getEast())) {
				rc.plantTree(Direction.getEast());
			}
			else if(rc.canPlantTree(Direction.getWest())) {
				rc.plantTree(Direction.getWest());
			}
		}
		
	}
}
