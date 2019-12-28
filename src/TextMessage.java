
public class TextMessage {

    private String messageText;
    private String timestamp;
    private long senderPhoneNumber;

    public TextMessage(String messageText, String timestamp, long senderPhoneNumber) {
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public long getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(long senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
