
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

class MySQLMethods {

    static Connection conn = null;
    static Statement st = null;
    static ResultSet rs = null;

    public MySQLMethods() {

    }

    public static Connection getConnection() {
        // First, check that the MySQL server is running.
        boolean result = isRunning("mysqld.exe");
        if (!result) {  // MySQL server is not running - turn it on.
            System.out.println("MySQL is not turned on. Hang on while the program turns it on...");
            try {
                Runtime.getRuntime().exec("C:\\xampp\\mysql\\bin\\mysqld.exe");  // Generally, this is where mysql is located.
            } catch (IOException ex) {
                System.out.println("Exception trying to start mysql: " + ex);
            }
            try {
                System.out.println("Wait while we try to establish a connection to the database...");
                System.out.println();
                System.out.println();
                //Force the program to wait for mysql to start.
                Thread.sleep(9500);
            } catch (InterruptedException ex) {
                System.out.println("Exception waiting on mysql to turn on: " + ex);
            }
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/phone_backup", "root", "granted27");
        } catch (Exception ex) {
            // If the database does not exist, then run the sql script that creates it.
            String basePath = new File("").getAbsolutePath();
            if (ex.getMessage().equals("Unknown database 'phone_backup'")) {  // Database was not created. Run the script that creates it.
                System.out.println("The phone backup database has not been created.");
                System.out.println("The program will attempt to create the database.");
                basePath = basePath.substring(0, basePath.lastIndexOf("\\"));  // Go back a directory.
                String createDB = basePath.replace("\\", "/") + "/create_database.bat";

                try {
                    Runtime.getRuntime().exec("cmd /c start \"\" \"" + createDB + "\"");
                } catch (IOException ex1) {
                    System.out.println("IOException trying to create the database: " + ex1);
                }
                try {
                    System.out.println("Wait while we try to establish a connection to the database...");
                    Thread.sleep(3000);  // Wait a few seconds before trying to establish a connection to the database that was just created.
                } catch (InterruptedException ex1) {
                    System.out.println("Exception waiting to establish a connection to the database after creating it: " + ex1);
                }
                // Now try getting a connection to the database, since it should be created.
                try {
                    conn = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/phone_backup", "root", "granted27");
                } catch (SQLException ex1) {
                    System.out.println("Exception trying to get a connection to the database: " + ex1);
                }
                System.out.println();
            }
        }
        return conn;
    }

    // Returns the number of phone calls, by type. If incoming = true, then the type 
    // is a received phone call. If it is false, then it was a dialed phone call.
    // Excludes phone calls that lasted 0 seconds.
    public static int getPhoneCallsTotal(boolean incoming) {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "";
            if (incoming) {
                query = "SELECT COUNT(*) FROM phone_calls WHERE call_type = 2;";
            } else {
                query = "SELECT COUNT(*) FROM phone_calls WHERE call_type != 0;";
            }

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get total number of dialed phone calls: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return -1;
    }

    public static int getTimeSpentOnPhone() {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT SUM(duration) FROM phone_calls;";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get total time spent on the phone: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return -1;
    }

    // getLongestPhoneCall: Returns a String array with the following (sequentially stored) information:
    // duration, date/time of phone call, and contact name
    public static String[] getLongestPhoneCall() {
        String[] longestPhoneCallInformation = {"", "", ""};
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT p.duration, DATE_FORMAT(p.call_timestamp, '%W %M %d, %Y'), c.person_name FROM phone_calls p JOIN (SELECT person_name, phone_number FROM contacts) c ON c.phone_number = p.contact_phone_number ORDER BY p.duration DESC LIMIT 1;";

            st = conn.createStatement();
            rs = st.executeQuery(query);
            // iterate through the java resultset
            while (rs.next()) {
                longestPhoneCallInformation[0] = rs.getString(1);
                longestPhoneCallInformation[1] = rs.getString(2);
                longestPhoneCallInformation[2] = rs.getString(3);
                return longestPhoneCallInformation;
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get the earliest phone call: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return null;
    }

    public static String getOneTimestamp(Contact c) {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT date_format(msg_timestamp, '%Y-%c-%e %k:%i:%s') FROM text_messages WHERE sender_phone_number = " + c.getPhoneNumber() + ";";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getString(1);
            }

        } catch (Exception e) {
            System.out.println("Exception trying to get the earliest phone call: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return null;
    }

    public static void closeStatement(Statement st) {
        try {
            st.close();
        } catch (SQLException ex) {
            System.out.println("Exception trying to close SQL statement set: " + ex.getMessage());
        }
    }

    public static void closePreparedStatement(PreparedStatement pst) {
        try {
            pst.close();
        } catch (Exception e) {
            System.out.println("Exception trying to close the prepared statement: " + e);
        }
    }

    public static void closeResultSet(ResultSet rs) {
        try {
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Exception trying to close result set: " + ex.getMessage());
        }
    }

    public static String getEarliestPhoneCall() {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT DATE_FORMAT(call_timestamp, '%W %M %d, %Y') FROM phone_calls ORDER BY call_timestamp ASC LIMIT 1;";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getString(1);
            }

        } catch (Exception e) {
            System.out.println("Exception trying to get the earliest phone call: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return null;
    }

    public static String createSQLTimestamp(String timestamp) throws Exception {
        String fixedTimestamp = "";
        // Get year.
        int indexOfComma = timestamp.indexOf(",");
        fixedTimestamp = timestamp.substring(indexOfComma + 2, indexOfComma + 6) + "-";

        // Get the month.
        switch (timestamp.substring(0, 3)) {
            case "Jan":
                fixedTimestamp += "1-";
                break;
            case "Feb":
                fixedTimestamp += "2-";
                break;
            case "Mar":
                fixedTimestamp += "3-";
                break;
            case "Apr":
                fixedTimestamp += "4-";
                break;
            case "May":
                fixedTimestamp += "5-";
                break;
            case "Jun":
                fixedTimestamp += "6-";
                break;
            case "Jul":
                fixedTimestamp += "7-";
                break;
            case "Aug":
                fixedTimestamp += "8-";
                break;
            case "Sep":
                fixedTimestamp += "9-";
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

    public static void closeConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println("Exception trying to close the connection: " + ex.getMessage());;
        }
    }

    public static boolean isRunning(String process) {
        boolean found = false;
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);

            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n"
                    + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n"
                    + "Set service = locator.ConnectServer()\n"
                    + "Set processes = service.ExecQuery _\n"
                    + " (\"select * from Win32_Process where name='" + process + "'\")\n"
                    + "For Each process in processes\n"
                    + "wscript.echo process.Name \n"
                    + "Next\n"
                    + "Set WSHShell = Nothing\n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input
                    = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            line = input.readLine();
            if (line != null) {
                if (line.equals(process)) {
                    found = true;
                }
            }
            input.close();

        } catch (Exception e) {
            System.out.println("Exception trying to check if mysqld.exe is running: " + e.getMessage());
        }
        return found;
    }

    public static String fixFilePath(String path) {
        path = path.replace("\"", "");
        path = path.replace("\\", "\\\\");
        return path;
    }

    public static void handleContact(String contactName, String phoneNumber) {
        boolean addContact = false;
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT COUNT(*) FROM contacts WHERE person_name = '" + contactName + "' OR person_name = '" + phoneNumber + "';";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                if (rs.getString(1).equals("0")) {  // The contact does not exist in the database (zero results were returned from the query). 
                    addContact = true;
                }
            }

            rs.close();
            if (addContact) {  //User wants to add contact to db or user always wants to add a new contact to the db.
                try {
                    Class.forName("com.mysql.jdbc.Driver");

                    String sql = "";
                    if (contactName.equals("(Unknown)")) {
                        // If this contact does not have a name, just use the phone number as its "name".
                        sql = "INSERT INTO contacts (person_name, phone_number) VALUES ('" + phoneNumber + "', '" + phoneNumber + "'); ";
                    } else {
                        sql = "INSERT INTO contacts (person_name, phone_number) VALUES ('" + contactName + "', '" + phoneNumber + "'); ";
                    }

                    PreparedStatement preparedStatement = conn.prepareStatement(sql);
                    preparedStatement.executeUpdate();
                    preparedStatement.close();
                } catch (SQLException sqle) {
                    System.out.println("SQL Exception trying to insert a new contact into the database: " + sqle);
                } catch (ClassNotFoundException cnfe) {
                    System.out.println("ClassNotFoundException trying to insert a new contact into the database: " + cnfe);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception checking if a contact exists in the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
    }

    public static void addMyPhoneNumber(long myPhoneNumber) {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            try {
                Class.forName("com.mysql.jdbc.Driver");

                String sql = "INSERT INTO contacts (phone_number, person_name) VALUES (" + myPhoneNumber + ", 'Me');";

                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.executeUpdate();
                System.out.println(sql);
                preparedStatement.close();
            } catch (SQLException sqle) {
                System.out.println("SQL Exception trying to insert my phone number into the database: " + sqle);
            } catch (ClassNotFoundException cnfe) {
                System.out.println("ClassNotFoundException trying to insert my phone number into the database: " + cnfe);
            }
        } catch (Exception e) {
            System.out.println("Exception adding my phone number to the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
    }

    public static int getContactID(String personName, long phoneNumber) {
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT id FROM contacts WHERE person_name = '" + personName + "' AND phone_number = " + phoneNumber + ";";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get a contact's ID: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }

        // No contact existed with that name and/or phone number.
        throw new Error("No contact existed with name = " + personName + " and/or phone number = " + phoneNumber + ".");
    }

    public static void updateContactName(Contact c) {
        conn = new MySQLMethods().getConnection();
        try {
            // create the java mysql update preparedstatement
            String query = "UPDATE contacts SET person_name = '" + c.getPersonName() + "' WHERE phone_number = " + c.getPhoneNumber() + ";";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            // execute the java preparedstatement
            preparedStmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("Exception trying to update a contact: " + e.getMessage());
        } finally {
            new MySQLMethods().closeConnection(conn);
        }
    }

    public static String getContactNameFromPhoneNumber(long phoneNumber) {
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT person_name FROM contacts WHERE phone_number = " + phoneNumber;

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get a contact's name from his/her ID: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }

        // No contact existed with that id. So, just return the id itself.
        return "" + phoneNumber;
    }

    public static long getMyPhoneNumber() {
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            // get the id that points to the phone number in the contacts table
            String query = "SELECT phone_number FROM contacts WHERE person_name = 'Me';";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            System.out.println("Exception fetching your phone number from the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return -1;
    }

    public static int getLastCreatedID(long senderPhoneNumber, String timestamp) {
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            // get the id that was last created during an insert statement
            String query = "SELECT MAX(id) FROM text_messages WHERE sender_phone_number = " + senderPhoneNumber + " AND msg_timestamp = '" + timestamp + "';";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Exception fetching last created id from the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return -1;
    }

    public static LinkedList<TextMessage> getTextMessages() {
        LinkedList<TextMessage> textMessages = new LinkedList<TextMessage>();
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            // Fetch all the text messages from the database, sorting them first
            // according to if they contain a picture (text_only = 1) and second according to msg_timestamp.
            // We want picture texts to appear last in the query results, because 
            // they appear last in the XML file created by SMS Backup & Restore.
            // (Putting them last in the query results will be more efficient.)
            String query = "(SELECT msg_text, DATE_FORMAT(msg_timestamp, '%Y-%c-%e %k:%i:%s'), sender_phone_number\n"
                    + "FROM text_messages\n"
                    + "WHERE text_only = 1\n"
                    + "ORDER BY msg_timestamp ASC)\n"
                    + "UNION ALL\n"
                    + "(SELECT msg_text, DATE_FORMAT(msg_timestamp, '%Y-%c-%e %k:%i:%s'), sender_phone_number\n"
                    + "FROM text_messages\n"
                    + "WHERE text_only = 0\n"
                    + "ORDER BY msg_timestamp ASC);";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                textMessages.add(new TextMessage(rs.getString(1), rs.getString(2), rs.getLong(3)));
            }
        } catch (Exception e) {
            System.out.println("Exception fetching text messages from the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return textMessages;
    }

    public static LinkedList<PhoneCall> getPhoneCalls() {
        LinkedList<PhoneCall> phoneCalls = new LinkedList<PhoneCall>();
        conn = new MySQLMethods().getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            // Fetch all the phone calls from the database, sorting according to
            // timestamp.
            String query = "SELECT DATE_FORMAT(call_timestamp, '%Y-%c-%e %k:%i:%s'), contact_phone_number, duration, call_type FROM phone_calls ORDER BY call_timestamp ASC;";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                phoneCalls.add(new PhoneCall(rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getInt(4)));
            }
        } catch (Exception e) {
            System.out.println("Exception fetching text messages from the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return phoneCalls;
    }

    // Fetches the last backup timestamp for text messages or phone calls (type determines which one we're fetching the timestamp for)
    public static String getLastBackupDate(String type) {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT DATE_FORMAT(backup_timestamp, '%a %b %d, %Y at %r') FROM last_backup_timestamps WHERE backup_name = '" + type + "';";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get last backup date/time: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return null;
    }

    // The below method executes an sql statement (originally intended to execute
    // insert statements, but may be able to handle other types of statements).
    public static void executeSQL(String sql) {
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            try {
                Class.forName("com.mysql.jdbc.Driver");

                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            } catch (SQLException sqle) {
                System.out.println("SQL Exception: " + sqle + ".\nThe SQL statement: " + sql);
            } catch (ClassNotFoundException cnfe) {
                System.out.println("ClassNotFoundException trying to insert my phone number into the database: " + cnfe);
            }
        } catch (Exception e) {
            System.out.println("Exception adding my phone number to the database: " + e.getMessage());
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
    }

    // updateBackup - updates the timestamp for when text messages or phone calls were backed up.
    // type determines whether it is the timestamp for texts or calls that we're updating.
    public static void updateBackup(String type) {
        conn = getConnection();
        try {
            // create the java mysql update preparedstatement
            String query = "INSERT INTO last_backup_timestamps (backup_name, backup_timestamp) VALUES (?, NOW());";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            preparedStmt.setString(1, type);

            // execute the java preparedstatement
            preparedStmt.executeUpdate();

            conn.close();
        } catch (Exception e) {
            if (e.toString().contains("Duplicate entry '" + type + "' for key 'PRIMARY'")) {
                // Try updating instead of inserting.
                try {
                    // create the java mysql update preparedstatement
                    String query = "UPDATE last_backup_timestamps SET backup_timestamp = NOW() WHERE backup_name = ?";
                    PreparedStatement preparedStmt = conn.prepareStatement(query);
                    preparedStmt.setString(1, type);

                    // execute the java preparedstatement
                    preparedStmt.executeUpdate();

                    conn.close();
                } catch (Exception e2) {
                    System.err.println("Exception trying to update the backup timestamp: " + e.getMessage());
                } finally {
                    closeConnection(conn);
                }
            } else {
                System.out.println("Error trying to update last backup timestamp for " + type + ": " + e);
            }
        } finally {
            closeConnection(conn);
        }
    }

    public static boolean phoneNumberAlreadyHandled(String phoneNumber, LinkedList<String> phoneNumbers) {
        for (int i = 0; i < phoneNumbers.size(); i++) {
            if (phoneNumbers.get(i).equals(phoneNumber)) {
                return true;
            }
        }
        return false;
    }

    public static ConcurrentHashMap getContacts() {
        ConcurrentHashMap oldContacts = new ConcurrentHashMap();
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT phone_number, person_name FROM contacts;";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                Contact c = new Contact(rs.getLong(1), rs.getString(2));
                oldContacts.put(c.getPhoneNumber() + c.getPersonName(), c);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get preexisting contacts: " + e);
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return oldContacts;
    }

    public static Map<Long, Long> getPhoneNumbers() {
        Map<Long, Long> phoneNumbers = new TreeMap<>();
        conn = getConnection();
        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT phone_number FROM contacts;";

            st = conn.createStatement();
            rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                long phoneNumber = rs.getLong(1);
                phoneNumbers.put(phoneNumber, phoneNumber);
            }
        } catch (Exception e) {
            System.out.println("Exception trying to get preexisting contacts: " + e);
        } finally {
            closeResultSet(rs);
            closeStatement(st);
            closeConnection(conn);
        }
        return phoneNumbers;
    }
}
