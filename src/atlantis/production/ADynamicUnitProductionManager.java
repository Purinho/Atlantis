package atlantis.production;

import atlantis.AGame;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.Select;
import java.util.List;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 */
public class ADynamicUnitProductionManager {

    public static void update() {
        ADynamicWorkerProductionManager.handleDynamicWorkerProduction();
        
        if (AGame.playsAsTerran()) {
            trainMarinesForBunkersIfNeeded();
            constructFactoriesIfNeeded();
        }
        else if (AGame.playsAsProtoss()) {
            makeScarabsIfNeeded();
        }
    }
    
    // === Terran ========================================
    
    private static void constructFactoriesIfNeeded() {
        for (AUnit factory : Select.ourOfType(AUnitType.Terran_Factory).listUnits()) {
            if (!factory.isTrainingAnyUnit()) {
                boolean cantAffordTankButCanAffordVulture = AGame.hasMinerals(250)
                        && !AGame.hasGas(70);
                
                if (cantAffordTankButCanAffordVulture) {
                    factory.train(AUnitType.Terran_Vulture);
                }
            }
        }
    }
    
    private static void trainMarinesForBunkersIfNeeded() {
        int bunkers = Select.countOurOfType(AUnitType.Terran_Bunker);
        if (bunkers > 0) {
            int marines = Select.countOurOfType(AUnitType.Terran_Marine);
            int shouldHaveMarines = defineOptimalNumberOfMarines(bunkers);
            
            // Force at least one marine per bunker
            if (marines < shouldHaveMarines) {
                for (int i = 0; i < shouldHaveMarines - marines; i++) {
                    AUnit idleBarrack = Select.ourOneIdle(AUnitType.Terran_Barracks);
                    if (idleBarrack != null) {
                        idleBarrack.train(AUnitType.Terran_Marine);
                    }
                    else {
                        break;
                    }
                }
            }
        }
    }
    
    private static int defineOptimalNumberOfMarines(int bunkers) {
        if (bunkers <= 0) {
            return 0;
        }
        else if (bunkers <= 1) {
            return 1;
        }
        else {
            return 4 * bunkers;
        }
    }
    
    // === Protoss ========================================

    private static void makeScarabsIfNeeded() {
        List<AUnit> reavers = Select.ourOfType(AUnitType.Protoss_Reaver).listUnits();
        for (AUnit reaver : reavers) {
            if (reaver.getScarabCount() < 3 && !reaver.isTrainingAnyUnit()) {
                reaver.train(AUnitType.Protoss_Scarab);
            }
        }
    }
    
}
