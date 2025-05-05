package de.ancash.sockets.async.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HttpRequest {
  private static final String[][] HttpReplies = {{"100", "Continue"},
                                                 {"101", "Switching Protocols"},
                                                 {"200", "OK"},
                                                 {"201", "Created"},
                                                 {"202", "Accepted"},
                                                 {"203", "Non-Authoritative Information"},
                                                 {"204", "No Content"},
                                                 {"205", "Reset Content"},
                                                 {"206", "Partial Content"},
                                                 {"300", "Multiple Choices"},
                                                 {"301", "Moved Permanently"},
                                                 {"302", "Found"},
                                                 {"303", "See Other"},
                                                 {"304", "Not Modified"},
                                                 {"305", "Use Proxy"},
                                                 {"306", "(Unused)"},
                                                 {"307", "Temporary Redirect"},
                                                 {"400", "Bad Request"},
                                                 {"401", "Unauthorized"},
                                                 {"402", "Payment Required"},
                                                 {"403", "Forbidden"},
                                                 {"404", "Not Found"},
                                                 {"405", "Method Not Allowed"},
                                                 {"406", "Not Acceptable"},
                                                 {"407", "Proxy Authentication Required"},
                                                 {"408", "Request Timeout"},
                                                 {"409", "Conflict"},
                                                 {"410", "Gone"},
                                                 {"411", "Length Required"},
                                                 {"412", "Precondition Failed"},
                                                 {"413", "Request Entity Too Large"},
                                                 {"414", "Request-URI Too Long"},
                                                 {"415", "Unsupported Media Type"},
                                                 {"416", "Requested Range Not Satisfiable"},
                                                 {"417", "Expectation Failed"},
                                                 {"500", "Internal Server Error"},
                                                 {"501", "Not Implemented"},
                                                 {"502", "Bad Gateway"},
                                                 {"503", "Service Unavailable"},
                                                 {"504", "Gateway Timeout"},
                                                 {"505", "HTTP Version Not Supported"}};

  private final String method, url;
  private final Map<String, Object> headers, params;
  private final int[] ver;
  private final int status;

  public HttpRequest(int status, String method, String url, Map<String, Object> headers, Map<String, Object> params, int[] ver) {
    this.method = method;
    this.status = status;
    this.url = url;
    this.headers = headers;
    this.params = params;
    this.ver = ver;
  }
  
  public int getStatus() {
	return status;
}

  public String getMethod() {
    return method;
  }

  public String getHeader(String key) {
    if (headers != null)
      return (String) headers.get(key.toLowerCase());
    else return null;
  }

  public Map<String, Object> getHeaders() {
    return headers;
  }

  public String getRequestURL() {
    return url;
  }

  public String getParam(String key) {
    return (String) params.get(key);
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public String getVersion() {
    return ver[0] + "." + ver[1];
  }

  public int compareVersion(int major, int minor) {
    if (major < ver[0]) return -1;
    else if (major > ver[0]) return 1;
    else if (minor < ver[1]) return -1;
    else if (minor > ver[1]) return 1;
    else return 0;
  }

  public static String getHttpReply(int codevalue) {
    String key, ret;
    int i;

    ret = null;
    key = "" + codevalue;
    for (i=0; i<HttpReplies.length; i++) {
      if (HttpReplies[i][0].equals(key)) {
        ret = codevalue + " " + HttpReplies[i][1];
        break;
      }
    }

    return ret;
  }

  public static String getDateHeader() {
    SimpleDateFormat format;
    String ret;

    format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    ret = "Date: " + format.format(new Date()) + " GMT";

    return ret;
  }
}