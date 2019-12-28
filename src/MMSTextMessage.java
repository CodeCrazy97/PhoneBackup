
import java.util.LinkedList;

public class MMSTextMessage extends TextMessage {

    private LinkedList<Long> recipients;

    public MMSTextMessage(long senderPhoneNumber, String timestamp, String messageText) {
        super(messageText, timestamp, senderPhoneNumber);
    }

    public LinkedList<Long> getRecipients() {
        return recipients;
    }

    public void addRecipientPhoneNumber(long phoneNumber) {
        recipients.add(phoneNumber);
    }
}
