package tests;

import game.Game;
import network.NetworkHandler;

public class Host {

    public static void main(String[] args) {

        // host port : 1231
        Game game = new Game(50);
        NetworkHandler networkHandler = new NetworkHandler(game, 2);

        // build packet for init
        StringBuilder initPacket = new StringBuilder();
        initPacket.append("INIT");
        initPacket.append(",");
        initPacket.append(game.getCurrentTableMatches());
        initPacket.append(",");
        initPacket.append(7);

        networkHandler.sendStateInfo(initPacket.toString());

    }
}
