package fr.delthas.skype;

public class SkypeImage extends SkypeFile {
    public SkypeImage(String name, byte[] content) {
        super(name, content);
    }

    public String getType() {
        return "image";
    }
}
