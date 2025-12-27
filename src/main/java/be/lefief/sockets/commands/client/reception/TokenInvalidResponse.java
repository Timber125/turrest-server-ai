package be.lefief.sockets.commands.client.reception;

import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.ClientSocketSubject;

import java.util.HashMap;
import java.util.Map;

public class TokenInvalidResponse extends ServerToClientCommand {

    public TokenInvalidResponse() {
        super(ClientSocketSubject.CORE, "TOKEN_INVALID", new HashMap<>());
    }

    @Override
    public Map<String, Object> getData() {
        return new HashMap<>();
    }
}
