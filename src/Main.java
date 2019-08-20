
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    
    public static void main(String[] args) {

        // Display when the phone calls were last backed up.
        String lastBackupDatePhoneCalls = new MySQLMethods().getLastBackupDate("phone calls");
        if (lastBackupDatePhoneCalls != null) {  // If there was a date of the last backup to begin with, then display it.
            System.out.println("Date of last phone calls backup: " + lastBackupDatePhoneCalls);
        }

        // Display when the text messages were last backed up.
        String lastBackupDateTextMessages = new MySQLMethods().getLastBackupDate("text messages");
        if (lastBackupDateTextMessages != null) {  // If there was a date of the last backup to begin with, then display it.
            System.out.println("Date of last text messages backup: " + lastBackupDateTextMessages);
        }
        
        String response = "";
        do {
            System.out.println("\nWhat would you like to do?");
            System.out.println("1. Backup text messages.");
            System.out.println("2. Backup phone calls.");
            System.out.println("3. Backup phone calls and text messages.");
            System.out.println("4. View your phone call records.");
            System.out.println("5. View you text messages.");
            System.out.println("6. Clear the screen.");
            System.out.println("E. Exit this program.");
            System.out.println();
            System.out.println("Enter your response below.");
            Scanner input = new Scanner(System.in);
            response = input.nextLine();
            
            if (response.charAt(0) == '1') {
                TextMessagesBackup backup = new TextMessagesBackup();
                System.out.println("Enter the path to the text messages XML file: ");
                String textMessageXMLFile = input.nextLine();
                textMessageXMLFile = new MySQLMethods().fixFilePath(textMessageXMLFile);
                try {
                    String[] args1 = {textMessageXMLFile};
                    backup.main(args1);
                } catch (Exception ex) {
                    System.out.println("Exception trying to backup text messages: " + ex.getMessage());
                }
            } else if (response.charAt(0) == '2') {
                CallsBackup backup = new CallsBackup();
                System.out.println("Enter the path to the phone calls XML file: ");
                String phoneCallsXMLFile = input.nextLine();
                phoneCallsXMLFile = new MySQLMethods().fixFilePath(phoneCallsXMLFile);
                try {
                    String[] args1 = {phoneCallsXMLFile};
                    backup.main(args1);
                } catch (Exception ex) {
                    System.out.println("Exception trying to backup phone calls: " + ex.getMessage());
                }
            } else if (response.charAt(0) == '3') {
                TextMessagesBackup backup = new TextMessagesBackup();
                System.out.println("Enter the path to the text messages XML file: ");
                String textMessageXMLFile = input.nextLine();
                textMessageXMLFile = new MySQLMethods().fixFilePath(textMessageXMLFile);
                try {
                    String[] args1 = {textMessageXMLFile};
                    backup.main(args1);
                } catch (Exception ex) {
                    System.out.println("Exception trying to backup text messages: " + ex.getMessage());
                }
                System.out.println("-----------------------------------------------");
                System.out.println("Finished backing up text messages.");
                System.out.println("Now we are going to backup the phone calls...\n");
                CallsBackup backup2 = new CallsBackup();

                // Set the list of phone numbers to what was already considered with the text messages (do this solely for efficiency).
                backup2.phoneNumbers = backup.phoneNumbers;
                System.out.println("Enter the path to the phone calls XML file: ");
                String phoneCallsXMLFile = input.nextLine();
                phoneCallsXMLFile = new MySQLMethods().fixFilePath(phoneCallsXMLFile);
                try {
                    String[] args1 = {phoneCallsXMLFile};
                    backup2.main(args1);
                } catch (Exception ex) {
                    System.out.println("Exception trying to backup phone calls: " + ex);
                }
            } else if (response.charAt(0) == 'E' || response.charAt(0) == 'e') {
                System.out.println();
                System.out.println("Bye!");
                System.out.println();
                System.exit(0);
            } else if (response.charAt(0) == '4') {
                System.out.println();
                ViewPhoneCalls viewPhoneCalls = new ViewPhoneCalls();
                try {
                    viewPhoneCalls.main(args);
                } catch (Exception ex) {
                    System.out.println("Exception trying to load phone records: " + ex);
                }
            } else if (response.charAt(0) == '5') {
                try {
                    String basePath = new File("").getAbsolutePath();
                    basePath = basePath.substring(0, basePath.lastIndexOf("\\"));  // Go back a directory.
                    basePath = basePath.replace("\\", "/");                         // Replace backslashes with forward slashes.
                    Runtime.getRuntime().exec(basePath + "/ViewData/SMSGui/bin/Debug/SMSGui.exe");
                } catch (IOException ex) {
                    System.out.println("Problem trying to load executable file for viewing text messages: " + ex);
                }
            } else if (response.charAt(0) == '6') {
                cls();
            } else {  // Catch invalid input.
                System.out.println();
                System.out.println("Invalid! Try again.");
            }
        } while (response.charAt(0) != 'E' && response.charAt(0) != 'e');
    }

    // cls: Performs the cls command on the command line.
    public static void cls() {
        try {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch (InterruptedException ex) {
                System.out.println("Exception trying to clear the screen: " + ex);
            }
        } catch (IOException ex) {
            System.out.println("Exception trying to clear the screen: " + ex);
        }
    }
}
