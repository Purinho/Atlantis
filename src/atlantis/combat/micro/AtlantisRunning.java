package atlantis.combat.micro;

import atlantis.Atlantis;
import atlantis.AtlantisGame;
import atlantis.combat.AtlantisCombatEvaluator;
import atlantis.combat.AtlantisCombatInformation;
import atlantis.debug.tooltip.TooltipManager;
import atlantis.enemy.AtlantisMap;
import atlantis.units.AUnit;
import atlantis.util.PositionUtil;
import atlantis.units.Select;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import bwapi.Position;
import bwta.BWTA;
import java.util.ArrayList;

/**
 * Handles best way of running from close enemies and information about the fact if given unit is running or
 * not.
 */
public class AtlantisRunning {

    private AUnit unit;
    private Position nextPositionToRunTo = null;
    private int lastRunTime = -1;

    /**
     * Maps AUnit to AtlantisRunning instances (to remove unit.isRunning method)
     */
//    private static Map<AUnit, AtlantisRunning> unitRunning = new HashMap<>();
//    private static ArrayList<AtlantisRunning> unitRunning = new ArrayList<>();
    // =========================================================
    public AtlantisRunning(AUnit unit) {
        super();
        this.unit = unit;
//        unitRunning.put(unit, this);
//        unitRunning.add(this);
    }

    // =========================================================
    // Hi-level methods
    /**
     * Indicates that this unit should be running from given enemy unit.
     */
    public static boolean runFrom(AUnit runner, AUnit chaser) {

        if (chaser == null) {
            chaser = Select.enemyRealUnits().nearestTo(runner.getPosition());
        }

        if (chaser == null) {
            return false;
        }
        /*else {
            return running.runFrom(nearestEnemy);
        }*/
//        checkRunningInfo(runner);
//        AtlantisRunning running = unitRunning.get(runner);
        AtlantisRunning running = new AtlantisRunning(runner);
        // Define position to run to
        running.nextPositionToRunTo = getPositionAwayFrom(runner, chaser.getPosition());

        // Remember the last time of the decision
        if (running.nextPositionToRunTo != null) {
            running.lastRunTime = AtlantisGame.getTimeFrames();
        }

        // =========================================================
        // Update tooltip
        if (running.nextPositionToRunTo != null) {
            running.updateRunTooltip();
        } else {
            TooltipManager.removeTooltip(runner);
            //unit.removeTooltip();
        }

        // =========================================================
        // Make unit run to the selected position
        if (running.nextPositionToRunTo != null && !running.nextPositionToRunTo.equals(runner.getPosition())) {
            runner.move(running.nextPositionToRunTo);
            running.updateRunTooltip();

            // If this is massive retreat, make all other units run as well
            if (AtlantisCombatEvaluator.evaluateSituation(runner) < 0.2) {
                running.notifyOurUnitsAroundToRunAsWell(runner, chaser);
            }

            return true;
        }

        return false;
    }

    /**
     *
     */
    public static Position getPositionAwayFrom(AUnit unit, Position runAwayFrom) {
        if (unit == null || runAwayFrom == null) {
            return null;
        }

//        if (AtlantisGame.getTimeSeconds() <= 350) {
        return findPositionToRun_preferMainBase(unit, runAwayFrom);
//        }
//        else {
//            return findPositionToRun_dontPreferMainBase(unit, runAwayFrom);
//        }
    }

    /**
     * Every unit that is relatively close to the unit that wants to run, should run as well, otherwise it
     * might block the escape route.
     */
    private void notifyOurUnitsAroundToRunAsWell(AUnit ourUnit, AUnit nearestEnemy) {

        // Get all of our units that are close to this unit. 
        //TODO: this cast seems to be safe, as units will be visible
        Collection<AUnit> ourUnitsNearby = (Collection<AUnit>) Select.our().inRadius(1.5, ourUnit.getPosition()).listUnits();

        // Tell them to run as well, not to block our escape route
        for (AUnit ourOtherUnit : ourUnitsNearby) {
            if (!isRunning(ourOtherUnit)) {
                runFrom(ourOtherUnit, null);
            }
        }
    }

    // =========================================================
    // Find position to run away
    /**
     * Running behavior which will make unit run toward main base.
     */
    private static Position findPositionToRun_preferMainBase(AUnit unit, Position runAwayFrom) {
        AUnit mainBase = Select.mainBase();
        if (mainBase != null) {
            if (PositionUtil.distanceTo(mainBase, unit) > 5) {
                return mainBase.getPosition();
//                return mainBase.translated(0, 3 * 64);
            }
        }

        return findPositionToRun_dontPreferMainBase(unit, runAwayFrom);
    }

    /**
     * Running behavior which will make unit run <b>NOT</b> toward main base, but <b>away from the enemy</b>.
     */
    private static Position findPositionToRun_dontPreferMainBase(AUnit unit, Position runAwayFrom) {
        int howManyTiles = 6;
        int maxTiles = 9;
        Position runTo = null;

        // =========================================================
        while (howManyTiles <= maxTiles) {
            double xDirectionToUnit = runAwayFrom.getX() - unit.getPosition().getX();
            double yDirectionToUnit = runAwayFrom.getY() - unit.getPosition().getY();

            double vectorLength = PositionUtil.distanceTo(runAwayFrom, unit.getPosition());
            double ratio = howManyTiles / vectorLength;

            // Add randomness of move if distance is big enough
            //        int xRandomness = howManyTiles > 3 ? (2 - AtlantisUtilities.rand(0, 4)) : 0;
            //        int yRandomness = howManyTiles > 3 ? (2 - AtlantisUtilities.rand(0, 4)) : 0;
            runTo = new Position(
                    (int) (unit.getPosition().getX() - ratio * xDirectionToUnit),
                    (int) (unit.getPosition().getY() - ratio * yDirectionToUnit)
            );

//            if (howManyTiles >= 8) {
            runTo = runTo.makeValid();
//            }

            if (Atlantis.getBwapi().isBuildable(runTo.toTilePosition(), true) && unit.hasPathTo(runTo.getPoint())
                    & Atlantis.getBwapi().hasPath(unit.getPosition(), runTo)
                    && BWTA.isConnected(unit.getPosition().toTilePosition(), runTo.toTilePosition())) {
                break;
            } else {
                howManyTiles++;
            }
        }

        // =========================================================
        if (runTo != null) {
            double dist = PositionUtil.distanceTo(unit.getPosition(), runTo);
            if (dist >= 0.8 && dist <= maxTiles + 1) {
                return runTo;
            }
        }

        return Select.mainBase().getPosition();
    }

    // =========================================================
    // Stop running
    public static void stopRunning(AUnit unit) {
//    	checkRunningInfo(unit);
//        unitRunning.get(u).nextPositionToRunTo = null;
        unit.getRunning().nextPositionToRunTo = null;
    }

    // =========================================================
    // Getters & Setters
    /**
     * Returns true if given unit is currently (this frame) running from an enemy.
     */
    public static boolean isRunning(AUnit unit) {
//    	checkRunningInfo(unit);
//        return unitRunning.get(unit).nextPositionToRunTo != null;
        return unit.getRunning().nextPositionToRunTo != null;
    }

    /**
     * Checks whether AtlantisCombatInformation exists for a given unit, creating an instance if necessary
     *
     * @param unit
     */
//	private static void checkRunningInfo(AUnit unit) {
//		if (!unitRunning.containsKey(unit)){
//			unitRunning.put(unit, new AtlantisRunning(unit));
//    	}
//	}
    public AUnit getUnit() {
        return unit;
    }

    /**
     * Returns the position where unit is running to (it's quite close to the unit, few tiles).
     */
    public static Position getNextPositionToRunTo(AUnit unit) {
//    	checkRunningInfo(u);
        return unit.getRunning().nextPositionToRunTo;
    }

    public static int getTimeSinceLastRun(AUnit unit) {
//    	checkRunningInfo(unit);
        return AtlantisGame.getTimeFrames() - unit.getRunning().lastRunTime;
    }

    private void updateRunTooltip() {
        String runTimer = String.format("%.1f",
                ((double) AtlantisRunManager.getHowManyFramesUnitShouldStillBeRunning(unit) / 30));
        TooltipManager.setTooltip(unit, "Run " + runTimer + "s");  //unit.setTooltip("Run " + runTimer + "s");
    }

}