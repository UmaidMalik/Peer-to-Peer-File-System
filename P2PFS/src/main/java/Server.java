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
import java.util.Scanner;


public class Server {



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
                Gson gson;
                String message = "";
                switch (messages[0]) {
                    case "LOGIN":
                        break;
                    case "REGISTER":
                        gson = new Gson();
                        Client client = gson.fromJson(messages[2], Client.class);

                        boolean containsClient = false;
                        for (i = 0; i < clientsList.size(); i++) {
                            if (client.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsClient = true;
                            }
                        }
                        System.out.println("\nRQ#: " + messages[1]);
                        System.out.println("RECEIVED REQUEST: " + messages[0] + client.info() + "\n");

                        if (!containsClient) {
                            clientsList.add(client);
                            writeJsonDatabase(); // used to turn clientsList into array of JSON objects and write to Client.json
                            System.out.println("CLIENT REGISTERED");
                            message = "*REGISTERED - RQ#: " + messages[1] + "\n";
                        } else {
                            System.out.println("CLIENT NOT REGISTERED: Client name exists already");
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
                                containsClient = true;
                                clientForDeregistration = clientsList.get(i);
                                clientsList.remove(i);
                            }
                        }

                        if (containsClient) {
                            writeJsonDatabase(); // used to turn clientsList into array of JSON objects and write to Client.json

                            message = "*DE-REGISTERED - RQ#: " + messages[1] + " Client: " + messages[2] + "\n";
                            System.out.println("MESSAGE TO CLIENT: DE-REGISTERED - RQ#: " + messages[1] + " Client: " + messages[2]);

                            messageToClient(message, clientForDeregistration.getClientIP(), clientForDeregistration.getClientPortUDP());
                            System.out.println("DE-REGISTER Successful");
                        }
                        // No further action is taken by the server if the client was already not registered
                        break;
                    case "PUBLISH":
                        int index = 0;
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
                            }
                        }

                        if (containsClient) {
                            for (i = 0 ; i < client.getListOfFiles().size(); i++) {
                                if (!clientsList.get(index).getListOfFiles().contains(client.getListOfFiles().get(i))
                                        && !client.getListOfFiles().get(i).isBlank()) {
                                    clientsList.get(index).getListOfFiles().add(client.getListOfFiles().get(i));
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' ADDED");
                                } else {
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' ALREADY EXISTS or IS BLANK");
                                }
                            }
                            writeJsonDatabase();
                            System.out.println("Client files added to JSON database: " + pathClientJSON);
                            message = "*PUBLISHED - RQ#: " + messages[1] + "\n";
                            System.out.println("MESSAGE TO CLIENT: PUBLISHED - RQ#: " + messages[1]);
                            System.out.println("PUBLISH Successful");
                        } else {
                            message = "*PUBLISH-DENIED - RQ#: " + messages[1] + " - REASON: Client name '"
                                    + client.getClientName() + "' does not exist" + "\n";
                            System.out.println("MESSAGE TO CLIENT: PUBLISH-DENIED - RQ#: " + messages[1]
                                    + "REASON: Client name '" + client.getClientName() + "' does not exist");
                            System.out.println("PUBLISH DENIED: Client not registered");
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
                            }
                        }

                        if (containsClient) {
                            for (i = 0 ; i < client.getListOfFiles().size(); i++) {
                                if (clientsList.get(index).getListOfFiles().contains(client.getListOfFiles().get(i))
                                        && !client.getListOfFiles().get(i).isBlank()) {
                                    clientsList.get(index).getListOfFiles().remove(client.getListOfFiles().get(i));
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' REMOVED");
                                } else {
                                    System.out.println("File: '" + client.getListOfFiles().get(i) + "' NOT FOUND or IS BLANK");
                                }
                            }
                            writeJsonDatabase();
                            System.out.println("Client files removed from JSON database: " + pathClientJSON);
                            message = "*REMOVED - RQ#: " + messages[1] + "\n";
                            System.out.println("MESSAGE TO CLIENT: REMOVED - RQ#: " + messages[1]);
                            System.out.println("REMOVE Successful");
                        } else {
                            message = "*REMOVE-DENIED - RQ#: " + messages[1] + " - REASON: Client name '"
                                    + client.getClientName() + "' does not exist" + "\n";
                            System.out.println("MESSAGE TO CLIENT: REMOVE-DENIED - RQ#: " + messages[1]
                                    + "REASON: Client name '" + client.getClientName() + "' does not exist");
                            System.out.println("REMOVE DENIED: Client not registered");
                        }

                        messageToClient(message, client.getClientIP(), client.getClientPortUDP());
                        break;
                    case "RETRIEVE-ALL":
                        break;
                    case "RETRIVE-INFOT":
                        break;
                    case "SEARCH-FILE":
                        break;
                    case "UPDATE-CONTACT":
                        gson = new Gson();
                        Client clientOld = gson.fromJson(messages[2], Client.class);
                        Client clientNew = gson.fromJson(messages[3], Client.class);

                        boolean containsOldClient = false;
                        boolean containsNewClient = false;

                        InetAddress ip = InetAddress.getByName("255.255.255.255");
                        String port = "1000";

                        // check if the new client name exist already
                        for (i = 0; i < clientsList.size(); i++) {
                            if (clientNew.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsNewClient = true;
                            }
                            if (clientOld.getClientName().equals(clientsList.get(i).getClientName())) {
                                containsOldClient = true;
                            }
                        }
                        if (!containsNewClient && containsOldClient) {
                            for (i = 0; i < clientsList.size(); i++) {
                                if (clientOld.getClientName().equals(clientsList.get(i).getClientName())) {
                                    clientsList.get(i).setClientName(clientNew.getClientName());
                                    clientsList.get(i).setClientIP(clientNew.getClientIP());
                                    clientsList.get(i).setClientPortUDP(clientNew.getClientPortUDP());
                                    clientsList.get(i).setClientPortTCP(clientNew.getClientPortTCP());
                                    System.out.println("Client contact information updated");
                                }
                            }
                            writeJsonDatabase();
                            System.out.println("Client information updated in JSON database: " + pathClientJSON);
                            message = "UPDATE-CONFIRMED* - RQ#: *" + messages[1] + "*" + clientNew + "*" + messages[3];
                            System.out.println("MESSAGE TO CLIENT: UPDATE-CONFIRMED - RQ#: " + messages[1] + clientNew);
                            System.out.println("UPDATE-CONTACT Successful");
                            ip = clientNew.getClientIP(); port = clientNew.getClientPortUDP();
                        }
                        else if (!containsOldClient) {
                            // client is not registered
                            message = "UPDATE-DENIED* - RQ#: *" + messages[1] + "*" + clientNew.getClientName() + "* REASON: client is not registered*" + messages[2];
                            System.out.println("MESSAGE TO CLIENT: UPDATE-DENIED - RQ#: " + messages[1] + clientNew);
                            System.out.println("UPDATE-DENIED: Client not registered");
                            ip = clientOld.getClientIP(); port = clientOld.getClientPortUDP();
                        }
                        else if (containsNewClient) {
                            // can't change to this name as it in use
                            message = "UPDATE-DENIED* - RQ#: *" + messages[1] + "*" + clientNew.getClientName() + "* REASON: new client name is in use*" + messages[2];
                            System.out.println("MESSAGE TO CLIENT: UPDATE-DENIED - RQ#: " + messages[1] + clientNew);
                            System.out.println("UPDATE-DENIED: New name is in use");
                            ip = clientOld.getClientIP(); port = clientOld.getClientPortUDP();
                        }
                        messageToClient(message, ip, port);
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
                System.out.println("Enter 1 to print list of register clients.");
                Scanner scanner = new Scanner(System.in);
                String input;
                while (true) {
                    input = scanner.nextLine();
                    if (input.equals("1")) {
                        printClientListData();
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

    public static void parseClientObject(JSONObject clientJson) throws UnknownHostException {
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


    public static void printClientListData() {
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
                } catch (UnknownHostException e) {
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
