package be.lefief.util;

import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ClientSubjectHandler {

    private final String subject;

    private final Map<String, CommandTopicHandler> topicHandlers;
    public ClientSubjectHandler(String subject){
        this(subject, new ArrayList<>());
    }

    public ClientSubjectHandler(String subject, Collection<CommandTopicHandler> topicHandlerCollection){
        this.subject = subject;
        topicHandlers = new HashMap<>();
        topicHandlerCollection.forEach(topicHandler -> topicHandlers.put(topicHandler.getTopic().toUpperCase(), topicHandler));
    }
    public void handle(ClientToServerCommand clientToServerCommand, SocketHandler socketHandler){
        topicHandlers.get(clientToServerCommand.getTopic().toUpperCase()).handle(clientToServerCommand, socketHandler);
    }
}
