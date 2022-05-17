/**
 * Copyright (c)2013 Enterprise Architecture Solutions Ltd.
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
 * 23.05.2013	JWC	1st coding.
 */
package com.enterprise_architecture.essential.widgets;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * FilterOutputStream that counts the number of bytes that have been written, enabling progress to be
 * tracked when sending the repository snapshot to Essential Viewer via HTTP Post.
 * @author Jonathan Carter
 * @version 1
 * @see com.enterprise_architecture.essential.widgets.CountingByteArrayEntity CountingByteArrayEntity
 *
 */
public class CountingOutputStream extends FilterOutputStream 
{
	/**
	 * The count of the total number of bytes written
	 */
	private long itsTransferredCount;
	
	/**
	 * The number of bytes that are to be sent / transmitted
	 */
	private long itsContentLength;
	
	/**
	 * Count number of bytes since last update message was sent
	 */
	private long itsByteCount;
	
	/**
	 * The listener that will receive progress update messages
	 */
	private final ProgressListener itsListener;
	
	/**
	 * The number of bytes transferred between each update message.
	 */
	private static final int UPDATE_SIZE = 10000;
	
	/**
	 * Constructor. Hand all stream processing off to the superclass but track the content length and the 
	 * listener to use.
	 * @param theOutStream the stream to write the content to
	 * @param theContentLength the number of bytes that will be transferred
	 * @param theListener the listener to which update messages should be sent.
	 */
	public CountingOutputStream(OutputStream theOutStream, long theContentLength, ProgressListener theListener) 
	{
		// TODO Auto-generated constructor stub
		super(theOutStream);
		itsTransferredCount = 0;
		itsContentLength = theContentLength;
		itsByteCount = 0;
		itsListener = theListener;
	}

	/**
	 * Extend the write method only to track each byte as it is written.
	 * Hand write implementation to the superclass and then update counters and any listeners via the #update method
	 * @param theInt the byte to write. 
	 */
	@Override
	public void write(int theInt) throws IOException 
	{
		// TODO Auto-generated method stub
		super.write(theInt);
		itsTransferredCount++;
		itsByteCount++;
		update();		
	}
		
	/**
	 * Update the registered listener with progress for the transfer of the compressed XML.
	 * Only send updates every #UPDATE_SIZE bytes to improve performance.
	 * Progress is calculated as (itsTransferredCount / itsContentLength) * 100
	 */
	protected void update()
	{	
		// Only report if we're at the end or every 1000 bytes.
		if((itsTransferredCount == itsContentLength) || (itsByteCount > UPDATE_SIZE))
		{
			float aProgressPercent = ((float)itsTransferredCount / (float)itsContentLength ) * 100;
			if(itsListener != null)
			{
				itsListener.updateProgress("", (int)aProgressPercent);
			}
			itsByteCount = 0;
		}		
	}

}
