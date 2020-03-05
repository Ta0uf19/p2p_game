package game;

public interface IGame {

    /**
     * update state of local game (from network)
     * you'll receive specific packet format
     * (this function will be called when a player send state to other players)
     * @param packet
     */
    void updateStateFromNetwork(String packet);

    /**
     * Send your updated state of game to others player (in network)
     * you'll need to send specific packet format
     * @param packet
     */
    String updateStateOnNetwork();
}
