
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class ViewPhoneCalls {

    // SQL connection, result set, and statement.
    static Connection conn = null;
    static Statement st = null;
    static ResultSet rs = null;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println("##########################################################################");
        System.out.println("Below are your phone call records...");
        System.out.println();
        // First, give some general information (number of phone calls dialed/received).
        int numberOfDialedCalls = new MySQLMethods().getPhoneCallsTotal(false);
        int numberOfReceivedCalls = new MySQLMethods().getPhoneCallsTotal(true);
        System.out.println("You have a total of " + (numberOfDialedCalls + numberOfReceivedCalls) + " phone calls on record dating back to " + new MySQLMethods().getEarliestPhoneCall());

        if (numberOfDialedCalls == -1) {
            System.out.println("You have zero dialed calls on record.");
        } else {
            System.out.println("Total number of dialed calls:   " + numberOfDialedCalls);
        }
        if (numberOfReceivedCalls == -1) {
            System.out.println("You have zero received calls on record.");
        } else {
            System.out.println("Total number of received calls: " + numberOfReceivedCalls);
        }
        System.out.println("Total time spent on the phone:  " + secondsFormatted(new MySQLMethods().getTimeSpentOnPhone()));
        
        System.out.println();

        conn = new MySQLMethods().getConnection();

        try {
            // create our mysql database connection
            String myDriver = "org.gjt.mm.mysql.Driver";
            Class.forName(myDriver);

            String query = "SELECT contact_phone_number, duration, DATE_FORMAT(call_timestamp, '%a %b %d, %Y at %r'), call_type FROM phone_calls ORDER BY call_timestamp DESC;";

            // create the java statement
            st = conn.createStatement();
            // execute the query, and get a java resultset
            rs = st.executeQuery(query);

            // count variable will keep up with how many phone calls have been displayed.
            // Every 25 phone calls, the program will ask the user if he/she wants to continue 
            // displaying calls. This is because it is quite slow to fetch and display
            // the phone calls from the database.
            int count = 0;

            // iterate through the java resultset
            while (rs.next()) {
                count++;
                if (count == 1) {  // First record - display a header.
                    String[] longestPhoneCallEver = new MySQLMethods().getLongestPhoneCall();
                    System.out.println("Your longest phone call ever was with " + longestPhoneCallEver[2] + " on " + longestPhoneCallEver[1] + ". It lasted a record " + secondsFormatted(Integer.parseInt(longestPhoneCallEver[0])) + ".");
                    System.out.println();
                    System.out.println();
                    System.out.printf("%-35s%-12s%-27s%-50s", "Timestamp", "Direction", "Contact", "Duration");
                    System.out.println("\n----------------------------------------------------------------------------------------------");
                }

                if (rs.getString(4).equals("2")) {  // I initiated the phone call (I was the dialer).
                    System.out.printf("%-35s%-12s%-27s%-50s", rs.getString(3), "Outgoing", new MySQLMethods().getContactNameFromPhoneNumber(rs.getLong(1)), secondsFormatted(rs.getInt(2)));
                    System.out.println();
                } else {  // The receiver initiated the phone call (I received the call).
                    System.out.printf("%-35s%-12s%-27s%-50s", rs.getString(3), "Incoming", new MySQLMethods().getContactNameFromPhoneNumber(rs.getLong(1)), secondsFormatted(rs.getInt(2)));
                    System.out.println();
                }

                // ask user if he/she would like to view the next 25 phone call records
                if (count % 25 == 0) {
                    String response = "";
                    do {
                        System.out.println("Load next 25 phone calls (y/n)?");
                        response = input.nextLine();
                    } while (response.charAt(0) != 'n' && response.charAt(0) != 'N' && response.charAt(0) != 'y' && response.charAt(0) != 'Y');
                    if (response.charAt(0) != 'Y' && response.charAt(0) != 'y') {  // Done loading.
                        break;
                    }
                }
            }
            if (count == 0) {
                System.out.println("No phone call records in the database.");
            }
        } catch (StringIndexOutOfBoundsException strOutEx) {
            // Do nothing. User pressed the enter key without any input.
        } catch (Exception e) {
            System.out.println("Error trying to get the phone call records: " + e.getMessage());
        } finally {
            new MySQLMethods().closeResultSet(rs);
            new MySQLMethods().closeStatement(st);
            new MySQLMethods().closeConnection(conn);
        }

        System.out.println();
        System.out.println(
                "##########################################################################");
    }

    // secondsFormatted: converts seconds to days, hours, minutes, and seconds.
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
            int days = hours / 24;       // Extract days out of hours.
            hours %= 24;                // Make hours less than 24.

            String daysString = "";
            String hoursString = "";
            String minutesString = "";
            String secondsString = "";

            if (days > 0) {
                if (days == 1) {
                    daysString = days + " day, ";
                } else {
                    daysString = days + " days, ";
                }
            }
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
            String duration = daysString + hoursString + minutesString + secondsString;  // Put the entire result in one string so we can check if a comma needs to be removed at the end.
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
}
