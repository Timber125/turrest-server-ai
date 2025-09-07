package be.lefief.game.map;

import lombok.Getter;

public enum TerrainType {
        GRASS(1),
        DIRT(2),
        FOREST(3),
        WATER_SHALLOW(4),
        WATER_DEEP(5),
        ROCKY(6),
        CASTLE(7),
        SPAWNER(8);

        @Getter
        int terrainTypeID;
        TerrainType(int terrainTypeID){
            this.terrainTypeID = terrainTypeID;
        }
}
