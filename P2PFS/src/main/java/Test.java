import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Test {

    // FOR TESTING PURPOSES ONLY

    public static void main(String[] arg) throws IOException, UnsupportedAudioFileException, LineUnavailableException {


        System.out.println("Ello Gov'nor\n");

        byte[] fileContent = Files.readAllBytes(Paths.get("src/main/files/shock.txt"));
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

        fileContent[0] = (byte) 85;
        fileContent[1] = (byte) 109;
        fileContent[2] = (byte) 97;
        fileContent[3] = (byte) 105;
        fileContent[4] = (byte) 100;
        fileContent[5] = (byte) 77;
        System.out.println("fileContent: " + new String(fileContent));

        String string = "123 102 77 122 64 200 88 46 99 98 97 96 95 94 93 92 91 90 89 88 87 86 85 84 83 82 81 80 70";
        String[] stringArray ;
        stringArray = string.split(" ");
        String line;
        System.out.println(stringArray.length);
        for (int i = 0; i < stringArray.length; i++) {
            fileContent[i] = (byte) Integer.parseInt(stringArray[i]);


        }
        line = new String(fileContent);
        for (int i = 0; i < stringArray.length; i++) {
            System.out.print(line.charAt(i));
        }

    }
}
