package be.lefief.game.map;

import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class Tile {

    private TerrainType terrainType;
    private boolean pathed;
    private Structure structure;
    private Set<Integer> owners = new HashSet<>();  // Players who can build here

    public Tile(TerrainType terrainType) {
        this.terrainType = terrainType;
        this.pathed = false;
        this.structure = null;
    }

    /**
     * Check if this tile has any structure on it.
     */
    public boolean hasStructure() {
        return structure != null;
    }

    /**
     * Creates a copy of this tile (for map cloning).
     */
    public Tile copy() {
        Tile copy = new Tile(this.terrainType);
        copy.pathed = this.pathed;
        // Note: structure is shared reference for roads (they're the same for all players)
        copy.structure = this.structure;
        copy.owners = new HashSet<>(this.owners);
        return copy;
    }

    // Ownership methods
    public Set<Integer> getOwners() {
        return Collections.unmodifiableSet(owners);
    }

    public void addOwner(int playerNumber) {
        owners.add(playerNumber);
    }

    public void removeOwner(int playerNumber) {
        owners.remove(playerNumber);
    }

    public void setOwners(Set<Integer> newOwners) {
        owners.clear();
        owners.addAll(newOwners);
    }

    public boolean canPlayerBuild(int playerNumber) {
        return owners.contains(playerNumber);
    }

    public boolean hasOwners() {
        return !owners.isEmpty();
    }
}
