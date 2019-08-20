
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NewClass {

    public static void main(String[] args) {

        String basePath = new File("").getAbsolutePath();
        basePath = basePath.replace("\\", "/");
        String filePath = basePath + "/src/lastTimePhoneCallsWereBackedUp.txt";  // Location of the information about the last backup.
        String lastBackupDate = getContents(filePath);
        if (lastBackupDate != null) {  // If there was a date of the last backup to begin with, then display it.
            try {
                System.out.println("not null");
                FileWriter f2 = new FileWriter(filePath, false);
                System.out.println("Last backed up : " + lastBackupDate);
                f2.write("date");
                f2.close();
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            }
        } else {
            System.out.println("contents WERE null!!!");
        }
    }

    public static String getContents(String filePath) {
        File fileToRead = new File(filePath);
        String contents = null;
        try (FileReader fileStream = new FileReader(fileToRead);
                BufferedReader bufferedReader = new BufferedReader(fileStream)) {

            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                contents = line;
            }

        } catch (Exception e) {
            System.out.println("Exception getting file contents: " + e);
        }
        return contents;
    }
}
