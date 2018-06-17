package fr.delthas.skype;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class GroupQuoteMessage {
    private String groupId;

    private String authorname;

    private Date date;

    private String quotedText;

    private String answerText;

    public GroupQuoteMessage(String groupId, String authorname, Date date, String quotedText, String answerText) {
        this.groupId = groupId;
        this.authorname = authorname;
        this.date = date;
        this.quotedText = quotedText;
        this.answerText = answerText;
    }

    @Override
    public String toString() {
        Instant timestamp = date.toInstant();

        SimpleDateFormat dateFormat = new SimpleDateFormat("d-MMM-yy HH:mm:ss");

        return String.format("<quote author=\"\" authorname=\"%s\" conversation=\"19:%s@thread.skype\" timestamp=\"%s\"><legacyquote>[%s] %s: </legacyquote>%s<legacyquote>&lt;&lt;&lt;</legacyquote></quote>%s", authorname, groupId, String.valueOf(timestamp.getEpochSecond()), dateFormat.format(date), authorname, quotedText, answerText);
    }
}
