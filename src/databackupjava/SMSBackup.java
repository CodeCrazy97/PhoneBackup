package databackupjava;

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

    public static void main(String[] args) throws IOException, SQLException {

        // Get a connection to the file that contains the text messages.
        Scanner keyboard = new Scanner(System.in);
        File file = new File("C:\\Users\\Ethan_2\\Documents\\Projects\\SMS\\sms-20190525094728.xml");
        if (!file.exists()) { //we might not want to add text to a file that already existed
            System.out.println("File does not exist.");
            System.exit(0);
        }

        //phoneNumbers (a linked list that stores all the phone numbers) is a data structure that saves the user from
        //having to confirm more than once whether or not to allow the program to create a new contact. Without the
        //phoneNumbers linked list, the program might ask the user multiple times if he/she would like to add a contact
        //to the database (this would happen if more than one message was sent/received from the same contact).
        LinkedList<String> phoneNumbers = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.
            try {
                //create the connection to the database
                Connection conn = new MySQLConnection().getConnection();

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

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the contact already exists in the database.//////////
/////////////If not, then create new contact only if user wants to.///////////////
//////////////////////////////////////////////////////////////////////////////////
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
                                            System.out.println("Do you want to add " + contactName + " (phone number: " + phoneNumber + ") to the database (y/n)?");
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

                                            String sql = "INSERT INTO contacts (name, phone_number) VALUES ('" + contactName + "', '" + phoneNumber + "'); ";
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
/////////////////////////////////////////////////////////////////////////////////////////////
////////Finished inserting new contact (if applicable).//////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////

                            //Get the timestamp of when the message was sent
                            String timestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.lastIndexOf("\" contact"));

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the message already exists in the database.//////////
//////////////////////////////////////////////////////////////////////////////////
                            try {
                                if (messageExists(createSQLTimestamp(timestamp), contactName)) {
                                    continue;
                                }
                            } catch (Exception ex) {
                                System.out.println("Exception trying to check if a text message exists: " + ex);
                            }
/////////////////////////////////////////////////////////////////////////////////////////////
////////Finished checking if it exists in the database.//////////////////////////////////////
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
                                    sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('" + messageQueue + "', " + incomingMessage + ", (SELECT id FROM contacts WHERE name = '" + contactName + "'), '" + createSQLTimestamp(timestamp) + "'); ";
                                } catch (Exception ex) {
                                    System.out.println("Exception: " + ex);
                                    continue;   // Don't want to continue trying to insert into the database for this message.
                                }

                                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                                preparedStatement.executeUpdate();
                                System.out.println("sql insert: " + sql);
                            } catch (SQLException sqle) {
                                System.out.println("SQL Exception ...: " + sqle);
                            } catch (ClassNotFoundException cnfe) {
                                System.out.println("ClassNotFoundException: " + cnfe);
                            }

                        } else { // mms (occurs on multiple lines)
                            if (currLine.contains("<mms text_only=")) {
                                if (currLine.contains("msg_box=\"1\"")) {  //  message is incoming
                                    mmsIncoming = 1;
                                } else if (currLine.contains("msg_box=\"2\"")) {  // message is outgoing
                                    mmsIncoming = 0;
                                }

                                mmsDate = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.indexOf("\" contact_name="));
                                mmsContactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.indexOf("\">"));
                            }
                            if (currLine.contains("ct=\"text/plain\"")) {  // Is a line with text. Indicate that this text message has a picture.
                                mmsContainsText = true;
                                mmsText = currLine.substring(currLine.indexOf("text=") + 6, currLine.indexOf("/>") - 2);
                            }

                            // Reached end of mms message.
                            if (currLine.length() >= 6 && currLine.contains("</mms>")) {  // end of mms - try inserting into database if this mms message contained text

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the message already exists in the database.//////////
//////////////////////////////////////////////////////////////////////////////////
                                try {
                                    if (messageExists(createSQLTimestamp(mmsDate), mmsContactName)) {
                                        continue;
                                    }
                                } catch (Exception ex) {
                                    System.out.println("Exception trying to check if an mms text message exists: " + ex);
                                }

                                try {
                                    Class.forName("com.mysql.jdbc.Driver");

                                    //Swap out certain characters. Apostrophes and newline characters need manipulation before being sent to the MySQL database.
                                    mmsText = fixSMSString(mmsText);
                                    String sql = "";
                                    try {
                                        if (mmsContainsText) {
                                            sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('[PICTURE] " + mmsText + "', " + mmsIncoming + ", (select id from contacts where name = '" + mmsContactName + "'), '" + createSQLTimestamp(mmsDate) + "'); ";
                                        } else {
                                            sql = "INSERT INTO messages (message_text, incoming, contact, sent_timestamp) VALUES ('[PICTURE]', '" + mmsIncoming + "', (select id from contacts where name = '" + mmsContactName + "'), '" + createSQLTimestamp(mmsDate) + "'); ";
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
                                } catch (ClassNotFoundException cnfe) {
                                    System.out.println("ClassNotFoundException: " + cnfe);
                                }
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

    // createSQLTimestamp: creates an SQL timestamp datatype.
    // timestamp: a string date time of the form "Apr 12, 2017 4:01:03 PM"
    // The return value for the above input would be 2017-04-12 16:01:03
    public static String createSQLTimestamp(String timestamp) throws Exception {
        String fixedTimestamp = "";
        // Get year.
        int indexOfComma = timestamp.indexOf(",");
        fixedTimestamp = timestamp.substring(indexOfComma + 2, indexOfComma + 6) + "-";

        // Get the month.
        switch (timestamp.substring(0, 3)) {
            case "Jan":
                fixedTimestamp += "01-";
                break;
            case "Feb":
                fixedTimestamp += "02-";
                break;
            case "Mar":
                fixedTimestamp += "03-";
                break;
            case "Apr":
                fixedTimestamp += "04-";
                break;
            case "May":
                fixedTimestamp += "05-";
                break;
            case "Jun":
                fixedTimestamp += "06-";
                break;
            case "Jul":
                fixedTimestamp += "07-";
                break;
            case "Aug":
                fixedTimestamp += "08-";
                break;
            case "Sep":
                fixedTimestamp += "09-";
                break;
            case "Oct":
                fixedTimestamp += "10-";
                break;
            case "Nov":
                fixedTimestamp += "11-";
                break;
            case "Dec":
                fixedTimestamp += "12-";
                break;
            default:
                throw new Exception("Something is wrong with the month!!!");
        }

        // Get the day.
        fixedTimestamp += timestamp.substring(timestamp.indexOf(" ") + 1, indexOfComma) + " ";

        // Get the time.
        // First, find out if it was morning (AM) or evening (PM).
        int hour = Integer.parseInt(timestamp.substring(indexOfComma + 7, timestamp.indexOf(":")));
        if (timestamp.substring(timestamp.length() - 2).equals("PM")) {
            if (hour == 12) {  // Don't add 12 - it is the afternoon, but the hour is 12 o'clock
                fixedTimestamp += hour;
            } else {
                fixedTimestamp += hour + 12;
            }
        } else if (hour == 12) { // Subtract twelve from the hour. The hour is midnight.
            fixedTimestamp += hour - 12;
        } else {
            fixedTimestamp += hour;
        }
        // Get minutes and seconds.
        fixedTimestamp += timestamp.substring(timestamp.indexOf(":"), timestamp.lastIndexOf(" "));

        return fixedTimestamp;
    }

    // Some strange things happen to text messages when they are turned into XML!
    // Below, I fix odd characters and turn them into what they should be.
    public static String fixSMSString(String message) {
        message = message.replace("&#55357;&#56832;", "☺");
        message = message.replace("�", "\'");  //Replace all � with apostraphes.
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

    public static boolean messageExists(String timestamp, String contactName) {
        //create the connection to the database
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/sms", "root", "");
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

}
