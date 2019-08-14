package com.company;

import jdk.nashorn.internal.codegen.CompilerConstants;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("What would you like to do?");
        System.out.println("1. Backup text messages.");
        System.out.println("2. Backup phone calls.");
        System.out.println("3. Both.");
        System.out.println("E. Exit this program.");
        System.out.println("Enter your response below.");
        Scanner input = new Scanner(System.in);
        String response = input.next();
        while (response.charAt(0) != '1' && response.charAt(0) != '2' && response.charAt(0) != '3' && response.charAt(0) != 'E' && response.charAt(0) != 'e') {
            System.out.println("Invalid! Try again.");
            System.out.println("Enter your response below.");
        }


        if (response.charAt(0) == '1') {
            SMSBackup backup = new SMSBackup();
            try {
                backup.main(args);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } else if (response.charAt(0) == '2') {
            CallsBackup backup = new CallsBackup();
            try {
                backup.main(args);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } else if (response.charAt(0) == '3') {
            SMSBackup backup = new SMSBackup();
            try {
                backup.main(args);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
            System.out.println("---------------------------------------------");
            System.out.println("Finished backing up text messages.");
            System.out.println("Now we are going to backup the phone calls...\n");
            CallsBackup backup2 = new CallsBackup();
            try {
                backup2.main(args);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } else if (response.charAt(0) == 'E' || response.charAt(0) == 'e') {
            System.out.println("Bye!");
            System.exit(0);
        }
    }

}
