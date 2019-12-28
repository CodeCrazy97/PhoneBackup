
public class SMSTextMessage extends TextMessage {

    private String contactName;
    private long recipientPhoneNumber;
    private boolean incoming;

    public SMSTextMessage(long myPhoneNumber, String contactName, long phoneNumber, boolean incoming, String timestamp, String messageText) {
        super(messageText, timestamp, phoneNumber);
        this.contactName = contactName;
        this.incoming = incoming;
        if (incoming) { // Someone sent me the text message.
            super.setSenderPhoneNumber(phoneNumber);
            this.recipientPhoneNumber = myPhoneNumber;
        } else {  // I sent the text message.
            super.setSenderPhoneNumber(myPhoneNumber);
            this.recipientPhoneNumber = phoneNumber;
        }
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
