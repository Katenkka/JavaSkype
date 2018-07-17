package fr.delthas.skype;

public enum SkypeFileType {
    PLAIN_FILE("file", "/views/original",  "/content/original", "RichText/Media_GenericFile", "https://login.skype.com/login/sso?go=webclient.xmm&docid=%s", "%s/views/thumbnail", "File.1")
    ,IMAGE("image", "/views/imgpsh_fullsize",  "/content/imgpsh", "RichText/UriObject", "https://api.asm.skype.com/s/i?%s", "%s/views/imgt1", "Picture.1")
    ,VIDEO("video", "/views/video",  "/content/original", "RichText/Media_Video", "https://login.skype.com/login/sso?go=webclient.xmm?vim=%s", "%s/views/thumbnail", "Video.1/Message.1")
    ,AUDIO("audio", "/views/audio",  "/content/original", "RichText/Media_AudioMsg", "https://login.skype.com/login/sso?go=webclient.xmm?am=%s", "%s", "Audio.1/Message.1")
    ;

    private String name;

    private String downloadUrlPart;

    private String uploadUrlPart;

    private String msgType;

    private String linkToFileFormat;

    private String thumbUrlFormat;

    private String uriObjectType;

    SkypeFileType(String name, String downloadUrlPart, String uploadUrlPart, String msgType, String linkToFileFormat, String thumbUrlFormat, String uriObjectType) {
        this.name = name;
        this.downloadUrlPart = downloadUrlPart;
        this.uploadUrlPart = uploadUrlPart;
        this.msgType = msgType;
        this.linkToFileFormat = linkToFileFormat;
        this.thumbUrlFormat = thumbUrlFormat;
        this.uriObjectType = uriObjectType;
    }

    public static SkypeFileType getEnumValueByName(String name) {
        for(SkypeFileType val : SkypeFileType.values()){
            if (val.getName().equals(name)) {
                return val;
            }
        }

        return PLAIN_FILE;
    }

    public static SkypeFileType getEnumValueByMsgType(String msgType) {
        for(SkypeFileType val : SkypeFileType.values()){
            if (val.getMsgType().equals(msgType)) {
                return val;
            }
        }

        return PLAIN_FILE;
    }

    public String getName() {
        return name;
    }

    public String getDownloadUrlPart() {
        return downloadUrlPart;
    }

    public String getUploadUrlPart() {
        return uploadUrlPart;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getLinkToFileFormat() {
        return linkToFileFormat;
    }

    public String getThumbUrlFormat() {
        return thumbUrlFormat;
    }

    public String getUriObjectType() {
        return uriObjectType;
    }
}
