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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    public static int RQ = 0;

    //private static String ip = "172.29.208.1";
    private static String ip = "localhost";
    private static InetAddress serverIP;
    private static int serverPortUDP = 4269;
    private static final String pathDownloads = "src/main/downloads/";
    private static final String pathFiles = "src/main/files/";

    static {
        try {
            serverIP = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //private static DatagramSocket datagramSocket;
    private static DatagramSocket datagramClientSocket;
    //private static DatagramPacket dPacketSend;
    private static int clientSocketPort = 1000;
    private static Client staticClient = new Client();

    private String clientName;
    private InetAddress clientIP;
    private String clientPortUDP;
    private String clientPortTCP;
    private ArrayList<String> listOfFiles;


    public Client(String clientName, InetAddress clientIP, String clientPortUDP, String clientPortTCP) {
        this.clientName = clientName;
        this.clientIP = clientIP;
        this.clientPortUDP = clientPortUDP;
        this.clientPortTCP = clientPortTCP;
        this.listOfFiles = new ArrayList<String>();
    }

    public Client() {
        this.clientName = "";
        this.clientIP = null;
        this.clientPortUDP = "";
        this.clientPortTCP = "";
        this.listOfFiles = new ArrayList<String>();
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
        System.out.println(InetAddress.getLocalHost().toString());

        // Generate port number for UDP and TCP, range of 1000 - 9999
        int min = 1000; int max = 9999;
        int randomPortUDP = ThreadLocalRandom.current().nextInt(min, max + 1);
        int randomPortTCP = ThreadLocalRandom.current().nextInt(min, max + 1);
        this.clientPortUDP = String.valueOf(randomPortUDP);
        this.clientPortTCP = String.valueOf(randomPortTCP);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Client client;
        //ClientFiles clientFiles;
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

        client.startClient();


    }

    private void startClient() throws IOException {
        String jsonString;
        String message;
        Gson gson;
        Scanner scannerInput = new Scanner(System.in);
        Scanner scannerFileInput = new Scanner(System.in);


        datagramClientSocket = new DatagramSocket(null);
        datagramClientSocket.setReuseAddress(true);
        datagramClientSocket.bind(new InetSocketAddress(this.clientIP, clientSocketPort));

        ClientReceiveRun clientReceiveRun = new ClientReceiveRun();
        Thread clientReceiveThread;

        DatagramClientSocketRun datagramClientSocketRun;
        Thread datagramClientSocketThread;

        datagramClientSocketRun = new DatagramClientSocketRun(this.clientIP, this.clientPortUDP);
        datagramClientSocketThread = new Thread(datagramClientSocketRun);
        datagramClientSocketThread.start();

        System.out.println("\nDEBUG: Enter 1 to recreate client data. FOR DEBUGGING PURPOSES ONLY" +
                "\nUNEXPECTED BEHAVIOUR WILL RESULT IF USED IMPROPERLY");
        System.out.println("Enter 2 to print available commands");
        System.out.println("Enter 3 to print to display client info\n");
        System.out.println("Running client...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    while (!staticClient.clientName.isBlank()) {
                        clientName = staticClient.getClientName();
                        clientIP = staticClient.getClientIP();
                        clientPortUDP = staticClient.getClientPortUDP();
                        clientPortTCP = staticClient.getClientPortTCP();
                        rebindDatagramPort(datagramClientSocketRun, datagramClientSocketThread);
                        staticClient = new Client();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();

        while(true){

            String input = scannerInput.nextLine();
            input = input.toUpperCase();
            input = input.replace(" ", "");
            switch(input) {
                case "1":
                    changeClientInfo();
                    rebindDatagramPort(datagramClientSocketRun, datagramClientSocketThread);
                    break;
                case "2":
                    // list commands
                    System.out.println("LOGIN - REGISTER - DE-REGISTER - PUBLISH - REMOVE - RETRIEVE-ALL - RETRIEVE-INFOT - SEARCH-FILE - UPDATE-CONTACT");
                    break;
                case "3":
                    // print info
                    System.out.println(info());
                    break;
                case "SIGNIN":
                case "SIGN-IN":
                case "LOGIN":
                    System.out.println("\nEnter client name to login as");
                    String inputLogin = scannerInput.nextLine();
                    ArrayList<String> inputLoginArray = removeQuotes(inputLogin);
                    inputLogin = inputLoginArray.get(0);
                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "LOGIN" + "*" + Client.RQ + "*" + inputLogin + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "LOGIN" + " " + Client.RQ + " " + inputLogin);

                    messageToServer(message);
                    System.out.println("RQ# " + Client.RQ + " LOGIN Command sent to server");
                    Client.RQ++;
                    break;
                case "REGISTER":
                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "REGISTER" + "*" + Client.RQ + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "REGISTER" + " " + Client.RQ + " " + this);

                    messageToServer(message);
                    System.out.println("RQ# " + Client.RQ + " REGISTER Command sent to server");
                    Client.RQ++;
                    break;
                case "DEREGISTER":
                case "DE-REGISTER":
                    message = "DE-REGISTER" + "*" + Client.RQ + "*" + getClientName();
                    System.out.println("MESSAGE TO SERVER: " + "DE-REGISTER" + " " + Client.RQ + " " + clientName);

                    messageToServer(message);
                    System.out.println("DE-REGISTER Command sent to server");
                    Client.RQ++;
                    break;
                case "PUBLISH":
                    System.out.println("\nSpecify file(s) to be published. Separate with space(s). " +
                            "Surround with quotations to encompass files containing spaces.");
                    String inputFile = scannerFileInput.nextLine();
                    ArrayList<String> fileList = removeQuotes(inputFile);
                    setListOfFiles(fileList);

                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "PUBLISH" + "*" + Client.RQ + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "PUBLISH" + " " + Client.RQ + " " + clientName + " " + listOfFiles.toString());

                    messageToServer(message);
                    setListOfFiles(new ArrayList<>());
                    System.out.println("RQ# " + Client.RQ + " PUBLISH Command sent to server");
                    Client.RQ++;
                    break;
                case "REMOVE":
                    System.out.println("\nSpecify file(s) to be removed. Separate with space(s). " +
                            "Surround with quotations to encompass files containing spaces.");
                    inputFile = scannerFileInput.nextLine();
                    fileList = removeQuotes(inputFile);
                    setListOfFiles(fileList);

                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "REMOVE" + "*" + Client.RQ + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "REMOVE" + " " + Client.RQ + " " + clientName + " " + listOfFiles.toString());

                    messageToServer(message);
                    setListOfFiles(new ArrayList<>());
                    System.out.println("RQ# " + Client.RQ + " PUBLISH Command sent to server");
                    Client.RQ++;
                    break;
                case "RETRIEVEALL":
                case "RETRIEVE-ALL":
                    message = "RETRIEVE-ALL" + "*" + Client.RQ + "*" + clientName;
                    System.out.println("MESSAGE TO SERVER: " + "RETRIEVE-ALL" + " " + Client.RQ);

                    messageToServer(message);
                    System.out.println("RETRIEVE-ALL Command sent to server");
                    Client.RQ++;
                    break;
                case "RETRIEVEINFOT":
                case "RETRIEVE-INFOT":
                    String inputName;
                    System.out.println("Enter name of peer to request information from");
                    inputName = scannerInput.nextLine();
                    ArrayList<String> inputArray = removeQuotes(inputName);
                    inputName = inputArray.get(0);
                    message = "RETRIEVE-INFOT" + "*" + Client.RQ + "*" + inputName + "*" + clientName;
                    System.out.println("MESSAGE TO SERVER: " + "RETRIEVE-INFOT" + " " + Client.RQ + " " + inputName);

                    messageToServer(message);
                    System.out.println("RETRIEVE-INFOT Command sent to server");
                    Client.RQ++;
                    break;
                case "SEARCHFILE":
                case "SEARCH-FILE":
                    System.out.println("Enter the name of file to search");
                    inputFile = scannerFileInput.nextLine();
                    inputArray = removeQuotes(inputFile);
                    inputFile = inputArray.get(0);
                    message = "SEARCH-FILE" + "*" + Client.RQ + "*" + inputFile + "*" + clientName;
                    System.out.println("MESSAGE TO SERVER: " + "SEARCH-FILE" + " " + Client.RQ + " " + inputFile);

                    messageToServer(message);
                    System.out.println("SEARCH-FILE Command sent to server");
                    Client.RQ++;
                    break;
                case "UPDATECONTACT":
                case "UPDATE-CONTACT":

                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    changeClientInfo();
                    gson = new Gson();
                    String jsonNewClient = gson.toJson(this);
                    message = "UPDATE-CONTACT" + "*" + Client.RQ + "*" + jsonString + "*" + jsonNewClient;
                    System.out.println("MESSAGE TO SERVER: " + "UPDATE-CONTACT" + " " + Client.RQ + " " + this);

                    messageToServer(message);
                    System.out.println("UPDATE-CONTACT Command sent to server\n");
                    Client.RQ++;
                    break;
                default:
            }



            clientReceiveThread = new Thread(clientReceiveRun);
            clientReceiveThread.start();
        }
    }

    private static class DatagramClientSocketRun implements Runnable {

        private InetAddress ip;
        private String socketPort;

        DatagramClientSocketRun(InetAddress ip, String socketPort) {
            this.ip = ip;
            this.socketPort = socketPort;
        }
        @Override
        public void run() {

            try {
                datagramClientSocket = new DatagramSocket(null);
                datagramClientSocket.setReuseAddress(true);
                datagramClientSocket.bind(new InetSocketAddress(ip, Integer.parseInt(socketPort)));
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }


    private class ClientReceiveRun implements Runnable {

        @Override
        public void run() {

                while (!datagramClientSocket.isClosed()) {
                    byte buffer[] = new byte[1048576];

                    // FOR DEBUG
                    //System.out.println("BOUND DATAGRAM PORT: " + datagramClientSocket.getLocalPort());

                    DatagramPacket dPacketReceive = new DatagramPacket(buffer, buffer.length);
                    try {
                        datagramClientSocket.receive(dPacketReceive);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    StringBuilder ret = new StringBuilder();
                    int i = 0;
                    byte current = buffer[0];
                    while (current != 0) {
                        ret.append((char) buffer[i]);
                        i++;
                        try {
                            current = buffer[i];
                        } catch (Exception e) {
                            current = 0;
                        }
                    }
                    String messageFromServer = ret.toString();
                    String[] message;
                    Gson gson;
                    if (messageFromServer.startsWith("*")) {
                        message = messageFromServer.split("\\*");
                        System.out.println("MESSAGE FROM SERVER: " + message[1]);

                    } else if (messageFromServer.startsWith("UPDATE-CONFIRMED") || messageFromServer.startsWith("LOGGED-IN")) {
                        message = messageFromServer.split("\\*");
                        gson = new Gson();
                        System.out.println("MESSAGE FROM SERVER: " + message[0] + message[1] + message[2] + " " + message[3]);
                        staticClient =  gson.fromJson(message[4], Client.class);

                    } else if (messageFromServer.startsWith("UPDATE-DENIED")) {
                        message = messageFromServer.split("\\*");
                        gson = new Gson();
                        System.out.println("MESSAGE FROM SERVER: " + message[0] + message[1] + message[2] + " " + message[3] + message[4]);
                        staticClient = gson.fromJson(message[5], Client.class);;
                    }
            }
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

    public ArrayList<String> getListOfFiles() {
        return listOfFiles;
    }

    public void setListOfFiles(ArrayList<String> listOfFiles) {
        this.listOfFiles = listOfFiles;
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

    @Override
    public String toString(){
        String ipString = String.valueOf(getClientIP());
        ipString = ipString.substring(ipString.lastIndexOf("/") + 1);
        return getClientName() + " " + ipString + " " + getClientPortUDP() + " " + getClientPortTCP();
    }

    /*
        This method validates if the IP address
        is in the format XXX.XXX.XXX.XXX
        where X is in the range of 0 to 255
     */
    public static boolean validateIPv4(String ipAddress) {
        // XXX.XXX.XXX.XXX where X = [0-9]+
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



    public static ArrayList<String> removeQuotes(String inputFile) {
        // This code was taken from
        // https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
        // Used for taking filenames surrounded by quotes ex. "file name.txt" as a single string of: file name.txt
        /************************************************************************/
        ArrayList<String> fileList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(inputFile);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                fileList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                fileList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                fileList.add(regexMatcher.group());
            }
        }
        /************************************************************************/
        return fileList;
    }

    public static void messageToServer(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket dPacketSend = new DatagramPacket(buffer, buffer.length, serverIP, serverPortUDP);
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(dPacketSend);
    }


    public void changeClientInfo() throws UnknownHostException {
        System.out.println("Enter Format: name ip udp tcp");

        Scanner scannerInput = new Scanner(System.in);
        String inputArgs = scannerInput.nextLine();
        ArrayList<String> inputsList;
        inputsList = removeQuotes(inputArgs);
        if (inputsList.size() != 4 ||
                !validateIPv4(inputsList.get(1)) ||
                !validatePortNumber(inputsList.get(2)) ||
                !validatePortNumber(inputsList.get(3))) {
            System.out.println("\nInvalid inputs submitted. You can try again to recreate client data.");
        } else {
            clientName = inputsList.get(0);
            clientIP = InetAddress.getByName(inputsList.get(1));
            clientPortUDP = String.valueOf(inputsList.get(2));
            clientPortTCP = String.valueOf(inputsList.get(3));
            System.out.println("Client created: " + this);
            System.out.println(info());
        }
    }

    public void  rebindDatagramPort(DatagramClientSocketRun datagramClientSocketRun,
                                    Thread datagramClientSocketThread) {
        // Rebind datagram port
        datagramClientSocketRun = new DatagramClientSocketRun(clientIP, clientPortUDP);
        datagramClientSocketThread = new Thread(datagramClientSocketRun);
        datagramClientSocketThread.start();
    }
}
