package be.lefief.game.turrest02.wave;

import be.lefief.game.turrest02.creep.CreepType;
import lombok.Getter;

import java.util.List;

@Getter
public class Wave {
    private final int tick;
    private final List<CreepType> creeps;

    public Wave(int tick, List<CreepType> creeps) {
        this.tick = tick;
        this.creeps = creeps;
    }
}
