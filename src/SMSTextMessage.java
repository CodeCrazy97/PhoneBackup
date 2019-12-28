
public class SMSTextMessage extends TextMessage {

    private String contactName;
    private long recipientPhoneNumber;
    private boolean incoming;

    public SMSTextMessage(String contactName, long senderPhoneNumber, long recipientPhoneNumber, String timestamp, String messageText) {
        super(messageText, timestamp, senderPhoneNumber);
        this.contactName = contactName;
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public long getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public void setRecipientPhoneNumber(long recipientPhoneNumber) {
        this.recipientPhoneNumber = recipientPhoneNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

}
