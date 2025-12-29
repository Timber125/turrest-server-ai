package be.lefief.game.turrest01.handlers;

import be.lefief.game.turrest01.commands.SendCreepCommand;
import be.lefief.sockets.ClientSession;
import be.lefief.sockets.SecuredClientToServerCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import be.lefief.util.CommandTopicHandler;
import org.springframework.stereotype.Component;

@Component
public class SendCreepHandler extends CommandTopicHandler<SendCreepCommand> {

    public SendCreepHandler() {
        super(SendCreepCommand.SUBJECT.name(), SendCreepCommand.TOPIC);
    }

    @Override
    public SecuredClientToServerCommand<SendCreepCommand> identify(ClientToServerCommand command, ClientSession clientSession) {
        return new SecuredClientToServerCommand<>(
                new SendCreepCommand(command.getData()),
                clientSession.getUserId(),
                clientSession.getUserName()
        );
    }
}
