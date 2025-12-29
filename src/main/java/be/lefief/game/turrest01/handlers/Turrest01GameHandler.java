package be.lefief.game.turrest01.handlers;

import be.lefief.game.Game;
import be.lefief.game.GameService;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.Turrest01Player;
import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.game.turrest01.building.BuildingDefinition;
import be.lefief.game.turrest01.commands.BuildingChangedResponse;
import be.lefief.game.turrest01.commands.PlaceBuildingCommand;
import be.lefief.game.turrest01.commands.PlaceTowerCommand;
import be.lefief.game.turrest01.commands.GetStatsCommand;
import be.lefief.game.turrest01.commands.ResourceEventCommand;
import be.lefief.game.turrest01.commands.ResourceUpdateResponse;
import be.lefief.game.turrest01.commands.SendCreepCommand;
import be.lefief.game.turrest01.commands.StatsResponseCommand;
import be.lefief.game.turrest01.commands.TowerPlacedCommand;
import be.lefief.game.turrest01.event.BuildingBuiltEvent;
import be.lefief.game.turrest01.event.CreepSentEvent;
import be.lefief.game.turrest01.event.TowerBuiltEvent;
import be.lefief.game.turrest01.resource.ResourceEventType;
import be.lefief.game.turrest01.creep.CreepType;
import be.lefief.game.turrest01.resource.PlayerResources;
import be.lefief.game.turrest01.resource.TurrestCost;
import be.lefief.game.turrest01.structure.TurrestBuilding;
import be.lefief.game.turrest01.tower.BasicTower;
import be.lefief.game.turrest01.tower.Tower;
import be.lefief.game.turrest01.tower.TowerDefinition;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.reception.ErrorMessageResponse;
import org.slf4j.Logger;

import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Turrest01GameHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Turrest01GameHandler.class);
    private final GameService gameService;

    public Turrest01GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void handlePlaceBuilding(SecuredClientToServerCommand<PlaceBuildingCommand> command, ClientSession clientSession) {
        PlaceBuildingCommand placeCommand = command.getCommand();
        int x = placeCommand.getX();
        int y = placeCommand.getY();
        int buildingTypeId = placeCommand.getBuildingType();

        UUID userId = clientSession.getUserId();
        LOG.info("[BUILD DEBUG] Player '{}' (userId={}) attempting to place building type {} at ({}, {})",
                command.getUserName(), userId, buildingTypeId, x, y);

        // Get the game for this player
        Game<?> game = gameService.getGameByUserId(userId);

        if (game == null) {
            LOG.warn("No game found for user {}", userId);
            clientSession.sendCommand(new ErrorMessageResponse("Not in a game"));
            return;
        }

        if (!(game instanceof TurrestGameMode01 turrestGame)) {
            LOG.warn("Game is not TurrestGameMode01");
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Find the player
        LOG.debug("[BUILD DEBUG] Game has {} players: {}",
                turrestGame.getPlayerByNumber().size(),
                turrestGame.getPlayerByNumber().keySet());
        Turrest01Player player = findPlayerBySession(turrestGame, clientSession);
        if (player == null) {
            LOG.warn("[BUILD DEBUG] Could not find player for session userId={}, game players: {}",
                    userId, turrestGame.getPlayerByNumber().values().stream()
                            .map(p -> "Player" + p.getPlayerNumber() + "(clientID=" +
                                    (p.getClientSession() != null ? p.getClientSession().getUserId() : "null") + ")")
                            .toList());
            clientSession.sendCommand(new ErrorMessageResponse("Player not found"));
            return;
        }
        LOG.info("[BUILD DEBUG] Found player {} for userId={}", player.getPlayerNumber(), userId);

        // Get building definition
        BuildingDefinition buildingDef = BuildingDefinition.fromId(buildingTypeId);
        if (buildingDef == null) {
            LOG.warn("Unknown building type: {}", buildingTypeId);
            clientSession.sendCommand(new ErrorMessageResponse("Unknown building type"));
            return;
        }

        // Get tile
        Tile tile = turrestGame.getGameMap().getTile(x, y);
        if (tile == null) {
            LOG.warn("Invalid tile position: ({}, {})", x, y);
            clientSession.sendCommand(new ErrorMessageResponse("Invalid position"));
            return;
        }

        // Check ownership - player can only build on their own territory
        if (!tile.canPlayerBuild(player.getPlayerNumber())) {
            LOG.info("Player {} cannot build on tile ({}, {}) - owners: {}",
                    player.getPlayerNumber(), x, y, tile.getOwners());
            clientSession.sendCommand(new ErrorMessageResponse("Cannot build on another player's territory"));
            return;
        }

        // Validate terrain
        if (!buildingDef.canBuildOn(tile.getTerrainType())) {
            LOG.info("Cannot build {} on terrain {}", buildingDef.getName(), tile.getTerrainType());
            clientSession.sendCommand(new ErrorMessageResponse(
                    "Cannot build " + buildingDef.getName() + " on " + tile.getTerrainType().name().toLowerCase()));
            return;
        }

        // Check for existing structure
        if (tile.hasStructure()) {
            LOG.info("Tile already has a structure");
            clientSession.sendCommand(new ErrorMessageResponse("Tile already occupied"));
            return;
        }

        // Check if player can afford
        PlayerResources resources = player.getResources();
        if (!resources.canAfford(buildingDef.getCost())) {
            LOG.info("Player cannot afford {}", buildingDef.getName());
            clientSession.sendCommand(new ErrorMessageResponse("Not enough resources"));
            return;
        }

        // All checks passed - place the building
        resources.subtract(buildingDef.getCost());
        resources.addProductionBonuses(buildingDef.getProductionBonus());
        tile.setStructure(new TurrestBuilding(buildingDef, player.getPlayerNumber()));

        // Record building event for stats
        turrestGame.recordEvent(new BuildingBuiltEvent(
                player.getPlayerNumber(),
                buildingDef.getId(),
                x, y
        ));

        LOG.info("Player {} built {} at ({}, {})",
                player.getPlayerNumber(), buildingDef.getName(), x, y);

        // Send resource update to the building player
        clientSession.sendCommand(new ResourceUpdateResponse(
                resources.getWood(),
                resources.getStone(),
                resources.getGold()
        ));

        // Send resource event animation for building cost
        clientSession.sendCommand(new ResourceEventCommand(
                ResourceEventType.BUILD_BUILDING,
                buildingDef.getCost(),
                x + 0.5, y + 0.5,  // Center of tile
                player.getPlayerNumber()
        ));

        // Broadcast building change to all players
        turrestGame.broadcastToAllPlayers(new BuildingChangedResponse(
                x, y, buildingDef.getId(), player.getPlayerNumber()
        ));
    }

    public void handlePlaceTower(SecuredClientToServerCommand<PlaceTowerCommand> command, ClientSession clientSession) {
        PlaceTowerCommand placeCommand = command.getCommand();
        int x = placeCommand.getX();
        int y = placeCommand.getY();
        int towerTypeId = placeCommand.getTowerType();

        UUID userId = clientSession.getUserId();
        LOG.info("[TOWER DEBUG] Player '{}' (userId={}) attempting to place tower type {} at ({}, {})",
                command.getUserName(), userId, towerTypeId, x, y);

        // Get the game for this player
        Game<?> game = gameService.getGameByUserId(userId);

        if (game == null) {
            LOG.warn("No game found for user {}", userId);
            clientSession.sendCommand(new ErrorMessageResponse("Not in a game"));
            return;
        }

        if (!(game instanceof TurrestGameMode01 turrestGame)) {
            LOG.warn("Game is not TurrestGameMode01");
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Find the player
        Turrest01Player player = findPlayerBySession(turrestGame, clientSession);
        if (player == null) {
            LOG.warn("[TOWER DEBUG] Could not find player for session userId={}", userId);
            clientSession.sendCommand(new ErrorMessageResponse("Player not found"));
            return;
        }

        // Get tower definition
        TowerDefinition towerDef = TowerDefinition.fromId(towerTypeId);
        if (towerDef == null) {
            LOG.warn("Unknown tower type: {}", towerTypeId);
            clientSession.sendCommand(new ErrorMessageResponse("Unknown tower type"));
            return;
        }

        // Get tile
        Tile tile = turrestGame.getGameMap().getTile(x, y);
        if (tile == null) {
            LOG.warn("Invalid tile position: ({}, {})", x, y);
            clientSession.sendCommand(new ErrorMessageResponse("Invalid position"));
            return;
        }

        // Check ownership - player can only build on their own territory
        if (!tile.canPlayerBuild(player.getPlayerNumber())) {
            LOG.info("Player {} cannot build tower on tile ({}, {}) - owners: {}",
                    player.getPlayerNumber(), x, y, tile.getOwners());
            clientSession.sendCommand(new ErrorMessageResponse("Cannot build on another player's territory"));
            return;
        }

        // Validate terrain
        if (!towerDef.canBuildOn(tile.getTerrainType())) {
            LOG.info("Cannot build {} on terrain {}", towerDef.getName(), tile.getTerrainType());
            clientSession.sendCommand(new ErrorMessageResponse(
                    "Cannot build " + towerDef.getName() + " on " + tile.getTerrainType().name().toLowerCase()));
            return;
        }

        // Check for existing structure
        if (tile.hasStructure()) {
            LOG.info("Tile already has a structure");
            clientSession.sendCommand(new ErrorMessageResponse("Tile already occupied"));
            return;
        }

        // Check if player can afford
        PlayerResources resources = player.getResources();
        if (!resources.canAfford(towerDef.getCost())) {
            LOG.info("Player cannot afford {}", towerDef.getName());
            clientSession.sendCommand(new ErrorMessageResponse("Not enough resources"));
            return;
        }

        // All checks passed - place the tower
        resources.subtract(towerDef.getCost());

        // Create tower based on type
        Tower tower = createTower(towerDef, player.getPlayerNumber(), x, y);
        turrestGame.getTowerManager().addTower(tower);

        // Record tower event for stats
        turrestGame.recordEvent(new TowerBuiltEvent(
                player.getPlayerNumber(),
                towerDef.getId(),
                x, y
        ));

        LOG.info("Player {} built {} at ({}, {}) - theoretical rate: {}/s, practical rate: {}/s",
                player.getPlayerNumber(), towerDef.getName(), x, y,
                towerDef.getTheoreticalFireRate(),
                towerDef.getPracticalFireRate(turrestGame.getTickRateMs()));

        // Send resource update to the building player
        clientSession.sendCommand(new ResourceUpdateResponse(
                resources.getWood(),
                resources.getStone(),
                resources.getGold()
        ));

        // Send resource event animation for tower cost
        clientSession.sendCommand(new ResourceEventCommand(
                ResourceEventType.BUILD_TOWER,
                towerDef.getCost(),
                x + 0.5, y + 0.5,  // Center of tile
                player.getPlayerNumber()
        ));

        // Broadcast tower placement to all players
        turrestGame.broadcastToAllPlayers(new TowerPlacedCommand(tower, turrestGame.getTickRateMs()));
    }

    private Tower createTower(TowerDefinition def, int playerNumber, int x, int y) {
        return switch (def) {
            case BASIC_TOWER -> new BasicTower(playerNumber, x, y);
        };
    }

    public void handleSendCreep(SecuredClientToServerCommand<SendCreepCommand> command, ClientSession clientSession) {
        SendCreepCommand sendCommand = command.getCommand();
        String creepTypeId = sendCommand.getCreepTypeId();

        UUID userId = clientSession.getUserId();
        LOG.info("[SEND CREEP] Player '{}' (userId={}) attempting to send creep type '{}'",
                command.getUserName(), userId, creepTypeId);

        // Get the game for this player
        Game<?> game = gameService.getGameByUserId(userId);

        if (game == null) {
            LOG.warn("No game found for user {}", userId);
            clientSession.sendCommand(new ErrorMessageResponse("Not in a game"));
            return;
        }

        if (!(game instanceof TurrestGameMode01 turrestGame)) {
            LOG.warn("Game is not TurrestGameMode01");
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Find the player
        Turrest01Player player = findPlayerBySession(turrestGame, clientSession);
        if (player == null) {
            LOG.warn("[SEND CREEP] Could not find player for session userId={}", userId);
            clientSession.sendCommand(new ErrorMessageResponse("Player not found"));
            return;
        }

        // Get creep type
        CreepType creepType = CreepType.fromId(creepTypeId);
        if (creepType == null) {
            LOG.warn("Unknown creep type: {}", creepTypeId);
            clientSession.sendCommand(new ErrorMessageResponse("Unknown creep type"));
            return;
        }

        // Check if player can afford
        TurrestCost sendCost = creepType.getSendCost();
        if (!sendCost.canAfford(player)) {
            LOG.info("Player {} cannot afford to send {}", player.getPlayerNumber(), creepType.getId());
            clientSession.sendCommand(new ErrorMessageResponse("Not enough resources"));
            return;
        }

        // Subtract cost
        sendCost.apply(player);

        // Record creep sent event for stats
        turrestGame.recordEvent(new CreepSentEvent(
                player.getPlayerNumber(),
                creepType.getId(),
                sendCost.getGold()
        ));

        LOG.info("Player {} sent {} to opponents",
                player.getPlayerNumber(), creepType.getId());

        // Send resource update to the sending player
        turrestGame.sendResourceUpdateToPlayer(player.getPlayerNumber());

        // Send resource event animation for send cost
        // Position at screen center (frontend will handle positioning for SEND_CREEP events)
        clientSession.sendCommand(new ResourceEventCommand(
                ResourceEventType.SEND_CREEP,
                sendCost,
                -1, -1,  // Special coordinates: frontend will use screen center
                player.getPlayerNumber()
        ));

        // Spawn creep for all other players
        turrestGame.getCreepManager().spawnSentCreep(creepType, player.getPlayerNumber(), turrestGame);
    }

    public void handleGetStats(SecuredClientToServerCommand<GetStatsCommand> command, ClientSession clientSession) {
        UUID userId = clientSession.getUserId();
        LOG.debug("[GET STATS] Player '{}' (userId={}) requesting stats", command.getUserName(), userId);

        // Get the game for this player
        Game<?> game = gameService.getGameByUserId(userId);

        if (game == null) {
            LOG.warn("No game found for user {}", userId);
            clientSession.sendCommand(new ErrorMessageResponse("Not in a game"));
            return;
        }

        if (!(game instanceof TurrestGameMode01 turrestGame)) {
            LOG.warn("Game is not TurrestGameMode01");
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Send stats response to the requesting player
        clientSession.sendCommand(new StatsResponseCommand(turrestGame.getGameStats()));
    }

    private Turrest01Player findPlayerBySession(TurrestGameMode01 game, ClientSession session) {
        UUID lookingFor = session.getUserId();
        LOG.debug("[BUILD DEBUG] Looking for player with clientID={}", lookingFor);
        for (Turrest01Player player : game.getPlayerByNumber().values()) {
            ClientSession playerSession = player.getClientSession();
            UUID playerId = playerSession != null ? playerSession.getUserId() : null;
            LOG.debug("[BUILD DEBUG] Checking player {} - userId={}, match={}",
                    player.getPlayerNumber(), playerId,
                    playerId != null && playerId.equals(lookingFor));
            if (playerSession != null && lookingFor.equals(playerId)) {
                return player;
            }
        }
        LOG.warn("[BUILD DEBUG] No player found for session clientID={}", lookingFor);
        return null;
    }
}
