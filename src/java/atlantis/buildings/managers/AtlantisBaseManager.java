package atlantis.buildings.managers;

import atlantis.AtlantisConfig;
import atlantis.AtlantisGame;
import atlantis.enemy.AtlantisEnemyUnits;
import atlantis.workers.AtlantisWorkerCommander;
import bwapi.Unit;

public class AtlantisBaseManager {

    public static void update(Unit base) {

        // Train new workers if allowed
        if (AtlantisWorkerCommander.shouldTrainWorkers(true)) {
            if (hasSlotToProduceUnit(base)) {
                base.train(AtlantisConfig.WORKER);
            }
        }
    }

    private static boolean hasSlotToProduceUnit(Unit base) {
        if (AtlantisGame.playsAsZerg()) {
            return !base.getLarva().isEmpty();
        }
        else {
            return !base.getTrainingQueue().isEmpty();
        }
    }

}
