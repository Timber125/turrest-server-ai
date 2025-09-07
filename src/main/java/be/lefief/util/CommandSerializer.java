package be.lefief.util;

import be.lefief.sockets.SocketCommand;
import be.lefief.sockets.commands.ClientToServerCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(CommandSerializer.class);
    private static final ObjectMapper OM = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    public static String serialize(SocketCommand socketCommand){
        if(socketCommand == null) return null;
        else {
            try {
                return OM.writeValueAsString(socketCommand);
            } catch (JsonProcessingException e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }
    }

    public static SocketCommand deserialize(String line){
        if(Strings.isBlank(line)) return null;
        try {
            return OM.readValue(line, SocketCommand.class);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public static ClientToServerCommand deserializeClientToServerCommand(String line){
        if(Strings.isBlank(line)) return null;
        try {
            return OM.readValue(line, ClientToServerCommand.class);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }
}
