package atlantis.workers;

import atlantis.AtlantisConfig;
import atlantis.AtlantisGame;
import atlantis.buildings.managers.AtlantisGasManager;
import atlantis.enemy.AtlantisEnemyUnits;
import atlantis.wrappers.SelectUnits;
import atlantis.wrappers.Units;
import java.util.Collection;
import jnibwapi.Unit;

/**
 * Manages all worker (SCV, Probe, Drone) actions.
 */
public class AtlantisWorkerCommander {

    /**
     * Executed only once per frame.
     */
    public static void update() {
        AtlantisGasManager.handleGasBuildings();
        handleNumberOfWorkersNearBases();

        for (Unit worker : SelectUnits.ourWorkers().list()) {
            
            // WORKER DEFENCE if needed: attack other workers, run from enemies etc.
            if (AtlantisWorkerDefenceManager.handleDefenceIfNeeded(worker)) {
                // Overrided and executed, do nothing
            }
            
            // Standard worker behavior
            else {
                AtlantisWorkerManager.update(worker);
            }
        }
    }

    // =========================================================
    
    public static boolean shouldTrainWorkers(boolean checkSupplyAndMinerals) {

        // Check MINERALS
        if (checkSupplyAndMinerals && AtlantisGame.getMinerals() < 50) {
            return false;
        }

        // Check FREE SUPPLY
        if (checkSupplyAndMinerals && AtlantisGame.getSupplyFree() == 0) {
            return false;
        }

        int workers = SelectUnits.ourWorkers().count();

        // Check if not TOO MANY WORKERS
        if (workers >= 27 * SelectUnits.ourBases().count()) {
            return false;
        }

        // Check if AUTO-PRODUCTION of WORKERS is active.
        if (workers >= AtlantisConfig.AUTO_PRODUCE_WORKERS_SINCE_N_WORKERS 
                && workers < AtlantisConfig.AUTO_PRODUCE_WORKERS_UNTIL_N_WORKERS
                && workers < AtlantisConfig.AUTO_PRODUCE_WORKERS_MAX_WORKERS
                && (!AtlantisGame.playsAsZerg() || SelectUnits.ourLarva().count() >= 2)) {
            return true;
        }
        // Check if ALLOWED TO PRODUCE IN PRODUCTION QUEUE
//        if (!AtlantisGame.getProductionStrategy().getThingsToProduceRightNow(false).contains(AtlantisConfig.WORKER)) {
//            return false;
//        }
//        if (!AtlantisGame.getProductionStrategy().getThingsToProduceRightNow(true).isEmpty()) {
//            return false;
//        }

        // // Check if not TOO MANY WORKERS
        // if (AtlantisUnitInformationManager.countOurWorkers() >= 27 * AtlantisUnitInformationManager.countOurBases())
        // {
        // return false;
        // }
        return false;
    }
    
    // =========================================================

    /**
     * Every base should have similar number of workers, more or less.
     */
    private static void handleNumberOfWorkersNearBases() {
        
        // Don't run every frame
        if (AtlantisGame.getTimeFrames() % 10 != 0) {
            return;
        }
        
        // =========================================================
        
        Collection<Unit> ourBases = SelectUnits.ourBases().list();
        if (ourBases.size() <= 1) {
            return;
        }
        
        // Count ratios of workers / minerals for every base
        Units baseWorkersRatios = new Units();
        for (Unit ourBase : ourBases) {
            int numOfWorkersNearBase = SelectUnits.ourWorkersThatGather().inRadius(15, ourBase).count();
            int numOfMineralsNearBase = SelectUnits.minerals().inRadius(10, ourBase).count() + 1;
            if (numOfWorkersNearBase <= 2) {
                continue;
            }
            double workersToMineralsRatio = (double) numOfWorkersNearBase / numOfMineralsNearBase;
//            System.out.println(ourBase + " / work:" + numOfWorkersNearBase + " / miner:" +numOfMineralsNearBase + " / RATIO:" + workersToMineralsRatio);
            baseWorkersRatios.setValueFor(ourBase, workersToMineralsRatio);
        }
        
        // Take the base with lowest and highest worker ratio
        Unit baseWithFewestWorkers = baseWorkersRatios.getUnitWithLowestValue();
        Unit baseWithMostWorkers = baseWorkersRatios.getUnitWithHighestValue();
        
        if (baseWithFewestWorkers == null || baseWithMostWorkers == null) {
//            System.err.println("baseWithFewestWorkers = " + baseWithFewestWorkers);
//            System.err.println("baseWithMostWorkers = " + baseWithMostWorkers);
            return;
        }
        
//        System.out.println("Fewest: " + baseWithFewestWorkers + " / " + baseWorkersRatios.getValueFor(baseWithFewestWorkers));
//        System.out.println("Most: " + baseWithMostWorkers + " / " + baseWorkersRatios.getValueFor(baseWithMostWorkers));
//        System.out.println();
        
        // If there's only 120% as many workers as minerals OR bases are too close, don't transfer
        if (baseWorkersRatios.getValueFor(baseWithMostWorkers) <= 1.2 || 
                baseWithMostWorkers.distanceTo(baseWithFewestWorkers) < 10) {
            return;
        }
        
        // If the difference is "significant" transfer one worker from base to base
        if (baseWorkersRatios.getValueFor(baseWithMostWorkers) - 0.1 > 
                baseWorkersRatios.getValueFor(baseWithFewestWorkers)) {
            Unit worker = SelectUnits.ourWorkersThatGather().inRadius(10, baseWithMostWorkers).first();
            if (worker != null) {
                worker.move(baseWithFewestWorkers);
            }
        }
    }

}
