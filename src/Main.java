
import jdk.nashorn.internal.codegen.CompilerConstants;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("What would you like to do?");
        System.out.println("1. Backup text messages.");
        System.out.println("2. Backup phone calls.");
        System.out.println("3. Backup phone calls and text messages.");
        System.out.println("E. Exit this program.");
        System.out.println("Enter your response below.");
        Scanner input = new Scanner(System.in);
        String response = input.next();
        while (response.charAt(0) != '1' && response.charAt(0) != '2' && response.charAt(0) != '3' && response.charAt(0) != 'E' && response.charAt(0) != 'e') {
            System.out.println("Invalid! Try again.");
            System.out.println("Enter your response below.");
            response = input.next();
        }

        if (response.charAt(0) == '1') {
            TextMessagesBackup backup = new TextMessagesBackup();
            System.out.println("Enter the path to the text message XML file: ");
            String textMessageXMLFile = input.next();
            textMessageXMLFile = textMessageXMLFile.replace("\"", "");
            try {
                String[] args1 = {textMessageXMLFile};
                backup.main(args1);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } else if (response.charAt(0) == '2') {
            CallsBackup backup = new CallsBackup();
            System.out.println("Enter the path to the phone calls XML file: ");
            String phoneCallsXMLFile = input.next();
            phoneCallsXMLFile = phoneCallsXMLFile.replace("\"", "");
            try {
                String[] args1 = {phoneCallsXMLFile};
                backup.main(args1);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } else if (response.charAt(0) == '3') {
            TextMessagesBackup backup = new TextMessagesBackup();
            System.out.println("Enter the path to the text message XML file: ");
            String textMessageXMLFile = input.next();
            textMessageXMLFile = textMessageXMLFile.replace("\"", "");
            try {
                String[] args1 = {textMessageXMLFile};
                backup.main(args1);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
            System.out.println("-----------------------------------------------");
            System.out.println("Finished backing up text messages.");
            System.out.println("Now we are going to backup the phone calls...\n");
            CallsBackup backup2 = new CallsBackup();

            // Set the list of phone numbers to what was already considered with the text messages (do this solely for efficiency).
            backup2.phoneNumbers = backup.phoneNumbers;
            System.out.println("Enter the path to the phone calls XML file: ");
            String phoneCallsXMLFile = input.next();
            phoneCallsXMLFile = phoneCallsXMLFile.replace("\"", "");
            try {
                String[] args1 = {phoneCallsXMLFile};
                backup.main(args1);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } else if (response.charAt(0) == 'E' || response.charAt(0) == 'e') {
            System.out.println("Bye!");
            System.exit(0);
        }
    }

}
