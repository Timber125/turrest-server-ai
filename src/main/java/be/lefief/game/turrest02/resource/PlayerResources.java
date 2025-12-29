package be.lefief.game.turrest02.resource;

import java.util.EnumMap;
import java.util.Map;

public class PlayerResources {
    private static final int STARTING_AMOUNT = 100;
    private static final int BASE_PRODUCTION = 1;

    private final Map<ResourceType, Integer> resources;
    private final Map<ResourceType, Integer> productionRates;

    public PlayerResources() {
        resources = new EnumMap<>(ResourceType.class);
        productionRates = new EnumMap<>(ResourceType.class);

        // Initialize with starting amounts and base production
        for (ResourceType type : ResourceType.values()) {
            resources.put(type, STARTING_AMOUNT);
            productionRates.put(type, BASE_PRODUCTION);
        }
    }

    public int getAmount(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }

    public int getWood() {
        return getAmount(ResourceType.WOOD);
    }

    public int getStone() {
        return getAmount(ResourceType.STONE);
    }

    public int getGold() {
        return getAmount(ResourceType.GOLD);
    }

    public void addGold(int amount) {
        resources.put(ResourceType.GOLD, resources.get(ResourceType.GOLD) + amount);
    }

    public int getProductionRate(ResourceType type) {
        return productionRates.getOrDefault(type, 0);
    }

    public void addProduction() {
        for (ResourceType type : ResourceType.values()) {
            int current = resources.get(type);
            int production = productionRates.get(type);
            resources.put(type, current + production);
        }
    }

    public boolean canAfford(ResourceCost cost) {
        return cost.canAfford(this);
    }

    public void subtract(ResourceCost cost) {
        resources.put(ResourceType.WOOD, resources.get(ResourceType.WOOD) - cost.getWood());
        resources.put(ResourceType.STONE, resources.get(ResourceType.STONE) - cost.getStone());
        resources.put(ResourceType.GOLD, resources.get(ResourceType.GOLD) - cost.getGold());
    }

    public void subtract(TurrestCost cost) {
        resources.put(ResourceType.WOOD, resources.get(ResourceType.WOOD) - cost.getWood());
        resources.put(ResourceType.STONE, resources.get(ResourceType.STONE) - cost.getStone());
        resources.put(ResourceType.GOLD, resources.get(ResourceType.GOLD) - cost.getGold());
        // Note: hitpoints are handled separately by TurrestCost.apply()
    }

    public void add(TurrestReward reward) {
        resources.put(ResourceType.WOOD, resources.get(ResourceType.WOOD) + reward.getWood());
        resources.put(ResourceType.STONE, resources.get(ResourceType.STONE) + reward.getStone());
        resources.put(ResourceType.GOLD, resources.get(ResourceType.GOLD) + reward.getGold());
        // Note: hitpoints are handled separately by TurrestReward.apply()
    }

    public void addProductionBonus(ResourceType type, int bonus) {
        int current = productionRates.getOrDefault(type, 0);
        productionRates.put(type, current + bonus);
    }

    public void addProductionBonuses(Map<ResourceType, Integer> bonuses) {
        for (Map.Entry<ResourceType, Integer> entry : bonuses.entrySet()) {
            addProductionBonus(entry.getKey(), entry.getValue());
        }
    }
}
