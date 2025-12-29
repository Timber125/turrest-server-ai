package be.lefief.game.turrest02.resource;

public enum ResourceType {
    WOOD(1),
    STONE(2),
    GOLD(3);

    private final int id;

    ResourceType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ResourceType fromId(int id) {
        for (ResourceType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ResourceType id: " + id);
    }
}
