package be.lefief.game.map;

import java.util.HashMap;
import java.util.Map;

public final class BuildingType {
    private static final Map<String, Integer> BUILDING_BY_ID = new HashMap<>(){{
        put("CASTLE", 0);
        put("SPAWNER", 1);
    }};

}
