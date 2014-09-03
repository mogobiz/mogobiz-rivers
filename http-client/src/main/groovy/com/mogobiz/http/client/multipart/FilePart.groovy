package com.mogobiz.http.client.multipart


/**
 * @author stephane.manciot@ebiznext.com
 * 
 */
interface FilePart extends Part
{

    String getFileName()

    byte[] getBodyPart()

	boolean isBinary()

	String getContentType()

	String getContentTransferEncoding()
}
