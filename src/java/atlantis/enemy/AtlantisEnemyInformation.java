package atlantis.enemy;

import atlantis.wrappers.SelectUnits;
import jnibwapi.Position;
import jnibwapi.Unit;

/**
 * Provides various useful infromation about the enemy whereabouts or if even know any enemy building.
 */
public class AtlantisEnemyInformation {

    /**
     * Returns true if we learned the location of any still-existing enemy building.
     */
    public static boolean hasDiscoveredEnemyBuilding() {
        if (AtlantisEnemyUnits.enemyUnitsDiscovered.isEmpty()) {
            return false;
        }

        for (Unit enemy : AtlantisEnemyUnits.enemyUnitsDiscovered) {
            if (enemy.isBuilding()) {
                return true;
            }
        }
        return false;
    }

    /**
     * If we learned about at least one still existing enemy base it returns first of them. Returns null
     * otherwise.
     */
    public static Unit hasDiscoveredEnemyBase() {
        if (!hasDiscoveredEnemyBuilding()) {
            return null;
        }

        for (Unit enemyUnit : AtlantisEnemyUnits.enemyUnitsDiscovered) {
            if (enemyUnit.isBase()) {
                return enemyUnit;
            }
        }

        return null;
    }

    /**
     * Gets oldest known enemy base.
     */
    public static Position getEnemyBase() {
//        System.out.println(AtlantisUnitInformationManager.enemyUnitsDiscovered.size());
        for (Unit enemyUnit : AtlantisEnemyUnits.enemyUnitsDiscovered) {
//            System.out.println(enemyUnit);
            if (enemyUnit.isBase() && enemyUnit.isAlive()) {
                return enemyUnit;
            }
        }

        return null;
    }

    /**
     *
     */
    public static Unit getNearestEnemyBuilding() {
        Unit mainBase = SelectUnits.mainBase();
        if (mainBase != null && !AtlantisEnemyUnits.enemyUnitsDiscovered.isEmpty()) {
            return SelectUnits.from(AtlantisEnemyUnits.enemyUnitsDiscovered).buildings().nearestTo(mainBase);
        }
        return null;
    }

}
