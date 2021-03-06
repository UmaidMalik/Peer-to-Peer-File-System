import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class Server {

    private static LogWriter logWriter;

    static {
        try {
            logWriter = new LogWriter("server.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int datagramSocketPort = 4269;
    private static final String pathClientJSON = "src/main/resources/Client.json";

    private DatagramSocket datagramServerSocket;

    private static ArrayList<Client> clientsList = new ArrayList<Client>();

    public Server(DatagramSocket datagramServerSocket) {
        this.datagramServerSocket = datagramServerSocket;
    }

    public void startServer() {
        try {
            while (!datagramServerSocket.isClosed()) {

                byte[] buffer = new byte[256];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                datagramServerSocket.receive(receivePacket);
                StringBuilder ret = new StringBuilder();
                int i = 0;
                byte current = buffer[0];
                while (current != 0)
                {
                    ret.append((char) buffer[i]);
                    i++;
                    try {
                        current = buffer[i];
                    } catch(Exception e) {
                        current = 0;
                    }
                }

                String receivedMessage = ret.toString();
                String[] messages = receivedMessage.split("\\*");
                StringBuilder stringBuilder = new StringBuilder();
                Gson gson;
                String toJson;
                String message = "";
                Client client = new Client();
                boolean containsClient = false;
                int index = 0;
                switch (messages[0]) {
                    case "LOGIN": //TODO FIX THIS
                        gson = new Gson();
                        Client registeredClient = new Client();
                        client = gson.fromJson(messages[3], Client.class);
                        // messages[1] -> RQ#, messages[2] -> inputLogin, messages[3] -> client object;
                        containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (clientsList.get(i).getClientName().equals(messages[2])) {
                                containsClient = true;
                                registeredClient = clientsList.get(i);
                                index = i;
                            }
                        }
                        System.out.println("\nRQ#: " + messages[1]);
                        System.out.println("RECEIVED REQUEST: " + messages[0] + " " + client.info() + "\n");
                        logWriter.lnlog("RQ#: " + messages[1] + "\n");
                        logWriter.log("RECEIVED REQUEST: " + messages[0] + " " + client.info() + "\n\n");

                        if (client.getClientName().equals(messages[2]) && containsClient) {
                            System.out.println("CLIENT: " + client.getClientName() + " ALREADY LOGGED-IN");
                            logWriter.log("CLIENT: " + client.getClientName() + " ALREADY LOGGED-IN\n");
                            message = "*ALREADY LOGGED-IN - RQ#: " + messages[1] + " CLIENT " + messages[2];
                        }
                        else if (containsClient) {
                            clientsList.get(index).setClientIP(client.getClientIP());
                            writeJsonDatabase();
                            toJson = gson.toJson(registeredClient);
                            System.out.println("CLIENT: " + client.getClientName() + " LOGGED IN");
                            logWriter.log("CLIENT: " + client.getClientName() + " LOGGED IN\n");
                            message = "LOGGED-IN* - RQ#: *" + messages[1] + "*" + registeredClient.getClientName() + "*" + toJson;
                        } else {
                            System.out.println("CLIENT: " + client.getClientName() + " LOGIN DENIED");
                            System.out.println("Client not found");
                            logWriter.log("CLIENT: " + client.getClientName() + " LOGIN DENIED\n");
                            logWriter.log("Client not found\n");
                            message = "*LOGIN-DENIED - RQ#: " + messages[1] + " REASON: CLIENT " + messages[2] + " NOT FOUND";
                        }
                        messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                        break;
                    case "REGISTER":
                        gson = new Gson();
                        client = gson.fromJson(messages[2], Client.class);

                        containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (client.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                            }
                        }
                        System.out.println("\nRQ#: " + messages[1]);
                        System.out.println("RECEIVED REQUEST: " + messages[0] + " " + client.info() + "\n");
                        logWriter.lnlog("RQ#: " + messages[1] + "\n");
                        logWriter.log("RECEIVED REQUEST: " + messages[0] + " " + client.info() + "\n\n");

                        if (!containsClient) {
                            clientsList.add(client);
                            writeJsonDatabase(); // used to turn clientsList into array of JSON objects and write to Client.json
                            System.out.println("CLIENT REGISTERED");
                            logWriter.log("CLIENT REGISTERED\n");
                            message = "*REGISTERED - RQ#: " + messages[1] + "\n";
                        } else {
                            System.out.println("CLIENT NOT REGISTERED: Client name exists already");
                            logWriter.log("CLIENT NOT REGISTERED: Client name exists already\n");
                            message = "*REGISTER-DENIED - RQ#: " + messages[1] + " - REASON: Name is already in use" +"\n";
                        }

                        messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                        break;
                    case "DE-REGISTER":
                        Client clientForDeregistration = new Client();
                        // messages[1] -> RQ#, messages[2] -> Client Name,

                        containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (messages[2].equals(clientsList.get(i).getClientName())) {
                                System.out.println("CLIENT FOUND: " + clientsList.get(i).getClientName());
                                logWriter.log("CLIENT FOUND: " + clientsList.get(i).getClientName() + "\n");
                                containsClient = true;
                                clientForDeregistration = clientsList.get(i);
                                clientsList.remove(i);
                            }
                        }

                        if (containsClient) {
                            writeJsonDatabase(); // used to turn clientsList into array of JSON objects and write to Client.json

                            message = "*DE-REGISTERED - RQ#: " + messages[1] + " Client: " + messages[2] + "\n";
                            System.out.println("MESSAGE TO CLIENT: DE-REGISTERED - RQ#: " + messages[1] + " Client: " + messages[2]);
                            logWriter.log("MESSAGE TO CLIENT: DE-REGISTERED - RQ#: " + messages[1] + " Client: " + messages[2] + "\n");

                            messageToClient(message, clientForDeregistration.getClientIP(), clientForDeregistration.getClientPortUDP());
                            System.out.println("DE-REGISTER Successful");
                            logWriter.log("DE-REGISTER Successful\n");
                        }
                        // No further action is taken by the server if the client was already not registered
                        break;
                    case "PUBLISH":
                        index = 0;
                        gson = new Gson();
                        client = gson.fromJson(messages[2], Client.class);
                        ArrayList<String> filesList = new ArrayList<String>();
                        // messages[1] -> RQ#, messages[2] -> client


                        containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (client.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                                index = i;
                                System.out.println("Client: " + client.getClientName() + " FOUND");
                                logWriter.log("Client: " + client.getClientName() + " FOUND\n");
                            }
                        }

                        if (containsClient) {
                            for (i = 0 ; i < client.getListOfFiles().size(); i++) {
                                if (!clientsList.get(index).getListOfFiles().contains(client.getListOfFiles().get(i))
                                        && !client.getListOfFiles().get(i).isBlank()) {
                                    clientsList.get(index).getListOfFiles().add(client.getListOfFiles().get(i));
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' ADDED");
                                    logWriter.log("File: '" + client.getListOfFiles().get(i) + "' ADDED\n");
                                } else {
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' ALREADY EXISTS or IS BLANK");
                                    logWriter.log("File: '" + client.getListOfFiles().get(i) + "' ALREADY EXISTS or IS BLANK\n");
                                }
                            }
                            writeJsonDatabase();
                            System.out.println("\nClient files added to JSON database: " + pathClientJSON);
                            logWriter.lnlog("Client files added to JSON database: " + pathClientJSON + "\n");
                            message = "*PUBLISHED - RQ#: " + messages[1] + "\n";
                            System.out.println("MESSAGE TO CLIENT: PUBLISHED - RQ#: " + messages[1]);
                            logWriter.log("MESSAGE TO CLIENT: PUBLISHED - RQ#: " + messages[1] + "\n");
                            System.out.println("PUBLISH Successful\n");
                            logWriter.log("PUBLISH Successful\n\n");
                        } else {
                            message = "*PUBLISH-DENIED - RQ#: " + messages[1] + " - REASON: Client name '"
                                    + client.getClientName() + "' does not exist" + "\n";
                            System.out.println("MESSAGE TO CLIENT: PUBLISH-DENIED - RQ#: " + messages[1]
                                    + "REASON: Client name '" + client.getClientName() + "' does not exist");
                            logWriter.log("MESSAGE TO CLIENT: PUBLISH-DENIED - RQ#: " + messages[1]
                                    + "REASON: Client name '" + client.getClientName() + "' does not exist\n");
                            System.out.println("PUBLISH DENIED: Client not registered\n");
                            logWriter.log("PUBLISH DENIED: Client not registered\n\n");
                        }

                        messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                        break;
                    case "REMOVE":
                        gson = new Gson();
                        index = 0;
                        client = gson.fromJson(messages[2], Client.class);
                        // messages[1] -> RQ#, messages[2] -> client

                        containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (client.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                                index = i;
                                System.out.println("Client: " + client.getClientName() + " FOUND");
                                logWriter.log("Client: " + client.getClientName() + " FOUND\n");
                            }
                        }

                        if (containsClient) {
                            for (i = 0; i < client.getListOfFiles().size(); i++) {
                                if (clientsList.get(index).getListOfFiles().contains(client.getListOfFiles().get(i))
                                        && !client.getListOfFiles().get(i).isBlank()) {
                                    clientsList.get(index).getListOfFiles().remove(client.getListOfFiles().get(i));
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' REMOVED");
                                    logWriter.log("File: '" + client.getListOfFiles().get(i) + "' REMOVED\n");
                                } else {
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' NOT FOUND or IS BLANK");
                                    logWriter.log("File: '" + client.getListOfFiles().get(i) + "' NOT FOUND or IS BLANK\n");
                                }
                            }
                            writeJsonDatabase();
                            System.out.println("Client files removed from JSON database: " + pathClientJSON);
                            logWriter.log("Client files removed from JSON database: " + pathClientJSON + "\n");
                            message = "*REMOVED - RQ#: " + messages[1] + "\n";
                            System.out.println("MESSAGE TO CLIENT: REMOVED - RQ#: " + messages[1]);
                            System.out.println("REMOVE Successful");
                            logWriter.log("MESSAGE TO CLIENT: REMOVED - RQ#: " + messages[1] + "\n");
                            logWriter.log("REMOVE Successful\n");
                        } else {
                            message = "*REMOVE-DENIED - RQ#: " + messages[1] + " - REASON: Client name '"
                                    + client.getClientName() + "' does not exist" + "\n";
                            System.out.println("MESSAGE TO CLIENT: REMOVE-DENIED - RQ#: " + messages[1]
                                    + "REASON: Client name '" + client.getClientName() + "' does not exist");
                            System.out.println("REMOVE DENIED: Client not registered");
                            logWriter.log("MESSAGE TO CLIENT: REMOVE-DENIED - RQ#: " + messages[1]
                                    + "REASON: Client name '" + client.getClientName() + "' does not exist\n");
                            logWriter.log("REMOVE DENIED: Client not registered\n");
                        }

                        messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                        break;
                    case "RETRIEVE-ALL":
                        // messages[1] -> RQ#, messages[2] -> registeredClientName
                        containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (messages[2].equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                                client = clientsList.get(i);
                                System.out.println("Client: " + client.getClientName() + " FOUND");
                                logWriter.log("Client: " + client.getClientName() + " FOUND\n");
                            }
                        }

                        if (containsClient) {
                            System.out.println("SENDING LIST OF REGISTERED CLIENTS TO REQUESTING CLIENT");
                            logWriter.log("SENDING LIST OF REGISTERED CLIENTS TO REQUESTING CLIENT\n");
                            stringBuilder.append("*\n");
                            for (i = 0; i < clientsList.size(); i++) {
                                stringBuilder.append(
                                        "\nCLIENT NAME: " + clientsList.get(i).getClientName() +
                                                "\nCLIENT IP: " + clientsList.get(i).getClientIP().getHostAddress() +
                                                "\nCLIENT TCP SOCKET#: " + clientsList.get(i).getClientPortTCP() +
                                                "\nLIST OF FILE(S): ");
                                if (!clientsList.get(i).getListOfFiles().isEmpty()) {
                                    for (int j = 0; j < clientsList.get(i).getListOfFiles().size(); j++) {
                                        stringBuilder.append("\n'" + clientsList.get(i).getListOfFiles().get(j) + "'");
                                    }
                                }
                                stringBuilder.append("\n");
                            }
                            message = stringBuilder.toString();
                            messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                            System.out.println("LIST OF REGISTERED CLIENT SENT");
                            logWriter.log("LIST OF REGISTERED CLIENT SENT\n");
                        }
                        break;
                    case "RETRIEVE-INFOT":
                        // messages[1] -> RQ#, messages[2] -> specific peer, messages[3] -> registeredClientName
                        boolean containsPeer = false;
                        containsClient = false;
                        stringBuilder = new StringBuilder();
                        Client clientPeer = new Client();

                        for (i = 0; i < clientsList.size(); i++) {
                            if (messages[3].equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                                client = clientsList.get(i);
                                System.out.println("Client: " + client.getClientName() + " FOUND");
                                logWriter.log("Client: " + client.getClientName() + " FOUND\n");
                            }
                        }
                        for (i = 0; i < clientsList.size(); i++) {
                            if (messages[2].equals(clientsList.get(i).getClientName())) {
                                containsPeer = true;
                                clientPeer = clientsList.get(i);
                                System.out.println("Specific peer: " + messages[2] + " FOUND");
                                logWriter.log("Specific peer: " + messages[2] + " FOUND\n");
                            }
                        }

                       stringBuilder.append("*");
                        if (containsClient && containsPeer) {
                            stringBuilder.append("\n\nCLIENT NAME: " + clientPeer.getClientName() +
                                    "\nCLIENT IP: " + clientPeer.getClientIP().getHostAddress() +
                                    "\nCLIENT TCP SOCKET#: " + clientPeer.getClientPortTCP() +
                                    "\nLIST OF FILE(S): ");
                            System.out.println("SENDING CLIENT DATA TO REQUESTING CLIENT");
                            logWriter.log("SENDING CLIENT DATA TO REQUESTING CLIENT\n");
                            for (i = 0; i < clientPeer.getListOfFiles().size(); i++) {
                                stringBuilder.append("\n'" + clientPeer.getListOfFiles().get(i) + "'");
                            }
                            stringBuilder.append("\n");
                            System.out.println("CLIENT DATA TO REQUESTING CLIENT SENT");
                            logWriter.log("CLIENT DATA TO REQUESTING CLIENT SENT\n");
                        } else if (containsClient && !containsPeer) {
                            stringBuilder.append("RETRIEVE-ERROR - RQ#: " + messages[1] + " - REASON: Requested name " + messages[2] + " does not exist");
                        }

                        if (containsClient) {
                            message = stringBuilder.toString();
                            messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                        }
                        break;
                    case "SEARCH-FILE":
                        // messages[1] -> RQ#, messages[2] -> file name, messages[3] -> registeredClientName
                        containsClient = false;
                        boolean containsFile = false;
                        stringBuilder = new StringBuilder();
                        for (i = 0; i < clientsList.size(); i++) {
                            if (messages[3].equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                                client = clientsList.get(i);
                                System.out.println("Client: " + client.getClientName() + " FOUND");
                                logWriter.log("Client: " + client.getClientName() + " FOUND\n");
                            }
                        }
                        if (containsClient) {
                            StringBuilder tempBuild = new StringBuilder();
                            for (i = 0; i < clientsList.size(); i++) {
                                if (clientsList.get(i).getListOfFiles().contains(messages[2])) {
                                    tempBuild.append("\n\nCLIENT NAME: " + clientsList.get(i).getClientName() +
                                    "\nCLIENT IP: " + clientsList.get(i).getClientIP().getHostAddress() +
                                            "\nCLIENT TCP SOCKET#: " + clientsList.get(i).getClientPortTCP() + "\n");
                                    System.out.println("CLIENT: " + clientsList.get(i).getClientName() +
                                            " has the file '" + messages[2] + "'");
                                    logWriter.log("CLIENT: " + clientsList.get(i).getClientName() +
                                            " has the file '" + messages[2] + "'\n");
                                    containsFile = true;
                                }
                            }
                            if (containsFile) {
                                System.out.println("Contains file:" + containsFile);
                                logWriter.log("Contains file:" + containsFile);
                                stringBuilder.append("*SEARCH-FILE - RQ: " + messages[1] + " FILE NAME: '" + messages[2]);
                                stringBuilder.append(tempBuild.toString());

                                System.out.println(stringBuilder);
                                System.out.println("SEARCH-FILE response to client " + messages[3] + " sent");
                                logWriter.log(stringBuilder + "\n");
                                logWriter.log("SEARCH-FILE response to client " + messages[3] + " sent\n");

                            } else if (!containsFile) {
                                stringBuilder.append("*SEARCH-ERROR - RQ: " + messages[1] + " FILE NAME: '" + messages[2] + "' NOT FOUND\n");
                                System.out.println("SEARCH-ERROR response to client " + messages[3] + " sent");
                                logWriter.log("SEARCH-ERROR response to client " + messages[3] + " sent\n");
                            }

                                message = stringBuilder.toString();
                                messageToClient(message, client.getClientIP(), client.getClientPortUDP());

                        }
                        break;
                    case "UPDATE-CONTACT":
                        gson = new Gson();
                        Client clientOld = gson.fromJson(messages[2], Client.class);
                        gson = new Gson();
                        Client clientNew = gson.fromJson(messages[3], Client.class);

                        boolean containsOldClient = false;
                        boolean containsNewClient = false;



                        // check if the new client name exist already
                        for (i = 0; i < clientsList.size(); i++) {
                            if (clientNew.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsNewClient = true;
                            }
                            if (clientOld.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsOldClient = true;
                            }
                        }
                        if ((!containsNewClient || (clientOld.getClientName().equals(clientNew.getClientName()))) && containsOldClient) {
                            for (i = 0; i < clientsList.size(); i++) {
                                if (clientOld.getClientName().equals(clientsList.get(i).getClientName())) {
                                    clientsList.get(i).setClientName(clientNew.getClientName());
                                    clientsList.get(i).setClientIP(clientNew.getClientIP());
                                    clientsList.get(i).setClientPortUDP(clientNew.getClientPortUDP());
                                    clientsList.get(i).setClientPortTCP(clientNew.getClientPortTCP());
                                    System.out.println("Client contact information updated");
                                    logWriter.log("Client contact information updated\n");
                                }
                            }
                            writeJsonDatabase();
                            System.out.println("Client information updated in JSON database: " + pathClientJSON);
                            message = "UPDATE-CONFIRMED* - RQ#: *" + messages[1] + "* " + clientNew + "*" + messages[3];
                            System.out.println("MESSAGE TO CLIENT: UPDATE-CONFIRMED - RQ#: " + messages[1] + " " + clientNew);
                            System.out.println("UPDATE-CONTACT Successful");
                            logWriter.log("Client information updated in JSON database: " + pathClientJSON + "\n");
                            logWriter.log("MESSAGE TO CLIENT: UPDATE-CONFIRMED - RQ#: " + messages[1] + " " + clientNew + "\n");
                            logWriter.log("UPDATE-CONTACT Successful\n");
                        }
                        else if (!containsOldClient) {
                            // client is not registered
                            message = "UPDATE-DENIED* - RQ#: *" + messages[1] + "*" + clientOld.getClientName() + "* REASON: client is not registered*" + messages[2];
                            System.out.println("MESSAGE TO CLIENT: UPDATE-DENIED - RQ#: " + messages[1] + " " + clientOld);
                            System.out.println("UPDATE-DENIED: Client not registered");
                            logWriter.log("MESSAGE TO CLIENT: UPDATE-DENIED - RQ#: " + messages[1] + " " + clientOld + "\n");
                            logWriter.log("UPDATE-DENIED: Client not registered\n");
                        }
                        else if (containsNewClient) {
                            // can't change to this name as it in use
                            message = "UPDATE-DENIED* - RQ#: *" + messages[1] + "*" + clientNew.getClientName() + "* REASON: new client name is in use*" + messages[2];
                            System.out.println("MESSAGE TO CLIENT: UPDATE-DENIED - RQ#: " + messages[1] + " " + clientNew);
                            System.out.println("UPDATE-DENIED: New name is in use");
                            logWriter.log("MESSAGE TO CLIENT: UPDATE-DENIED - RQ#: " + messages[1] + " " + clientNew + "\n");
                            logWriter.log("UPDATE-DENIED: New name is in use\n");
                        }
                        messageToClient(message, clientOld.getClientIP(), clientOld.getClientPortUDP());
                        break;
                    default:
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws IOException {


        DatagramSocket datagramSocket = new DatagramSocket(datagramSocketPort);


        Server server = new Server(datagramSocket);
        server.readJsonDatabase();



        // DEBUG: USE THIS TO CHECK IF JSON FILE WAS CORRECTLY PARSED
        /**/
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Enter 0/EXIT to end program" +
                        "\nEnter 1 to print list of register clients.");
                Scanner scanner = new Scanner(System.in);
                String input;
                while (true) {
                    input = scanner.nextLine();
                    switch (input.toUpperCase()) {
                        case "1":
                            try { printClientListData();} catch (IOException e) {e.printStackTrace();}
                            break;
                        case "0":
                        case "EXIT":
                            System.out.println("Exiting... ");
                            System.exit(0);
                            break;
                    }
                }
            }
        }).start();
        /**/









        server.startServer();

    }

    public static ArrayList<Client> getClientsList() {
        return clientsList;
    }

    public static void setClientsList(ArrayList<Client> clients_List) {
        clientsList = clients_List;
    }

    public static void parseClientObject(JSONObject clientJson) throws IOException {
        Client client = new Client();

        JSONObject clientObject = (JSONObject) clientJson.get("client");
        // get name, get ip, get udp port, get tcp port, and registration
        String name = (String) clientObject.get("clientName");
        String ip = (String) clientObject.get("clientIP");
        String udp = (String) clientObject.get("clientPortUDP");
        String tcp = (String) clientObject.get("clientPortTCP");
        ArrayList<String> list = (ArrayList<String>) clientObject.get("listOfFiles");


        client.setClientName(name);
        client.setClientIP(InetAddress.getByName(ip));
        client.setClientPortUDP(udp);
        client.setClientPortTCP(tcp);
        client.setListOfFiles(list);

        //System.out.println("IP: "+ client.getClientIP().getHostAddress());

        clientsList.add(client);
    }


    public static void printClientListData() throws IOException {
        System.out.println("\nCurrent list of clients registered in the server.");
        System.out.println("Clients Registered: " + clientsList.size());
        for (int i = 0; i < clientsList.size(); i++) {
            System.out.println(clientsList.get(i).info());
            if (!clientsList.get(i).getListOfFiles().isEmpty()) {
                System.out.println("LIST OF FILE(S): ");
                for (int j = 0; j < clientsList.get(i).getListOfFiles().size(); j++) {
                    System.out.println("\"" + clientsList.get(i).getListOfFiles().get(j) + "\"");
                }
            }
        }
        System.out.println("\nEnd of client list.");
    }


    public void writeJsonDatabase() throws IOException {

        ArrayList<JSONObject> jsonClient = new ArrayList<JSONObject>();
        ArrayList<JSONObject> jsonObjects = new ArrayList<JSONObject>();
        JSONArray clientJSONArray = new JSONArray();
        for (int j = 0; j < clientsList.size(); j++) {
            jsonClient.add(new JSONObject());
            jsonObjects.add(new JSONObject());
        }

        for (int k = 0; k < jsonObjects.size(); k++) {

            jsonClient.get(k).put("clientName", clientsList.get(k).getClientName());
            jsonClient.get(k).put("clientIP", clientsList.get(k).getClientIP().getHostAddress());
            jsonClient.get(k).put("clientPortUDP", clientsList.get(k).getClientPortUDP());
            jsonClient.get(k).put("clientPortTCP", clientsList.get(k).getClientPortTCP());
            jsonClient.get(k).put("listOfFiles", clientsList.get(k).getListOfFiles());
            jsonObjects.get(k).put("client", jsonClient.get(k));
            clientJSONArray.add(jsonObjects.get(k));
        }

        try (FileWriter fileWriter = new FileWriter(pathClientJSON)) {
            fileWriter.write(clientJSONArray.toJSONString());
            fileWriter.flush();
        }
    }

    public void readJsonDatabase() {
        JSONParser jsonParser = new JSONParser();
        Object object;
        JSONArray clientJsonList = null;
        try(FileReader reader = new FileReader(pathClientJSON)) {
            object = jsonParser.parse(reader);
            clientJsonList = (JSONArray) object;
            clientJsonList.forEach(clientJson -> {
                try {
                    parseClientObject((JSONObject)clientJson);
                } catch (IOException e ) {
                    e.printStackTrace();
                }
            });
        } catch (ParseException | IOException e) {
            System.out.println("Json Database is empty");
        }
    }

    public void messageToClient(String message, InetAddress ip, String port) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket dPacketSend = new DatagramPacket(buffer, buffer.length, ip, Integer.parseInt(port));
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(dPacketSend);
    }
}
