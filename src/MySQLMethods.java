
import java.io.BufferedReader;
import javax.swing.JOptionPane;
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

class MySQLMethods {

    public MySQLMethods() {

    }

    public static Connection getConnection() {
        // Try getting a connection to the database. 
        Connection conn = null;

        boolean result = isRunning("mysqld.exe");
        if (!result) {
            try {
                Runtime.getRuntime().exec("C:\\xampp\\mysql\\bin\\mysqld.exe", null, new File("C:\\xampp\\mysql\\bin"));
            } catch (IOException ex) {
                System.out.println("Exception sleeping: " + ex);
            }
            try {
                //Force the program to wait for mysql to start.
                Thread.sleep(9500);
            } catch (InterruptedException ex) {
                System.out.println("Exception sleeping: " + ex);
            }
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/phone_backup", "root", "");
        } catch (Exception ex) {
            // If the database does not exist, then run the sql script that creates it.
            String basePath = new File("").getAbsolutePath();
            if (ex.getMessage().equals("Unknown database 'phone_backup'")) {  // Database was not created. Run the script that creates it.

                basePath = basePath.substring(0, basePath.lastIndexOf("\\"));
                String createDB = basePath.replace("\\", "/") + "/create_database.bat";
                System.out.println("path to create db: " + createDB);

                System.exit(0);
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
            e.printStackTrace();
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
        Connection conn = getConnection();
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
                    System.out.println(sql);
                } catch (SQLException sqle) {
                    System.out.println("SQL Exception: " + sqle);
                } catch (ClassNotFoundException cnfe) {
                    System.out.println("ClassNotFoundException: " + cnfe);
                }
            }
            st.close();
            closeConnection(conn);
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
}
