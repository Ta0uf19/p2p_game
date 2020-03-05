package tests;

import game.Game;
import network.NetworkHandler;

public class Host {

    public static void main(String[] args) {

        // host port : 1231
        Game game = new Game(5);
        NetworkHandler networkHandler = new NetworkHandler(game, 3);

    }
}
