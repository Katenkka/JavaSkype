package fr.delthas.skype;

public class SkypeImage extends SkypeFile {
    public SkypeImage(String name, byte[] content) {
        super(name, content);
        type = SkypeFileType.IMAGE;
    }

    public String getUriObjectParams(String thumbUrl, String fullUrl) {
        return String.format(" url_thumbnail=\"%s\"", String.format(thumbUrl, fullUrl));
    }
}
