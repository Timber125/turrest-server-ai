package be.lefief.sockets.handlers;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
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
    public abstract void accept(SecuredClientToServerCommand<T> command, SocketHandler socketHandler);

    void accept(T command, SocketHandler socketHandler){
        accept(secure(socketHandler, command), socketHandler);
    }

    SecuredClientToServerCommand<T> secure(SocketHandler socketHandler, T command){
        return new SecuredClientToServerCommand<T>(
                command,
                socketHandler.getClientID(),
                Optional.ofNullable(socketHandler.getClientName()).orElse("<unauthenticated>")
        );
    }

}
