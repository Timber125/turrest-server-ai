package be.lefief.sockets.handlers.routing;

import be.lefief.sockets.SocketHandler;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.util.ClientSubjectHandler;
import be.lefief.util.CommandTopicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommandRouter {
    private static final Logger LOG = LoggerFactory.getLogger(CommandRouter.class);
    private final Map<String, ClientSubjectHandler> subjectHandlerMap;

    public CommandRouter(

    ) {
        subjectHandlerMap = new HashMap<>();
    }

    public void register(CommandTopicHandler<?> commandTopicHandler) {
        if (!subjectHandlerMap.containsKey(commandTopicHandler.getSubject().toUpperCase())) {
            LOG.warn("Did not find registered subject {} for topic {} - creating default", commandTopicHandler.getSubject().toUpperCase(), commandTopicHandler.getTopic().toUpperCase());
            subjectHandlerMap.put(commandTopicHandler.getSubject().toUpperCase(), new ClientSubjectHandler(commandTopicHandler.getSubject().toUpperCase()));
        }
        LOG.info("Registering handler for Subject {} - Topic {}", commandTopicHandler.getSubject().toUpperCase(), commandTopicHandler.getTopic().toUpperCase());
        subjectHandlerMap.get(commandTopicHandler.getSubject().toUpperCase()).getTopicHandlers().put(commandTopicHandler.getTopic().toUpperCase(), commandTopicHandler);
    }

    public void handle(ClientToServerCommand command, SocketHandler socketHandler) {
        subjectHandlerMap.get(command.getSubject().toUpperCase()).handle(command, socketHandler);
    }


}
