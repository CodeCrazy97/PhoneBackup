
import jdk.nashorn.internal.codegen.CompilerConstants;

import java.util.Scanner;
import javax.swing.JFrame;

public class Main {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Text Messages & Phone Call Backup Tool");
        Scanner x = new Scanner(System.in);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new MainJPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setSize(600, 600);
        // Fill screen below.
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

}
