package network;

import game.Game;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Some docs
 * @Host => called only by host
 * @Player => called only by player
 */
public class NetworkHandler {


    private final static int PORT = 1231;
    private final static int SOCKET_TIMEOUT = 15000;

    // is the player host?
    private boolean isHost;

    // total player online
    private int totalPlayers;

    // nombre de joueur qui doivent se connecter
    private int connectedPlayers;

    private DatagramSocket skt_out; 	// called by Timer to send state
    private DatagramSocket skt_in;		// to receive state of other players

    public int playerNum;
    private List<InetAddress> playerAddresses;	// addresses for UDP
    private List<Integer>         playerPorts;		// ports for UDP

    /**
     * game.Game state sync
     */
    private Runnable updateGameState;
    private Thread updateThread;
    private boolean gameStart;
    private boolean[] isInGame;  // players in game
    private Game game;

    /**
     * Hosting a game
     * @param connectedPlayers ( how much players we want to start game (we don't count host)?)
     * @param connectedPlayers
     */
    public NetworkHandler(Game game, int connectedPlayers)
    {
        this.isHost = true;

        this.game = game;
        this.game.setNetworkHandler(this);

        this.totalPlayers = connectedPlayers + 1;
        this.playerAddresses = new ArrayList<InetAddress>();
        this.playerPorts = new ArrayList<Integer>();
        this.playerNum = 0;
        this.connectedPlayers = connectedPlayers;

        initUpdateStateThread();

        // init host
        initHost();
    }

    /**
     * Start a client
     * @param address
     * @param port
     */
    public NetworkHandler(Game game, String address, int port)
    {
        this.isHost = false;
        this.game = game;
        this.game.setNetworkHandler(this);

        initUpdateStateThread();

        // init player
        initPlayer(address, port);
    }

    /**
     * @Host
     * @Init
     * Init host
     *
     * 1. receive UDP addresses one by one from all other players
     * 2. send UDP addesses of other players to each one
     * 3. waiting a start signal from all players
     * 4. starting thread (listen for incoming data) to update state of game (for each player)
     * 5. Host send his own start signal and start game
     */
    private void initHost()
    {
        try	{
            skt_in = new DatagramSocket(PORT);
            skt_in.setSoTimeout(SOCKET_TIMEOUT);
        }
        catch(Exception e){
            System.out.print("Host could not initialise in_port");
        }

        // fill playerAddresses and playerPorts
        getClients();
        // send details of all other players to all players
        sendAddressesToClients();

        // waiting for ready signal
        for (int i = 0 ; i<connectedPlayers ; i++)
        {
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                skt_in.receive(packet);
            }
            catch (Exception e) {
                System.err.println("Host: Did not receive all ready signals");
                System.exit(0);
            }

            System.out.println("Received " + Integer.toString(i+1) + " ready signal");
        }

        // initialising skt_out
        try	{
            skt_out = new DatagramSocket();
        }
        catch(Exception e){
            System.out.print("Host: Could not initialise out_port");
        }

        // starting network update state thread
        updateThread = new Thread(updateGameState);

        // sending start signal to all players and starting my game
        for (int i = 0 ; i< connectedPlayers ; i++)
        {
            for (int j = 0 ; j<5 ; j++)
            {
                try
                {
                    String strt = "start";
                    byte[] buf = new byte[256];
                    buf = strt.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, playerAddresses.get(i) , playerPorts.get(i));
                    skt_in.send(packet);

                    // send game state
                    //String strt = this.game;
                    DatagramPacket packetState = new DatagramPacket(buf, buf.length, playerAddresses.get(i) , playerPorts.get(i));
                    skt_in.send(packetState);
                }
                catch (Exception e) {
                    System.err.println("Host: Couldn't send start signal");
                }
            }
        }
        System.out.println("Sent all start signals");

        // start game for host
        startGame();

    }

    /**
     * @Player
     * @Init
     * Init player
     *
     * 1. send empty packet to host (to say hi)
     * 2. receive addresses/ports of other players (setNumPlayersPNoAddressPorts)
     * 3. starting thread (listen for incoming data) to update state of game (for each player)
     * 5. Player send his own start signal and start game
     *
     *
     * @param hostAddress of host
     * @param hostPort of host
     */
    private void initPlayer(String hostAddress, int hostPort)
    {

        boolean acknowledged = false;			// details of all other players are sent with acknowledgement
        String ackdata = "";					// this will contain the data of other players

        try
        {
            // preparing an empty packet to send to host
            skt_in = new DatagramSocket();
            skt_in.setSoTimeout(SOCKET_TIMEOUT);

            // empty ('hello') packet that will be sent to the host first
            byte[] buf 			  = new byte[256];
            InetAddress address   = InetAddress.getByName(hostAddress);
            DatagramPacket packet = new DatagramPacket(buf, buf.length,address, hostPort);

            // acknowledgement packet that will be received later from the server
            byte[] ackbuf 		  = new byte[256];
            DatagramPacket ackpkt = new DatagramPacket(ackbuf, ackbuf.length);

            while(!acknowledged)
            {
                // sending first empty packet to host
                try {
                    skt_in.send(packet);
                }
                catch (Exception e) {System.out.println("Client couldn't send its empty packet");}

                try
                {
                    // waiting to receive details of other players from host
                    // if timeout, send empty packet above again
                    skt_in.receive(ackpkt);

                    // got it!
                    ackdata = new String(ackpkt.getData());
                    acknowledged = true;
                }
                catch (SocketTimeoutException e) {
                    System.out.println("Client: waiting for acknowledgement..");
                }
            }
        }
        catch(Exception e) {
            System.out.println("Network Error: Client");
        }

        // received data from other players, filling playerAddresses and playerPorts
        setNumPlayersPNoAddressPorts(ackdata, hostAddress, hostPort);
        System.out.println("got acknowledgement: " + ackdata);
        System.out.println("connected players (excluding me): " + Integer.toString(connectedPlayers));

        // initialising skt_out
        try	{
            skt_out = new DatagramSocket();
        }
        catch(Exception e){
            System.out.print("Client: could not initialise out_port");
        }

        // starting network update thread
        updateThread = new Thread(updateGameState);

        // informing I'm ready to start
        sendReadySignal();

        // start on receiving start signal
        boolean toStart = false;

        while (!toStart)
        {
            try
            {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                skt_in.receive(packet);
                String strt = new String(packet.getData());

                //System.out.println("to Start: " + strt);

                if (strt.trim().equals("start"))
                    toStart = true;
            }
            catch (Exception e) {System.err.println("Client: Couldn't receive start signal");System.exit(0);}
        }

        startGame();
    }



    /**
     * @Host
     * @Sync: called only by host <!>
     *     Récupérer le state : l'IP / port des autres joueurs
     *      1. add new player ip/adresse in our list (to send it later to others players)
     *
     */
    private void getClients()
    {
        int curConnected = 0;

        try
        {
            //
            while (curConnected != connectedPlayers)
            {
                boolean newPlayer = true;

                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                skt_in.receive(packet);

                InetAddress cur_addr = packet.getAddress();
                int cur_port = packet.getPort();

                // check if already there => takes care of multiple connections
                for (int i = 0 ; i<curConnected ; i++)
                    if (playerAddresses.get(i).equals(cur_addr) && playerPorts.get(i)==cur_port)
                        newPlayer = false;

                if (newPlayer)
                {
                    playerAddresses.add(cur_addr);
                    playerPorts.add(cur_port);
                    System.out.println("got player " + Integer.toString(curConnected+1));
                    curConnected += 1;
                }
            }
        }
        catch(Exception e){
            System.out.print(e + "\n");
            System.exit(0);
        }

        return;
    }

    /**
     * @Host
     * @Sync : called only by host <!> Send a player data to others players
     * byte array protocol => "your player no., connectedPlayers, totalPlayers, hostname of next player, port no of next player ... ]
     */
    private void sendAddressesToClients()
    {
        // for each player (excluding host)
        for (int i = 0 ; i<connectedPlayers ; i++)
        {
            String to_send = Integer.toString(i+1);
            to_send       += "," + Integer.toString(connectedPlayers);
            to_send       += "," + Integer.toString(totalPlayers);

            for (int j = 0 ; j<connectedPlayers ; j++)
                // remove himself
                if (j!=i)
                {
                    to_send += "," + playerAddresses.get(j).getHostName();
                    to_send += "," + Integer.toString(playerPorts.get(j));
                }

            byte[] buf = new byte[256];
            buf = to_send.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, playerAddresses.get(i) , playerPorts.get(i));
            try {
                skt_in.send(packet);
            }
            catch (Exception e) {
                System.out.println("Host couldn't send the data for all others to client " + Integer.toString(i+1));
            }

            System.out.println("sent player " + Integer.toString(i+1) + "  data to other players");
        }
    }



    /**
     * @Player
     * @UpdateState
     * Update state of players (ip/port)
     *   after receving ack from host , add all received host/port to our lists
     *
     * @param ackdata
     * @param hostAddress
     * @param hostPort
     */
    private void setNumPlayersPNoAddressPorts(String ackdata, String hostAddress, int hostPort)
    {
        try
        {
            // parsing for other players
            int i = 0;
            for (String cur: ackdata.split(","))
            {
                if (i==0)
                    this.playerNum = Integer.parseInt(cur.trim());

                else if (i==1)
                {
                    this.connectedPlayers = Integer.parseInt(cur.trim());
                    this.playerAddresses  = new ArrayList<InetAddress>();
                    this.playerPorts      = new ArrayList<Integer>();

                    // for host
                    this.playerAddresses.add(InetAddress.getByName(hostAddress));
                    this.playerPorts.add(hostPort);
                }

                else if (i==2)
                {
                    this.totalPlayers = Integer.parseInt(cur.trim());
                    System.out.println("Connected Players : " + connectedPlayers);
                    System.out.println("Total Players     : " + totalPlayers);
                }

                else if (i%2 == 1) playerAddresses.add(InetAddress.getByName(cur.trim()));
                else
                {
                    playerPorts.add(Integer.parseInt(cur.trim()));
                }

                i+=1;
            }
        }
        catch (Exception e) {
            System.out.println(e+"\n");
        }

    }

    /**
     * @Player
     * Sending ready signal to host
     */
    private void sendReadySignal()
    {
        try
        {
            byte[] rdybuf = new byte[256];
            DatagramPacket rdypkt = new DatagramPacket(rdybuf, rdybuf.length, playerAddresses.get(0) , playerPorts.get(0));
            skt_in.send(rdypkt);
            System.out.println("Sent ready signal");
        }
        catch (Exception e) {
            System.err.println("I couldn't send ready signal");
            e.printStackTrace(System.out);
        }
    }

    /**
     * @Player
     * @Host
     *
     * Send state info to all others players
     * @param stateData
     */
    public void sendStateInfo(String stateData)
    {
        //System.out.println("sending: " + stateData);
        for (int i = 0 ; i < connectedPlayers ; i++)
        {
            byte[] buf = new byte[256];
            buf = stateData.getBytes();

            DatagramPacket packet = new DatagramPacket(buf, buf.length , playerAddresses.get(i) , playerPorts.get(i));

            try {
                skt_out.send(packet);
                System.out.println("sent a packet");
            }
            catch (Exception e) {
                System.err.println("I couldn't send state info");
            }
        }
    }

    /**
     * Lancement du jeu
     */
    private void startGame()
    {
        System.out.println("starting my game");
        System.out.println("Player No. " + Integer.toString(this.playerNum));


        this.isInGame     = new boolean[totalPlayers];
        Arrays.fill(this.isInGame,true);

        this.gameStart = true;
        updateThread.start();
    }

    /**
     * Définir un thread qui update state de jeu
     */
    private void initUpdateStateThread()
    {
        this.updateGameState = new Runnable()
        {
            @Override
            public void run()
            {

                // wait for game to start, else receives unnecessary messages!
                //System.out.println("LISTEN THREAD: waiting for game to start");
                //while (!gameStart) {System.out.println("still in loop");}
                //System.out.println("LISTEN THREAD: IGI");

                try
                {
                    // while game is not over
                    while (true)
                    {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);

                        try {
                            skt_in.receive(packet);
                        }
                        catch (SocketTimeoutException e) {
                            System.out.println("haven't received a packet in long :'("); continue;
                        }

                        String update = new String(packet.getData());

                        System.out.println("received a packet");
                        System.out.println("received: " + update);

                        // call game for update
                        if (update.trim().equals("start")) continue;

                        // update game state
                        game.updateStateFromNetwork(update);

                    }
                }

                catch (IOException e)
                {
                    System.err.println("Error in state update");
                    e.printStackTrace();
                }
            }
        };
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }
}
