package databackupjava;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

public class MySQLConnection {

    public MySQLConnection() {

    }

    public Connection getConnection() {
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
                System.out.println("path to createdb: " + createDB);
                try {
                    Runtime.getRuntime().exec("cmd /c start \"\" \"" + createDB + "\"");
                } catch (IOException ex1) {
                    System.out.println("IOException: " + ex1);
                }
            }
        }
        return conn;
    }
}
