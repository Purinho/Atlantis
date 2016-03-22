package atlantis.combat.micro.zerg;

import atlantis.combat.group.AtlantisGroupManager;
import atlantis.enemy.AtlantisEnemyInformation;
import atlantis.enemy.AtlantisMap;
import atlantis.scout.AtlantisScoutManager;
import atlantis.wrappers.SelectUnits;
import jnibwapi.Position;
import jnibwapi.Unit;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 */
public class ZergOverlordManager {

    public static void update(Unit unit) {

        // We know enemy building
        if (AtlantisEnemyInformation.hasDiscoveredEnemyBuilding()) {
            actWhenWeKnowEnemy(unit);
        } 

        // =========================================================
        // We don't know any enemy building
        else {
            actWhenDontKnowEnemyLocation(unit);
        }
    }

    // =========================================================
    
    /**
     * We know at least one enemy building location.
     */
    private static void actWhenWeKnowEnemy(Unit overlord) {
        Position goTo = null; 
        
        goTo = AtlantisMap.getMainBaseChokepoint();
        if (goTo == null) {
            goTo = SelectUnits.mainBase();
        }

//        unit.setTooltip("Retreat");
//        if (goTo != null && goTo.distanceTo(unit) > 3) {
//            unit.setTooltip("--> Retreat");
//            unit.move(goTo, false);
//        }

//        Position medianUnitPosition = AtlantisGroupManager.getAlphaGroup().getMedianUnitPosition();
//        if (medianUnitPosition != null) {
//            if (medianUnitPosition.distanceTo(overlord) > 1) {
//                overlord.move(medianUnitPosition);
//            }
//        }

        if (goTo != null) {
            overlord.move(goTo);
        }
    }

    /**
     * We don't know at any enemy building location.
     */
    private static void actWhenDontKnowEnemyLocation(Unit unit) {
        AtlantisScoutManager.tryToFindEnemy(unit);
        unit.setTooltip("Find enemy");
    }

}
