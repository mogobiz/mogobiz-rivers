package com.mogobiz.http.client.multipart

/**
 * @author stephane.manciot@ebiznext.com
 * 
 */
final class MultipartFactory {

	private MultipartFactory() {
	}

	static FilePart createFilePart(final String name, 
		final String fileName, 
		final byte[] data, 
		final boolean binary = false, 
		final String contentType, 
		final String contentTransferEncoding) {
		return new FilePart(){
			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.Part#isParamPart()
			 */
			public final boolean isParamPart() {
				return false
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.Part#isFilePart()
			 */
			public final boolean isFilePart() {
				return true
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.FilePart#getFileName()
			 */
			public final String getFileName() {
				return fileName
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.Part#getName()
			 */
			public final String getName() {
				return name
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.FilePart#getBodyPart()
			 */
			public final byte[] getBodyPart() {
				return data
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.FilePart#isBinary()
			 */
			@Override
			public boolean isBinary() {
				return binary
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.FilePart#getContentType()
			 */
			@Override
			public String getContentType(){
				return contentType;
			}

			/* (non-Javadoc)
			 * @see fr.laposte.ccmu.mashup.multipart.FilePart#getContentTransferEncoding()
			 */
			@Override
			public String getContentTransferEncoding(){
				return contentTransferEncoding;
			}
		}
	}

	static ParamPart createParamPart(final String name, final String value) {
		return new ParamPart(){
			/*
			 * (non-Javadoc)
			 *
			 * @see fr.laposte.ccmu.mashup.multipart.Part#isParamPart()
			 */
			public final boolean isParamPart() {
				return true
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see fr.laposte.ccmu.mashup.multipart.Part#isFilePart()
			 */
			public final boolean isFilePart() {
				return false
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see fr.laposte.ccmu.mashup.multipart.Part#getName()
			 */
			public final String getName() {
				return name
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see fr.laposte.ccmu.mashup.multipart.ParamPart#getStringValue()
			 */
			public final String getValue() {
				return value
			}
		}
	}
}
