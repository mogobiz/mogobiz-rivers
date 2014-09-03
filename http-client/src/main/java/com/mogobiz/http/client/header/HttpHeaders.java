package com.mogobiz.http.client.header;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author smanciot
 * 
 */
public final class HttpHeaders extends ArrayList <Header>
{

    private static final long        serialVersionUID = 8409237860752302720L;

    private static final String      MULTIPART_RE     =
                                                          "(multipart/form-data)(; boundary=)(.*)";

    private static final String      CHARSET_RE       = "(; charset=)(.*)";

    private static final String      CONTENT_TYPE_RE  = "(Content-Type: )(.*)";

    public static final String       CONTENT_TYPE     = "Content-Type";

    public static final String       CONTENT_LENGTH   = "Content-Length";

    public static final String       HOST             = "Host";

    public static final String       DATE             = "Date";

    public static final String       USER_AGENT       = "User-Agent";

    private final String             formats[]        = {
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEEEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMMM d HH:mm:ss yyyy" };

    private final String              format           = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Constructor for HeadersImpl.
     */
    public HttpHeaders()
    {
    }

    /* LIST METHODS */

    /**
     * @see java.util.List#add(int, Object)
     */
    public final void add(int index, Header o)
    {
        if (contains(o))
        {
            super.remove(o);
        }
        super.add(index, o);
    }

    /**
     * @see java.util.Collection#add(Object)
     */
    public final boolean add(Header o)
    {
        if (contains(o))
        {
            super.remove(o);
        }
        return super.add(o);
    }

    /**
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public final boolean addAll(Collection < ? extends Header> c)
    {
        Iterator < ? > it = c.iterator();
        boolean ret = true;
        while (it.hasNext())
        {
            ret = ret & add((Header) it.next());
        }
        return ret;
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public final boolean addAll(int index, Collection < ? extends Header> c)
    {
        Iterator < ? > it = c.iterator();
        while (it.hasNext())
        {
            add(index++, (Header) it.next());
        }
        return true;
    }

    /* COLLECTION METHODS */

    /**
     * @see java.util.Collection#iterator()
     */
    public Iterator <Header> iterator()
    {
        Collections.sort(this);
        return super.iterator();
    }

    /* BUSINESS METHODS */

    /**
     * Method addHeader.
     *
     * @param line
     */
    public void addHeader(String line)
    {
        if (line.indexOf(":") > 0)
        {
            String name = line.substring(0, line.indexOf(":"));
            String value = line.substring(line.indexOf(":") + 1);
            addHeader(name, value);
        }
    }

    /**
     * Method addHeader.
     *
     * @param name
     * @param value
     */
    public void addHeader(String name, String value)
    {
        int index = indexOf(new HeaderImpl(name, ""));
        if (index >= 0)
        {
            Header header = (Header) get(index);
            add(new HeaderImpl(header.getHeaderName(), header.getHeaderValue() + ", " + value));
        }
        else setHeader(name, value);
    }

    /**
     * Method addIntHeader.
     *
     * @param name
     * @param value
     */
    public void addIntHeader(String name, int value)
    {
        addHeader(name, "" + value);
    }

    /**
     * Method addDateHeader.
     *
     * @param name
     * @param value
     */
    public void addDateHeader(String name, long value)
    {
        DateFormat df = new SimpleDateFormat(format, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        addHeader(name, df.format(new Date(value)));
    }

    /**
     * Method containsHeader.
     *
     * @param name
     * @return boolean
     */
    public boolean containsHeader(String name)
    {
        return this.contains(new HeaderImpl(name, ""));
    }

    /* SETTERS */

    /**
     * Method setHeader.
     *
     * @param line
     */
    public void setHeader(String line)
    {
        if (line.indexOf(":") > 0)
        {
            String name = line.substring(0, line.indexOf(":"));
            String value = line.substring(line.indexOf(":") + 1);
            setHeader(name, value);
        }
    }

    /**
     * Method setHeader.
     *
     * @param name
     * @param value
     */
    public void setHeader(String name, String value)
    {
        add(new HeaderImpl(name, value));
    }

    /**
     * Method setIntHeader.
     *
     * @param name
     * @param value
     */
    public void setIntHeader(String name, int value)
    {
        setHeader(name, "" + value);
    }

    /**
     * Method setDateHeader.
     *
     * @param name
     * @param value
     */
    public void setDateHeader(String name, long value)
    {
        DateFormat df = new SimpleDateFormat(format, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        setHeader(name, df.format(new Date(value)));
    }

    /* GETTERS */

    /**
     * Method getHeaderNames.
     *
     * @return Enumeration
     */
    public Enumeration < String > getHeaderNames()
    {
        Vector < String > v = new Vector < String >();
        Iterator <Header> it = iterator();
        while (it.hasNext())
        {
            v.add(((Header) it.next()).getHeaderName());
        }
        return v.elements();
    }

    /**
     * Method getHeaders.
     *
     * @param name
     * @return Enumeration
     */
    public Enumeration < ? > getHeaders(String name)
    {
        int index = indexOf(new HeaderImpl(name, ""));
        if (index >= 0)
        {
            return new Enumerator(((Header) get(index)).getHeaderValues());
        }
        else
        {
            return null;
        }
    }

    /**
     * Method getHeader.
     *
     * @param name
     * @return String
     */
    public String getHeader(String name)
    {
        int index = indexOf(new HeaderImpl(name, ""));
        if (index >= 0)
        {
            return ((Header) get(index)).getHeaderValues()[0];
        }
        return null;
    }

    /**
     * Method getIntHeader.
     *
     * @param name
     * @return int
     */
    public int getIntHeader(String name)
    {
        String header = getHeader(name);
        if (header != null)
        {
            return new Integer(header).intValue();
        }
        return -1;
    }

    /**
     * Method getDateHeader.
     *
     * @param name
     * @return long
     */
    public long getDateHeader(String name)
    {
        String value = getHeader(name);
        if (value == null)
        {
            return (-1L);
        }

        // Work around a bug in SimpleDateFormat in pre-JDK1.2b4
        // (Bug Parade bug #4106807)
        value += " ";

        // Attempt to convert the date header in a variety of formats
        for (int i = 0; i < formats.length; i++)
        {
            try
            {
                Date date = new SimpleDateFormat(formats[i], Locale.US).parse(value);
                return (date.getTime());
            }
            catch (ParseException e)
            {
                ;
            }
        }
        throw new IllegalArgumentException(value);
    }

    public static String getContentType(String contentType)
    {
        String ret = null;
        RE charsetRe = null;
        try
        {
            charsetRe = new RE(CONTENT_TYPE_RE);
        }
        catch (RESyntaxException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        // RE charsetRe = new RE(CHARSET_RE);
        if (charsetRe.match(contentType))
        {
            ret = charsetRe.getParen(2);
        }
        return ret;
    }

    public static String getCharset(String contentType)
    {
        String charset = null;
        RE charsetRe = null;
        try
        {
            charsetRe = new RE(CHARSET_RE);
        }
        catch (RESyntaxException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        // RE charsetRe = new RE(CHARSET_RE);
        if (charsetRe.match(contentType))
        {
            charset = charsetRe.getParen(2);
        }
        return charset;
    }

    public static String getMultipartBoundary(String contentType)
    {
        String boundary = null;
        RE multipartRe = null;
        try
        {
            multipartRe = new RE(MULTIPART_RE);
        }
        catch (RESyntaxException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        if (multipartRe.match(contentType))
        {
            boundary = multipartRe.getParen(3);
        }
        return boundary;
    }

    public static boolean isMultipart(String contentType)
    {
        boolean multipart = false;
        RE multipartRe = null;
        try
        {
            multipartRe = new RE(MULTIPART_RE);
        }
        catch (RESyntaxException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        if (multipartRe.match(contentType))
        {
            multipart = true;
        }
        return multipart;
    }

  	public static String getServerTime() {
  		Calendar calendar = Calendar.getInstance();
  		SimpleDateFormat dateFormat = new SimpleDateFormat(
  				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  		return dateFormat.format(calendar.getTime());
  	}

    private final class HeaderImpl implements Header
    {

        private static final String HEADER_RE      = "(.*)(: )(.*)";

        private static final String HEADER_NAME_RE = "(.*)(: )";

        private String              value;

        private String              headerName;

        private String[]            headerValues;

        protected int               position       = 0;

        /**
         * @param line
         *            - line
         */
        public HeaderImpl(String line)
        {
            RE header = null;
            try
            {
                header = new RE(HEADER_RE);
            }
            catch (RESyntaxException e)
            {
                throw new RuntimeException(e.getMessage());
            }
            // RE header = new RE(HEADER_RE);
            if (header.match(line))
            {
                initHeader(header.getParen(1), header.getParen(2));
            }
            else throw new IllegalArgumentException(line + " is not a valid Header");
        }

        public HeaderImpl(String name, String value)
        {
            initHeader(name, value);
        }

        /**
         * @param name
         *            - name
         * @param values
         *            - values
         */
        public HeaderImpl(String name, String[] values)
        {
            this.headerName = normalize(name.trim());
            this.headerValues = values;
            // if (name.indexOf(":") > 0) {
            // headerName = name.substring(0, name.indexOf(":"));
            // }
            value = "";
            if (values != null)
            {
                if (values.length > 0)
                {
                    for (int i = 0; i < values.length; i++)
                    {
                        value += values[i] + ", ";
                    }
                    value = value.substring(0, value.length() - 2);
                }
            }
        }

        /**
         * Method getHeaderName.
         *
         * @return String
         * @uml property=headerName
         */
        public String getHeaderName()
        {
            return headerName;
        }

        /**
         * Method getValue.
         *
         * @return String
         */
        public String getHeaderValue()
        {
            return value;
        }

        /**
         * Method getHeaderValues.
         *
         * @return String[]
         * @uml property=headerValues
         */
        public String[] getHeaderValues()
        {
            return headerValues;
        }

        /**
         * Method getPosition.
         *
         * @return int
         */
        public int getHeaderPosition()
        {
            return position;
        }

        /**
         * @see Object#toString()
         */
        public String toString()
        {
            return headerName + ": " + value + "\r\n";
        }

        /**
         * @see Object#equals(Object)
         */
        public boolean equals(Object obj)
        {
            if(obj == null)
            {
                return false;
            }
            if(obj == this)
            {
                return true;
            }
            if (obj instanceof Header)
            {
                /*
                 * A Header name should be case insensitive
                 */
                return ((Header) obj).getHeaderName().toLowerCase().equals(
                    getHeaderName().toLowerCase());
            }
            return false;
        }

        /**
         * @see Comparable#compareTo(Object)
         */
        public int compareTo(Header o)
        {
            return new Integer(getHeaderPosition())
                .compareTo(new Integer(o.getHeaderPosition()));
        }

        private void initHeader(String name, String value)
        {
            this.headerName = normalize(name.trim());
            value = value.trim();
            this.value = value;
            // if (name.indexOf(":") > 0) {
            // headerName = name.substring(0, name.indexOf(":"));
            // }
            StringTokenizer str = new StringTokenizer(value, ",");
            int nb = str.countTokens();
            if (nb > 0)
            {
                headerValues = new String[nb];
                int i = 0;
                while (str.hasMoreTokens())
                {
                    headerValues[i] = str.nextToken().trim();
                    i++;
                }
            }
            else headerValues = new String[] {value };
        }

        private String normalize(String name)
        {
            RE header = null;
            try
            {
                header = new RE(HEADER_NAME_RE);
            }
            catch (RESyntaxException e)
            {
                throw new RuntimeException(e.getMessage());
            }
            // RE header = new RE(HEADER_NAME_RE);
            if (header.match(name))
            {
                name = header.getParen(1);
            }
            return name;
        }

    }

}
