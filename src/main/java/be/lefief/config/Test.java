package be.lefief.config;

import be.lefief.controller.SocketController;
import be.lefief.lobby.LobbyManager;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.handlers.routing.CommandRouter;
import be.lefief.util.ClientListener;
import be.lefief.util.SocketManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
public class Test {

    @Bean
    public SocketController socketController() {
        return new SocketController(1234);
    }

    @Bean
    Object socketManager(
            SocketController socketController,
            LobbyManager lobbyManager,
            CommandRouter commandRouter
    ) {
        socketController.startAcceptConnections(lobbyManager, commandRouter);
        return new Object();
    }
}
