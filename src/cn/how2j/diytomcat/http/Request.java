package cn.how2j.diytomcat.http;

import cn.how2j.diytomcat.catalina.Connector;
import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Engine;
import cn.how2j.diytomcat.catalina.Service;
import cn.how2j.diytomcat.util.MiniBrowser;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.Converter;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public class Request extends BaseRequest{

    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;
    private String method;
    private String queryString;
    private Map<String, String[]> parameterMap;
    private Map<String, String> headerMap;
    private Map<String, Object> attributesMap;
    private Cookie[] cookies;
    private HttpSession session;
    private Connector connector;
    private boolean forwarded;

    public Request(Socket socket,  Connector connector) throws IOException {
        this.parameterMap = new HashMap();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();
        this.socket = socket;
        this.connector = connector;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        parseMethod();
        if(!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri, context.getPath());
            if(StrUtil.isEmpty(uri))
                uri = "/";
        }

        parseParameters();
        parseHeaders();
        parseCookies();
    }

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    private void parseContext() {
        Service service = connector.getService();
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;
        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is,false);
        requestString = new String(bytes, "utf-8");
    }

    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                // System.out.println(pair.length());
                // System.out.println("pair:"+pair);
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    private void parseUri() {
        String temp;

        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString(){
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }
    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString || 0==queryString.length())
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                String values[] = parameterMap.get(name);
                if (null == values) {
                    values = new String[] { value };
                    parameterMap.put(name, values);
                } else {
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }

    public Enumeration getHeaderNames() {
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }

    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }
    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];

            headerMap.put(headerName, headerValue);
        }
    }

    public String getLocalAddr() {

        return socket.getLocalAddress().getHostAddress();
    }

    public String getLocalName() {

        return socket.getLocalAddress().getHostName();
    }

    public int getLocalPort() {

        return socket.getLocalPort();
    }
    public String getProtocol() {

        return "HTTP:/1.1";
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();

        return StrUtil.subAfter(temp, "/", false);

    }

    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();

    }

    public int getRemotePort() {
        return socket.getPort();
    }
    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return getHeader("host").trim();
    }

    public int getServerPort() {
        return getLocalPort();
    }
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }
    public String getRequestURI() {
        return uri;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());

        return url;
    }
    public String getServletPath() {
        return uri;
    }
    public Cookie[] getCookies() {
        return cookies;
    }
    public String getJSessionIdFromCookie() {
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
    public HttpSession getSession() {
        return session;
    }
    public void setSession(HttpSession session) {
        this.session = session;
    }
    public Connector getConnector() {
        return connector;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }
}