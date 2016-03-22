package atlantis.production;

import atlantis.AtlantisConfig;
import atlantis.AtlantisGame;
import atlantis.constructing.AtlantisConstructingManager;
import atlantis.production.strategies.AtlantisProductionStrategy;
import atlantis.production.strategies.ZergProductionStrategy;
import atlantis.wrappers.SelectUnits;
import java.util.ArrayList;
import jnibwapi.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;

public class AtlantisProduceUnitManager {

    /**
     * Is responsible for training new units and issuing construction requests for buildings.
     */
    protected static void update() {
        AtlantisProductionStrategy productionStrategy = AtlantisConfig.getProductionStrategy();

        ArrayList<ProductionOrder> produceNow = productionStrategy.getThingsToProduceRightNow(false);
        
//        if (!produceNow.isEmpty()) {
//            System.out.println("== Produce:");
//            for (ProductionOrder productionOrder : produceNow) {
//                System.out.println(productionOrder.getUnitType());
//            }
//        }
        
        for (ProductionOrder order : produceNow) {

            // =========================================================
            // Produce UNIT
            if (order.getUnitType() != null) {
                UnitType unitType = order.getUnitType();
                if (unitType.isBuilding()) {
                    produceBuilding(unitType, order);
                } else {
                    produceUnit(unitType);
                }
            } 

            // =========================================================
            // Produce UPGRADE
            else if (order.getUpgrade() != null) {
                UpgradeType upgrade = order.getUpgrade();
                researchUpgrade(upgrade);
            }
        }
    }

    // =========================================================
    // Hi-level produce
    
    private static void produceUnit(UnitType unitType) {
        AtlantisProductionStrategy.getProductionStrategy().produceUnit(unitType);
    }

    private static void researchUpgrade(UpgradeType upgrade) {
        UnitType buildingType = UnitTypes.getUnitType(upgrade.getWhatUpgradesTypeID());
        if (buildingType != null) {
            Unit building = SelectUnits.ourBuildings().ofType(buildingType).first();
            if (building != null) {
                building.upgrade(upgrade);
            }
        }
    }

    // =========================================================
    // Lo-level produce
    
    private static void produceBuilding(UnitType unitType, ProductionOrder order) {
        if (!unitType.isBuilding()) {
            System.err.println("produceBuilding has been given wrong argument: " + unitType);
        }
        
        AtlantisConstructingManager.requestConstructionOf(unitType, order, null);
    }
    
//    private static void produceWorker() {
//        AtlantisConfig.getProductionStrategy().produceWorker();
//    }
//
//    private static void produceInfantry(UnitType infantryType) {
//        AtlantisConfig.getProductionStrategy().produceInfantry(infantryType);
//    }

}
