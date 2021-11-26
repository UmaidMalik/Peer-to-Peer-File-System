import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Test {

    // FOR TESTING PURPOSES ONLY

    public static void main(String[] arg) throws IOException {


        System.out.println("Ello Gov'nor\n");

        byte[] fileContent = Files.readAllBytes(Paths.get("src/main/resources/Client.json"));
        ArrayList<byte[]> bytesList = new ArrayList<byte[]>();
        int limit = 100;
        byte[] byter = new byte[limit];
       System.out.println("fileContent.length: " + fileContent.length + " bytes or " + (fileContent.length / 1000.0f) + " kilobytes\n");

        for (int i = 0; i < fileContent.length; i++) {
            System.out.print(fileContent[i] + " ");
            if(i % limit != 0) {
                byter[i % limit] = fileContent[i];
            }



            if ((i % limit == 0 && i != 0)) {
                System.out.println();

                byter[0] = fileContent[i - limit]; // <------- the fix

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
                remaining[0] = fileContent[i - remaining.length + 1];  // <------- the fix
                bytesList.add(remaining);
            }
        } // TODO: check if all byte array contain the right data, ... it doesn't
            // nice i fixed it ;)

        System.out.println("\nbytesList.size(): " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            System.out.println("bytesList.get(" + i + ").length: " +  bytesList.get(i).length + " Data: " + new String(bytesList.get(i)));
        }

    }
}
