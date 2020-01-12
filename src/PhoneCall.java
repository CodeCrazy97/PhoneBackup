
public class PhoneCall {

    private String timestamp;
    private long contactPhoneNumber;
    private int duration;
    private int type;

    public PhoneCall(String timestamp, long contactPhoneNumber, int duration, int type) {
        this.timestamp = timestamp;
        this.contactPhoneNumber = contactPhoneNumber;
        this.duration = duration;
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getContactPhoneNumber() {
        return contactPhoneNumber;
    }

    public void setContactPhoneNumber(long contactPhoneNumber) {
        this.contactPhoneNumber = contactPhoneNumber;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getCallType() {
        return type;
    }

    public void setCallType(int type) {
        this.type = type;
    }
    
    

}
