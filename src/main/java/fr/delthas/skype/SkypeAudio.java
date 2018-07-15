package fr.delthas.skype;

/**
 * @deprecated
 * not implemented yet
 */
public class SkypeAudio extends SkypeFile {
    public SkypeAudio(String name, byte[] content) {
        super(name, content);
        type = SkypeFileType.AUDIO;
    }

    public String getUriObjectParams(String fullUrl) {
        return "";
    }
}
