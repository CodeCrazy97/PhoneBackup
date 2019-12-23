
import com.mysql.jdbc.MySQLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;

class TextMessagesBackup {

    static Connection conn = null;
    static ResultSet rs = null;
    static Statement st = null;
    static LinkedList<TextMessage> textMessages;

    public static void main(String[] args) throws IOException, SQLException {

        // Try to connect to the database. If we can't connect, then display error message and exit.
        conn = new MySQLMethods().getConnection();
        if (conn == null) {
            System.out.println("\nUnable to connect to the database. \nPlease check the connection. \nTry manually starting MySQL server. \nYou may need to delete the aria_log.* file, \nlocated in C:\\xampp\\mysql\\data");
            return;
        }
        // Get the path, replacing common invalid characters such as quotes.
        String path = new MySQLMethods().fixFilePath(args[0]);

        // Get a connection to the file that contains the text messages.
        File file = new File(path);  // Full file path to the text messages XML file.
        if (!file.exists()) {
            System.out.println("File does not exist.");
            throw new FileNotFoundException("Path to text messages XML file does not exist.");
        }

        // Get all the text messages, place them into a linked list. 
        // Will use the linked list to check if texts already exist.
        textMessages = new MySQLMethods().getTextMessages();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.
            try {
                String currLine;  //The line in the file currently being viewed by the program. The xml file is
                // broken up by lines; so, one line represents a single SMS text message. MMS messages (text messages
                // that contain pictures or contain a lot of text) span multiple lines and are handled a little differently.

                System.out.println();
                System.out.println("Getting ready to backup text messages. This may take a few minutes.");

                int beginTimeMillis = (int) System.currentTimeMillis();
                while ((currLine = br.readLine()) != null) {
                    if (currLine != null && currLine.contains(" body=")) {  //If the line starts with " body=", then it is a line that contains a text message.

                        //Fetch the phone number that the message was sent/received to/from.
                        String phoneNumber = currLine.substring(29, currLine.indexOf("\" date"));
                        int phoneNumberInteger = fixPhoneNumber(phoneNumber);

                        //Get the contact that the message was sent/received to/from
                        String contactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.lastIndexOf("\""));
                        contactName = fixSMSString(contactName);
                        if (contactName.equals("(Unknown)")) {  // This message does not come from one of my contacts - use the phone number as a stand-in for the name of the contact.
                            contactName = phoneNumber;
                        }

                        //Get the timestamp of when the message was sent
                        String timestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.lastIndexOf("\" contact"));

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the sms message already exists in the database.//////
/////////////Old way: query database.
/////////////New way: check linked list of text messages. When/if matching message
/////////////is found, delete it from the list and skip the current message in XML file.
//////////////////////////////////////////////////////////////////////////////////
                        try {
                            if (messageExists(new MySQLMethods().createSQLTimestamp(timestamp), contactName, phoneNumberInteger)) {
                                // Go to next text message in the XML file.
                                continue;
                            }
                        } catch (Exception ex) {
                            System.out.println("Exception trying to check if a text message exists: " + ex);
                        }

                        //messageQueue is the actual text of the currently viewed message. The text is between  body= and toa=" in the line.
                        String messageQueue = currLine.substring(currLine.indexOf(" body=") + 7, currLine.indexOf("toa=\"") - 2);
                        //Swap out certain characters. Apostrophes and newline characters need manipulation before being sent to the MySQL database.
                        messageQueue = fixSMSString(messageQueue);

                        //////////////////////////////////////////////////////////////////////////////////
                        /*
                        No need to check if a contact already exists. This will be taken care of when inserting
                        text messages into the database:
                        "insert into text_messages (contact_id, ...) values ((select id from contacts where person_name = xxx and phone_number = yyy..."
                        If the above insert fails due to there not being the appropriate person_name and 
                        phone_number, then we can create the new contact. Afterwards, the text message can
                        be inserted. 
                        This way there is no need to explicitly check if the contact exists.
                         */
                        //////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
////////Finished checking if the sms message exists in the database./////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
                        
                        String sqlInsert = "";  // Used for debugging perposes (in case an insertion fails, would want to display what the insertion was).
                        try {
                            Class.forName("com.mysql.jdbc.Driver");

                            String sql = "";
                            try {
                                conn = new MySQLMethods().getConnection();  // try again to connect. Connection closes after being left open for too long.
                                sql = "INSERT INTO text_messages (msg_text, incoming, contact_id, sent_timestamp) VALUES ('" + messageQueue + "', " + incomingMessage + ", (SELECT id FROM contacts WHERE person_name = '" + contactName + "'), '" + new MySQLMethods().createSQLTimestamp(timestamp) + "'); ";
                            } catch (Exception ex) {
                                System.out.println("Exception: " + ex);
                                continue;   // Don't want to continue trying to insert into the database for this message.
                            }
                            sqlInsert = sql;

                            PreparedStatement preparedStatement = conn.prepareStatement(sql);
                            preparedStatement.executeUpdate();
                            System.out.println(sql);
                            preparedStatement.close();
                        } catch (SQLException sqle) {
                            System.out.println("SQL Exception ...: " + sqle);
                            System.out.println("SMS Insertion failure: " + sqlInsert);
                        } catch (ClassNotFoundException cnfe) {
                            System.out.println("ClassNotFoundException: " + cnfe);
                        }
                    } else { // mms (occurs on multiple lines)
                        if (currLine.contains("type=\"151\"")) {  // The recipient of the mms message. If there are more than one recipients, then this is a group message.
                            String currentPhoneNumber = currLine.substring(currLine.indexOf("address=\"") + 9, currLine.indexOf("\" type="));

                            mmsGroupMessagePhoneNumbers.add(currentPhoneNumber);
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

                            // Check to see that contact has a name. If he/she doesn't have a name, then use the phone number as a name.
                            if (mmsContactName.equals("(Unknown)")) {
                                mmsContactName = groupMessagePhoneNumber;
                            }

                            // If the phone number was not already considered during this run of the program, then will need to see if it is in the db.
                            // Check to see that the sender is already in the db.
                            if (!phoneNumberConsidered(mmsPhoneNumber)) {
                                new MySQLMethods().handleContact(mmsContactName, mmsPhoneNumber);
                            }
                        }
                        if (currLine.contains("ct=\"text/plain\"")) {  // Is a line with text. Indicate that this text message has a picture.
                            mmsContainsText = true;
                            mmsText = currLine.substring(currLine.indexOf("text=") + 6, currLine.indexOf("/>") - 2);
                        }

                        // Reached end of mms message.
                        if (currLine.length() >= 6 && currLine.contains("</mms>")) {  // end of mms - try inserting into database if this mms message contained text

                            // Get the names of all the people the group message was sent to (if it was a group message).
                            // If the phone number does not have a name in the database, then just use the number itself.
                            alsoSentTo = "Recipients: ";
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
                            }
                            mmsGroupMessagePhoneNumbers.clear();

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the mms message already exists in the database.//////
//////////////////////////////////////////////////////////////////////////////////
                            try {
                                if (messageExists(new MySQLMethods().createSQLTimestamp(mmsDate), mmsContactName)) {
                                    // Reset the recipient count next message.
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
                                    conn = new MySQLMethods().getConnection();   // Fetch the connection again (connections close after a certain amount of time).
                                    if (recipientCount >= 2) {
                                        if (mmsContainsText) {
                                            sql = "INSERT INTO text_messages (msg_text, incoming, contact_id, sent_timestamp) VALUES ('[GROUP MSG] " + mmsText + "\\n\\n" + alsoSentTo + "', " + mmsIncoming + ", (select id from contacts where person_name = '" + mmsContactName + "'), '" + new MySQLMethods().createSQLTimestamp(mmsDate) + "'); ";
                                        } else {
                                            sql = "INSERT INTO text_messages (msg_text, incoming, contact_id, sent_timestamp) VALUES ('[GROUP MSG PIC]" + "\\n\\n" + alsoSentTo + "', " + mmsIncoming + ", (select id from contacts where person_name = '" + mmsContactName + "'), '" + new MySQLMethods().createSQLTimestamp(mmsDate) + "'); ";
                                        }
                                    } else if (mmsContainsText) {
                                        sql = "INSERT INTO text_messages (msg_text, incoming, contact_id, sent_timestamp) VALUES ('[PICTURE] " + mmsText + "', " + mmsIncoming + ", (select id from contacts where person_name = '" + mmsContactName + "'), '" + new MySQLMethods().createSQLTimestamp(mmsDate) + "'); ";
                                    } else {
                                        sql = "INSERT INTO text_messages (msg_text, incoming, contact_id, sent_timestamp) VALUES ('[PICTURE]', '" + mmsIncoming + "', (select id from contacts where person_name = '" + mmsContactName + "'), '" + new MySQLMethods().createSQLTimestamp(mmsDate) + "'); ";
                                    }
                                } catch (Exception ex) {
                                    System.out.println("Exception: " + ex);
                                }

                                mmsContainsText = false;  // Reset so we don't accidentally reinsert a message.
                                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                                preparedStatement.executeUpdate();
                                System.out.println(sql);
                                preparedStatement.close();
                            } catch (SQLException sqle) {
                                System.out.println("SQL Exception ...: " + sqle + "\nInsertion failure: " + sql);
                            } catch (ClassNotFoundException cnfe) {
                                System.out.println("ClassNotFoundException: " + cnfe);
                            }

                            // Reset the recipient count for next message.
                            recipientCount = 0;

                        }
                    }
                }
                int endTimeMillis = (int) System.currentTimeMillis();
                System.out.println("Finished backing up text messages! That took " + secondsFormatted((endTimeMillis - beginTimeMillis) / 1000) + ".");

                // Try to update the timestamp for the text messages backup.
                new MySQLMethods().updateBackup("text messages");

            } finally {
                if (conn != null) {
                    new MySQLMethods().closeConnection(conn);
                }
            }
        }
    }

    public static boolean phoneNumberConsidered(String phoneNumber) {
        for (int i = 0; i < phoneNumbers.size(); i++) {
            if (phoneNumbers.get(i).equals(phoneNumber)) {
                return true;
            }
        }

        // We have now considered this phone number. Add it to the list.
        phoneNumbers.add(phoneNumber);
        return false;
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

    public static boolean messageExists(String timestamp, String contactName, int phoneNumber) {
        for (int i = 0; i < textMessages.size(); i++) {
            if (textMessages.get(i).getSenderName().equals(contactName) && textMessages.get(i).getSenderPhoneNumber() == phoneNumber && textMessages.get(i).getTimestamp().equals(timestamp)) {
                // Remove the text message from the linked list (this will shorten the linked list so that next time we search through it, there's not as much to look at).
                textMessages.remove(i);
                return true;
            }
        }
        return false;
    }

    public static String getContactName(String phoneNumber) {
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT person_name FROM contacts WHERE phone_number = " + phoneNumber;
            if (phoneNumber.length() >= 10) {
                // Include an OR clause to check for the phone number without the area code (sometimes, I include contacts without their area code)
                query += " OR phone_number = " + phoneNumber.substring(phoneNumber.length() - 7) + ";";
            } else {
                query += ";";
            }

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getString(1);
            }

        } catch (Exception e) {
            System.out.println("Got an exception : " + e.getMessage());
        } finally {
            new MySQLMethods().closeResultSet(rs);
            new MySQLMethods().closeStatement(st);
            new MySQLMethods().closeConnection(conn);
        }

        // No contact existed with that phone number. So, just return the number itself.
        return phoneNumber;
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

    // fixPhoneNumber: takes a string and strips characters typically found in a phone number so that the number becomes an integer.
    public static int fixPhoneNumber(String phoneNumber) throws NumberFormatException {
        phoneNumber = phoneNumber.replace("\\", "");
        phoneNumber = phoneNumber.replace("-", "");
        phoneNumber = phoneNumber.replace(" ", "");
        phoneNumber = phoneNumber.replace(")", "");
        phoneNumber = phoneNumber.replace("(", "");

        // Try converting the phone number to an integer. If this cannot be done, then an error will be thrown.
        return Integer.parseInt(phoneNumber);
    }
}
