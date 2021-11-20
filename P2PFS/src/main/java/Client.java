import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    public static int RQ = 0;

    // private static String ip = "255.255.255";
    private static String ip = "localhost";
    private static InetAddress serverIP;
    private static int serverPortUDP = 4269;

    static {
        try {
            serverIP = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private String clientName;
    private InetAddress clientIP;
    private String clientPortUDP;
    private String clientPortTCP;
    private boolean isRegistered;

    public Client(String clientName, InetAddress clientIP, String clientPortUDP, String clientPortTCP) {
        this.clientName = clientName;
        this.clientIP = clientIP;
        this.clientPortUDP = clientPortUDP;
        this.clientPortTCP = clientPortTCP;
        this.isRegistered = false;

    }

    public Client() {
        this.clientName = "";
        this.clientIP = null;
        this.clientPortUDP = "";
        this.clientPortTCP = "";
        this.isRegistered = false;
    }

    public void generateClient() throws UnknownHostException {

        // Generate a name for the client
        System.out.println("Generating default client data...");
        String clientName = RandomGenerator.generateName();
        this.clientName = clientName;


        // Retrieve the IP address of the client
        System.out.println("Retrieving your IP address...");
        this.clientIP = InetAddress.getLocalHost();
        System.out.println("Get host name: " + InetAddress.getLocalHost().getHostAddress());


        // Generate port number for UDP and TCP, range of 1000 - 9999
        int min = 1000; int max = 9999;
        int randomPortUDP = ThreadLocalRandom.current().nextInt(min, max + 1);
        int randomPortTCP = ThreadLocalRandom.current().nextInt(min, max + 1);
        this.clientPortUDP = String.valueOf(randomPortUDP);
        this.clientPortTCP = String.valueOf(randomPortTCP);

        this.isRegistered = false;
    }

    public static void main(String[] args) throws IOException {
        Client client;
        if (args.length != 4 ||  // check if arguments are invalid, then generate a client
                !validateIPv4(args[1]) ||
                !validatePortNumber(args[2]) ||
                !validatePortNumber(args[3])) {
            System.out.println("Invalid arguments submitted");
            client = new Client();
            client.generateClient();
            System.out.println("Client generated.\n" +  client.info());
        }
        else { // else arguments are valid, then create client from input
            client = new Client(args[0], InetAddress.getByName(args[1]), args[2], args[3]);
            System.out.println();
            System.out.println("Parsed client data. " + client.info());
        }


        Scanner scannerInput = new Scanner(System.in);
        Scanner scannerFileInput = new Scanner(System.in);

        /*
        / make static
         */
        DatagramSocket datagramSocket = new DatagramSocket();
        DatagramSocket datagramClientSocket = new DatagramSocket(Integer.parseInt(client.getClientPortUDP()));



        byte buffer[] = null;

        System.out.println("Running...");
        while(true){
            String input = scannerInput.nextLine();

            input = input.toUpperCase();
            switch(input) {
                case "REGISTER":
                    Gson jsonObject = new Gson();
                    String jsonString = jsonObject.toJson(client);
                    String message = "REGISTER@" + Client.RQ + "@" + jsonString + "\n";
                    buffer = message.getBytes();
                    DatagramPacket dPacketSend = new DatagramPacket(buffer, buffer.length, serverIP, serverPortUDP);

                    datagramSocket.send(dPacketSend);
                    System.out.println("RQ# " + Client.RQ + " REGISTER Command sent to server");
                    Client.RQ++;
                    break;
                case "DE-REGISTER":
                    System.out.println("DE-REGISTER Command sent to server");
                    break;
                case "PUBLISH":
                    System.out.println("PUBLISH Command sent to server");
                    break;
                case "REMOVE":
                    System.out.println("\nSpecify file(s) to be removed. Separate with spaces.");
                    String inputFile = scannerFileInput.nextLine();
                    String[] inputFiles = inputFile.split("\\s+");
                    StringBuilder files = new StringBuilder();
                    for (int i = 0; i < inputFiles.length; i++) {
                        files.append(inputFiles[i] + " ");
                    }
                    System.out.println(files);
                    String messageToServer = "REMOVE " + Client.RQ + " " + files;
                    System.out.println("RQ# " + Client.RQ + " REMOVE Command sent to server");
                    System.out.println("messageToServer: " + messageToServer);
                    Client.RQ++;
                    break;
                case "RETRIEVE-ALL":
                    System.out.println("RETRIEVE-ALL Command sent to server");
                    break;
                case "RETRIEVE":
                    System.out.println("RETRIEVE Command sent to server");
                    break;
                case "RETRIEVE-INFOT":
                    System.out.println("RETRIEVE-INFOT Command sent to server");
                default:
            }

            new Thread(new Runnable() {
                @Override
                public void run() {



            boolean waiting = true;
            while (waiting) {



                byte buffer[] = new byte[256];
                DatagramPacket dPacketReceive = new DatagramPacket(buffer, buffer.length);

                try {
                    datagramClientSocket.receive(dPacketReceive);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String messageFromServer = new String(dPacketReceive.getData(), 0, dPacketReceive.getLength());




                    if (messageFromServer.startsWith("REGISTER")) {
                        System.out.println("MESSAGE FROM SERVER: " + messageFromServer);

                        waiting = false;
                    }


                }

                }
            }).start();

        }


    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public InetAddress getClientIP() {
        return clientIP;
    }

    public void setClientIP(InetAddress clientIP) {
        this.clientIP = clientIP;
    }

    public String getClientPortUDP() {
        return clientPortUDP;
    }

    public void setClientPortUDP(String clientPortUDP) {
        this.clientPortUDP = clientPortUDP;
    }

    public String getClientPortTCP() {
        return clientPortTCP;
    }

    public void setClientPortTCP(String clientPortTCP) {
        this.clientPortTCP = clientPortTCP;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public String info() {

        String ipString = String.valueOf(getClientIP());
        ipString = ipString.substring(ipString.lastIndexOf("/") + 1);
        return
                "\nCLIENT NAME: "   + getClientName()       +
                "\nCLIENT IP: "     + ipString        +
                "\nUDP SOCKET#: "   + getClientPortUDP()    +
                "\nTCP SOCKET#: "   + getClientPortTCP();
    }

    /*
        This method validates if the IP address
        is in the format XXX.XXX.XXX.XXX
        where X is in the range of 0 to 255
     */
    public static boolean validateIPv4(String ipAddress) {

        String ipError = "The IP address format is incorrect: ";

        if (ipAddress.isBlank() || ipAddress.contains(" ")) {
            System.out.println(ipError + ipAddress + " is either blank or contains spaces");
            return false;
        }

        if (!(ipAddress.matches("[0-9]+") || ipAddress.contains("."))) {
            System.out.println(ipError + ipAddress + " contains non-integers");
            return false;
        }



        String[] splitValues = ipAddress.split("\\.");
        if (splitValues.length != 4) {
            System.out.println(ipError + "splitValues.length = " +
                    splitValues.length +
                    " ...it should be 4");
            return false;
        }

        for (int i = 0; i < splitValues.length; i++) {
            if (Integer.parseInt(splitValues[i]) < 0 || Integer.parseInt(splitValues[i]) > 255) {
                System.out.println(ipError + " splitValues[" + i + "] = " +
                        splitValues[i] +
                        " ...should range from 0 to 255");
                return false;
            }
        }

        return true;
    }

    /*
        To check if the UDP or TCP port
        number is in the range of 1000 to 9999
     */
    public static boolean validatePortNumber(String portNumber) {

       int portNumberInteger = Integer.parseInt(portNumber);
       if (portNumberInteger < 1000 || portNumberInteger > 9999) {
           System.out.println("Incorrect port number used: " + portNumber + " ...should be from 1000 to 9999");
           return false;
       }
       return true;
    }
}
