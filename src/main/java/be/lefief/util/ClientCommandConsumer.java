package be.lefief.util;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.handlers.routing.CommandRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class ClientCommandConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ClientCommandConsumer.class);

    public static Consumer<String> createCommandConsumer(ClientSession clientSession, CommandRouter commandRouter){
        return (message) -> {
            LOG.info("Client {} @ {} sent: {}", Optional.ofNullable(clientSession.getUserId()).map(UUID::toString).orElse("<unauthenticated>"), clientSession.getRemoteAddress(), message);
            Optional.ofNullable(CommandSerializer.deserializeClientToServerCommand(message))
                    .ifPresent(deserializedCommand -> commandRouter.handle(deserializedCommand, clientSession));
        };
    }

}
