package be.lefief.game.validation;

import be.lefief.game.Game;
import be.lefief.game.GameService;
import be.lefief.game.map.Tile;
import be.lefief.game.turrest02.Turrest02Player;
import be.lefief.game.turrest02.TurrestGameMode02;
import be.lefief.sockets.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralized validation logic for game commands.
 * Provides common validation checks to ensure game integrity.
 */
@Component
public class CommandValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CommandValidator.class);

    private final GameService gameService;

    public CommandValidator(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Validate that a player can perform actions in a game.
     * Checks: game exists, game is running, player exists, player is alive.
     */
    public ValidationResult validatePlayerCanAct(UUID userId) {
        Game<?> game = gameService.getGameByUserId(userId);

        if (game == null) {
            return ValidationResult.failure("Not in a game");
        }

        if (!game.isGameIsRunning()) {
            return ValidationResult.failure("Game is not running");
        }

        return ValidationResult.success();
    }

    /**
     * Validate game is Turrest02 and get it.
     */
    public TurrestGameMode02 getValidTurrest02Game(UUID userId) {
        Game<?> game = gameService.getGameByUserId(userId);

        if (game == null) {
            return null;
        }

        if (!(game instanceof TurrestGameMode02)) {
            return null;
        }

        return (TurrestGameMode02) game;
    }

    /**
     * Validate that a player is alive and connected.
     */
    public ValidationResult validatePlayerIsAlive(Turrest02Player player) {
        if (player == null) {
            return ValidationResult.failure("Player not found");
        }

        if (!player.isConnected()) {
            return ValidationResult.failure("Player is disconnected");
        }

        if (!player.isAlive()) {
            return ValidationResult.failure("Player is eliminated");
        }

        return ValidationResult.success();
    }

    /**
     * Validate tile placement.
     * Checks: tile exists, player owns tile, tile not occupied.
     */
    public ValidationResult validateTilePlacement(TurrestGameMode02 game, int x, int y, int playerNumber) {
        Tile tile = game.getGameMap().getTile(x, y);

        if (tile == null) {
            return ValidationResult.failure("Invalid position");
        }

        if (!tile.canPlayerBuild(playerNumber)) {
            return ValidationResult.failure("Cannot build on another player's territory");
        }

        if (tile.hasStructure()) {
            return ValidationResult.failure("Tile already occupied");
        }

        return ValidationResult.success();
    }

    /**
     * Find player by session in a Turrest02 game.
     */
    public Turrest02Player findPlayerBySession(TurrestGameMode02 game, ClientSession session) {
        UUID lookingFor = session.getUserId();
        for (Turrest02Player player : game.getPlayerByNumber().values()) {
            ClientSession playerSession = player.getClientSession();
            if (playerSession != null && lookingFor.equals(playerSession.getUserId())) {
                return player;
            }
        }
        return null;
    }
}
