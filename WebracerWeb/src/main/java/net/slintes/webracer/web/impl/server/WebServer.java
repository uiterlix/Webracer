package net.slintes.webracer.web.impl.server;

import net.slintes.webracer.race.Car;
import net.slintes.webracer.race.Race;
import net.slintes.webracer.race.UICallback;
import net.slintes.webracer.web.impl.server.commands.client.ClientCommand;
import net.slintes.webracer.web.impl.server.commands.client.ClientCommandFactory;
import net.slintes.webracer.web.impl.server.commands.client.ClientCommandType;
import net.slintes.webracer.web.impl.server.commands.client.impl.ClientRegisterCarCommand;
import net.slintes.webracer.web.impl.server.commands.client.impl.ClientRegisterClientCommand;
import net.slintes.webracer.web.impl.server.commands.client.impl.ClientUpdatePositionCommand;
import net.slintes.webracer.web.impl.server.commands.server.ServerCommand;
import net.slintes.webracer.web.impl.server.commands.server.impl.*;
import net.slintes.webracer.web.impl.server.netty.WebServerStarter;
import net.slintes.webracer.web.impl.server.netty.WebSocketAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * this class manages the communication between the race component and the browser clients
 */
public class WebServer implements UICallback {

    private final Race race;
    private final ClientCommandFactory clientCommandFactory;

    // TODO is this synchronizing enough?
    private Map<String, WebSocketAdapter> clientSessions = Collections.synchronizedMap(new HashMap<>());

    public WebServer(Race race) {
        this.race = race;
        this.clientCommandFactory = new ClientCommandFactory();
    }

    public void startServer() {
        String track = new TrackConverter().convertToJson(race.getTrack());
        WebServerStarter webServerStarter = new WebServerStarter(this, track);
        webServerStarter.start();
    }

    /* methods called from WebsocketAdapter */

    public void onMessage(WebSocketAdapter wsa, String message) {

        // convert the websocket message to a client command
        ClientCommand clientCommand = clientCommandFactory.getClientCommand(message);
        if(clientCommand == null){
            return;
        }

        String clientId = getClientId(wsa);
        if (clientId == null) {
            // may only be null if command is registerClient
            if(!clientCommand.getType().equals(ClientCommandType.RegisterClient)){
                return;
            }
        }

        // handle client command
        switch (clientCommand.getType()){
            case RegisterClient:
                ClientRegisterClientCommand registerClientCommand = (ClientRegisterClientCommand) clientCommand;
                String newClientId = registerClientCommand.getClientId();
                clientSessions.put(newClientId, wsa);
                race.registerClient(newClientId);
                break;
            case RegisterCar:
                ClientRegisterCarCommand registerCarCommand = (ClientRegisterCarCommand) clientCommand;
                String name = registerCarCommand.getName();
                int startPos = race.registerCar(clientId, name);
                if(startPos == 0){
                    sendCommand(clientId, new ServerMessageCommand("Sorry " + name + ", joining not possible at the moment"));
                }
                break;
            case UpdatePosition:
                ClientUpdatePositionCommand updatePositionCommand = (ClientUpdatePositionCommand) clientCommand;
                if(updatePositionCommand.isCrashed()){
                    race.crash(clientId, updatePositionCommand.getXPos(), updatePositionCommand.getYPos(),
                            updatePositionCommand.getAngle());
                }
                else if (updatePositionCommand.isFinished()) {
                    race.finish(clientId, updatePositionCommand.getXPos(), updatePositionCommand.getYPos(),
                            updatePositionCommand.getAngle());
                }
                else {
                    race.nextPosition(clientId, updatePositionCommand.getXPos(), updatePositionCommand.getYPos(),
                            updatePositionCommand.getSpeed(), updatePositionCommand.getAngle());
                }
                break;
        }

    }

    public void unRegisterClient(WebSocketAdapter wsa) {
        String clientId = getClientId(wsa);
        if(clientId != null){
            clientSessions.remove(clientId);
            race.unRegisterClient(clientId);
        }
    }


    /* UICallback Methods (= commands from race) */

    @Override
    public void start() {
        sendCommand(new ServerStartCommand());
    }

    @Override
    public void addCar(Car car) {
        sendCommand(new ServerAddCarCommand(race.getTrack(), car));
    }

    @Override
    public void addCar(String clientId, Car car) {
        sendCommand(clientId, new ServerAddCarCommand(race.getTrack(), car));
    }

    @Override
    public void removeCar(String clientId) {
        sendCommand(new ServerRemoveCarCommand(clientId));
    }

    @Override
    public void updateCar(Car car) {
        sendCommand(new ServerUpdateCarCommand(car));
    }

    @Override
    public void showMessage(String message) {
        sendCommand(new ServerMessageCommand(message));
    }

    @Override
    public void reset() {
        sendCommand(new ServerResetCommand());
    }


    /* private methods */

    private void sendCommand(ServerCommand command) {
        // send command to all clients
        clientSessions.forEach((clientId, wsa) -> sendCommand(clientId, wsa, command));
    }

    private void sendCommand(String clientId, ServerCommand command) {
        // send command to given client
        WebSocketAdapter wsa = clientSessions.get(clientId);
        if(wsa != null){
            sendCommand(clientId, wsa, command);
        } else {
            System.out.println("client not found: " + clientId);
            race.unRegisterClient(clientId);
        }

    }

    private void sendCommand(String clientId, WebSocketAdapter wsa, ServerCommand command) {
        // send command to given websocketadapter
        String json = command.getJson();
        System.out.println("message to client " + clientId + ": " + json);
        try {
            // send message
            wsa.getRemote().sendString(json);
        } catch (IOException e) {
            System.out.println("error sending message" + e.getMessage());
        }
    }

    private String getClientId(WebSocketAdapter wsa){
        // get client id for this websocket adapter
        Optional<Map.Entry<String, WebSocketAdapter>> entryOptional = clientSessions.entrySet().stream().filter(e -> e.getValue().equals(wsa)).findFirst();
        if(entryOptional.isPresent()){
            return entryOptional.get().getKey();
        }
        return null;
    }
}
