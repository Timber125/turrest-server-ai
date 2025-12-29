package be.lefief.game.turrest02.handlers;

import be.lefief.game.GameService;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest02.Turrest02Player;
import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.game.turrest02.building.BuildingDefinition;
import be.lefief.game.turrest02.commands.*;
import be.lefief.game.turrest02.creep.CreepType;
import be.lefief.game.turrest02.event.BuildingBuiltEvent;
import be.lefief.game.turrest02.event.CreepSentEvent;
import be.lefief.game.turrest02.event.TowerBuiltEvent;
import be.lefief.game.turrest02.resource.PlayerResources;
import be.lefief.game.turrest02.resource.ResourceEventType;
import be.lefief.game.turrest02.resource.TurrestCost;
import be.lefief.game.turrest02.structure.TurrestBuilding;
import be.lefief.game.turrest02.tower.GenericTower;
import be.lefief.game.turrest02.tower.Tower;
import be.lefief.game.turrest02.tower.TowerDefinition;
import be.lefief.game.validation.CommandValidator;
import be.lefief.game.validation.ValidationResult;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.reception.ErrorMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class Turrest02GameHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Turrest02GameHandler.class);
    private final GameService gameService;
    private final CommandValidator commandValidator;

    public Turrest02GameHandler(GameService gameService, CommandValidator commandValidator) {
        this.gameService = gameService;
        this.commandValidator = commandValidator;
    }

    public void handlePlaceBuilding(SecuredClientToServerCommand<PlaceBuildingCommand> command, ClientSession clientSession) {
        PlaceBuildingCommand placeCommand = command.getCommand();
        int x = placeCommand.getX();
        int y = placeCommand.getY();
        int buildingTypeId = placeCommand.getBuildingType();

        UUID userId = clientSession.getUserId();
        LOG.info("[BUILD DEBUG] Player '{}' (userId={}) attempting to place building type {} at ({}, {})",
                command.getUserName(), userId, buildingTypeId, x, y);

        // Validate player can act
        ValidationResult canAct = commandValidator.validatePlayerCanAct(userId);
        if (canAct.isInvalid()) {
            clientSession.sendCommand(new ErrorMessageResponse(canAct.getErrorMessage()));
            return;
        }

        // Get the game for this player
        TurrestGameMode02 turrestGame = commandValidator.getValidTurrest02Game(userId);
        if (turrestGame == null) {
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Find the player
        LOG.debug("[BUILD DEBUG] Game has {} players: {}",
                turrestGame.getPlayerByNumber().size(),
                turrestGame.getPlayerByNumber().keySet());
        Turrest02Player player = commandValidator.findPlayerBySession(turrestGame, clientSession);

        // Validate player is alive
        ValidationResult playerAlive = commandValidator.validatePlayerIsAlive(player);
        if (playerAlive.isInvalid()) {
            LOG.warn("[BUILD DEBUG] Player validation failed: {}", playerAlive.getErrorMessage());
            clientSession.sendCommand(new ErrorMessageResponse(playerAlive.getErrorMessage()));
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

        // Validate player can act
        ValidationResult canAct = commandValidator.validatePlayerCanAct(userId);
        if (canAct.isInvalid()) {
            clientSession.sendCommand(new ErrorMessageResponse(canAct.getErrorMessage()));
            return;
        }

        // Get the game for this player
        TurrestGameMode02 turrestGame = commandValidator.getValidTurrest02Game(userId);
        if (turrestGame == null) {
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Find the player and validate alive
        Turrest02Player player = commandValidator.findPlayerBySession(turrestGame, clientSession);
        ValidationResult playerAlive = commandValidator.validatePlayerIsAlive(player);
        if (playerAlive.isInvalid()) {
            LOG.warn("[TOWER DEBUG] Player validation failed: {}", playerAlive.getErrorMessage());
            clientSession.sendCommand(new ErrorMessageResponse(playerAlive.getErrorMessage()));
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
        return new GenericTower(def, playerNumber, x, y);
    }

    public void handleSendCreep(SecuredClientToServerCommand<SendCreepCommand> command, ClientSession clientSession) {
        SendCreepCommand sendCommand = command.getCommand();
        String creepTypeId = sendCommand.getCreepTypeId();

        UUID userId = clientSession.getUserId();
        LOG.info("[SEND CREEP] Player '{}' (userId={}) attempting to send creep type '{}'",
                command.getUserName(), userId, creepTypeId);

        // Validate player can act
        ValidationResult canAct = commandValidator.validatePlayerCanAct(userId);
        if (canAct.isInvalid()) {
            clientSession.sendCommand(new ErrorMessageResponse(canAct.getErrorMessage()));
            return;
        }

        // Get the game for this player
        TurrestGameMode02 turrestGame = commandValidator.getValidTurrest02Game(userId);
        if (turrestGame == null) {
            clientSession.sendCommand(new ErrorMessageResponse("Invalid game type"));
            return;
        }

        // Find the player and validate alive
        Turrest02Player player = commandValidator.findPlayerBySession(turrestGame, clientSession);
        ValidationResult playerAlive = commandValidator.validatePlayerIsAlive(player);
        if (playerAlive.isInvalid()) {
            LOG.warn("[SEND CREEP] Player validation failed: {}", playerAlive.getErrorMessage());
            clientSession.sendCommand(new ErrorMessageResponse(playerAlive.getErrorMessage()));
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

        // Get the game for this player (stats can be requested even if game not running)
        TurrestGameMode02 turrestGame = commandValidator.getValidTurrest02Game(userId);
        if (turrestGame == null) {
            clientSession.sendCommand(new ErrorMessageResponse("Not in a game"));
            return;
        }

        // Send stats response to the requesting player
        clientSession.sendCommand(new StatsResponseCommand(turrestGame.getGameStats()));
    }
}
