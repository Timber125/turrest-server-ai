package be.lefief.util;

import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.handlers.routing.CommandRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class ClientCommandConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCommandConsumer.class);

    public static Consumer<String> createCommandConsumer(SocketHandler socketHandler, CommandRouter commandRouter){
        return (message) -> {
            LOG.info("Client {} @ {} sent: {}", Optional.ofNullable(socketHandler.getClientID()).map(UUID::toString).orElse("<unauthenticated>"), socketHandler.getClientSocket().getInetAddress(), message);
            Optional.ofNullable(CommandSerializer.deserializeClientToServerCommand(message))
                    .ifPresent(deserializedCommand -> commandRouter.handle(deserializedCommand, socketHandler));
        };
    }

}
