package be.lefief.sockets.handlers;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.commands.client.emission.CreateLobbyCommand;
import be.lefief.util.CommandTopicHandler;

import java.util.Optional;

public abstract class CommandHandler<T extends ClientToServerCommand> {

    private final CommandTopicHandler<T> topicHandler;

    protected CommandHandler(CommandTopicHandler<T> topicHandler){
        this.topicHandler = topicHandler;
    }

    public void openChannel(){
        topicHandler.registerListener(this);
    }

    public void closeChannel(){
        topicHandler.removeListener(this);
    }
    public abstract void accept(SecuredClientToServerCommand<T> command, ClientSession clientSession);

    void accept(T command, ClientSession clientSession){
        accept(secure(clientSession, command), clientSession);
    }

    SecuredClientToServerCommand<T> secure(ClientSession clientSession, T command){
        return new SecuredClientToServerCommand<T>(
                command,
                clientSession.getClientID(),
                Optional.ofNullable(clientSession.getClientName()).orElse("<unauthenticated>")
        );
    }

}
