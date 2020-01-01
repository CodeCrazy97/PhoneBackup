
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class TESTIterateOverFile {

    public static void main(String[] args) throws FileNotFoundException {
        Scanner input = new Scanner(System.in);


        System.out.println("C:\\Users\\Ethan\\Documents\\Projects\\SMS\\XML Files\\sms-20200101082028.xml");
        

        TextMessagesBackup tmb = new TextMessagesBackup();

        System.out.println("Enter the path to the text messages XML file: ");
        String textMessageXMLFile = "C:\\Users\\Ethan\\Documents\\Projects\\SMS\\XML Files\\sms-20200101082028.xml"; //input.nextLine();
        textMessageXMLFile = new MySQLMethods().fixFilePath(textMessageXMLFile);
        try {
            String[] args1 = {textMessageXMLFile};
            tmb.main(args1);
        } catch (Exception ex) {
            System.out.println("Exception trying to backup text messages: " + ex);
        }
        System.exit(0);
                 
                 
        System.out.println("Enter the path to the file: ");
        //String path = input.nextLine();
        //path = new MySQLMethods().fixFilePath(path);

        System.out.println("C:\\Users\\Ethan\\Documents\\Projects\\SMS\\XML Files\\sms-20191228161211.xml");
        String path = "C:\\Users\\Ethan\\Documents\\Projects\\SMS\\XML Files\\sms-20191228161211.xml"; //input.nextLine();
        path = new MySQLMethods().fixFilePath(path);
        System.out.println("Path: " + path);

        File file = new File(path);  // Full file path to the text messages XML file.
        if (!file.exists()) {
            System.out.println("File does not exist.");
            throw new FileNotFoundException("Path to text messages XML file does not exist.");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.
            String currLine;  //The line in the file currently being viewed by the program. The xml file is
            // broken up by lines; so, one line represents a single SMS text message. MMS messages (text messages
            // that contain pictures or contain a lot of text) span multiple lines and are handled a little differently.

            while ((currLine = br.readLine()) != null) {
                if (currLine.contains("<mms ")) {
                    System.out.println("\n\n\n");
                }
                if (currLine.length() >= 1501) {
                    System.out.println(currLine.substring(0, 1500));
                } else {
                    System.out.println(currLine);
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
