package fr.delthas.skype;

public class SkypeImage extends SkypeFile {
    public SkypeImage(String name, byte[] content) {
        super(name, content);
        type = SkypeFileType.IMAGE;
    }

    public String getUriObjectParams(String fullUrl) {
        return String.format(" url_thumbnail=\"%s\"", String.format(type.getThumbUrlFormat(), fullUrl));
    }
}
