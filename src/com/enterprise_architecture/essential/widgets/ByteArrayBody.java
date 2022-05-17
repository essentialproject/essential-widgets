/**
 * Copyright (c)2009 Enterprise Architecture Solutions Ltd.
 * This file is part of Essential Architecture Manager, 
 * the Essential Architecture Meta Model and The Essential Project.
 *
 * Essential Architecture Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Essential Architecture Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Essential Architecture Manager.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 24.11.2009	JWC	1st coding.
 * 
 */
package com.enterprise_architecture.essential.widgets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.mime.content.InputStreamBody;

/**
 * Provides an Apache HTTP Client MIME body created from ByteArrays, enabling to user to send
 * files such as images without having to create a physical file on the client first. Implemented
 * by extending the InputStreamBody which provides the required capabilities EXCEPT that the InputStreamBody
 * is NOT repeatable. Therefore, any authentication scenarios will raise a ClientProtocolException 
 * with the reason that the body is not repeatable as the stream cannot be re-read. 
 * <br>
 * ByteArrayBody adds a byte array attribute that saves the byte array (e.g. a file or image) so that
 * the body is repeatable.
 * @author Jonathan Carter <jonathan.carter@e-asolutions.com>
 * @version 1.0
 * @see org.apache.http.entity.mime.content.InputStreamBody
 *
 */
public class ByteArrayBody extends InputStreamBody 
{
	private byte[] itsContent;
	
	/**
	 * Construct a new ByteArrayBody to send specified byte array with the specified filename
	 * @param theByteArray the contents of the "file" to send, e.g. an image or a streamed file
	 * @param theFilename the name of the file that should be used by the recipient of this ContentBody
	 */
	public ByteArrayBody(final byte[] theByteArray, String theFilename)
	{
		// Initialise the underlying InputStreamBody
		super(new ByteArrayInputStream(theByteArray), theFilename);
		
		// Save theByteArray to make this repeatable.
		initialiseByteArray(theByteArray);
	}
	
	/**
	 * Construct a new ByteArrayBody to send specified byte array, of the specified MIME type with 
	 * the specified filename
	 * @param theByteArray the contents of the "file" to send, e.g. an image or a streamed file
	 * @param theMIMEType the MIME type of the contents of theByteArray
	 * @param theFilename the name of the file that should be used by the recipient of this ContentBody
	 */
	public ByteArrayBody(final byte[] theByteArray, String theMIMEType, String theFilename)
	{
		// Initialise the underlying InputStreamBody
		super(new ByteArrayInputStream(theByteArray), theMIMEType, theFilename);
		
		// Save theByteArray to make this repeatable
		initialiseByteArray(theByteArray);
	}
	
	/**
	 * Override the underlying InputStreamBody to return a ByteArrayInputStream of the body contents
	 * @return a ByteArrayInputStream for the contents - enabling this to be repeatable.
	 * @see java.io.ByteArrayInputStream
	 */
	@Override
	public InputStream getInputStream()
	{
		ByteArrayInputStream aByteStream = new ByteArrayInputStream(itsContent);
		return aByteStream;
	}
	
	/**
	 * Override the underlying InputStreamBody method to return the length of the contents.
	 * Many report the the InputStreamBody returns -1 for this, so as we know the length of the 
	 * content, calculate it and return it.
	 * @return the length of the content in the byte array.
	 */
	@Override
	public long getContentLength()
	{
		return itsContent.length;
	}
	
	/**
	 * Override the underlying InputStreamBody method to make sure any writing that is performed
	 * on this uses the content attribute and not passed onto the underlying InputStreamBody - to ensure 
	 * that this ContentBody is repeatable.
	 * @param theOutStream the output stream that the content in this ByteArrayBody is to be 
	 * written to.
	 * @throws IOException in the case of any problems writing to theOutStream 
	 */
	@Override
	public void writeTo(OutputStream theOutStream) throws IOException
	{
		theOutStream.write(itsContent, 0, itsContent.length);
	}
	
	/**
	 * Directly set the content of this ByteArrayBody to be the specified byte array.
	 * @param theContent the new byte array that is to be loaded into the ByteArrayBody.
	 */
	public void setContent(final byte[] theContent)
	{
		// Get the content from theContent into itsContent
		itsContent = theContent;
	}
	
	/**
	 * Initialise the contents of the ByteArrayBody to be the specified byte array.
	 * @param theByteArray the byte array to use as the content
	 */
	private void initialiseByteArray(byte[] theByteArray)
	{
		if(theByteArray != null)
		{
			itsContent = theByteArray;
		}
		else
		{
			itsContent = new byte[0];
		}
	}
	
}
