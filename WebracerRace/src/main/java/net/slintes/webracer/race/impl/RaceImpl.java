package net.slintes.webracer.race.impl;

import net.slintes.webracer.db.WebracerDB;
import net.slintes.webracer.race.Race;
import net.slintes.webracer.race.UICallback;
import net.slintes.webracer.track.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementaion of Race
 */
public class RaceImpl implements Race {

    private static final int MAX_NR_CARS = 10;

    private final Track track;
    private final WebracerDB raceDB;
    private UICallback uiCallback;
    private RaceControl raceControl;

    private final List<Client> clients = new ArrayList<>();

    public RaceImpl(Track track, WebracerDB raceDB){
        this.track = track;
        this.raceDB = raceDB;
    }

    @Override
    public void setUICallback(UICallback uiCallback) {
        this.uiCallback = uiCallback;
        raceControl = new RaceControl(uiCallback, raceDB, clients);
    }

    @Override
    public String getTrack() {
        return track.getTrack();
    }

    @Override
    synchronized public boolean registerClient(String clientId) {

        // send existing cars to new new client
        clients.stream().filter(c -> c.getStartPosition() > 0).forEach(c -> uiCallback.addCar(clientId, c));

        clients.add(new Client(clientId));
        return raceControl.getState().equals(RaceState.WAITING) && !maxNrCarsReached();
    }

    @Override
    synchronized public void unRegisterClient(String clientId) {
        Client client = getClient(clientId);
        if(client == null){
            return;
        }
        clients.remove(client);
        uiCallback.removeCar(clientId);
        if(client.getName() != null){
            raceControl.removeCar(client.getName());
        }
    }

    @Override
    synchronized public int registerCar(String clientId, String name) {

        Client client = getClient(clientId);
        if(client.getName() != null) {
            // client already registered a car...
            return client.getStartPosition();
        }

        if(!raceControl.getState().equals(RaceState.WAITING) || maxNrCarsReached()){
            return 0;
        }

        int startPosition = getNextStartPosition();
        client.setStartPosition(startPosition);
        client.setName(name);

        uiCallback.addCar(client);
        raceControl.addCar(client);

        return startPosition;
    }

    @Override
    synchronized public void nextPosition(String clientId, int xPos, int yPos, int speed, int angle) {
        Client client = updatePosition(clientId, xPos, yPos, speed, angle);
        uiCallback.updateCar(client);
    }

    @Override
    synchronized public void crash(String clientId, int xPos, int yPos, int angle) {
        Client client = updatePosition(clientId, xPos, yPos, 0, angle);
        client.setCrashed(true);
        uiCallback.updateCar(client);

        raceControl.crash(client);

        checkForDrivingCars();
    }


    @Override
    synchronized public void finish(String clientId, int xPos, int yPos, int angle) {
        Client client = updatePosition(clientId, xPos, yPos, 0, angle);
        client.setFinished(true);
        client.setResultTime(raceControl.getRaceTime());
        client.setResultPosition(getNrFinishedCars());

        uiCallback.updateCar(client);

        if(client.getResultPosition() == 1){
            raceControl.raceWon(client);
        }
        else {
            raceControl.raceFinished(client);
        }

        checkForDrivingCars();
    }

    private Client getClient(String clientId){
        return clients.stream().filter(c -> c.getClientId().equals(clientId)).findFirst().orElse(null);
    }

    private boolean maxNrCarsReached(){
        return getNrOfCars() >= MAX_NR_CARS;
    }

    private int getNrOfCars(){
        return (int)clients.stream().filter(c -> c.getStartPosition() > 0).count();
    }

    private int getNextStartPosition(){
        // find the first start position which is not used already
        int startPos = 0;
        for(int i = 1; i <= MAX_NR_CARS; i++){
            final int finalI = i;
            if(clients.stream().allMatch(c -> c.getStartPosition() != finalI)) {
                startPos = i;
                break;
            }
        }
        return startPos;
    }

    private Client updatePosition(String clientId, int xPos, int yPos, int speed, int angle){
        Client client = getClient(clientId);
        client.setxPos(xPos);
        client.setyPos(yPos);
        client.setSpeed(speed);
        client.setAngle(angle);
        return client;
    }

    private void checkForDrivingCars() {
        // finish the race when all cars crashed or finished
        boolean drivingCars = clients.stream().filter(c -> c.getStartPosition() > 0).anyMatch(c -> !c.isCrashed() && !c.isFinished());
        if(!drivingCars){
            raceControl.raceReady();
        }
    }

    private int getNrFinishedCars(){
        return new Long(clients.stream().filter(Client::isFinished).count()).intValue();
    }

}
