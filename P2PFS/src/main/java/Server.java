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


public class Server {

    private static final int datagramSocketPort = 4269;
    private static String pathClientJSON = "src/main/resources/Client.json";

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
                if (receivedMessage.startsWith("REGISTER")) {
                    String[] messages = receivedMessage.split("@");
                    Gson gson = new Gson();
                    Client client = gson.fromJson(messages[2], Client.class);

                    boolean containsClient = false;
                    for (i = 0; i < clientsList.size(); i++) {
                        if (client.getClientName().equals(clientsList.get(i).getClientName())) {
                            containsClient = true;
                        }
                    }

                    if (!containsClient) {
                        client.setRegistered(true);
                        clientsList.add(client);
                        writeJsonDatabase(); // used to turn clientsList into array of JSON objects and write to Client.json
                    }


                    System.out.println("\n");
                    System.out.println("RQ#: " + messages[1]);
                    System.out.println("RECEIVED REQUEST: " + messages[0] + client.info() + "\n");
                    if (client.isRegistered())
                        System.out.println("CLIENT REGISTERED");
                    else
                        System.out.println("CLIENT NOT REGISTERED: Client name exists already");

                    if (client.isRegistered()) {

                        String message = "REGISTERED - RQ#: " + messages[1] + "\n";

                        buffer = message.getBytes();
                        DatagramPacket dPacketSend = new DatagramPacket(buffer, buffer.length, client.getClientIP(), Integer.parseInt(client.getClientPortUDP()));

                        DatagramSocket datagramSocket = new DatagramSocket();
                        datagramSocket.send(dPacketSend);
                    }

                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws IOException {



        DatagramSocket datagramSocket = new DatagramSocket(datagramSocketPort);
        Server server = new Server(datagramSocket);


        // TODO REMOVE THESE TWO LINES
        //clientList = new Gson().fromJson(fileReader, new TypeToken<ArrayList<Client>>() {}.getType());



        server.readJsonDatabase();


        // DEBUG: USE THIS TO CHECK IF JSON FILE WAS CORRECTLY PARSED
        /**/ Server.printClientListData();









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
        boolean registered = (boolean) clientObject.get("isRegistered");


        client.setClientName(name);
        client.setClientIP(InetAddress.getByName(ip));
        client.setClientPortUDP(udp);
        client.setClientPortTCP(tcp);
        client.setRegistered(registered);

        //System.out.println("IP: "+ client.getClientIP().getHostAddress());

        clientsList.add(client);
    }

    public static void printClientListData() {
        System.out.println("\nCurrent list of clients registered in the server.\n");
        System.out.println("clientsList.size(): " + clientsList.size());
        for (int i = 0; i < clientsList.size(); i++) {
            System.out.println(clientsList.get(i).info());
        }
        System.out.println("\nEnd of client list.");
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
            e.printStackTrace();
        }
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
            jsonClient.get(k).put("isRegistered", clientsList.get(k).isRegistered());
            jsonObjects.get(k).put("client", jsonClient.get(k));
            clientJSONArray.add(jsonObjects.get(k));
        }

        try (FileWriter fileWriter = new FileWriter(pathClientJSON)) {
            fileWriter.write(clientJSONArray.toJSONString());
            fileWriter.flush();
        }
    }
}
