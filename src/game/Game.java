package game;

import network.NetworkHandler;

public class Game implements IGameNetwork {

    private NetworkHandler networkHandler;
    private int initTableMatches = 0;
    private int currentTableMatches = 0;
    private int maxRemoveMatches = 0;
    private int removedMatches = 0;

    // if user can play?
    private boolean play = false;
    private int playerId;
    private int receiveplayerId;

    public Game() {

    }
    public Game(int initTableMatches) {
        this.initTableMatches = initTableMatches;
        this.currentTableMatches = initTableMatches;
    }

    // packet : INIT,nombre allumette init, nombre max all
    @Override
    public void updateStateFromNetwork(String packet) {

        String newPacket = packet.trim();
        String data[] = newPacket.split(",");

        String packetType = data[0];
        System.out.println("Packet game received:  " + newPacket);

        if(packetType.equals("INIT")) {
            this.currentTableMatches = Integer.parseInt(data[1]);
            this.maxRemoveMatches = Integer.parseInt(data[2]);
        }
        if(packetType.equals("PLAY")) {
            this.removedMatches = Integer.parseInt(data[1]);
            this.currentTableMatches = Integer.parseInt(data[2]);
            this.receiveplayerId = Integer.parseInt(data[3]);
        }


        // i receive packet
        System.out.println(this);


    }

    // send data to network
    @Override
    public void updateStateOnNetwork() {

        // packet play
        StringBuilder packet = new StringBuilder();
        packet.append("PLAY");
        packet.append(",");
        packet.append(this.removedMatches);
        packet.append(",");
        packet.append(this.currentTableMatches);
        packet.append(",");
        packet.append(this.playerId);


        // send data to network
        networkHandler.sendStateInfo(packet.toString());
    }

    public void play()
    {
        if(networkHandler.playerNum % networkHandler.getTotalPlayers() == receiveplayerId) {

            currentTableMatches--;

            // send new data to other players
            updateStateOnNetwork();
        }
    }

    public void setNetworkHandler(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    public int getCurrentTableMatches() {
        return currentTableMatches;
    }

    @Override
    public String toString() {
        return "Game{" +
                "networkHandler=" + networkHandler +
                ", currentTableMatches=" + currentTableMatches +
                ", maxRemoveMatches=" + maxRemoveMatches +
                ", playerId=" + playerId +
                ", play=" + play +
                '}';
    }

    public static void main(String[] args)
    {



    }
}
