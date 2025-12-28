package be.lefief.sockets;

import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.commands.ClientToServerCommand;

import java.time.LocalDateTime;
import java.util.UUID;

public class SecuredClientToServerCommand<T extends ClientToServerCommand> {
    private final UUID userId;
    private final String userName;
    private final LocalDateTime serverReceivedTime;
    private final T command;
    public SecuredClientToServerCommand(
            T command,
            UUID userId,
            String userName
    ) {
        this.command = command;
        this.userId = userId;
        this.userName = userName;
        this.serverReceivedTime = LocalDateTime.now();
    }

    public T getCommand(){
        return command;
    }

    public static <T extends ClientToServerCommand> SecuredClientToServerCommand<T> from(ClientSession clientSession, T clientToServerCommand, UserProfileService userProfileService) {
        if (clientSession.getUserId() == null) {
            return new SecuredClientToServerCommand(clientToServerCommand,
                    null,
                    null
            );
        }
        UserData userData = userProfileService.findByID(clientSession.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Illegal user ID")); // unauthenticated
        return new SecuredClientToServerCommand(
                clientToServerCommand,
                clientSession.getUserId(),
                userData.getName()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDateTime getServerReceivedTime() {
        return serverReceivedTime;
    }


}
