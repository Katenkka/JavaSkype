package fr.delthas.skype;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SkypeFile {
    protected String name;

    protected byte[] content;

    public SkypeFile(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public String getType() {
        return "file";
    }

    public static SkypeFile getFile(Skype skype, FormattedMessage formatted, boolean image) {
        Document parsed = Jsoup.parse(formatted.body);

        Element uriobject = parsed.getElementsByTag("URIObject").first();

        String name = uriobject.getElementsByTag("originalname").first().attr("v");
        //String size = uriobject.getElementsByTag("filesize").first().attr("v");
        String urlFull = uriobject.attr("uri");
        String urlThumb = uriobject.attr("url_thumbnail");
        //String urlView = uriobject.getElementsByTag("a").first().attr("href");

        String downloadUrl = image ? urlFull + "/views/imgpsh_fullsize" : urlFull + "/views/original";

        return image ? new SkypeImage(name, skype.getFile(downloadUrl)) : new SkypeFile(name, skype.getFile(downloadUrl));
    }
}
