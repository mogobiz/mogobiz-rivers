package com.mogobiz.http.client.header;

public interface Header extends Comparable <Header>
{

    /**
     * Method getHeaderName.
     * 
     * @return String
     */
    String getHeaderName();

    /**
     * Method getHeaderValue.
     * 
     * @return String
     */
    String getHeaderValue();

    /**
     * Method getHeaderValues.
     * 
     * @return String[]
     */
    String[] getHeaderValues();

    /**
     * Method getHeaderPosition.
     * 
     * @return int
     */
    int getHeaderPosition();

}
