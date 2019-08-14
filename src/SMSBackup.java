package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

class SMSBackup {

    //phoneNumbers (a linked list that stores all the phone numbers) is a data structure that saves the user from
    //having to confirm more than once whether or not to allow the program to create a new contact. Without the
    //phoneNumbers linked list, the program might ask the user multiple times if he/she would like to add a contact
    //to the database (this would happen if more than one message was sent/received from the same contact).
    public static LinkedList<String> phoneNumbers = new LinkedList<>();
    //create the connection to the database
    public static Connection conn = new MySQLConnection().getConnection();

    public static void main(String[] args) throws IOException, SQLException {

        // Get a connection to the file that contains the text messages.
        Scanner keyboard = new Scanner(System.in);
        File file = new File("C:\\Users\\A521646\\OneDrive - Valvoline\\Documents\\Projects\\SMS\\sms-20190708125353b.xml");
        if (!file.exists()) { //we might not want to add text to a file that already existed
            System.out.println("File does not exist.");
            System.exit(0);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.
            try {
                try {
                    String currLine;  //The line in the file currently being viewed by the program. The xml file is
                    // broken up by lines; so, one line represents a single SMS text message. MMS messages (text messages
                    // that contain pictures or contain a lot of text) span multiple lines and are handled a little differently.

                    String mmsContactName = "";
                    int mmsIncoming = 1;  // 1 = true (the message is from someone to me); 0 = false (the message was sent from my phone to someone else)
                    String mmsDate = "";  // The date the mms message was sent.
                    String mmsText = "";  // Text contained in the mms message.
                    boolean mmsContainsText = false;  // Whether or not the mms contains text. If it does not contain text.

                    System.out.println("About to start backing up your text messages.");
                    System.out.println("This program will NOT remove any text messages already in the database.");
                    System.out.println("This may take a several minutes.\n");

                    // recipientCount ~ the number of people a message was sent to
                    int recipientCount = 0;
                    LinkedList<String> mmsGroupMessagePhoneNumbers = new LinkedList<>();  // Will hold all the phone numbers that a group message was sent to.
                    String alsoSentTo = "Also sent to: ";  // A string that will tell who else a group message was sent to.
                    String groupMessagePhoneNumber = "";  // Used to store the number for a group message. This will be needed if the message comes from a number that is not associated with a contact name.
                    while ((currLine = br.readLine()) != null) {
                        if (currLine != null && currLine.contains(" body=")) {  //If the line starts with " body=", then it is a line that contains a text message.
//messageQueue is the actual text of the currently viewed message. The text is between  body= and toa=" in the line.
                            String messageQueue = currLine.substring(currLine.indexOf(" body=") + 7, currLine.indexOf("toa=\"") - 2);

                            //Swap out certain characters. Apostrophes and newline characters need manipulation before being sent to the MySQL database.
                            messageQueue = fixSMSString(messageQueue);

                            //Fetch the phone number that the message was sent/received to/from.
                            String phoneNumber = currLine.substring(29, currLine.indexOf("\" date"));
                            //Replace irrelevant characters in the phone number. (Remember: phoneNumber needs to fit in the database as a bigint, not a string.)
                            phoneNumber = phoneNumber.replace("\\", "");
                            phoneNumber = phoneNumber.replace("-", "");
                            phoneNumber = phoneNumber.replace(" ", "");
                            phoneNumber = phoneNumber.replace(")", "");
                            phoneNumber = phoneNumber.replace("(", "");

                            //Get the contact that the message was sent/received to/from
                            String contactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.lastIndexOf("\""));
                            contactName = fixSMSString(contactName);
//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the contact already exists in the database.//////////
/////////////If not, then create new contact only if user wants to.///////////////
//////////////////////////////////////////////////////////////////////////////////
                            handleContact(contactName, phoneNumber);
/////////////////////////////////////////////////////////////////////////////////////////////
////////Finished inserting new contact (if applicable).//////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////

                            //Get the timestamp of when the message was sent
                            String timestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.lastIndexOf("\" contact"));

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the sms message already exists in the database.//////
//////////////////////////////////////////////////////////////////////////////////
                            try {
                                if (messageExists(new MySQLConnection().createSQLTimestamp(timestamp), contactName)) {
                                    continue;
                                }
                            } catch (Exception ex) {
                                System.out.println("Exception trying to check if a text message exists: " + ex);
                            }
/////////////////////////////////////////////////////////////////////////////////////////////
////////Finished checking if the sms message exists in the database./////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////

                            boolean incomingMessage = false;
                            //Determine if the message is incoming or outgoing
                            if (currLine.contains("type=\"2\" ")) {  //Outgoing

                            } else {  //Incoming
                                incomingMessage = true;
                            }

                            try {
                                Class.forName("com.mysql.jdbc.Driver");

                                String sql = "";
                                try {
                                    sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('" + messageQueue + "', " + incomingMessage + ", (SELECT id FROM contacts WHERE name = '" + contactName + "'), '" + new MySQLConnection().createSQLTimestamp(timestamp) + "'); ";
                                } catch (Exception ex) {
                                    System.out.println("Exception: " + ex);
                                    continue;   // Don't want to continue trying to insert into the database for this message.
                                }

                                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                                preparedStatement.executeUpdate();
                                System.out.println("sql insert: " + sql);
                            } catch (SQLException sqle) {
                                System.out.println("SQL Exception ...: " + sqle);
                                System.out.println(currLine + "\n");
                            } catch (ClassNotFoundException cnfe) {
                                System.out.println("ClassNotFoundException: " + cnfe);
                            }

                        } else { // mms (occurs on multiple lines)
                            if (currLine.contains("type=\"151\"")) {  // The recipient of the mms message. If there are more than one recipients, then this is a group message.
                                String currentPhoneNumber = currLine.substring(currLine.indexOf("address=\"") + 9, currLine.indexOf("\" type="));

                                System.out.println("Phone num: " + currentPhoneNumber);
                                mmsGroupMessagePhoneNumbers.add(currentPhoneNumber);
                                System.out.println("List size: " + mmsGroupMessagePhoneNumbers.size());

                                recipientCount++;
                            }
                            if (currLine.contains("<mms text_only=")) {
                                if (currLine.contains("msg_box=\"1\"")) {  //  message is incoming
                                    mmsIncoming = 1;
                                } else if (currLine.contains("msg_box=\"2\"")) {  // message is outgoing
                                    mmsIncoming = 0;
                                }

                                mmsDate = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.indexOf("\" contact_name="));
                                mmsContactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.indexOf("\">"));
                                String mmsPhoneNumber = currLine.substring(currLine.indexOf("address=\"") + 9, currLine.indexOf("\" d_rpt="));
                                groupMessagePhoneNumber = mmsPhoneNumber;

                                // Fix apostrophe for SQL querying/inserting.
                                mmsContactName = fixSMSString(mmsContactName);
                                // Check to see that the sender is already in the db.
                                handleContact(mmsContactName, mmsPhoneNumber);
                            }
                            if (currLine.contains("ct=\"text/plain\"")) {  // Is a line with text. Indicate that this text message has a picture.
                                mmsContainsText = true;
                                mmsText = currLine.substring(currLine.indexOf("text=") + 6, currLine.indexOf("/>") - 2);
                            }

                            // Reached end of mms message.
                            if (currLine.length() >= 6 && currLine.contains("</mms>")) {  // end of mms - try inserting into database if this mms message contained text

                                /*
                                // Get the names of all the people the group message was sent to (if it was a group message).
                                // If the phone number does not have a name in the database, then just use the number itself.
                                if (recipientCount >= 2) {
                                    while (mmsGroupMessagePhoneNumbers.size() > 0) {
                                        String currentPhoneNumber = getContactName(mmsGroupMessagePhoneNumbers.get(0));
                                        // Add the phone number or contact name to the list.
                                        if (mmsGroupMessagePhoneNumbers.size() > 1) {  // There are others to add to the string of numbers/contacts, so include a comma.
                                            alsoSentTo += currentPhoneNumber + ", ";
                                        } else {
                                            alsoSentTo += currentPhoneNumber;
                                        }
                                        // Remove that phone number from the list.
                                        mmsGroupMessagePhoneNumbers.remove(0);
                                    }
                                    System.out.println("List of people/numbers: " + alsoSentTo);
                                    alsoSentTo = "Also sent to: ";

                                }
                                mmsGroupMessagePhoneNumbers.clear();
                                recipientCount = 0;
                                */

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the mms message already exists in the database.//////
//////////////////////////////////////////////////////////////////////////////////
                                try {
                                    if (messageExists(new MySQLConnection().createSQLTimestamp(mmsDate), mmsContactName)) {
                                        // Reset the recipient count for next message.
                                        recipientCount = 0;
                                        continue;
                                    }
                                } catch (Exception ex) {
                                    System.out.println("Exception trying to check if an mms text message exists: " + ex);
                                }
                                String sql = "";
                                try {
                                    Class.forName("com.mysql.jdbc.Driver");

                                    //Swap out certain characters. Apostrophes and newline characters need manipulation before being sent to the MySQL database.
                                    mmsText = fixSMSString(mmsText);

                                    try {
                                        // nestedQuery will be used to fetch the name of the contact, if the contact has a name (else condition).
                                        // If the contact does not have a name, then the phone number will be used as the contact's "name" (if condition).
                                        String nestedQuery = "";
                                        if (mmsContactName.equals("(Unknown)")) {
                                            nestedQuery = "'" + groupMessagePhoneNumber + "'";  // Put single quotes around the phone number so it can go directly into the MySQL database.
                                        } else {
                                            nestedQuery = "(select id from contacts where name = '" + mmsContactName + "'); ";
                                        }

                                        if (recipientCount >= 2) {
                                            if (mmsContainsText) {
                                                sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('[GROUP MSG] " + mmsText + "', " + mmsIncoming + ", " + nestedQuery + ", '" + new MySQLConnection().createSQLTimestamp(mmsDate) + "'); ";
                                            } else {
                                                sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('[GROUP MSG PIC]', " + mmsIncoming + ", " + nestedQuery + ", '" + new MySQLConnection().createSQLTimestamp(mmsDate) + "'); ";
                                            }
                                        } else if (mmsContainsText) {
                                            sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('[PICTURE] " + mmsText + "', " + mmsIncoming + ", " + nestedQuery + ", '" + new MySQLConnection().createSQLTimestamp(mmsDate) + "'); ";
                                        } else {
                                            sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('[PICTURE]', '" + mmsIncoming + "', " + nestedQuery + ", '" + new MySQLConnection().createSQLTimestamp(mmsDate) + "'); ";
                                        }
                                    } catch (Exception ex) {
                                        System.out.println("Exception: " + ex);
                                    }

                                    mmsContainsText = false;  // Reset so we don't accidentally reinsert a message.
                                    PreparedStatement preparedStatement = conn.prepareStatement(sql);
                                    preparedStatement.executeUpdate();
                                    System.out.println("Successfully inserted a new MMS message: " + sql);
                                } catch (SQLException sqle) {
                                    System.out.println("SQL Exception ...: " + sqle);
                                    System.out.println("Failed to insert " + sql + " into the database.");
                                    System.out.println(currLine + "\n");
                                } catch (ClassNotFoundException cnfe) {
                                    System.out.println("ClassNotFoundException: " + cnfe);
                                }

                                // Reset the recipient count for next message.
                                recipientCount = 0;

                            }
                        }
                    }
                } finally {
                    if (conn != null) {
                        conn.close();
                    }
                }
            } catch (SQLException sqle) {
                System.out.println("SQL Exception :) => " + sqle);
            }
        }
    }

    // Some strange things happen to text messages when they are turned into XML!
    // Below, I fix odd characters and turn them into what they should be.
    public static String fixSMSString(String message) {
        message = message.replace("&#55357;&#56832;", "â˜º");
        message = message.replace("ï¿½", "\'");  //Replace all ï¿½ with apostraphes.
        message = message.replace("&#10;", "\\n");  //Replace all &#10; with newline characters.
        message = message.replace("\'", "\\'");
        message = message.replace(" &#55357;&#56846", "\uD83D\uDE0E");
        message = message.replace("&#55357;&#56397;&#55356;&#57339;", "\uD83D\uDC4D");
        message = message.replace("&#55357;&#56837;", "\uD83D\uDE04");
        message = message.replace("&#55357;&#56397;", "\uD83D\uDC4D");
        message = message.replace("&amp;", "&");  //ampersand symbol
        message = message.replace("&apos;", "\\'");  // single quote
        return message;
    }

    public static void handleContact(String contactName, String phoneNumber) {
        Scanner keyboard = new Scanner(System.in);
        boolean addContact = false;
        if (!phoneNumbers.contains(phoneNumber)) {  //If this phone number has not been viewed before...
            try {
                // create our mysql database connection
                String myDriver = "org.gjt.mm.mysql.Driver";
                Class.forName(myDriver);

                String query = "SELECT COUNT(*) FROM contacts WHERE name = '" + contactName + "';";

                // create the java statement
                Statement st = conn.createStatement();

                // execute the query, and get a java resultset
                ResultSet rs = st.executeQuery(query);

                // iterate through the java resultset
                while (rs.next()) {
                    if (rs.getString(1).equals("0")) {  //The contact does NOT exist in the database.
                        System.out.println("*** ALERT ***");
                        if (contactName.equals("(Unknown)")) {
                            System.out.println("Do you want to add " + phoneNumber + " to the database (y/n)? (This contact does not have a name.)");
                        } else {
                            System.out.println("Do you want to add " + contactName + " (phone number: " + phoneNumber + ") to the database (y/n)?");
                        }
                        String input2 = keyboard.nextLine();
                        if (input2.charAt(0) == 'y' || input2.charAt(0) == 'Y') {
                            addContact = true;
                        }
                        phoneNumbers.add(phoneNumber);  //This "new" phone number has been looked at - don't ask the user for a confirmation again.
                    }
                }
                rs.close();
                if (addContact) {  //User wants to add contact to db.
                    try {
                        Class.forName("com.mysql.jdbc.Driver");

                        String sql = "";
                        if (contactName.equals("(Unknown)")) {
                            // If this contact does not have a name, just use the phone number as its "name".
                            sql = "INSERT INTO contacts (name, phone_number) VALUES ('" + phoneNumber + "', '" + phoneNumber + "'); ";
                        } else {
                            sql = "INSERT INTO contacts (name, phone_number) VALUES ('" + contactName + "', '" + phoneNumber + "'); ";
                        }

                        PreparedStatement preparedStatement = conn.prepareStatement(sql);
                        preparedStatement.executeUpdate();

                    } catch (SQLException sqle) {
                        System.out.println("SQL Exception: " + sqle);
                    } catch (ClassNotFoundException cnfe) {
                        System.out.println("ClassNotFoundException: " + cnfe);
                    }
                }
                st.close();
            } catch (Exception e) {
                System.err.println("Got an exception! ");
                System.err.println(e.getMessage());
            }
        }
    }

    public static boolean messageExists(String timestamp, String contactName) {
        //create the connection to the database
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/phone_backup", "root", "");
        } catch (SQLException ex) {
            Logger.getLogger(SMSBackup.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean exists = false;
//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the message already exists in the database.//////////
//////////////////////////////////////////////////////////////////////////////////
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT * FROM messages WHERE sent_timestamp = '" + timestamp + "' AND contact = (SELECT id FROM contacts WHERE name = '" + contactName + "');";  //Prevents from putting duplicate messages in the database ("duplicates" are messages that have been sent to the same address at the same time)
            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                exists = true;
                break;
            }
            rs.close();
            st.close();
            conn.close();
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                System.out.println("SQL Exception while trying to close connection: " + ex);
            }
        }
        return exists;
    }

    public static String getContactName(String phoneNumber) {
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT name FROM contacts WHERE phone_number = " + phoneNumber;
            if (phoneNumber.length() >= 10) {
                // Include an OR clause to check for the phone number without the area code (sometimes, I include contacts without their area code)
                query += " OR phone_number = " + phoneNumber.substring(phoneNumber.length() - 7) + ";";
            } else {
                query += ";";
            }

            System.out.println("query = " + query);

            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getString(1);
            }
            rs.close();
            st.close();
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
        }

        // No contact existed with that phone number. So, just return the number itself.
        return phoneNumber;
    }

}
