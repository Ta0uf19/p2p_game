package tests;
import network.NetworkHandler;

public class Client2 {

    public static void main(String[] args) {
        NetworkHandler networkHandle2 = new NetworkHandler("127.0.0.1", 1231);
        networkHandle2.sendStateInfo("Hello");
    }
}
