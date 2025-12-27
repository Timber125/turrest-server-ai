package be.lefief.util;

import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.sockets.handlers.CommandHandler;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class CommandTopicHandler<T extends ClientToServerCommand> {

    @Getter
    private final String topic;
    @Getter
    private final String subject;
    private final Set<CommandHandler<T>> listeners;

    public CommandTopicHandler(String subject, String topic) {
        this.subject = subject;
        this.topic = topic;
        this.listeners = new HashSet<>();
    }

    public void registerListener(CommandHandler<T> commandHandler){
        listeners.add(commandHandler);
    }

    public void removeListener(CommandHandler<T> commandHandler){
        listeners.remove(commandHandler);
    }

    public abstract SecuredClientToServerCommand<T> identify(ClientToServerCommand command, ClientSession clientSession);
    public void handle(ClientToServerCommand clientToServerCommand, ClientSession clientSession) {
        listeners.forEach(listener -> listener.accept(identify(clientToServerCommand, clientSession), clientSession));
    }
}
