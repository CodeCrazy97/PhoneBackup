import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

class MySQLConnection {

    public MySQLConnection() {

    }

    public static Connection getConnection() {
        // Try getting a connection to the database. 
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/sms", "root", "");
        } catch (Exception ex) {
            // If the database does not exist, then run the sql script that creates it.
            String basePath = new File("").getAbsolutePath();
            if (ex.getMessage().equals("Unknown database 'sms'")) {  // Database was not created. Run the script that creates it.
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
                try {  // Now try getting a connection to the database, since it should be created.
                    conn = DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/sms", "root", "");
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

}
