
public class SMSTextMessage extends TextMessage {

    private long recipientPhoneNumber;
    private boolean incoming;

    public SMSTextMessage(long senderPhoneNumber, long recipientPhoneNumber, String timestamp, String messageText) {
        super(messageText, timestamp, senderPhoneNumber);
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public long getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(long recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

}
