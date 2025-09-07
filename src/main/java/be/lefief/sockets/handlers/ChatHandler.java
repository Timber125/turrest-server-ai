package be.lefief.sockets.handlers;

import be.lefief.service.lobby.LobbyService;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.client.emission.GlobalChatCommand;
import be.lefief.sockets.commands.factories.CommandFactory;
import be.lefief.sockets.handlers.routing.GlobalChatHandler;
import org.springframework.stereotype.Service;

@Service
public class ChatHandler extends CommandHandler<GlobalChatCommand> {
    private final LobbyService lobbyService;
    public ChatHandler(LobbyService lobbyService, GlobalChatHandler globalChatHandler){
        super(globalChatHandler);
        this.lobbyService = lobbyService;
        openChannel();
    }
    private String getMessageData(SecuredClientToServerCommand<GlobalChatCommand> securedClientToServerCommand){
        return securedClientToServerCommand.getCommand().getData().get(GlobalChatCommand.MESSAGE).toString();
    }

    @Override
    public void accept(SecuredClientToServerCommand<GlobalChatCommand> socketCommand, SocketHandler socketHandler) {
        lobbyService.emitGlobalMessage(CommandFactory.USER_MESSAGE(socketCommand.getServerReceivedTime(), socketCommand.getClientName(), socketCommand.getClientId(), getMessageData(socketCommand)));
    }

}
