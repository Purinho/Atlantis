package atlantis.combat.micro;

import atlantis.AGame;
import atlantis.debug.APainter;
import atlantis.information.AMap;
import atlantis.position.APosition;
import atlantis.scout.AScoutManager;
import atlantis.units.AUnit;
import atlantis.units.Select;
import atlantis.units.Units;
import atlantis.units.actions.UnitActions;
import atlantis.util.PositionUtil;
import bwapi.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 */
public class ARunManager {

    private AUnit unit;
    private APosition runTo;
    private int _updated_at = -1;
    private Units closeEnemies;

    // =========================================================
    public ARunManager(AUnit unit) {
        this.unit = unit;
    }

    // =========================================================
    private boolean makeUnitRun() {
        if (unit == null) {
            return false;
        } 
        
        // === Unit STUCK ======================================
        
        else if (unit.isStuck()) {
            unit.setTooltip("Stuck!!!");
            if (unit.isMoving()) {
                unit.holdPosition();
            }
            markAsNotRunning();
            return false;
        }
        
        // === Run to is EMPTY =================================
        
        else if (runTo == null) {
//            AGame.sendMessage(unit + " RunTo empty, hold");
            if (!unit.type().isTank()) {
                System.out.println(unit.getShortNamePlusId() + " RunTo empty");
            }
//            if (!closeEnemies.isEmpty() && unit.moveAwayFrom(closeEnemies.first().getPosition(), 0.5)) {
//                return true;
//            }
//            unit.holdPosition();
            markAsNotRunning();

            if (unit.isMoving()) {
                unit.holdPosition();
            }
            
            unit.setTooltip("My legs!");

            return false;
        } 
        
        // === Valid run position ==============================
        
        else {

            // Update last time run order was issued
            _updated_at = AGame.getTimeFrames();
//            APainter.paintLine(unit.getPosition(), runTo, Color.Orange);
//            boolean hasMoved = unit.move(runTo, UnitActions.RUN);
            unit.move(runTo, UnitActions.RUN);

            // Make all other units very close to it run as well
            notifyNearbyUnitsToMakeSpace(unit);

//            if (hasMoved) {
            return true;
//            } else {
//                APosition position = unit.getPosition();
////                APainter.paintLine(position.translateByPixels(-26, -26), position.translateByPixels(25, 25), Color.Red);
////                APainter.paintLine(position.translateByPixels(-25, -25), position.translateByPixels(26, 26), Color.Red);
////                APainter.paintLine(position.translateByPixels(-26, 26), position.translateByPixels(25, -25), Color.Red);
////                APainter.paintLine(position.translateByPixels(-25, 25), position.translateByPixels(26, -26), Color.Red);
//                markAsNotRunning();
//                return false;
//            }
        }
    }

    // =========================================================
    public boolean run() {

        // Define which enemies are considered as close enough to be dangerous
        closeEnemies = defineCloseEnemies(unit);
        if (closeEnemies.isEmpty()) {
            markAsNotRunning();
//            System.err.println("No enemies to run to for " + unit);
            return false;
        }

        // ===========================================
//        int maxEnemiesToRunFromNearestEnemy = unit.isVulture() ? 2 : 2;
        int maxEnemiesToRunFromNearestEnemy = 1;

        // ===========================================
        // Define "center of gravity" for the set of enemies
        APosition medianRunFrom;
        if (closeEnemies.size() <= maxEnemiesToRunFromNearestEnemy) {
            medianRunFrom = Select.from(closeEnemies.list()).nearestTo(unit).getPosition();
        } else {
            medianRunFrom = closeEnemies.median();
        }
        medianRunFrom = medianRunFrom.makeValid();

        // Run from given position
        return runFrom(medianRunFrom);
    }

    public boolean runFrom(Object unitOrPosition) {
        if (unitOrPosition == null) {
            System.err.println("Empty position to run to for " + unit);
            markAsNotRunning();
            return false;
        }

        APosition runFrom = null;
        if (unitOrPosition instanceof AUnit) {
            runFrom = ((AUnit) unitOrPosition).getPosition();
        } else if (unitOrPosition instanceof APosition) {
            runFrom = (APosition) unitOrPosition;
        }

//        if (runFrom != null) {
//            System.out.println("Run from " + runFrom + ", dist: " + runFrom.distanceTo(unit));
//        }
        // === Define run to position ==============================
        
        runTo = getPositionAwayFrom(unit, runFrom);
        
        // =========================================================

        if (runTo != null) {
            double dist = runTo.distanceTo(unit);
            unit.setTooltip("" + String.format("%.1f", dist));
        } else {
            unit.setTooltip("NULL");
        }

        // === Actual run order ====================================
        
        return makeUnitRun();
    }

    // =========================================================
    /**
     *
     */
    public static APosition getPositionAwayFrom(AUnit unit, APosition runAwayFrom) {
        if (unit == null || runAwayFrom == null) {
            return null;
        }

        AUnit mainBase = Select.mainBase();

        if (AGame.getTimeSeconds() <= 310 && mainBase != null && !unit.isWorker() && mainBase.distanceTo(unit) > 22) {
            return unit.getRunManager().findPositionToRun_preferMainBase(unit, runAwayFrom);
        } else {
            return unit.getRunManager().findPositionToRun_preferAwayFromEnemy(unit, runAwayFrom);
        }
    }

    // =========================================================
    /**
     * Running behavior which will make unit run toward main base.
     */
    private APosition findPositionToRun_preferMainBase(AUnit unit, APosition runAwayFrom) {
        AUnit mainBase = Select.mainBase();
        if (mainBase != null) {
            if (PositionUtil.distanceTo(mainBase, unit) > 5) {
                return mainBase.getPosition();
//                return mainBase.translated(0, 3 * 64);
            }
        }

        return findPositionToRun_preferAwayFromEnemy(unit, runAwayFrom);
    }

    /**
     * Running behavior which will make unit run <b>NOT</b> toward main base, but <b>away from the enemy</b>.
     */
    private APosition findPositionToRun_preferAwayFromEnemy(AUnit unit, APosition runAwayFrom) {
        APosition runTo = null;

        // === Run directly away from the enemy ========================================
        
//        if (closeEnemies != null && !unit.getPosition().isCloseToMapBounds()) {
//            runTo = findRunPositionShowYourBackToEnemy(unit, runAwayFrom);
//        }
        
        // === Get run to position - as far from enemy as possible =====================

        if (runTo == null) {
            double expectedLength = unit.isVulture() ? 5.5 : (unit.isWorker() ? 3 : 2);
            runTo = findRunPositionAtAnyDirection(unit, expectedLength);
        }
        
        // =============================================================================
        
        return runTo;
    }

    /**
     * Simplest case: add enemy-to-you-vector to your own position.
     */
    private static APosition findRunPositionShowYourBackToEnemy(AUnit unit, APosition runAwayFrom) {
        double minTiles = unit.isVulture() ? 5 : (unit.isWorker() ? 3 : 1.2);

        double maxDist = minTiles;

        double currentDist = maxDist;
        while (currentDist >= minTiles) {

            // Check if this is good position
            APosition runTo = canRunByShowingBackToEnemyTo(unit, runAwayFrom, currentDist, minTiles, maxDist);

            // Also check if can run further (avoid corner shitholes)
            if (runTo != null) {
                double distBonus = unit.isVulture() ? 0.6 : 1;
                APosition doubleRunTo = canRunByShowingBackToEnemyTo(
                        unit, runAwayFrom, currentDist + distBonus, minTiles, maxDist
                );

                // If is okay as well, return it
                if (doubleRunTo != null) {
                    return runTo;
                }
            }

            currentDist -= 2;
        }

        return null;
    }

    private static APosition canRunByShowingBackToEnemyTo(AUnit unit, APosition runAwayFrom,
            double dist, double minDist, double maxDist) {
        APosition runTo;
        double vectorLength = unit.getPosition().distanceTo(runAwayFrom);

        double vectorX = runAwayFrom.getX() - unit.getPosition().getX();
        double vectorY = runAwayFrom.getY() - unit.getPosition().getY();
        double ratio = dist / vectorLength;

        // Apply opposite 2D vector
        runTo = new APosition((int) (unit.getX() - ratio * vectorX), (int) (unit.getY() - ratio * vectorY));

        // === Ensure position is in bounds ========================================
        
        int oldX = runTo.getX();
        int oldY = runTo.getY();

        runTo = runTo.makeValidFarFromBounds();

        // If vector changed (meaning we almost reached the map boundaries) disallow it
        if (runTo.getX() != oldX || runTo.getY() != oldY) {
            return null;
        }
        
        // =========================================================

        // If run distance is acceptably long and it's connected, it's ok.
        if (isPossibleAndReasonablePosition(unit.getPosition(), runTo, dist * 0.6, 1.6 * dist, true)) {
            APainter.paintLine(unit.getPosition(), runTo, Color.Purple);
            APainter.paintLine(unit.getPosition().translateByPixels(-1, -1), runTo, Color.Purple);
            APainter.paintLine(unit.getPosition().translateByPixels(1, 1), runTo, Color.Purple);
            return runTo;
        } else {
            return null;
        }
    }

    /**
     * Returns a place where run to, searching in all directions, which is walkable, inbounds and most distant
     * to given runAwayFrom position.
     */
    private static APosition findRunPositionAtAnyDirection(AUnit unit, double expectedLength) {

        // === Define run from ========================================
        Units unitsInRadius = Select.enemyRealUnits().melee().inRadius(4, unit).units();
        APosition runAwayFrom = unitsInRadius.median();
        if (runAwayFrom == null) {
            return null;
        }

        // =========================================================
        
        APosition unitPosition = unit.getPosition();
        int tx = unitPosition.getTileX();
        int ty = unitPosition.getTileY();

        // Build list of possible run positions, basically around the clock
        ArrayList<APosition> potentialPositionsList = new ArrayList<>();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != -2 && dx != 2 && dy != -2 && dy != 2) {
                    continue;
                }

//                // Define point
//                APosition potentialPosition = APosition.createFrom(
//                        tx + dx * expectedVectorLength, ty + dy * expectedVectorLength
//                );
//                
//                // Scale vector if needed
//                double vectorLength = potentialPosition.distanceTo(unit) + 0.01;
//                if (Math.abs(vectorLength - expectedVectorLength) > 0.1) {
//                    potentialPosition = APosition.createFrom(
//                            (int) (tx + dx * vectorLength * expectedVectorLength / vectorLength), 
//                            (int) (ty + dy * expectedVectorLength / vectorLength)
//                    );
//                }
                // Define vevtor
                int vectorX = dx;
                int vectorY = dy;
                double vectorLength = Math.sqrt(vectorX * vectorX + vectorY * vectorY);

                // Normalize
                vectorX /= vectorLength;
                vectorY /= vectorLength;

                // Scale vector
                vectorX *= expectedLength;
                vectorY *= expectedLength;

                // Create position
                APosition potentialPosition = APosition.create(
                        (int) (tx + vectorX),
                        (int) (ty + vectorY)
                );

                // Make sure it's inbounds
//                    if (unitsInRadius.size() <= 1
//                            && (!unit.isWorker() || !unit.getPosition().isCloseToMapBounds())) {
//                    if (!unit.isWorker() || !unit.getPosition().isCloseToMapBounds()) {
//                        potentialPosition = potentialPosition.makeValidFarFromBounds();
//                    } else {
                    potentialPosition = potentialPosition.makeValidFarFromBounds();
//                        potentialPosition = potentialPosition.makeValid();
//                    }

                // If has path to given point, add it to the list of potential points
                APainter.paintLine(unitPosition, potentialPosition, Color.Red);
                if (isPossibleAndReasonablePosition(unitPosition, potentialPosition,
                        expectedLength * 0.6, 1.6 * expectedLength, false)) {
                    potentialPositionsList.add(potentialPosition);
                    APainter.paintLine(unitPosition, potentialPosition, Color.Orange);
                }
            }
        }
        
        // =========================================================
        // Find the location that would be most distant to the enemy location
        double mostDistant = -99999;
        APosition bestPosition = null;
        for (APosition position : potentialPositionsList) {
            double dist = runAwayFrom.distanceTo(position);
            if (bestPosition == null || dist >= mostDistant) {
                bestPosition = position;
                mostDistant = dist;
            }
        }
        
        // =========================================================

        if (bestPosition != null) {
            APainter.paintLine(unit, bestPosition, Color.Green);
            APainter.paintLine(unit.getPosition().translateByPixels(1, 1), bestPosition.translateByPixels(1, 1), Color.Green);
        }
        
//        AtlantisPainter.paintCircleFilled(unit.getPosition(), 7, Color.Purple);
//        AtlantisPainter.paintLine(unit.getPosition(), bestPosition, Color.Green);
//        AtlantisPainter.paintLine(unit.getPosition().translateByPixels(1, 1), bestPosition.translateByPixels(1, 1), Color.Green);
        return bestPosition;
    }

    private static Units defineCloseEnemies(AUnit unit) {
        double radius;

        if (unit.getType().isVulture()) {
            radius = 5;
        } else {
            radius = 5;
        }

        return Select.enemy().combatUnits().canAttack(unit, radius).units();
    }

    /**
     * Tell other units that might be blocking our escape route to move.
     */
    private void notifyNearbyUnitsToMakeSpace(AUnit unit) {
        double safetyRadiusSize = (unit.getType().getDimensionLeft() + unit.getType().getDimensionUp())
                / 64 * 1.35;

        Select<?> units = Select.ourRealUnits().inRadius(safetyRadiusSize, unit);
        List<AUnit> otherUnits = units.listUnits();
        for (AUnit otherUnit : otherUnits) {
            if (!otherUnit.isRunning() && !unit.equals(otherUnit)) {
                boolean result = otherUnit.runFrom(unit);
                otherUnit.setTooltip("Make space (" + otherUnit.distanceTo(unit) + ")");
            }
        }
    }

    // =========================================================
    /**
     * Returns true if given run position is traversable, land-connected and not very, very far
     */
    public static boolean isPossibleAndReasonablePosition(APosition unitPosition, APosition position,
            double minDist, double maxDist, boolean allowCornerPointsEtc) {
        
//        boolean isOkay = position.distanceTo(unit) > (minDist - 0.2) 
//                && AMap.isWalkable(position) && unit.hasPathTo(position)
//                && Select.all().inRadius(0.2, position).count() <= 1
//                //                && Atlantis.getBwapi().getUnitsInRadius(unit, 1).isEmpty()
//                //                && AtlantisMap.isWalkable(position.translateByTiles(-1, -1))
//                //                && AtlantisMap.isWalkable(position.translateByTiles(1, 1))
//                && AMap.getGroundDistance(unit, position) <= maxDist;
//                ;

        if (!AMap.isWalkable(position)) {
            APainter.paintCircleFilled(position, 10, Color.Red);
        }
        
        if (!AMap.isWalkable(position.translateTilesTowards(unitPosition, -1))) {
            APainter.paintCircleFilled(position, 16, Color.Yellow);
        }

//        boolean isOkay = AMap.isWalkable(position)
//                && unit.hasPathTo(position)
        boolean isOkay = AMap.isWalkable(position)
                && AMap.isWalkable(position.translateTilesTowards(unitPosition, -1))
//                && Select.neutral().inRadius(0.3, position).count() == 0
                && Select.all().inRadius(0.15, position).count() <= 1
                //                && Atlantis.getBwapi().getUnitsInRadius(unit, 1).isEmpty()
                //                && AtlantisMap.isWalkable(position.translateByTiles(-1, -1))
                //                && AtlantisMap.isWalkable(position.translateByTiles(1, 1))
//                && AMap.getGroundDistance(unit, position) <= maxDist;
                ;
        
//        System.err.println(unit + " @" + (int) AtlantisMap.getGroundDistance(unit, position));

        if (isOkay && !allowCornerPointsEtc) {
            isOkay = AMap.isPositionFarFromAnyRegionPolygonPoint(unitPosition);
        }

        return isOkay;
    }

    // === Getters ========================================
    public APosition getRunToPosition() {
        return runTo;
    }

    public boolean isRunning() {
        if (runTo != null) {
            int framesAgo = AGame.getTimeFrames() - _updated_at;
            if (framesAgo <= 1) {
                return true;
            } else {
                markAsNotRunning();
                return false;
            }
        } else {
            return false;
        }
    }

    public void markAsNotRunning() {
        runTo = null;
        _updated_at = -1;
    }

}