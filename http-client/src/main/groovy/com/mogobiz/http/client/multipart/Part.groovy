package com.mogobiz.http.client.multipart

/**
 * @author stephane.manciot@ebiznext.com
 * 
 */
interface Part {

	/**
	 * @return String
	 */
	String getName();

	/**
	 * Method isFilePart.
	 * 
	 * @return boolean
	 */
	boolean isFilePart();

	/**
	 * Method isParamPart.
	 * 
	 * @return boolean
	 */
	boolean isParamPart();
}
