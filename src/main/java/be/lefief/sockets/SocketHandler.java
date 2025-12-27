package be.lefief.sockets;

import be.lefief.repository.UserData;
import be.lefief.service.userprofile.UserProfileService;
import be.lefief.sockets.commands.ServerToClientCommand;
import be.lefief.sockets.commands.client.reception.DisplayChatCommand;
import be.lefief.util.CommandSerializer;
import be.lefief.util.ServerClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityNotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SocketHandler implements ClientSession {
    private static final Logger LOG = LoggerFactory.getLogger(SocketHandler.class);
    private final Socket clientSocket;
    private final PrintWriter out;
    private final BufferedReader in;
    private Runnable onClose;
    private Consumer<String> onMessage;
    private final ConcurrentLinkedQueue<ServerToClientCommand> outgoingMessageQueue;
    private final ExecutorService threadPoolExecutor;
    private Supplier<UserData> userDataSupplier;

    public SocketHandler(Socket socket) {
        this.clientSocket = socket;
        this.outgoingMessageQueue = new ConcurrentLinkedQueue<>();
        this.threadPoolExecutor = Executors.newFixedThreadPool(1);
        ServerClock.TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                if(userDataSupplier == null) {
                    sendMessage(CommandSerializer.serialize(new DisplayChatCommand("did not receive login information in time, please retry")));
                }
            }
        }, 3000L);
        try {
            // Create input and output streams for communication
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Could not accept connection");
        }
    }

    public void run() {
        try {
            String inputLine;
            // Continue receiving messages from the client
            while ((inputLine = in.readLine()) != null) {
                onMessage.accept(inputLine);
            }
            close();
        } catch (IOException e) {
            LOG.error("Client {} disconnected", getClientID());
            close(); //?
        }
    }

    @Override
    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void sendCommand(ServerToClientCommand serverToClientCommand){
        final String cmd = CommandSerializer.serialize(serverToClientCommand);
        threadPoolExecutor.submit(() -> sendMessage(cmd));
    }
    @Override
    public void close(){
        try {
            in.close();
        } catch (IOException e) {
            LOG.error("Could not close inputstream: " + e.getMessage());
        }

        out.close();

        try {
            clientSocket.close();
        } catch (IOException e) {
            LOG.error("Could not close outputstream: " + e.getMessage());
        }

        if(onClose != null)
            onClose.run();
    }


    @Override
    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public PrintWriter getOut() {
        return out;
    }

    public BufferedReader getIn() {
        return in;
    }

    @Override
    public UUID getClientID() {
        return (userDataSupplier == null) ? null : userDataSupplier.get().getId();
    }

    public Runnable getOnClose() {
        return onClose;
    }

    @Override
    public void setOnMessage(Consumer<String> stringConsumer) {
        this.onMessage = stringConsumer;
    }

    public Consumer<String> getOnMessage(){
        return onMessage;
    }

    @Override
    public String getClientName() {
        return getUserData().getName();
    }

    @Override
    public String getUserIdentifiedClientName(){
        return String.format("%s(%s)", getClientName(), "#" + getClientID().toString().substring(0, 4));
    }

    @Override
    public void authenticate(UserProfileService userProfileService, UUID clientId) {
        userDataSupplier = () -> userProfileService.findByID(clientId).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public UserData getUserData(){
        return (userDataSupplier == null) ? null : userDataSupplier.get();
    }

    @Override
    public String getRemoteAddress() {
        return clientSocket.getInetAddress().toString();
    }
}
