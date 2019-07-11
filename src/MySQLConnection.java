package backupcallsandsms;

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
}
