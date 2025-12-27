package be.lefief.game.map;

import lombok.Data;

@Data
public class Tile {

    private TerrainType terrainType;
    private boolean pathed;
    private Building building;

    public Tile(TerrainType terrainType) {
        this.terrainType = terrainType;
        this.pathed = false;
        this.building = null;
    }
}
