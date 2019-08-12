
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

class CallsBackup {

    //phoneNumbers (a linked list that stores all the phone numbers) is a data structure that saves the user from
    //having to confirm more than once whether or not to allow the program to create a new contact. Without the
    //phoneNumbers linked list, the program might ask the user multiple times if he/she would like to add a contact
    //to the database (this would happen if more than one message was sent/received from the same contact).
    public static LinkedList< String> phoneNumbers = new LinkedList<>();
    public static JTextArea output = null;  //  text area to display output
    public static void main(String[] args) throws IOException, SQLException {
        // Get the path, replacing common invalid characters such as quotes.
        String path = args[0].replace("\"", "");
        
        //Read through the new text messages.
        File file = new File(path);
        if (!file.exists()) { //we might not want to add text to a file that already existed
            output.append("\nFile does not exist.");
            throw new FileNotFoundException("Path to phone calls XML file does not exist.");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {  //Try reading from the text messages file.

            String currLine;  //The line in the file currently being viewed by the program. The xml file is
            //broken up by lines; so, one line represents a single text message.

            output.append("\nAbout to start backing up your phone calls.");
            output.append("\nThis program will NOT remove any phone calls already in the database.");
            output.append("\nThis may take a few minutes.\n");

            while ((currLine = br.readLine()) != null) {
                if (!currLine.contains("(Unknown)") && currLine.contains("duration")) {   // Line contains a call, and that call is from a contact.
                    //create the connection to the database
                    Connection conn = new MySQLMethods(output).getConnection();

                    try {
                        String phoneNumber = currLine.substring(currLine.indexOf("call number=\"") + 13, currLine.indexOf("\" duration"));
                        output.append("\nPHONE NUMBER: " + phoneNumber);
                        String duration = currLine.substring(currLine.indexOf("duration=\"") + 10, currLine.indexOf("\" date="));
                        String callTimestamp = currLine.substring(currLine.indexOf("readable_date=\"") + 15, currLine.indexOf("\" contact_name="));
                        String contactName = currLine.substring(currLine.indexOf("contact_name=\"") + 14, currLine.indexOf("\" />"));
                        contactName = fixForInsertion(contactName);
                        String incomingInt = currLine.substring(currLine.indexOf("type=\"") + 6, currLine.indexOf("\" presentation"));
                        int incoming = 1;  // 1 = true, 0 = false
                        if (incomingInt.equals("2")) {  // I dialed the number.
                            incoming = 0;
                        }
                        String myDriver = "org.gjt.mm.mysql.Driver";

                        Class.forName(myDriver);

                        try {  // check that the call does not already xist in the database
                            String query = "SELECT COUNT(*) FROM phone_calls WHERE call_timestamp = '" + new MySQLMethods(output).createSQLTimestamp(callTimestamp) + "'; ";
                            output.append("\nquery: " + query);
                            // create the java statement
                            Statement st = conn.createStatement();

                            // execute the query, and get a java resultset
                            ResultSet rs = st.executeQuery(query);

                            // iterate through the java resultset
                            boolean exists = false;
                            while (rs.next()) {
                                if (!rs.getString(1).equals("0")) {  //The call exists in the database
                                    exists = true;
                                }
                            }
                            rs.close();
                            st.close();
                            if (exists) {  // don't insert a call into the database if it already exists
                                continue;
                            }  // else, go on to insertion
                        } catch (Exception e) {
                            System.err.println("Exception trying to see if the call exists: " + e.getMessage());
                        }
                        try {  // now try inserting the call into the database
                            Class.forName("com.mysql.jdbc.Driver");

                            // First, check to see that the contact exists in the database.
                            new MySQLMethods(output).handleContact(contactName, phoneNumber, phoneNumbers);

                            String sql = "INSERT INTO phone_calls (contact_id, call_timestamp, duration, incoming) VALUES ((SELECT id FROM contacts WHERE name = '" + contactName + "'), '" + new MySQLMethods(output).createSQLTimestamp(callTimestamp) + "', " + duration + ", " + incoming + "); ";

                            PreparedStatement preparedStatement = conn.prepareStatement(sql);
                            preparedStatement.executeUpdate();

                            output.append("\nSQL insert statement: " + sql);
                        } catch (SQLException sqle) {
                            output.append("\nSQL Exception: " + sqle);
                        } catch (ClassNotFoundException cnfe) {
                            output.append("\nClassNotFoundException: " + cnfe);
                        }
                    } catch (Exception ex) {
                        output.append("\nException : " + ex);
                    } finally {
                        conn.close();
                    }
                }
            }
            JOptionPane.showMessageDialog(null, "Finished backing up phone calls.", "Success!", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            output.append("\n SQL Exception : " + ex);
        } catch (IOException ex) {
            output.append("\nIOException : " + ex);
        }
    }

    public static String fixForInsertion(String sql) {
        sql = sql.replace("\'", "\\'");
        sql = sql.replace("&amp;", "&");
        return sql;
    }
}
