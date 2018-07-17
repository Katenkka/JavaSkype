package fr.delthas.skype;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * @deprecated
 * not implemented yet
 */
public class SkypeVideo extends SkypeFile {
    private int width;

    private int height;

    public SkypeVideo(String name, byte[] content) {
        super(name, content);
        type = SkypeFileType.VIDEO;

        InputStream fis = new ByteArrayInputStream(content);

        GetHeight ps = new GetHeight();
        try {
            ps.find(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getWidth() {
        return width;
    }

    private int getHeight() {
        return height;
    }

    public String getUriObjectParams(String urlFull) {
        return String.format(" url_thumbnail=\"%s\" width=\"%d\" height=\"%d\"", String.format(type.getThumbUrlFormat(), urlFull), getWidth(), getHeight());
    }

    private class GetHeight {
        byte[] lastTkhd;
        List<String> containers = Arrays.asList(
                "moov",
                "mdia",
                "trak"
        );

        long readUint32(byte[] b, int s) {
            long result = 0;
            result |= ((b[s + 0] << 24) & 0xFF000000);
            result |= ((b[s + 1] << 16) & 0xFF0000);
            result |= ((b[s + 2] << 8) & 0xFF00);
            result |= ((b[s + 3]) & 0xFF);
            return result;
        }

        double readFixedPoint1616(byte[] b, int s) {
            return ((double) readUint32(b, s)) / 65536;
        }

        private void find(InputStream fis) throws IOException {
            while (fis.available() > 0) {
                byte[] header = new byte[8];
                fis.read(header);

                long size = readUint32(header, 0);
                String type = new String(header, 4, 4, "ISO-8859-1");
                if (containers.contains(type)) {
                    find(fis);
                } else {
                    if (type.equals("tkhd")) {
                        lastTkhd = new byte[(int) (size - 8)];
                        fis.read(lastTkhd);
                    } else {
                        if (type.equals("hdlr")) {
                            byte[] hdlr = new byte[(int) (size - 8)];
                            fis.read(hdlr);
                            if (hdlr[8] == 0x76 && hdlr[9] == 0x69 && hdlr[10] == 0x64 && hdlr[11] == 0x65) {
                                System.out.println("Video Track Header identified");
                                Double v = readFixedPoint1616(lastTkhd, lastTkhd.length - 8);
                                width = v.intValue();
                                System.out.println("width: " + width);
                                Double h = readFixedPoint1616(lastTkhd, lastTkhd.length - 4);
                                height = h.intValue();
                                System.out.println("height: " + height);
                            }
                        } else {
                            fis.skip(size - 8);
                        }
                    }
                }
            }
        }
    }
}
