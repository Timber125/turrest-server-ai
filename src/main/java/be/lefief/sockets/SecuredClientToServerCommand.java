package be.lefief.sockets;

import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.commands.ClientToServerCommand;

import java.time.LocalDateTime;
import java.util.UUID;

public class SecuredClientToServerCommand<T extends ClientToServerCommand> {
    private final UUID clientId;
    private final String clientName;
    private final LocalDateTime serverReceivedTime;
    private final T command;
    public SecuredClientToServerCommand(
            T command,
            UUID clientId,
            String clientName
    ) {
        this.command = command;
        this.clientId = clientId;
        this.clientName = clientName;
        this.serverReceivedTime = LocalDateTime.now();
    }

    public T getCommand(){
        return command;
    }

    public static <T extends ClientToServerCommand> SecuredClientToServerCommand<T> from(ClientSession clientSession, T clientToServerCommand, UserProfileService userProfileService) {
        if (clientSession.getClientID() == null) {
            return new SecuredClientToServerCommand(clientToServerCommand,
                    null,
                    null
            );
        }
        UserData userData = userProfileService.findByID(clientSession.getClientID())
                .orElseThrow(() -> new IllegalArgumentException("Illegal client ID")); // unauthenticated
        return new SecuredClientToServerCommand(
                clientToServerCommand,
                clientSession.getClientID(),
                userData.getName()
        );
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public LocalDateTime getServerReceivedTime() {
        return serverReceivedTime;
    }


}
