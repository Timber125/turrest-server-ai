package be.lefief.game.turrest02.structure;

public enum TurrestStructureType {
    ROAD(0),
    BUILDING(1);

    private final int id;

    TurrestStructureType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static TurrestStructureType fromId(int id) {
        for (TurrestStructureType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TurrestStructureType id: " + id);
    }
}
