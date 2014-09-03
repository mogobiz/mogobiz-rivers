package com.mogobiz.http.client

import com.mogobiz.http.client.header.Header
import com.mogobiz.http.client.header.HttpHeaders
import com.mogobiz.http.client.multipart.FilePart
import com.mogobiz.http.client.multipart.ParamPart
import com.mogobiz.http.client.multipart.Part
import groovy.json.JsonSlurper
import groovy.util.slurpersupport.GPathResult
import org.cyberneko.html.parsers.SAXParser
import org.xml.sax.SAXParseException

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author stephane.manciot@ebiznext.com
 *
 */
final class HTTPClient {

    static final String DEFAULT_CHARSET = 'utf-8'

    private static final Logger logger = Logger.getLogger(HTTPClient.class.getName())

    private static HTTPClient client

    static final enum METHOD {
        GET, POST, PUT, DELETE, HEAD
    }

    static
    {
        installOpenSSLTrustManager()
    }

    private HTTPClient() {
        // singleton
    }

    static HTTPClient getInstance() {
        if (client == null) {
            client = new HTTPClient()
        }
        return client
    }

    HttpURLConnection doHead(
            final Map config = [:], String url,
            final Map<String, String> params = null, final HttpHeaders headers = null, final boolean follow = true) {
        long before = System.currentTimeMillis()
        boolean debug = config['debug'] ? config['debug'] : false
        String method = METHOD.HEAD
        String charset = config['charset'] ? config['charset'] : DEFAULT_CHARSET
        final String u = params ? addParams(url.indexOf('?') < 0 ? url + '?' : url, params, charset).toString() : url
        try {
            HttpURLConnection conn = openConnection(config, u)
            conn.requestMethod = method
            conn.setDoOutput(false)
            conn.setRequestProperty("Accept-Charset", charset)
            addHeaders(headers, ['Accept-Charset'], conn)

            if (debug) {
                outputHeaders(conn)
            }

            return handleRedirection(conn, config, follow)
        }
        finally {
            if (debug) {
                outputDuration(u, method, before)
            }
        }
    }

    HttpURLConnection doGet(
            final Map config = [:], String url,
            final Map<String, String> params = null, final HttpHeaders headers = null, final boolean follow = true) {
        long before = System.currentTimeMillis()
        boolean debug = config['debug'] ? config['debug'] : false
        String method = METHOD.GET
        String charset = config['charset'] ? config['charset'] : DEFAULT_CHARSET
        final String u = params ? addParams(url.indexOf('?') < 0 ? url + '?' : url, params, charset).toString() : url
        try {
            HttpURLConnection conn = openConnection(config, u)
            conn.requestMethod = method
            conn.setDoOutput(false)
            conn.setRequestProperty("Accept-Charset", charset)
            addHeaders(headers, ['Accept-Charset'], conn)

            if (debug) {
                outputHeaders(conn)
            }

            return handleRedirection(conn, config, follow)
        }
        finally {
            if (debug) {
                outputDuration(u, method, before)
            }
        }
    }

    HttpURLConnection doPost(
            final Map config = [:],
            final String url,
            final Map<String, String> params = null,
            final String body = '', final HttpHeaders headers = null, boolean follow = true) {
        long before = System.currentTimeMillis()
        boolean debug = config['debug'] ? config['debug'] : false
        String method = METHOD.POST
        try {
            String charset = config['charset'] ? config['charset'] : DEFAULT_CHARSET
            HttpURLConnection conn = openConnection(config, url)
            conn.setDoOutput(true) // Triggers POST.
            conn.requestMethod = method
            conn.setRequestProperty('Accept-Charset', charset)
            if (params && !params.isEmpty()) {
                conn.setRequestProperty('Content-Type', 'application/x-www-form-urlencoded')
            }
            final String b = params ? addParams(body, params, charset).toString() : body
            if (debug) {
                logger.info(b)
            }
            byte[] bytes = b.getBytes(charset)
            int len = bytes.length
            conn.setRequestProperty('Content-Length', '' + len)
            conn.setRequestProperty('Length', '' + len)
            addHeaders(headers, ['Accept-Charset', 'Content-Length', 'Length'], conn)

            if (debug) {
                outputHeaders(conn)
            }

            def o = conn.outputStream
            o.write(bytes)
            o.flush()
            o.close()

            return handleRedirection(conn, config, follow)
        }
        finally {
            if (debug) {
                outputDuration(url, method, before)
            }
        }
    }

    HttpURLConnection doDelete(
            final Map config = [:],
            final String url,
            final Map<String, String> params = null, final HttpHeaders headers = null, final boolean follow = true) {
        long before = System.currentTimeMillis()
        boolean debug = config['debug'] ? config['debug'] : false
        String method = METHOD.DELETE
        String charset = config['charset'] ? config['charset'] : DEFAULT_CHARSET
        final String u = params ? addParams(url.indexOf('?') < 0 ? url + '?' : url, params, charset).toString() : url
        try {
            HttpURLConnection conn = openConnection(config, u)
            conn.setDoOutput(false)
            conn.requestMethod = method
            conn.setRequestProperty('Accept-Charset', charset)
            addHeaders(headers, ['Accept-Charset'], conn)

            if (debug) {
                outputHeaders(conn)
            }

            return handleRedirection(conn, config, follow)
        }
        finally {
            if (debug) {
                outputDuration(u, method, before)
            }
        }
    }

    HttpURLConnection doPut(
            final Map config = [:],
            final String url,
            final Map<String, String> params = null,
            final String content = '', final HttpHeaders headers = null, final boolean follow = true) {
        long before = System.currentTimeMillis()
        boolean debug = config['debug'] ? config['debug'] : false
        String method = METHOD.PUT
        try {
            HttpURLConnection conn = openConnection(config, url)
            conn.setDoOutput(true)
            conn.requestMethod = method
            String charset = config['charset'] ? config['charset'] : DEFAULT_CHARSET
            conn.setRequestProperty('Accept-Charset', charset)
            final def s = params ? addParams(content, params, charset).toString() : content
            if (debug) {
                logger.info(s)
            }
            byte[] bytes = s.getBytes(charset)
            int len = bytes.length
            conn.setRequestProperty('Content-Length', '' + len)
            conn.setRequestProperty('Length', '' + len)
            addHeaders(headers, ['Accept-Charset', 'Content-Length', 'Length'], conn)

            if (debug) {
                outputHeaders(conn)
            }

            OutputStream o = conn.outputStream
            o.write(bytes)
            o.flush()
            o.close()

            return handleRedirection(conn, config, follow)
        }
        finally {
            if (debug) {
                outputDuration(url, method, before)
            }
        }
    }

    HttpURLConnection doMultipart(
            final Map config = [:],
            final String url,
            final List<Part> parts = null,
            final HttpHeaders headers = null,
            final boolean follow = true){
        long before = System.currentTimeMillis()
        boolean debug = config['debug'] ? config['debug'] : false
        String method = METHOD.POST

        PrintWriter writer = null

        try{
            String charset = config['charset'] ? config['charset'] : DEFAULT_CHARSET
            // Just generate some unique random value.
            String boundary = Long.toHexString(System.currentTimeMillis())
            // Line separator required by multipart/form-data.
            String CRLF = System.getProperty("line.separator")
            HttpURLConnection conn = openConnection(config, url)
            conn.setDoOutput(true); // Triggers POST.
            conn.requestMethod = method
            conn.setRequestProperty('Accept-Charset', charset)
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
            addHeaders(headers, ['Accept-Charset', 'Content-Type'], conn)

            outputHeaders(conn)

            OutputStream output = conn.outputStream
            writer = new PrintWriter(new OutputStreamWriter(output, charset), true) // true = autoFlush, important!

            // Send parts
            parts?.each { Part part ->
                def key = part.name
                if(part.paramPart){
                    ParamPart paramPart = ((ParamPart)part)
                    String value = paramPart.value
                    // if(!value) value=''
                    if(value){
                        writer.append("--" + boundary).append(CRLF)
                        writer.append("Content-Disposition: form-data; name=\""+key+"\"").append(CRLF)
                        writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF)
                        writer.append(CRLF)
                        writer.append(URLEncoder.encode(value, charset)).append(CRLF).flush()
                    }
                }
                else if(part.filePart){
                    FilePart filePart = (FilePart)part
                    boolean binary = filePart.binary
                    writer.append("--" + boundary).append(CRLF)
                    writer.append("Content-Disposition: form-data; name=\"" + key
                            + "\"; filename=\"" + filePart.fileName + "\"").append(CRLF)
                    String contentType = (binary ? (filePart.contentType ? filePart.contentType :
                            URLConnection.guessContentTypeFromName(filePart.fileName)) :
                            "text/plain; charset=" + charset)
                    writer.append("Content-Type: " + contentType).append(CRLF)
                    if(binary){
                        writer.append("Content-Transfer-Encoding: binary").append(CRLF)
                    }
                    writer.append(CRLF).flush()
                    InputStream input = null
                    try {
                        input = new ByteArrayInputStream(filePart.bodyPart)
                        byte[] buffer = new byte[1024]
                        for (int length = 0; (length = input.read(buffer)) > 0;) {
                            output.write(buffer, 0, length);
                        }
                        output.flush() // Important! Output cannot be closed. Close of writer will close output as well.
                    } finally {
                        if (input != null) try { input.close() } catch (IOException ignored) {}
                    }
                    writer.append(CRLF).flush() // CRLF is important! It indicates end of binary boundary.
                }
            }

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF)

            return handleRedirection(conn, config, follow)
        }
        finally{
            if (writer != null) writer.close()
            if (debug) {
                outputDuration(url, method, before)
            }
        }
    }

    def closeConnection(HttpURLConnection conn) {
        try {
            conn?.inputStream?.close()
            conn?.outputStream?.close()
            conn?.disconnect()
        }
        catch (IOException io) {
            // rien à faire
        }
    }

    def String getText(Map config = [:], HttpURLConnection conn) {
        String charset = config['charset']
        String text = conn.responseCode >= 400 ? conn.errorStream?.getText(charset ? charset : DEFAULT_CHARSET) :
                conn.content?.getText(charset ? charset : DEFAULT_CHARSET)
        boolean debug = config['debug'] ? config['debug'] : false
        if (debug) {
            logger.info(text)
        }
        return text
    }

    def GPathResult parseTextAsXML(Map config = [:], HttpURLConnection conn, SAXParser parser = null) {
        def text = getText(config, conn)
        def slurper = parser ? new XmlSlurper(parser) : new XmlSlurper()
        try {
            return slurper.parseText(text)
        }
        catch (SAXParseException e) {
            logger.log(Level.SEVERE, text)
            return null
        }
    }

    def Map parseTextAsJSON(Map config = [:], HttpURLConnection conn) {
        try {
            return new JsonSlurper().parseText(getText(config, conn)) as Map
        }
        catch (SAXParseException e) {
            logger.log(Level.SEVERE, e.message)
            return null
        }
    }

    def SAXParser getHtmlParser(Map config = [:]) {
        def parser = new SAXParser()
        String charset = config['charset']
        parser.setProperty('http://cyberneko.org/html/properties/default-encoding', charset ? charset : DEFAULT_CHARSET)
        return parser
    }

    private addHeaders(HttpHeaders headers, List<String> excluded, HttpURLConnection conn) {
        headers?.findAll { Header header ->
            !(header.headerName in excluded)
        }?.each { Header header ->
            def name = header.getHeaderName()
            def value = header.getHeaderValue()
            conn.setRequestProperty(name, value)
        }
    }

    private HttpURLConnection handleRedirection(final HttpURLConnection conn, final Map config, final boolean follow) {
        if (conn.responseCode == 302 || conn.responseCode == 301) {
            if (follow) {
                def headers = new HttpHeaders()
                def location = conn.headerFields['Location']?.iterator()?.next()
                def cookies = conn.headerFields['Set-Cookie']
                if (cookies) {
                    headers.setHeader('Cookie', cookies as String)
                }
                logger.log(Level.FINE, 'Perform redirection to -> ' + location)
                switch (conn.requestMethod) {
                    case METHOD.GET:
                        return doGet(config, location, null, headers)
                    default:
                        return doPost(config, location, null, null, headers)
                }
            } else {
                logger.log(Level.FINE, 'Redirection not handled')
            }
        }
        return conn
    }

    private HttpURLConnection openConnection(Map config = [:], String url) {
        def conn = null
        logger.log(Level.FINE, 'Open connection to -> ' + url)
        String proxyHost = config['proxyHost']
        String proxyPort = config['proxyPort']
        String proxyUser = config['proxyUser']
        String proxyPass = config['proxyPass']
        if (proxyHost != null
                && proxyHost.trim().length() > 0
                && proxyPort != null
                && proxyPort.trim().length() > 0) {
            InetSocketAddress socket = InetSocketAddress.createUnresolved(proxyHost, Integer.parseInt(proxyPort))
            Proxy proxy = new Proxy(Proxy.Type.HTTP, socket)
            conn = url.toURL().openConnection(proxy)
            if (proxyUser != null && proxyUser.trim().length() > 0) {
                AuthenticatorSelector.setProxyAuth(proxyHost, proxyUser, proxyPass)
            }
        } else {
            conn = url.toURL().openConnection()
        }
        return conn as HttpURLConnection
    }

    private StringBuffer addParams(String s, Map<String, String> params, String charset) {
        StringBuffer buffer = new StringBuffer(s)
        boolean first = true
        params?.keySet()?.each { key ->
            String value = params.get(key)
            // if(!value) value=''
            if (value) {
                buffer.append(first ? '' : '&').append(key).append('=').append(URLEncoder.encode(value, charset))
                if (first) {
                    first = false
                }
            }
        }
        buffer
    }

    private void outputHeaders(HttpURLConnection conn) {
        StringBuffer headers = new StringBuffer()
        headers.append('Request Headers -> {\r\n')
        Map<String, List<String>> requestProperties = conn.requestProperties
        requestProperties?.keySet()?.each { String key ->
            headers.append('\t').append(key).append(': ').append(requestProperties.get(key).get(0)).append('\r\n')
        }
        headers.append('}')
        logger.info(headers.toString())
    }

    private void outputDuration(String url, String method, long before) {
        StringBuffer buffer = new StringBuffer()
        buffer.append('Perform ').append(method).append(' request to ').append(url).append(' within ').append((System.currentTimeMillis() - before)).append(' ms')
        logger.info(buffer.toString())
    }

    /**
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     */
    private static void installOpenSSLTrustManager() throws NoSuchAlgorithmException,
            KeyManagementException {
        def trustManager = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0]
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }

        TrustManager[] trustAllCerts = new TrustManager[1]
        trustAllCerts[0] = trustManager

        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true
            }
        }

        SSLContext sc = SSLContext.getInstance('SSL')
        sc.init(null, trustAllCerts, new SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        HttpsURLConnection.setDefaultHostnameVerifier(hv)
    }

}

/**
 * @version $Id $
 *
 * @author stephane.manciot@ebiznext.com
 *
 */
class UserPassword
{
    final String user
    final String pass

    UserPassword(String user, String pass)
    {
        this.user = user
        this.pass = pass
    }
}

/**
 * @version $Id $
 *
 * @author stephane.manciot@ebiznext.com
 *
 */
class AuthenticatorSelector extends Authenticator
{

    private static Map < String, UserPassword > proxies = new HashMap < String, UserPassword >()

    private static final Logger logger = Logger.getLogger(AuthenticatorSelector.class.getName())

    public static synchronized void setProxyAuth(String proxyHost, String proxyUser,
                                                 String proxyPass)
    {
        if (proxyUser == null)
        {
            proxies.remove(proxyHost)
        }
        else
        {
            if (proxies.size() == 0)
            {
                setDefault(new AuthenticatorSelector())
            }
            proxies.put(proxyHost, new UserPassword(proxyUser, proxyPass))
        }
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        String proxy = this.getRequestingHost()
        UserPassword up = proxies.get(proxy)
        if (up != null)
        {
            logger.log(Level.FINE, "**************" + proxy + "**********************")
            logger.log(Level.FINE, up.user + ":**********")
            logger.log(Level.FINE, "**************" + proxy + "**********************")
            return new PasswordAuthentication(up.user, up.pass.toCharArray())
        }
        else
        {
            return null
        }
    }
}

