package fr.delthas.skype;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

class WebConnector {
  private static final Logger logger = Logger.getLogger("fr.delthas.skype.web");
  private static final String SERVER_HOSTNAME = "https://api.skype.com";
  private final Skype skype;
  private final String username, password;
  private String skypeToken;
  private boolean updated = false;

  public WebConnector(Skype skype, String username, String password) {
    this.skype = skype;
    this.username = username;
    this.password = password;
  }

  private static String getPlaintext(String string) {
    if (string == null) {
      return null;
    }
    return Jsoup.parseBodyFragment(string).text();
  }

  public synchronized long refreshTokens(String token) throws IOException {
    logger.finer("Refreshing tokens");
    long expire = generateToken(token);
    updateContacts();
    return expire;
  }

  public void block(User user) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/" + user.getUsername() + "/block", "reporterIp", "127.0.0.1");
  }

  public void unblock(User user) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/" + user.getUsername() + "/unblock");
  }

  public void sendContactRequest(User user, String greeting) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/auth-request/" + user.getUsername(), "greeting", greeting);
  }

  public void acceptContactRequest(ContactRequest contactRequest) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/auth-request/" + contactRequest.getUser().getUsername() + "/accept");
  }

  public void declineContactRequest(ContactRequest contactRequest) throws IOException {
    sendRequest(Method.PUT, "/users/self/contacts/auth-request/" + contactRequest.getUser().getUsername() + "/decline");
  }

  public void removeFromContacts(User user) throws IOException {
    sendRequest(Method.DELETE, "/users/self/contacts/" + user.getUsername());
  }

  public byte[] getAvatar(User user) throws IOException {
    return sendRequest(Method.GET, user.getAvatarUrl(), true).bodyAsBytes();
  }

  public byte[] getFileAsBytes(String url) throws IOException {
    return prepareConnectWithAuthorizationToken(Method.GET, url, true)
            .header("Accept", "application/json")
            .execute().bodyAsBytes();
  }

  private String getUriObject(String content, String url, String type, String uriObjectExtraAttr, String title, String desc, HashMap<String, String> keyvals) {
    String titleTag = title == null ? "" : (title.equals("") ? "<Title/>" : String.format("<Title>Title: %s</Title>", title));
    String descTag = desc == null ? "" : ( desc.equals("") ? "<Description/>" : String.format("<Description>Description: %s</Description>", desc));

    StringBuilder valTags = new StringBuilder();
    Iterator it = keyvals.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();
      valTags.append(String.format("<%s v=\"%s\"/>", pair.getKey(),pair.getValue()));
      it.remove(); // avoids a ConcurrentModificationException
    }

    return String.format("<URIObject type=\"%s\" uri=\"%s\"%s>%s%s%s%s</URIObject>", type, url, uriObjectExtraAttr, titleTag, descTag, content, valTags.toString());
  }

  void sendFile(Group group, SkypeFile file) throws IOException {
    SkypeFileType fileType = file.getEnumType();
    String uploadId = getIdForUploadUrl(group, file.getName(), fileType);

    String fullUrl = "https://api.asm.skype.com/v1/objects/" + uploadId;
    int statusCode = putFileReturnStatusCode(file.getContent(), fullUrl + fileType.getUploadUrlPart());
    if (statusCode != 201) {
      throw new IOException("Couldn't create a file by PUT request, received code " + Integer.toString(statusCode));
    }

    HashMap<String, String> keyvals = new HashMap<>();
    keyvals.put("OriginalName", file.getName());

    String messageBody;

    String linkToFile = String.format(fileType.getLinkToFileFormat(), uploadId);
    String viewLink = String.format("<a href=\"%s\">%s</a>", linkToFile, linkToFile);

    switch (fileType) {
      case IMAGE:
        messageBody = this.getUriObject(String.format("%s<meta type=\"photo\" originalName=\"%s\"/>", viewLink, file.getName()), fullUrl, fileType.getUriObjectType(), file.getUriObjectParams(fullUrl), "", "", keyvals);
        break;

/*      case VIDEO:
        keyvals.put("FileSize", String.format("%d", 0));

        messageBody = this.getUriObject(String.format("To view this video message, go to: %s", viewLink), fullUrl, fileType.getUriObjectType(), file.getUriObjectParams(fullUrl), null, null, keyvals);
        break;*/

/*      case AUDIO:
        keyvals.put("FileSize", String.format("%d", 0));

        messageBody = this.getUriObject(String.format("To hear this voice message, go to: %s", viewLink), fullUrl, fileType.getUriObjectType(), file.getUriObjectParams(fullUrl), null, null, keyvals);
        break;*/

      default:
        keyvals.put("FileSize", String.format("%d", file.getContent().length));

        messageBody = this.getUriObject(viewLink, fullUrl, fileType.getUriObjectType(), file.getUriObjectParams(fullUrl), file.getName(), file.getName(), keyvals);
    }

    group.sendMessage(messageBody, true, fileType.getMsgType(), "Content-Type: application/user+xml");
  }

    private String getIdForUploadUrl(Group group, String fileName, SkypeFileType fileType) throws IOException {
        JSONObject meta = new JSONObject();
        switch(fileType) {
            case IMAGE:
                meta.put("type","pish/image");
                break;

            default:
                meta.put("type", "sharing/file");
                meta.put("filename", fileName);
        }

        JSONArray permission = new JSONArray();
        permission.put("read");

        JSONObject groupPermission = new JSONObject();
        groupPermission.put("19:" + group.getId() + "@thread.skype", permission);

        meta.put("permissions", groupPermission);

        Connection conn = prepareConnectWithAuthorizationToken(Method.POST, "https://api.asm.skype.com/v1/objects", true)
                .requestBody(meta.toString())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Client-Version", "908/1.117.0.21");

        String idResponse = conn.execute().body();

        JSONObject json = new JSONObject(idResponse);

        String uploadId = json.getString("id");

        if (uploadId == null) {
            throw new IOException("Error while parsing file id response, no uploadId ");
        }

        return uploadId;
    }

    /**
   * Do not use as it corrupts at least pictures. Use native HttpURLConnection instead, implemented in
   * @see #putFileReturnStatusCode(byte[], String)
   *
   * @param fileBytes
   * @param urlFull
   * @return
   * @throws IOException
   *
   * @deprecated
   */
  private int putFileReturnStatusCodeJsoup(byte[] fileBytes, String urlFull) throws IOException {
    Connection putConnection = prepareConnectWithAuthorizationToken(Method.PUT, urlFull, true)
            .requestBody(new String(fileBytes))
            .header("Content-Type", "multipart/form-data; charset=utf-8");

    return putConnection.execute().statusCode();
  }

  private int putFileReturnStatusCode(byte[] fileContents, String urlFull) throws IOException {
    java.net.URL url = new java.net.URL(urlFull);
    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);

    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Authorization", "skype_token " + skypeToken);
    conn.setRequestProperty("Content-Type", "multipart/form-data");
    conn.setRequestProperty("Content-Length", String.valueOf(fileContents.length));

    java.io.OutputStream out = conn.getOutputStream();
    out.write(fileContents);
    out.close();

    return conn.getResponseCode();
  }

  public void updateUser(User user) throws IOException {
    String reponse = sendRequest(Method.GET, "/users/" + user.getUsername() + "/profile/public", "clientVersion", "0/7.12.0.101/").body();
    JSONObject userJSON = new JSONObject(reponse);
    userJSON.put("username", user.getUsername());
    updateUser(userJSON, false);
  }

  private void updateContacts() throws IOException {
    if (updated) {
      return;
    }
    updated = true;
    String selfResponse = sendRequest(Method.GET, "/users/self/profile").body();
    JSONObject selfJSON = new JSONObject(selfResponse);

    User loggedUser = updateUser(selfJSON, false, username);

    String profilesResponse =
            sendRequest(Method.GET, "https://contacts.skype.com/contacts/v2/users/" + loggedUser.getLiveUsername() + "/contacts", true).body();

    try {
      JSONObject json = new JSONObject(profilesResponse);
      if (json.optString("message", null) != null) {
        throw new ParseException("Error while parsing contacts response: " + json.optString("message"));
      }
      JSONArray profilesJSON = json.getJSONArray("contacts");
      for (int i = 0; i < profilesJSON.length(); i++) {
        User user = updateUser(profilesJSON.getJSONObject(i), true);
        if (user != null && !user.getUsername().equalsIgnoreCase("echo123")) {
          skype.addContact(user.getUsername());
        }
      }
    } catch (JSONException e) {
      throw new ParseException(e);
    }
  }

  private User updateUser(JSONObject userJSON, boolean newContactType) throws ParseException {
    return updateUser(userJSON, newContactType, null);
  }

  private User updateUser(JSONObject userJSON, boolean newContactType, String username) throws ParseException {
    String userUsername;
    String userFirstName = null;
    String userLastName = null;
    String userMood = null;
    String userCountry = null;
    String userCity = null;
    String userDisplayName = null;
    String userAvatarUrl = null;
    try {
      if (!newContactType) {
        userUsername = userJSON.optString("username");
        userFirstName = userJSON.optString("firstname", null);
        userLastName = userJSON.optString("lastname", null);
        userMood = userJSON.optString("mood", null);
        userCountry = userJSON.optString("country", null);
        userCity = userJSON.optString("city", null);
        userDisplayName = userJSON.optString("displayname", null);
        userAvatarUrl = userJSON.optString("avatarUrl");
      } else {
        if (userJSON.optBoolean("blocked", false)) { return null; }
        if (!userJSON.optBoolean("authorized", false)) { return null; }
        if (userJSON.optBoolean("suggested", false)) { return null; }

        String mri = userJSON.getString("mri");
        int senderBegin = mri.indexOf(':');
        int network;
        try {
          network = Integer.parseInt(mri.substring(0, senderBegin));
        } catch (NumberFormatException e) {
          logger.warning("Error while parsing entity " + mri + ": unknown network format:" + mri);
          return null;
        }
        if (network != 8) {
          return null;
        }
        userUsername = mri.substring(senderBegin + 1);
        userDisplayName = userJSON.optString("display_name", null);
        JSONObject profileJSON = userJSON.getJSONObject("profile");

        if (profileJSON.has("name")) {
            JSONObject nameJSON = profileJSON.getJSONObject("name");
            userFirstName = nameJSON.optString("first", null);
            userLastName = nameJSON.optString("surname", null);
        }

        userMood = profileJSON.optString("mood", null);
        if (profileJSON.has("locations")) {
          JSONObject locationJSON = profileJSON.optJSONArray("locations").optJSONObject(0);
          if (locationJSON != null) {
            userCountry = locationJSON.optString("country", null);
            userCity = locationJSON.optString("city", null);
          }
        }
        userAvatarUrl = profileJSON.optString("avatar_url");
      }
    } catch (JSONException e) {
      throw new ParseException(e);
    }
    User user = skype.getUser(username != null ? username : userUsername);
    user.setCity(getPlaintext(userCity));
    user.setCountry(getPlaintext(userCountry));
    user.setDisplayName(getPlaintext(userDisplayName));
    user.setFirstName(getPlaintext(userFirstName));
    user.setLastName(getPlaintext(userLastName));
    user.setMood(getPlaintext(userMood));
    user.setAvatarUrl(userAvatarUrl);
    user.setLiveUsername(userUsername);
    return user;
  }

  private long generateToken(String token) throws IOException {
    String response;
    if (token == null) {
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        // md5 will always be available according to javadoc
        throw new RuntimeException(e);
      }
      byte[] md5hash = md.digest(String.format("%s\nskyper\n%s", username, password).getBytes(StandardCharsets.UTF_8));
      String base64hash = Base64.getEncoder().encodeToString(md5hash);
      logger.finest("Getting Skype token");
      response = sendRequest(Method.POST, "/login/skypetoken", "scopes", "client", "clientVersion", "0/7.12.0.101/", "username", username,
              "passwordHash", base64hash).body();
    } else {
      logger.finest("Getting Microsoft token");
      response = sendRequest(Method.POST, "/rps/skypetoken", "scopes", "client", "clientVersion", "0/7.12.0.101/", "access_token", token, "partner", "999").body();
    }
    try {
      JSONObject jsonResponse = new JSONObject(response);
      if (!jsonResponse.has("skypetoken")) {
        if (jsonResponse.has("status") && jsonResponse.getJSONObject("status").has("text")) {
          IOException e = new IOException("Error while connecting to Skype: " + jsonResponse.getJSONObject("status").getString("text"));
          logger.log(Level.SEVERE, "", e);
          throw e;
        } else {
          IOException e = new IOException("Unknown error while connecting to Skype: " + jsonResponse);
          logger.log(Level.SEVERE, "", e);
          throw e;
        }
      }
      skypeToken = jsonResponse.getString("skypetoken");
      return System.nanoTime() + jsonResponse.getInt("expiresIn") * 1000000000L;
    } catch (JSONException e_) {
      ParseException e = new ParseException("Error while parsing skypetoken response: " + response, e_);
      logger.log(Level.SEVERE, "", e);
      throw e;
    }
  }

  //@todo
  private Connection prepareConnectWithAuthorizationToken(Method method, String apiPath, boolean absoluteApiPath) {
    String url = absoluteApiPath ? apiPath : SERVER_HOSTNAME + apiPath;
    Connection conn = Jsoup.connect(url).maxBodySize(100 * 1024 * 1024).timeout(10000).method(method).ignoreContentType(true).ignoreHttpErrors(true);
    logger.finest("Sending " + method + " request at " + url);
    conn.header("Authorization", "skype_token " + skypeToken);
    return conn;
  }

  private Response sendRequest(Method method, String apiPath, boolean absoluteApiPath, String... keyval) throws IOException {
    String url = absoluteApiPath ? apiPath : SERVER_HOSTNAME + apiPath;
    Connection conn = Jsoup.connect(url).maxBodySize(100 * 1024 * 1024).timeout(10000).method(method).ignoreContentType(true).ignoreHttpErrors(true);
    logger.finest("Sending " + method + " request at " + url);
    if (skypeToken != null) {
      conn.header("X-Skypetoken", skypeToken);
      conn.header("Accept", "application/json");
    } else {
      logger.fine("No token sent for the request at: " + url);
    }
    conn.data(keyval);
    return conn.execute();
  }

  private Response sendRequest(Method method, String apiPath, String... keyval) throws IOException {
    return sendRequest(method, apiPath, false, keyval);
  }
}
