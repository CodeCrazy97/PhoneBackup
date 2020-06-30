
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

class CallsBackup {

    static Connection conn = null;
    static Statement st = null;
    static ResultSet rs = null;
    static LinkedList<PhoneCall> databasePhoneCalls;

    public static LinkedList<String> phoneNumbers = new LinkedList<>();

    public static void main(String[] args) throws IOException, SQLException {
        // Get the path, replacing common invalid characters such as quotes.
        String path = new MySQLMethods().fixFilePath(args[0]);

        //Read through the new text messages.
        File file = new File(path);
        if (!file.exists()) { //we might not want to add text to a file that already existed
            System.out.println("File does not exist.");
            throw new FileNotFoundException("Path to phone calls XML file does not exist.");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.

            String currLine;  //The line in the file currently being viewed by the program. The xml file is
            //broken up by lines; so, one line represents a single text message.
            System.out.println();
            System.out.println("Getting ready to backup your phone calls...");

            databasePhoneCalls = new MySQLMethods().getPhoneCalls();

            LinkedList<PhoneCall> phoneCallsToInsert = new LinkedList<>();

            // The phone_number is the unique key that will identify each contact.
            // The contact's name (person_name in the database) is the value in the below maps.
            ContactsManager contactsManager = new ContactsManager();

            int beginTimeMillis = (int) System.currentTimeMillis();
            while ((currLine = br.readLine()) != null) {
                if (!currLine.contains("(Unknown)") && currLine.contains("duration")) {   // Line contains a call, and that call is from a contact.

                    try {
                        String contactPhoneNumberStr = (currLine.substring(currLine.indexOf("call number=\"") + 13, currLine.indexOf("\" duration"))).replaceAll("\\D+","");  // Get the phone number from the line. Keep only the numbers in the phone number.
                        long contactPhoneNumber = Long.parseLong(contactPhoneNumberStr);
                        String callTimestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.indexOf("\" contact_name="));
                        callTimestamp = new MySQLMethods().createSQLTimestamp(callTimestamp);

                        // Check if the phone call record already exists in the database.
                        // If it exists, then go on to the next call and don't place it
                        // in the list of calls to insert.
                        // If it does not already exist, then place it in the list of 
                        // calls that will need to be inserted into the database.
                        try {
                            if (phoneCallExists(callTimestamp, contactPhoneNumber)) {
                                // This phone call already exists in the DB. Go to next phone call in the XML file.
                                continue;
                            }
                        } catch (Exception ex) {
                            System.out.println("Exception trying to check if a text message exists: " + ex);
                        }

                        // At this point, we know that the call does not already exist in the database. 
                        // Fetch the other information (duration, contact name, calltype) and create 
                        // a new instance for insertion.
                        int duration = Integer.parseInt(currLine.substring(currLine.indexOf("duration=\"") + 10, currLine.indexOf("\" date=")));
                        String contactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.indexOf("\" />"));
                        contactName = fixForInsertion(contactName);
                        int callType = Integer.parseInt(currLine.substring(currLine.indexOf("type=\"") + 6, currLine.indexOf("\" presentation")));  // 1 = incoming, 2 = outgoing, 3 = incoming and the contact left a voicemail

                        Contact c = new Contact(contactPhoneNumber, contactName);
                        contactsManager.handleContact(c);

                        phoneCallsToInsert.add(new PhoneCall(callTimestamp, contactPhoneNumber, duration, callType));

                    } catch (Exception ex) {
                        System.out.println("Exception : " + ex);
                    }
                }
            }

            // update the database with new contacts
            contactsManager.updateDatabase();

            // Insert the contacts, if any need to be inserted.
            // Insert the phone calls into the database.
            try {
                if (phoneCallsToInsert.size() > 0) {

                    // Build the SQL insert string.
                    String sql = "INSERT INTO phone_calls (contact_phone_number, duration, call_timestamp, call_type) VALUES ";
                    for (int i = 0; i < phoneCallsToInsert.size(); i++) {
                        sql += "(" + phoneCallsToInsert.get(i).getContactPhoneNumber() + ", " + phoneCallsToInsert.get(i).getDuration() + ", '" + phoneCallsToInsert.get(i).getTimestamp() + "', " + phoneCallsToInsert.get(i).getCallType() + "), ";
                    }

                    // Chop of the last comma, replace it with a semicolon.
                    sql = sql.substring(0, sql.lastIndexOf(",")) + ";";

                    new MySQLMethods().executeSQL(sql);
                    System.out.println("Successfully backed up " + phoneCallsToInsert.size() +  " phone calls!");
                }
            } catch (Exception e) {
                System.out.println("Exception trying to create multiple inserts for phone calls: " + e);
            }

            int endTimeMillis = (int) System.currentTimeMillis();
            System.out.println("Finished backing up your phone calls. That took " + secondsFormatted((endTimeMillis - beginTimeMillis) / 1000) + ".");

            // Try to update the timestamp for the phone calls backup.
            new MySQLMethods().updateBackup("phone calls");

        } catch (IOException ex) {
            System.out.println("IOException : " + ex);
        }
    }

    public static boolean phoneCallExists(String callTimestamp, long contactPhoneNumber) {
        for (int i = 0; i < databasePhoneCalls.size(); i++) {
            if (databasePhoneCalls.get(i).getContactPhoneNumber() == contactPhoneNumber && databasePhoneCalls.get(i).getTimestamp().equals(callTimestamp)) {
                // Remove the phone call from the linked list (this will shorten the linked list so that next time we search through it, there's not as much to look at).
                databasePhoneCalls.remove(i);
                return true;
            }
        }
        return false;
    }

    public static String fixForInsertion(String sql) {
        sql = sql.replace("\'", "\\'");
        sql = sql.replace("&amp;", "&");
        return sql;
    }

    // secondsFormatted: converts seconds to hours, minutes, and seconds.
    // For example: input 95 seconds would return "1 minute, 35 seconds"
    public static String secondsFormatted(int seconds) {
        if (seconds == 0) {  // Avoid further processing if there are zero seconds.
            return "0 seconds";
        }
        try {
            int minutes = seconds / 60;    // Extract minutes out of the seconds.
            seconds %= 60;               // Make seconds less than 60.
            int hours = minutes / 60;      // Extract hours out of seconds.
            minutes %= 60;               // Make minutes less than 60. 

            String hoursString = "";
            String minutesString = "";
            String secondsString = "";

            if (hours > 0) {
                if (hours == 1) {
                    hoursString = hours + " hour, ";
                } else {
                    hoursString = hours + " hours, ";
                }
            }
            if (minutes > 0) {
                if (minutes == 1) {
                    minutesString = minutes + " minute, ";
                } else {
                    minutesString = minutes + " minutes, ";
                }
            }
            if (seconds > 0) {
                if (seconds == 1) {
                    secondsString = seconds + " second";
                } else {
                    secondsString = seconds + " seconds";
                }
            }
            String duration = hoursString + minutesString + secondsString;  // Put the entire result in one string so we can check if a comma needs to be removed at the end.
            if (duration.charAt(duration.length() - 2) == ',') {
                return duration.substring(0, duration.length() - 2);
            } else {
                return duration;
            }
        } catch (Exception ex) {
            System.out.println("Exception trying to format total time spent on the phone: " + ex);
        }
        return null;
    }

}
