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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.entity.ByteArrayEntity;

/**
 * Extended ByteArrayEntity for the HttpClient that uses a CountingOutputStream to track progress 
 * of sending the ByteArray to the Essential Viewer Report Service.
 * @author Jonathan Carter
 * @version 1
 * @see com.enterprise_architecture.essential.widgets.CountingOutputStream CountingOutputStream
 */
public class CountingByteArrayEntity extends ByteArrayEntity 
{
	/**
	 * Listener for progress updates
	 */
	private ProgressListener itsListener;
	
	/**
	 * Default constructor that includes the listener that requires sending progress updates.
	 * @param theByteArray the compressed XML repository snapshot
	 * @param theListener the listener that is waiting for update events.
	 */
	public CountingByteArrayEntity(byte[] theByteArray, ProgressListener theListener) 
	{
		super(theByteArray);
		itsListener = theListener;		
	}

	/**
	 * Override the method to write to the specified output stream. Create an instance
	 * of the CountingOutputStream and send it a reference to the progress listener
	 */
	@Override
	public void writeTo(OutputStream theOutStream) throws IOException
	{
		super.writeTo(new CountingOutputStream(theOutStream, getContentLength(), itsListener));
	}
}
