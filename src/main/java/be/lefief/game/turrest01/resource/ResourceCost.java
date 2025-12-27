package be.lefief.game.turrest01.resource;

import lombok.Getter;

@Getter
public class ResourceCost {
    private final int wood;
    private final int stone;
    private final int gold;

    public ResourceCost(int wood, int stone, int gold) {
        this.wood = wood;
        this.stone = stone;
        this.gold = gold;
    }

    public boolean canAfford(PlayerResources resources) {
        return resources.getAmount(ResourceType.WOOD) >= wood
                && resources.getAmount(ResourceType.STONE) >= stone
                && resources.getAmount(ResourceType.GOLD) >= gold;
    }
}
