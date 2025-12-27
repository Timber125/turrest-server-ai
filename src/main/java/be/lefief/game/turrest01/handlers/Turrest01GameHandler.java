package be.lefief.game.turrest01.handlers;

import be.lefief.game.Game;
import be.lefief.game.GameService;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest01.Turrest01Player;
import be.lefief.game.turrest01.TurrestGameMode01;
import be.lefief.game.turrest01.building.BuildingDefinition;
import be.lefief.game.turrest01.commands.BuildingChangedResponse;
import be.lefief.game.turrest01.commands.PlaceBuildingCommand;
import be.lefief.game.turrest01.commands.ResourceUpdateResponse;
import be.lefief.game.turrest01.resource.PlayerResources;
import be.lefief.game.turrest01.structure.TurrestBuilding;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.client.reception.ErrorMessageResponse;
import org.slf4j.Logger;
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

        LOG.info("Player {} attempting to place building type {} at ({}, {})",
                command.getClientName(), buildingTypeId, x, y);

        // Get the game for this player
        String sessionKey = clientSession.getClientID() + ":" + clientSession.getTabId();
        Game<?> game = gameService.getGameBySessionKey(sessionKey);

        if (game == null) {
            LOG.warn("No game found for session {}", sessionKey);
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
            LOG.warn("Could not find player for session");
            clientSession.sendCommand(new ErrorMessageResponse("Player not found"));
            return;
        }

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

        LOG.info("Player {} built {} at ({}, {})",
                player.getPlayerNumber(), buildingDef.getName(), x, y);

        // Send resource update to the building player
        clientSession.sendCommand(new ResourceUpdateResponse(
                resources.getWood(),
                resources.getStone(),
                resources.getGold()
        ));

        // Broadcast building change to all players
        turrestGame.broadcastToAllPlayers(new BuildingChangedResponse(
                x, y, buildingDef.getId(), player.getPlayerNumber()
        ));
    }

    private Turrest01Player findPlayerBySession(TurrestGameMode01 game, ClientSession session) {
        // Match by clientID only - tabId can differ if user switches tabs
        for (Turrest01Player player : game.getPlayerByNumber().values()) {
            ClientSession playerSession = player.getClientSession();
            if (playerSession != null &&
                session.getClientID().equals(playerSession.getClientID())) {
                return player;
            }
        }
        LOG.warn("No player found for session clientID={}", session.getClientID());
        return null;
    }
}
