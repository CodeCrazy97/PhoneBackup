
import java.util.LinkedList;


public class NewClass {

    public static LinkedList<String> ll = new LinkedList<>();
    public static void main(String[] args) {
        
        for (int i = 0; i < 50; i++) {
            ll.add("" + i);
        }
        //ll.add("hi");
        System.out.println("linked list size : " + ll.size());
        long beg1 = System.currentTimeMillis();
        new MySQLMethods().handleContact("john", "12345");
        long mid = System.currentTimeMillis();
        inThere("hi");
        long end1 = System.currentTimeMillis();
        System.out.println("mysql: " + (mid - beg1));
        System.out.println("linked list: " + (end1 - mid));
        
    }
    
    public static boolean inThere(String s) {
        for (int i = 0; i < ll.size(); i++) {
            if (ll.get(i).equals(s)) {
                System.out.println("yay");
                return true;
            }
        }
        return false;
    }
}
