import com.google.gson.Gson;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client implements Runnable {

    private static LogWriter logWriter;

    static {
        try {
            logWriter = new LogWriter("client.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static int RQ = 0;
    public static int currentRQ = RQ;

    //private static String ip = "192.168.2.20";
    private static String ip = "localhost";
    private static InetAddress serverIP;
    private static int serverPortUDP = 4269;
    private static final String pathDownloads = "src/main/downloads/";
    private static final String pathFiles = "src/main/files/";
    private static FileOutputStream fileOutputStream;
    private static File downloadFile;
    private static boolean enableVerboseFileTransfer = true;
    private static int toggle = 1;

    private static ServerSocket clientServerSocket;
    private static ArrayList<Socket> clientSockets;
    private static Socket clientSocket;

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
    private static Client staticClient;
    private static HashMap<Integer, byte[]> hashMapBytes;

    static {
        try {
            staticClient = new Client();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String clientName;
    private InetAddress clientIP;
    private String clientPortUDP;
    private String clientPortTCP;
    private ArrayList<String> listOfFiles;


    public Client(String clientName, InetAddress clientIP, String clientPortUDP, String clientPortTCP) throws IOException {
        this.clientName = clientName;
        this.clientIP = clientIP;
        this.clientPortUDP = clientPortUDP;
        this.clientPortTCP = clientPortTCP;
        this.listOfFiles = new ArrayList<String>();
    }

    public Client() throws IOException {
        this.clientName = "";
        this.clientIP = null;
        this.clientPortUDP = "";
        this.clientPortTCP = "";
        this.listOfFiles = new ArrayList<String>();
    }

    public void generateClient() throws IOException {

        // Generate a name for the client
        System.out.println("Generating default client data...");
        //logWriter.write(timeStamp + ": Generating default client data...\n"); logWriter.flush();
        logWriter.log("Generating default client data...\n");
        String clientName = RandomGenerator.generateName();
        this.clientName = clientName;


        // Retrieve the IP address of the client
        System.out.println("Retrieving your IP address...");
        logWriter.log("Retrieving your IP address...\n");

        this.clientIP = InetAddress.getLocalHost();
        System.out.println("Get host name: " + InetAddress.getLocalHost().getHostAddress());
        logWriter.log("Get host name: " + InetAddress.getLocalHost().getHostAddress() + "\n");

        // Generate port number for UDP and TCP, range of 1000 - 9999
        System.out.println("Generating random UDP and TCP port numbers. Range 1000 - 9999");
        logWriter.log("Generating random UDP and TCP port numbers. Range 1000 - 9999\n");
        int min = 1000; int max = 9999;
        int randomPortUDP = ThreadLocalRandom.current().nextInt(min, max + 1);
        int randomPortTCP = ThreadLocalRandom.current().nextInt(min, max + 1);
        this.clientPortUDP = String.valueOf(randomPortUDP);
        this.clientPortTCP = String.valueOf(randomPortTCP);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Client client;
        if (args.length != 4 ||  // check if arguments are invalid, then generate a client
                !validateIPv4(args[1]) ||
                !validatePortNumber(args[2]) ||
                !validatePortNumber(args[3])) {
            System.out.println("Invalid arguments submitted");
            client = new Client();
            client.generateClient();
            System.out.println("Client generated.\n" +  client.info());
            logWriter.log("Client generated.\n" +  client.info() + "\n");

        }
        else { // else arguments are valid, then create client from input
            client = new Client(args[0], InetAddress.getByName(args[1]), args[2], args[3]);
            System.out.println();
            System.out.println("Parsed client data. " + client.info());
            logWriter.log("Parsed client data. " + client.info() + "\n");
        }
        clientServerSocket = new ServerSocket(Integer.parseInt(client.getClientPortTCP())); //TODO <-- check for other changes
        clientSockets = new ArrayList<Socket>();

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
        //
        System.out.println("\nEnter 0/EXIT to end program" +
                "\nEnter 1 to recreate client data. <- FOR DEBUGGING PURPOSES ONLY: " +
                "UNEXPECTED BEHAVIOUR WILL RESULT IF USED IMPROPERLY");
        System.out.println("Enter 2 to print available commands");
        System.out.println("Enter 3 to print to display client info");
        System.out.println("Enter 4 to toggle to verbose file transfer");
        System.out.println("Enter 5 to reconfigure server IP address");
        System.out.println("Running client..."); logWriter.log("Running client...\n");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (!staticClient.clientName.isBlank()) {
                        clientName = staticClient.getClientName();
                        clientIP = staticClient.getClientIP();
                        clientPortUDP = staticClient.getClientPortUDP();
                        clientPortTCP = staticClient.getClientPortTCP();
                        rebindDatagramPort(datagramClientSocketRun, datagramClientSocketThread);
                        try {
                            clientServerSocket.close(); // TODO verify functionality
                            clientServerSocket = new ServerSocket(Integer.parseInt(clientPortTCP));
                            staticClient = new Client();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();


        Thread clientServer = new Thread(this);
        clientServer.start();

        while(true){

            String input = scannerInput.nextLine();
            input = input.toUpperCase();
            input = input.replace(" ", "");
            logWriter.log(input + "\n");
            switch(input) {
                case "0":
                case "EXIT":
                    System.out.println("Exiting... ");
                    System.exit(0);
                    break;
                case "1":
                    changeClientInfo();
                    rebindDatagramPort(datagramClientSocketRun, datagramClientSocketThread);
                    break;
                case "2":
                    // list commands
                    System.out.println("LOGIN - REGISTER - DE-REGISTER - PUBLISH - REMOVE - " +
                            "RETRIEVE-ALL - RETRIEVE-INFOT - SEARCH-FILE - UPDATE-CONTACT - DOWNLOAD");
                    System.out.println("1: Recreate client data - 2: Print commands - 3: Print client info - " +
                            "4: Toggle verbose file transfer - 5: Reconfigure server IP");
                    break;
                case "3":
                    // print info
                    System.out.println(info());
                    System.out.println("Server IP and UDP configuration: "
                            + serverIP.getHostAddress() + " " + serverPortUDP);
                    break;
                case "4":
                    toggle = (toggle + 1) % 2;
                    if (toggle == 1) {
                        System.out.println("Verbose File Transfer Enabled");
                        enableVerboseFileTransfer = true;
                    } else if (toggle == 0) {
                        System.out.println("Verbose File Transfer Disabled");
                        enableVerboseFileTransfer = false;
                    }
                    break;
                case "5":
                    String newIP = scannerInput.nextLine();

                    if (validateIPv4(newIP)) {
                        serverIP = InetAddress.getByName(newIP);
                        System.out.println("Server IP: " + serverIP.getHostAddress());
                    } else {
                        System.out.println("Invalid IP - Server IP not configured: " +  serverIP.getHostAddress());
                    }
                    break;
                case "SIGNIN":
                case "SIGN-IN":
                case "LOGIN":
                    System.out.println("\nEnter client name to login as"); logWriter.lnlog("Enter client name to login as\n");
                    String inputLogin = scannerInput.nextLine(); logWriter.log(inputLogin + "\n");
                    ArrayList<String> inputLoginArray = removeQuotes(inputLogin);
                    inputLogin = inputLoginArray.get(0);
                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "LOGIN" + "*" + Client.RQ + "*" + inputLogin + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "LOGIN" + " " + Client.RQ + " " + inputLogin);
                    logWriter.log("MESSAGE TO SERVER: " + "LOGIN" + " " + Client.RQ + " " + inputLogin + "\n");

                    messageToServer(message);
                    System.out.println("RQ# " + Client.RQ + " LOGIN Command sent to server");
                    logWriter.log("RQ# " + Client.RQ + " LOGIN Command sent to server\n");
                    Client.RQ++;
                    break;
                case "REGISTER":
                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "REGISTER" + "*" + Client.RQ + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "REGISTER" + " " + Client.RQ + " " + this);
                    logWriter.log("MESSAGE TO SERVER: " + "REGISTER" + " " + Client.RQ + " " + this + "\n");

                    messageToServer(message);
                    System.out.println("RQ# " + Client.RQ + " REGISTER Command sent to server");
                    logWriter.log("RQ# " + Client.RQ + " REGISTER Command sent to server\n");
                    Client.RQ++;
                    break;
                case "DEREGISTER":
                case "DE-REGISTER":
                    message = "DE-REGISTER" + "*" + Client.RQ + "*" + getClientName();
                    System.out.println("MESSAGE TO SERVER: " + "DE-REGISTER" + " " + Client.RQ + " " + clientName);
                    logWriter.log("MESSAGE TO SERVER: " + "DE-REGISTER" + " " + Client.RQ + " " + clientName + "\n");

                    messageToServer(message);
                    System.out.println("DE-REGISTER Command sent to server");
                    logWriter.log("DE-REGISTER Command sent to server\n");
                    Client.RQ++;
                    break;
                case "PUBLISH":
                    System.out.println("\nSpecify file(s) to be published. Separate with space(s). " +
                            "Surround with quotations to encompass files containing spaces.");
                    logWriter.lnlog("Specify file(s) to be published. Separate with space(s). " +
                            "Surround with quotations to encompass files containing spaces.\n");
                    String inputFile = scannerFileInput.nextLine();
                    logWriter.log(inputFile + "\n");
                    ArrayList<String> fileList = removeQuotes(inputFile);
                    setListOfFiles(fileList);

                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "PUBLISH" + "*" + Client.RQ + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "PUBLISH" + " " + Client.RQ + " " + clientName + " " + listOfFiles.toString());
                    logWriter.log("MESSAGE TO SERVER: " + "PUBLISH" + " " + Client.RQ + " " + clientName + " " + listOfFiles.toString() + "\n");

                    messageToServer(message);
                    setListOfFiles(new ArrayList<>());
                    System.out.println("RQ# " + Client.RQ + " PUBLISH Command sent to server");
                    logWriter.log("RQ# " + Client.RQ + " PUBLISH Command sent to server\n");
                    Client.RQ++;
                    break;
                case "REMOVE":
                    System.out.println("\nSpecify file(s) to be removed. Separate with space(s). " +
                            "Surround with quotations to encompass files containing spaces.");
                    logWriter.log("\nSpecify file(s) to be removed. Separate with space(s). " +
                            "Surround with quotations to encompass files containing spaces.\n");
                    inputFile = scannerFileInput.nextLine(); logWriter.log(inputFile + "\n");
                    fileList = removeQuotes(inputFile);
                    setListOfFiles(fileList);

                    gson = new Gson();
                    jsonString = gson.toJson(this);
                    message = "REMOVE" + "*" + Client.RQ + "*" + jsonString;
                    System.out.println("MESSAGE TO SERVER: " + "REMOVE" + " " + Client.RQ + " " + clientName + " " + listOfFiles.toString());
                    logWriter.log("MESSAGE TO SERVER: " + "REMOVE" + " " + Client.RQ + " " + clientName + " " + listOfFiles.toString() + "\n");

                    messageToServer(message);
                    setListOfFiles(new ArrayList<>());
                    System.out.println("RQ# " + Client.RQ + " PUBLISH Command sent to server");
                    logWriter.log("RQ# " + Client.RQ + " PUBLISH Command sent to server\n");
                    Client.RQ++;
                    break;
                case "RETRIEVEALL":
                case "RETRIEVE-ALL":
                    message = "RETRIEVE-ALL" + "*" + Client.RQ + "*" + clientName;
                    System.out.println("MESSAGE TO SERVER: " + "RETRIEVE-ALL" + " " + Client.RQ);
                    logWriter.log("MESSAGE TO SERVER: " + "RETRIEVE-ALL" + " " + Client.RQ + "\n");

                    messageToServer(message);
                    System.out.println("RETRIEVE-ALL Command sent to server");
                    logWriter.log("RETRIEVE-ALL Command sent to server\n");
                    Client.RQ++;
                    break;
                case "RETRIEVEINFOT":
                case "RETRIEVE-INFOT":
                    String inputName;
                    System.out.println("Enter name of peer to request information from");
                    logWriter.log("Enter name of peer to request information from\n");
                    inputName = scannerInput.nextLine();
                    ArrayList<String> inputArray = removeQuotes(inputName);
                    inputName = inputArray.get(0); logWriter.log(inputName + "\n");
                    message = "RETRIEVE-INFOT" + "*" + Client.RQ + "*" + inputName + "*" + clientName;
                    System.out.println("MESSAGE TO SERVER: " + "RETRIEVE-INFOT" + " " + Client.RQ + " " + inputName);
                    logWriter.log("MESSAGE TO SERVER: " + "RETRIEVE-INFOT" + " " + Client.RQ + " " + inputName + "\n");

                    messageToServer(message);
                    System.out.println("RETRIEVE-INFOT Command sent to server");
                    logWriter.log("RETRIEVE-INFOT Command sent to server\n");
                    Client.RQ++;
                    break;
                case "SEARCHFILE":
                case "SEARCH-FILE":
                    System.out.println("Enter the name of file to search");
                    logWriter.log("Enter the name of file to search\n");
                    inputFile = scannerFileInput.nextLine();
                    inputArray = removeQuotes(inputFile);
                    inputFile = inputArray.get(0);
                    logWriter.log(inputFile + "\n");
                    message = "SEARCH-FILE" + "*" + Client.RQ + "*" + inputFile + "*" + clientName;
                    System.out.println("MESSAGE TO SERVER: " + "SEARCH-FILE" + " " + Client.RQ + " " + inputFile);
                    logWriter.log("MESSAGE TO SERVER: " + "SEARCH-FILE" + " " + Client.RQ + " " + inputFile + "\n");

                    messageToServer(message);
                    System.out.println("SEARCH-FILE Command sent to server");
                    logWriter.log("SEARCH-FILE Command sent to server\n");
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
                    logWriter.log("MESSAGE TO SERVER: " + "UPDATE-CONTACT" + " " + Client.RQ + " " + this + "\n");

                    messageToServer(message);
                    System.out.println("UPDATE-CONTACT Command sent to server\n");
                    logWriter.log("UPDATE-CONTACT Command sent to server\n\n");
                    Client.RQ++;
                    break;
                case "DOWNLOAD":
                    System.out.println("Enter IP and TCP port of client in one line to connect to: ");
                    inputName = scannerInput.nextLine();
                    inputArray = removeQuotes(inputName);

                    String filename;
                    System.out.println("Enter name of file to download: ");
                    filename = scannerFileInput.nextLine();
                    ArrayList<String> inputs = removeQuotes(filename);
                    filename = inputs.get(0);
                    if (validateIPv4(inputArray.get(0)) && validatePortNumber(inputArray.get(1))) {
                        System.out.println("Sending connection and download request to peer");
                        clientDownloadRequest(InetAddress.getByName(inputArray.get(0)), inputArray.get(1), filename);
                        Client.currentRQ = Client.RQ;
                        Client.RQ++;
                    } else {
                        System.out.println("Invalid inputs submitted");
                    }

                    break;
                default:
            }


              clientReceiveThread = new Thread(clientReceiveRun);
              clientReceiveThread.start();
        }
    }

    @Override
    public void run() {
        Socket connectionSocket;
        DataOutputStream outputStreamToClient;
        while (!clientServerSocket.isClosed()) {
            try {

            connectionSocket = clientServerSocket.accept();
            System.out.println("CONNECTION ACCEPTED");
            System.out.println("IP: " + connectionSocket.getInetAddress().getHostAddress());
            System.out.println("PORT: " + clientServerSocket.getLocalPort());

            outputStreamToClient = new DataOutputStream(connectionSocket.getOutputStream());


            clientSockets.add(connectionSocket);
            clientHandler(connectionSocket);

            } catch (Exception e) {

            }

        }

    }

    private static void clientHandler(Socket socket) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DataOutputStream outputStreamToClient = new DataOutputStream(socket.getOutputStream());
                    BufferedReader inputStreamFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String clientMessage;
                    boolean running = true;

                    while (running) {

                        clientMessage = inputStreamFromClient.readLine();


                        if (clientMessage.startsWith("CLOSE")) {
                            for (int i = 0; i < clientSockets.size(); i++) {
                                if (clientSockets.get(i).getPort() == socket.getPort()) {
                                    clientSockets.get(i);
                                    running = false;
                                }
                            }
                        }
                        else if (clientMessage.startsWith("DOWNLOAD")) {

                            String[] messages = clientMessage.split("\\*");
                            if (enableVerboseFileTransfer) {
                                System.out.println("MESSAGE FROM PEER: " + messages[0] + " - RQ#: " + messages[1] + " '" + messages[2] + "'");
                                logWriter.log("MESSAGE FROM PEER: " + messages[0] + " - RQ#: " + messages[1] + " '" + messages[2] + "'" + "\n");
                            }


                            System.out.println("Reading content... ");
                            logWriter.log("Reading content... \n");
                            // read entire file and put into an array of bytes
                            byte[] fileContent = Files.readAllBytes(Paths.get(pathFiles + messages[2])); // "src/main/files/" + "filename"
                            ArrayList<byte[]> bytesList = new ArrayList<byte[]>();
                            ArrayList<String> byteStringList = new ArrayList<String>();
                            int limit = 200; // chunks are limited to 200 bytes max
                            byte[] byter = new byte[limit];

                            System.out.println("File length: " + fileContent.length + " bytes / "
                                    + (fileContent.length / 1000.0f) + " kilobytes\n");
                            logWriter.log("File length: " + fileContent.length + " bytes / "
                                    + (fileContent.length / 1000.0f) + " kilobytes\n\n");

                            for (int i = 0; i < fileContent.length; i++) {
                                // DEBUG
                                // System.out.print(fileContent[i] + " ");
                                if(i % limit != 0) {
                                    byter[i % limit] = fileContent[i];
                                }

                                if ((i % limit == 0 && i != 0)) {
                                    // System.out.println();

                                    byter[0] = fileContent[i - limit];

                                    bytesList.add(byter);
                                    byter = new byte[limit];
                                } else if (i == fileContent.length - 1) {
                                    int value = 0;
                                    for (int j = 0; j < bytesList.size(); j++) {
                                        value += bytesList.get(j).length;
                                    }
                                    value = fileContent.length - value;
                                    byte[] remaining = new byte[value];
                                    for (int k = 0; k < remaining.length; k++) {
                                        remaining[k] = byter[k];
                                    }
                                    remaining[0] = fileContent[i - remaining.length + 1];
                                    bytesList.add(remaining);
                                }
                            }

                            /*
                            System.out.println("\nbytesList.size(): " + bytesList.size());
                            for (int i = 0; i < bytesList.size(); i++) {
                                System.out.println("bytesList.get(" + i + ").length: " +  bytesList.get(i).length + " Data: " + new String(bytesList.get(i)));
                            }
                            */
                            StringBuilder fileData = new StringBuilder();
                            for (int i = 0; i < bytesList.size(); i++) {
                                for (int j = 0; j < bytesList.get(i).length; j++) {
                                    String byteString = bytesList.get(i)[j] + " ";
                                    fileData.append(byteString);
                                }
                                byteStringList.add(fileData.toString());
                                fileData = new StringBuilder();
                            }

                            /*
                            System.out.println("byteStringList: ");
                            for (int i = 0; i < byteStringList.size(); i++) {
                                System.out.println(byteStringList.get(i));
                            }
                            */

                            for (int i = 0; i < bytesList.size(); i++) {
                                if (i == (bytesList.size() - 1)) {
                                    outputStreamToClient.writeBytes("FILE-END" + "*" + currentRQ + "*"
                                            + messages[2] + "*" + i + "*" + byteStringList.get(i) + "*" + "\n");
                                    System.out.println("MESSAGE TO PEER: FILE-END - RQ#: " + currentRQ + " '"
                                            + messages[2] + "' - CHUNK#: " + i + " DATA: " + byteStringList.get(i));
                                    logWriter.log("MESSAGE TO PEER: FILE-END - RQ#: " + currentRQ + " '"
                                            + messages[2] + "' - CHUNK#: " + i + " DATA: " + byteStringList.get(i) + "\n");
                                } else {
                                    outputStreamToClient.writeBytes("FILE" + "*" + currentRQ + "*"
                                            + messages[2] + "*" + i + "*" + byteStringList.get(i) + "*" + "\n");
                                    if (enableVerboseFileTransfer) {
                                        System.out.println("MESSAGE TO PEER: FILE - RQ#: " + currentRQ + " '"
                                                + messages[2] + "' - CHUNK#: " + i + " DATA: " + byteStringList.get(i));
                                        logWriter.log("MESSAGE TO PEER: FILE - RQ#: " + currentRQ + " '"
                                                + messages[2] + "' - CHUNK#: " + i + " DATA: " + byteStringList.get(i) + "\n");
                                    }
                                }
                            }



                        }
                        // Thread.sleep(10); TODO
                    }
                } catch (Exception e) {

                }

            }
        }).start();

    }

    private static void clientDownloadRequest(InetAddress clientIP, String clientPortTCP, String filename) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                String message;
                boolean waiting = true;
                hashMapBytes = new HashMap<Integer, byte[]>();
                try {
                    clientSocket = new Socket(clientIP, Integer.parseInt(clientPortTCP));
                    DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                    BufferedReader inputStreamFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    dataOutputStream.writeBytes("DOWNLOAD" + "*" + Client.RQ + "*" + filename + "\n");
                    if (enableVerboseFileTransfer) {
                        System.out.println("MESSAGE TO PEER: DOWNLOAD - RQ#: " + Client.RQ + " - '" + filename + "' ");
                        logWriter.log("MESSAGE TO PEER: DOWNLOAD - RQ#: " + Client.RQ + " - '" + filename + "' \n");
                    }

                    int finalChunk = -1;
                    int countBytes = 0;

                    while (waiting) {
                        message = inputStreamFromClient.readLine();
                        String[] messages = message.split("\\*");
                        if (enableVerboseFileTransfer) {
                            System.out.println("MESSAGE FROM PEER: " + messages[0] + " - RQ#: " + messages[1]
                                    + " '" + messages[2] + "' - CHUNK#: " + messages[3] + " DATA: " + messages[4]);
                            logWriter.log("MESSAGE FROM PEER: " + messages[0] + " - RQ#: " + messages[1]
                                    + " '" + messages[2] + "' - CHUNK#: " + messages[3] + " DATA: " + messages[4] + "\n");
                        }


                        if (messages[0].equals("FILE") || messages[0].equals("FILE-END")) {
                            String[] bytesString = messages[4].split(" ");
                            byte[] bytes = new byte[bytesString.length];
                            for (int i = 0; i < bytes.length; i++) {
                                bytes[i] = (byte) Integer.parseInt(bytesString[i]);
                            }


                            switch (messages[0]) {
                                case "FILE":
                                    hashMapBytes.put(Integer.parseInt(messages[3]), bytes);
                                    break;
                                case "FILE-END":
                                    hashMapBytes.put(Integer.parseInt(messages[3]), bytes);
                                    finalChunk = Integer.parseInt(messages[3]);
                                    break;
                            }
                            if (hashMapBytes.size() == finalChunk + 1) {
                                dataOutputStream.writeBytes("CLOSE");
                                clientSocket.close();
                                waiting = false;
                                downloadFile = new File(pathDownloads + filename);
                                fileOutputStream = new FileOutputStream(downloadFile);
                                for (int i = 0; i < hashMapBytes.size(); i++) {
                                    fileOutputStream.write(hashMapBytes.get(i));
                                    fileOutputStream.flush();
                                    countBytes += hashMapBytes.get(i).length;
                                }
                                if (countBytes < 1000) {
                                    System.out.println("FILE '" + filename + "' DOWNLOADED, SIZE: " + countBytes + " bytes");
                                }
                                else if (countBytes < 1000000) {
                                    System.out.println("FILE '" + filename + "' DOWNLOADED, SIZE: " + countBytes / 1000.0f + " kilobytes");
                                }
                                else {
                                    System.out.println("FILE '" + filename + "' DOWNLOADED, SIZE: " + countBytes / 1000000.0f + " megabytes");
                                }
                            }


                        }

                        //Thread.sleep(10); TODO
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
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
                    byte buffer[] = new byte[1048576]; // Buffer size set to 1 MB

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
                        try {logWriter.log("MESSAGE FROM SERVER: " + message[1] + "\n");} catch (IOException e) {e.printStackTrace();}


                    } else if (messageFromServer.startsWith("UPDATE-CONFIRMED") || messageFromServer.startsWith("LOGGED-IN")) {
                        message = messageFromServer.split("\\*");
                        gson = new Gson();
                        System.out.println("MESSAGE FROM SERVER: " + message[0] + message[1] + message[2] + " " + message[3]);
                        try {logWriter.log("MESSAGE FROM SERVER: " + message[0] + message[1] + message[2] + " " + message[3] + "\n");} catch (IOException e) {e.printStackTrace();}
                        staticClient =  gson.fromJson(message[4], Client.class);

                    } else if (messageFromServer.startsWith("UPDATE-DENIED")) {
                        message = messageFromServer.split("\\*");
                        gson = new Gson();
                        System.out.println("MESSAGE FROM SERVER: " + message[0] + message[1] + message[2] + " " + message[3] + message[4]);
                        try {logWriter.log("MESSAGE FROM SERVER: " + message[0] + message[1] + message[2] + " " + message[3] + message[4] + "\n");} catch (IOException e) {e.printStackTrace();}
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

    public String info() throws IOException {

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
    public static boolean validateIPv4(String ipAddress) throws IOException {
        // XXX.XXX.XXX.XXX where X = [0-9]+
        String ipError = "The IP address format is incorrect: ";

        if (ipAddress.isBlank() || ipAddress.contains(" ")) {
            System.out.println(ipError + ipAddress + " is either blank or contains spaces");
            logWriter.log(ipError + ipAddress + " is either blank or contains spaces\n");
            return false;
        }

        if (!(ipAddress.matches("[0-9]+") || ipAddress.contains("."))) {
            System.out.println(ipError + ipAddress + " contains non-integers");
            logWriter.log(ipError + ipAddress + " contains non-integers\n");
            return false;
        }

        String[] splitValues = ipAddress.split("\\.");
        if (splitValues.length != 4) {
            System.out.println(ipError + "splitValues.length = " +
                    splitValues.length +
                    " ...it should be 4");
            logWriter.log(ipError + "splitValues.length = " +
                    splitValues.length +
                    " ...it should be 4\n");
            return false;
        }

        for (int i = 0; i < splitValues.length; i++) {
            if (Integer.parseInt(splitValues[i]) < 0 || Integer.parseInt(splitValues[i]) > 255) {
                System.out.println(ipError + " splitValues[" + i + "] = " +
                        splitValues[i] +
                        " ...should range from 0 to 255");
                logWriter.log(ipError + " splitValues[" + i + "] = " +
                        splitValues[i] +
                        " ...should range from 0 to 255\n");
                return false;
            }
        }

        return true;
    }

    /*
        To check if the UDP or TCP port
        number is in the range of 1000 to 9999
     */
    public static boolean validatePortNumber(String portNumber) throws IOException {

       int portNumberInteger = Integer.parseInt(portNumber);
       if (portNumberInteger < 1000 || portNumberInteger > 9999) {
           System.out.println("Incorrect port number used: " + portNumber + " ...should be from 1000 to 9999");
           logWriter.log("Incorrect port number used: " + portNumber + " ...should be from 1000 to 9999\n");
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


    public void changeClientInfo() throws IOException {
        System.out.println("Enter Format: name ip udp tcp");

        Scanner scannerInput = new Scanner(System.in);
        String inputArgs = scannerInput.nextLine();
        logWriter.log(inputArgs + "\n");
        ArrayList<String> inputsList;
        inputsList = removeQuotes(inputArgs);
        if (inputsList.size() != 4 ||
                !validateIPv4(inputsList.get(1)) ||
                !validatePortNumber(inputsList.get(2)) ||
                !validatePortNumber(inputsList.get(3))) {
            System.out.println("\nInvalid inputs submitted. You can try again to recreate client data.");
            logWriter.log("\nInvalid inputs submitted. You can try again to recreate client data.\n");
        } else {
            clientName = inputsList.get(0);
            clientIP = InetAddress.getByName(inputsList.get(1));
            clientPortUDP = String.valueOf(inputsList.get(2));
            clientPortTCP = String.valueOf(inputsList.get(3));
            System.out.println("Client created: " + this);
            System.out.println(info());
            logWriter.log("Client created: " + this + "\n"
                    + info() + "\n");
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
