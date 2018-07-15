package fr.delthas.skype;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SkypeFile {
    private String name;

    private byte[] content;

    protected SkypeFileType type = SkypeFileType.PLAIN_FILE;

    SkypeFile(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public String getUriObjectParams(String fullUrl) {
        return String.format(" url_thumbnail=\"%s\"", String.format(type.getThumbUrlFormat(), fullUrl));
    }

    /**
     * @deprecated , left for backward compatibility
     * @see #getEnumType()
     * @return
     */
    public String getType() {
        return getEnumType().getName();
    }

    public SkypeFileType getEnumType() {
        return type;
    }

    static SkypeFile getFile(Skype skype, FormattedMessage formatted, SkypeFileType fileType) {
        Document parsed = Jsoup.parse(formatted.body);
        Element uriobject = parsed.getElementsByTag("URIObject").first();

        String name = uriobject.getElementsByTag("originalname").first().attr("v");
        //String size = uriobject.getElementsByTag("filesize").first().attr("v");
        String urlFull = uriobject.attr("uri");
        String urlThumb = uriobject.attr("url_thumbnail");
        //String urlView = uriobject.getElementsByTag("a").first().attr("href");

        String downloadUrl = urlFull + fileType.getDownloadUrlPart();
        byte[] content = skype.getFile(downloadUrl);

        return Skype.getSkypeFile(name, content, fileType);
    }
}
