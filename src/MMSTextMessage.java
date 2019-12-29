
import java.util.LinkedList;

public class MMSTextMessage extends TextMessage {

    private Long[] recipients;
    private boolean containsPicture;

    public MMSTextMessage(long senderPhoneNumber, String timestamp, String messageText, boolean containsPicture, Long[] recipients) {
        super(messageText, timestamp, senderPhoneNumber);
        this.containsPicture = containsPicture;
        this.recipients = recipients;
    }

    public Long[] getRecipients() {
        return recipients;
    }

    public void setContainsPicture(boolean containsPicture) {
        this.containsPicture = containsPicture;
    }

    public boolean containsPicture() {
        return containsPicture;
    }
    

}
