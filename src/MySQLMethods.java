
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

class MySQLMethods {

    public MySQLMethods() {
        
    }

    public static Connection getConnection() {
        // Try getting a connection to the database. 
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/phone_backup", "root", "");
        } catch (Exception ex) {
            // If the database does not exist, then run the sql script that creates it.
            String basePath = new File("").getAbsolutePath();
            if (ex.getMessage().equals("Unknown database 'phone_backup'")) {  // Database was not created. Run the script that creates it.
                String createDB = basePath.replace("\\", "/") + "/create_database.bat";

                try {
                    Runtime.getRuntime().exec("cmd /c start \"\" \"" + createDB + "\"");
                } catch (IOException ex1) {
                    System.out.println("IOException: " + ex1);
                }
                try {
                    Thread.sleep(750);  // Wait a few seconds before trying to establish a connection to the database that was just created.
                } catch (InterruptedException ex1) {
                    System.out.println("Exception sleeping: " + ex1);
                }

                // Now try getting a connection to the database, since it should be created.
                try {
                    conn = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/phone_backup", "root", "");
                } catch (SQLException ex1) {
                    System.out.println("Exception trying to get a connection to the database: " + ex1);
                }
            }
        }
        return conn;
    }

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

    public static void closeConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ex) {
            System.out.println("Exception trying to close the connection: " + ex.getMessage());;
        }
    }

    public static void handleContact(String contactName, String phoneNumber, LinkedList<String> phoneNumbers) {
        System.out.println("\nChecking contact...name = " + contactName + ", phone = " + phoneNumber);
        boolean addContact = false;
        Connection conn = getConnection();
        if (!phoneNumbers.contains(phoneNumber)) {  //If this phone number has not been viewed before...
            try {
                // create our mysql database connection
                String myDriver = "org.gjt.mm.mysql.Driver";
                Class.forName(myDriver);

                String query = "SELECT COUNT(*) FROM contacts WHERE name = '" + contactName + "' OR name = '" + phoneNumber + "';";

                // create the java statement
                Statement st = conn.createStatement();

                // execute the query, and get a java resultset
                ResultSet rs = st.executeQuery(query);

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
                closeConnection(conn);
            } catch (Exception e) {
                System.err.println("Got an exception! ");
                System.err.println(e.getMessage());
                closeConnection(conn);
            }
        }
    }

}