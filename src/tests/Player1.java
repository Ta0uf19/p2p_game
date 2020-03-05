package tests;
import game.Game;
import network.NetworkHandler;

public class Player1 {

    public static void main(String[] args) {

        // instance of local game
        Game game = new Game();


        NetworkHandler networkHandle = new NetworkHandler("127.0.0.1", 1231);
    }
}
