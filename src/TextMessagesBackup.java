
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

class TextMessagesBackup {

    static Connection conn = null;
    static ResultSet rs = null;
    static Statement st = null;
    static LinkedList<TextMessage> databaseTextMessages;

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
        // Will be used to check if texts already exist.
        databaseTextMessages = new MySQLMethods().getTextMessages();

        // xmlFileContacts - a map of all the contacts in the XML file that were texted and/or texts were received from.
        // databaseContacts - all the contacts stored in the database.
        // duplicates - all contacts that appear in both the xmlFileContacts and databaseContacts data structures.
        // 
        // The phone_number is the unique key that will identify each contact.
        // The contact's name (person_name in the database) is the value in the below maps.
        ContactsManager contactsManager = new ContactsManager();

        // Fetch the user's phone number. If it doesn't exist, ask that they provide it.
        long myPhoneNumber = new MySQLMethods().getMyPhoneNumber();
        try {
            if (myPhoneNumber == -1) {
                System.out.println("It appears that you have not entered your phone number. Please enter your phone number, including the area code.");
                Scanner input = new Scanner(System.in);
                String myPhoneNumberStr = input.nextLine();
                myPhoneNumber = fixPhoneNumber(myPhoneNumberStr);
                new MySQLMethods().addMyPhoneNumber(myPhoneNumber);
            }
        } catch (Exception e) {
            System.out.println("Something was wrong with the phone number you entered.");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.
            try {
                String currLine;  //The line in the file currently being viewed by the program. The xml file is
                // broken up by lines; so, one line represents a single SMS text message. MMS messages (text messages
                // that contain pictures or contain a lot of text) span multiple lines and are handled a little differently.

                System.out.println();
                System.out.println("Getting ready to backup text messages. This may take a few minutes.");

                int beginTimeMillis = (int) System.currentTimeMillis();

                // smsTextsToInsert - the sms text messages that will need to be inserted into the database.
                LinkedList<SMSTextMessage> smsTextsToInsert = new LinkedList<>();

                // mmsTextsToInsert - the mms text messages that will need to be inserted into the database.
                LinkedList<MMSTextMessage> mmsTextsToInsert = new LinkedList<>();

                // currMMSTextMessage - the current MMS text message being created.
                // (This is needed since MMS texts span multiple lines.)
                MMSTextMessage currMMSTextMessage = null;

                boolean mmsIncoming = true;

                long mmsPhoneNumber = -1;
                long mmsSenderPhoneNumber = -1;
                String mmsTimestamp = null;
                String mmsContactName = null;
                String mmsMessageText = null;
                boolean mmsContainsPicture = false;
                Map<Long, Long> allMMSRecipientPhoneNumbers = new TreeMap<>();
                LinkedList<Long> mmsRecipients = new LinkedList<>();
                boolean skipMMS = false; // Determines if program should skip certain mms messages.

                while ((currLine = br.readLine()) != null) {

                    if (currLine != null && currLine.contains("<sms protocol=")) {  //If the line starts with "<sms protocol=", then it is a line that contains a text message.

                        //Fetch the phone number that the message was sent/received to/from.
                        String phoneNumber = currLine.substring(currLine.indexOf("address=\"") + 9, currLine.indexOf("\" date"));
                        long phoneNumberLong = fixPhoneNumber(phoneNumber);

                        //Get the contact that the message was sent/received to/from
                        String contactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.lastIndexOf("\""));
                        contactName = fixSMSString(contactName);
                        if (contactName.equals("(Unknown)")) {  // This message does not come from one of my contacts - use the phone number as a stand-in for the name of the contact.
                            contactName = "" + fixPhoneNumber(phoneNumber);
                        }

                        //Get the timestamp of when the message was sent
                        String timestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.lastIndexOf("\" contact"));
                        timestamp = new MySQLMethods().createSQLTimestamp(timestamp);

                        // Update the list of contacts.
                        Contact c = new Contact(phoneNumberLong, contactName);
                        contactsManager.handleContact(c);

                        // Define sender and receiver phone numbers based on if the text was incoming or outgoing.
                        long senderPhoneNumber = phoneNumberLong;  // senderPhoneNumber - used only to check who sent a text message
                        long recipientPhoneNumber = phoneNumberLong; // recipientPhoneNumber - used only to check who received the sms text message
                        if (currLine.contains("type=\"1\"")) {  // Text message was incoming.
                            recipientPhoneNumber = myPhoneNumber;
                        } else { //Set my phone number as the phone number the message was sent from.
                            senderPhoneNumber = myPhoneNumber;
                        }

//////////////////////////////////////////////////////////////////////////////////
/////////////Check to see if the sms message already exists in the database.//////
/////////////Old way: query database.
/////////////New way: check linked list of text messages. When/if matching message
/////////////is found, delete it from the list and skip the current message in XML file.
//////////////////////////////////////////////////////////////////////////////////
                        try {
                            if (messageExists(timestamp, senderPhoneNumber)) {
                                // This message already exists. Go to next text message in the XML file.
                                continue;
                            }
                        } catch (Exception ex) {
                            System.out.println("Exception trying to check if a text message exists: " + ex);
                        }

                        //messageQueue is the actual text of the currently viewed message. The text is between  body= and toa=" in the line.
                        String messageQueue = currLine.substring(currLine.indexOf(" body=") + 7, currLine.indexOf("toa=\"") - 2);
                        messageQueue = fixSMSString(messageQueue);  // Replace certain characters in the string.

                        SMSTextMessage smstm = new SMSTextMessage(senderPhoneNumber, recipientPhoneNumber, timestamp, messageQueue);

                        // The text message does not exist in the database. Add it to the queue for insertion.
                        smsTextsToInsert.add(smstm);

                    } else if (currLine.contains("<mms ")) {  // MMS text message (either a group message or a message that contains a picture). These texts occur over multiple lines, and are terminated by "</mms>"

                        // Clear any variables from previous MMS message.
                        mmsContactName = null;
                        mmsMessageText = "";
                        mmsPhoneNumber = -1;
                        mmsRecipients.clear();
                        mmsSenderPhoneNumber = -1;
                        mmsTimestamp = null;

                        // First line of the mms message - assume we should NOT skip it.
                        skipMMS = false;

                        // Figure out if the message contained an image.
                        if (currLine.contains("<mms text_only=\"1\"")) {  // Does not contain a picture.
                            mmsContainsPicture = false;
                        } else {
                            mmsContainsPicture = true;
                        }

                        // Get phone number.
                        boolean ignoreRecipientNames = false;
                        int indexOfPhoneNumberClosingQuotes = getIndexOfClosingQuotes(currLine, "address=\"", '"');
                        try {
                            mmsPhoneNumber = fixPhoneNumber(currLine.substring(currLine.indexOf("address=\"") + 9, indexOfPhoneNumberClosingQuotes));
                        } catch (NumberFormatException nfe) { // SOmething was wrong with the phone number. Likely, it was the combination of group phone numbers (these are delimited by a tilda)
                            ignoreRecipientNames = true;
                        }

                        // Figure out if the message was incoming or outgoing.
                        if (currLine.contains("msg_box=\"1\"")) { // Incoming message.
                            mmsIncoming = true;
                            mmsSenderPhoneNumber = mmsPhoneNumber;
                        } else {  // Outgoing message.
                            mmsIncoming = false;
                            mmsSenderPhoneNumber = myPhoneNumber;
                        }

                        if (!ignoreRecipientNames) {
                            // Get the contact name.
                            int indexOfContactNameClosingQuotes = getIndexOfClosingQuotes(currLine, "contact_name=\"", '"');
                            mmsContactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, indexOfContactNameClosingQuotes);
                            if (mmsContactName.equals("(Unknown)")) { // Unknown contact. Use phone number as name.
                                mmsContactName = "" + mmsSenderPhoneNumber;
                            } else { // Contact name is known. Use the stored name.
                                mmsContactName = fixSMSString(mmsContactName);
                            }
                        } else { // Otherwise, the mmsContactName is actually a group of contact names, delimited by a comma. Do not try inserting these.
                            mmsContactName = "Me";
                            mmsPhoneNumber = myPhoneNumber;
                        }

                        // Get message timestamp and text.
                        int indexOfTimestampClosingQuotes = getIndexOfClosingQuotes(currLine, "readable_date=\"", '"');
                        mmsTimestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, indexOfTimestampClosingQuotes);
                        mmsTimestamp = new MySQLMethods().createSQLTimestamp(mmsTimestamp);

                        // Update the list of contacts.
                        Contact c = new Contact(mmsPhoneNumber, mmsContactName);
                        contactsManager.handleContact(c);

                        // Check if the MMS message already exists. If so, then no need to collect other variables related to it.
                        try {
                            if (messageExists(mmsTimestamp, mmsSenderPhoneNumber)) {
                                skipMMS = true;

                                // This message already exists. Go to next mms text message in the XML file.
                                continue;
                            }
                        } catch (Exception ex) {
                            System.out.println("Exception trying to check if a text message exists: " + ex);
                        }

                    } else if (!skipMMS && currLine.contains("ct=\"text/plain\"")) { // This line contains a text message.
                        int indexOfMessageOpeningQuotes = -1;
                        int indexOfMessageClosingQuotes = -1;
                        if (currLine.contains("text=\"")) {
                            indexOfMessageOpeningQuotes = currLine.indexOf("text=\"") + 6;
                            indexOfMessageClosingQuotes = getIndexOfClosingQuotes(currLine, "text=\"", '"');
                        } else if (currLine.contains("text='")) {
                            indexOfMessageOpeningQuotes = currLine.indexOf("text='") + 6;
                            indexOfMessageClosingQuotes = getIndexOfClosingQuotes(currLine, "text='", '\'');
                        }
                        if (indexOfMessageOpeningQuotes != -1 || indexOfMessageClosingQuotes != -1) {
                            mmsMessageText = currLine.substring(indexOfMessageOpeningQuotes, indexOfMessageClosingQuotes);
                            mmsMessageText = fixSMSString(mmsMessageText);
                        } else {
                            throw new Error("The index of the closing/opening quotes is invalid. It probably could not be found. Current line of XML file: \n\n" + currLine + "\n\n\n");
                        }
                    } else if (!skipMMS && currLine.contains("<addr address=\"") && currLine.contains("type=\"151\"")) {
                        // Fetch the recipient's phone number.
                        int indexOfPhoneNumberClosingQuotes = getIndexOfClosingQuotes(currLine, "<addr address=\"", '"');
                        long phoneNumber = Long.parseLong(currLine.substring(currLine.indexOf("<addr address=\"") + 15, indexOfPhoneNumberClosingQuotes));
                        mmsRecipients.add(phoneNumber);
                        allMMSRecipientPhoneNumbers.put(phoneNumber, phoneNumber);
                    } else if (!skipMMS && currLine.contains("</mms>")) { //End of the MMS
                        // Create a new instance of MMS text message class.
                        if (mmsContactName != null && mmsSenderPhoneNumber != -1 && mmsTimestamp != null) {
                            Long[] dereferencedMMSRecipients = mmsRecipients.toArray(new Long[mmsRecipients.size()]); // Copy recipients over to an array (cannot directly pass the linked list).
                            mmsTextsToInsert.add(new MMSTextMessage(mmsSenderPhoneNumber, mmsTimestamp, mmsMessageText, mmsContainsPicture, dereferencedMMSRecipients));
                        } else {
                            throw new Error("Something is wrong with the mms message. A variable is null.");
                        }
                    }
                }

                // Remove the duplicate contacts from both XML and Database maps.
                contactsManager.removeDuplicates();
                // Now, update any contacts that have had a name change.
                contactsManager.updateContacts();

                try {
                    // Before inserting any text messages, add any new contacts discovered in the XML file to the database.
                    if (contactsManager.getXmlFileContacts().size() > 0) {
                        Set<Map.Entry<String, Contact>> entrySet = contactsManager.getXmlFileContacts().entrySet();
                        String sql = "INSERT INTO contacts (phone_number, person_name) VALUES ";
                        for (Map.Entry<String, Contact> entry : entrySet) {
                            // Create the multiple insert string.
                            // (Using a batch insert instead of creating a single
                            // insert for each contact will improve performance.)
                            sql += "(" + entry.getValue().getPhoneNumber() + ", '" + entry.getValue().getPersonName() + "'), ";
                        }

                        if (sql.contains("'")) { // The sql statement has something to insert.
                            // Chop of the last comma, replace it with a semicolon.
                            sql = sql.substring(0, sql.lastIndexOf(",")) + ";";

                            System.out.println(sql);
                            new MySQLMethods().executeSQL(sql);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception trying to create the multiple inserts for contacts: " + e);
                }

                // Now, loop over all the recipient phone numbers that appeared in the mms text messages. See if any are not in the database. Add them if they aren't.
                Map<Long, Long> phoneNumbers = new MySQLMethods().getPhoneNumbers();
                try {
                    if (allMMSRecipientPhoneNumbers.size() > 0) {
                        Set<Map.Entry<Long, Long>> entrySet = allMMSRecipientPhoneNumbers.entrySet();
                        String sql = "INSERT INTO contacts (phone_number, person_name) VALUES ";
                        for (Map.Entry<Long, Long> entry : entrySet) {
                            if (!phoneNumbers.containsKey(entry.getKey())) {  // This phone number did not exist in the database.
                                sql += "(" + entry.getKey() + ", '" + entry.getKey() + "'), ";
                            }
                        }

                        if (sql.contains("'")) { // The sql statement has something to insert.
                            // Chop of the last comma, replace it with a semicolon.
                            sql = sql.substring(0, sql.lastIndexOf(",")) + ";";

                            System.out.println(sql);
                            new MySQLMethods().executeSQL(sql);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception trying to add MMS recipient(s) to the database: " + e);
                }

                // Try to insert the text messages.
                try {
                    if (smsTextsToInsert.size() > 0) {

                        String sql = "INSERT INTO text_messages (msg_text, sender_phone_number, msg_timestamp, text_only) VALUES ";
                        String sqlRecipients = "INSERT INTO text_message_recipients (contact_phone_number, text_message_id) VALUES ";
                        for (int i = 0; i < smsTextsToInsert.size(); i++) {
                            sql += "('" + smsTextsToInsert.get(i).getMessageText() + "', " + smsTextsToInsert.get(i).getSenderPhoneNumber() + ", '" + smsTextsToInsert.get(i).getTimestamp() + "', 1), ";
                            // Create a recipient for the text message.
                            sqlRecipients += "(" + smsTextsToInsert.get(i).getRecipientPhoneNumber() + ", (SELECT MAX(id) FROM text_messages WHERE sender_phone_number = " + smsTextsToInsert.get(i).getSenderPhoneNumber() + " AND msg_timestamp = '" + smsTextsToInsert.get(i).getTimestamp() + "')), ";
                        }

                        // Chop of the last comma, replace it with a semicolon.
                        sql = sql.substring(0, sql.lastIndexOf(",")) + ";";
                        sqlRecipients = sqlRecipients.substring(0, sqlRecipients.lastIndexOf(",")) + ";";

                        System.out.println(sql);
                        System.out.println(sqlRecipients);

                        new MySQLMethods().executeSQL(sql);
                        new MySQLMethods().executeSQL(sqlRecipients);
                    }
                } catch (Exception e) {
                    System.out.println("Exception trying to create multiple inserts for text messages: " + e);
                }

                // Insert the MMS text messages into the database.
                try {
                    if (mmsTextsToInsert.size() > 0) {
                        String sqlTextMessages = "INSERT INTO text_messages (msg_text, sender_phone_number, msg_timestamp, text_only) VALUES ";
                        String sqlTextMessageRecipients = "INSERT INTO text_message_recipients (contact_phone_number, text_message_id) VALUES ";
                        for (int i = 0; i < mmsTextsToInsert.size(); i++) {
                            if (mmsTextsToInsert.get(i).containsPicture()) {
                                sqlTextMessages += "('" + mmsTextsToInsert.get(i).getMessageText() + "', " + mmsTextsToInsert.get(i).getSenderPhoneNumber() + ", '" + mmsTextsToInsert.get(i).getTimestamp() + "', 0), ";
                            } else {
                                sqlTextMessages += "('" + mmsTextsToInsert.get(i).getMessageText() + "', " + mmsTextsToInsert.get(i).getSenderPhoneNumber() + ", '" + mmsTextsToInsert.get(i).getTimestamp() + "', 1), ";
                            }
                            // Loop over recipients.
                            Long[] recipients = mmsTextsToInsert.get(i).getRecipients();
                            for (int j = 0; j < recipients.length; j++) {
                                sqlTextMessageRecipients += "(" + recipients[j] + ", (SELECT MAX(id) FROM text_messages WHERE sender_phone_number = " + mmsTextsToInsert.get(i).getSenderPhoneNumber() + " AND msg_timestamp = '" + mmsTextsToInsert.get(i).getTimestamp() + "')), ";
                            }
                        }
                        // Chop of the last comma, replace it with a semicolon.
                        sqlTextMessages = sqlTextMessages.substring(0, sqlTextMessages.lastIndexOf(",")) + ";";
                        sqlTextMessageRecipients = sqlTextMessageRecipients.substring(0, sqlTextMessageRecipients.lastIndexOf(",")) + ";";

                        System.out.println(sqlTextMessages);
                        System.out.println(sqlTextMessageRecipients);

                        new MySQLMethods().executeSQL(sqlTextMessages);
                        new MySQLMethods().executeSQL(sqlTextMessageRecipients);
                    }
                } catch (Exception e) {
                    System.out.println("Exception trying to create multiple inserts for text messages: " + e);
                }

                int endTimeMillis = (int) System.currentTimeMillis();
                System.out.println("Finished backing up text messages! That took " + secondsFormatted((endTimeMillis - beginTimeMillis) / 1000) + ".");

                // Try to update the timestamp for the text messages backup.
                new MySQLMethods().updateBackup("text messages");
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static int getIndexOfClosingQuotes(String bigStr, String beginString, char quoteType) {
        int indexOfClosingQuotes = bigStr.indexOf(beginString) + beginString.length();
        while (bigStr.charAt(indexOfClosingQuotes) != quoteType) {
            indexOfClosingQuotes++;
        }
        return indexOfClosingQuotes;
    }

    public static boolean phoneNumberExists(long phoneNumber) {
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT COUNT(*) FROM contacts WHERE phone_number = " + phoneNumber + ";";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception checking if phone number exists: " + e);
        } finally {
            new MySQLMethods().closeResultSet(rs);
            new MySQLMethods().closeStatement(st);
            new MySQLMethods().closeConnection(conn);
        }
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

    public static boolean messageExists(String timestamp, long phoneNumber) {
        for (int i = 0; i < databaseTextMessages.size(); i++) {
            if (databaseTextMessages.get(i).getSenderPhoneNumber() == phoneNumber && databaseTextMessages.get(i).getTimestamp().equals(timestamp)) {
                // Remove the text message from the linked list (this will shorten the linked list so that next time we search through it, there's not as much to look at).
                databaseTextMessages.remove(i);
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
    public static long fixPhoneNumber(String phoneNumber) throws NumberFormatException {
        phoneNumber = phoneNumber.replace("\\", "");
        phoneNumber = phoneNumber.replace("-", "");
        phoneNumber = phoneNumber.replace(" ", "");
        phoneNumber = phoneNumber.replace(")", "");
        phoneNumber = phoneNumber.replace("(", "");

        // Try converting the phone number to an integer. If this cannot be done, then an error will be thrown.
        return Long.parseLong(phoneNumber);
    }
}
