/**
 * Copyright (c)2006-2013 Enterprise Architecture Solutions Ltd.
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
 * 07.11.2006	JWC	1st coding.
 * 16.11.2006 	JWC Added HTTP authentication.
 * 08.12.2008	JWC	Refactored package name.
 * 19.11.2009	JWC	Added sending of GraphWidget image snapshots to Viewer
 * 16.05.2013	JWC Commented out the sending of GraphWidget images to Viewer.
 * 					This will be replaced by a UX control to switch on/off in next version
 * 23.05.2013	JWC Added improved progress tracking
 * 13.06.2013	JWC Add controls to send graph images
 */
package com.enterprise_architecture.essential.widgets;

import edu.stanford.smi.protege.model.KnowledgeBase;

/**
 * Client for the EAS Architecture Reporting Service.
 * Uses a SwingWorker thread to make the request in a separate thread
 * whilst allowing the User Interface tab, EASReportTab to continue.
 * <br/>
 * Used by the EasReportTab
 * @version 2.0 - Added progress tracking via a listener pattern<br/>
 * @version 1.3 - Remove sending of graph images to Viewer<br/>
 * @version 1.2 - Send GraphWidget images to Viewer
 * @since v1.0
 * @author Jonathan W. Carter
 * @see com.enterprise_architecture.essential.widgets.EasReportTab EasReportTab
 * 
 */
public class ReportServiceClient 
{
	private String itsURL;
	private String itsReportXML;
	private String itsUID;
	private String itsPassword;
	private boolean itIsFinished;
	private boolean itIsSuccess;
	private int itsReturnCode;
	
	// 19.11.2009 JWC Auto-layout and URL for images receiver
	private String itsAutoLayout;
	private String itsImageURL;
	private String itsImageURLSuffix;
	private KnowledgeBase itsKBRef;
	
	// 23.05.2013 JWC Listener for progress update messages
	private ProgressListener itsListener = null;
	
	// 13.06.2013 JWC Switch for the graph images
	private boolean itIsSendingImages = true;
	private String itsProxyHost = "";
	private String itsProxyPort = "";

	/**
	 * Enquire whether the ReportServiceClient will send the graph images or not
	 * @return the itIsSendingImages
	 */
	public boolean isItIsSendingImages() {
		return itIsSendingImages;
	}

	/**
	 * Control the ReportServiceClient in terms of whether it is sending graph images
	 * to the Essential Viewer ReportService.
	 * @param itIsSendingImages the itIsSendingImages to set
	 */
	public void setItIsSendingImages(boolean itIsSendingImages) {
		this.itIsSendingImages = itIsSendingImages;
	}

	/**
	 * Default constructed.
	 * Everything is initialised.
	 * @param theListener the EasReportTab object listening for progress update messages
	 *
	 */
	public ReportServiceClient(ProgressListener theListener)
	{
		itsURL = "";
		itsReportXML = "";
		itsUID = "";
		itsPassword = "";
		itIsFinished = false;
		itIsSuccess = false;
		itsReturnCode = 0;
		
		// 19.11.2009 JWC
		itsAutoLayout = "";
		itsImageURL = "";
		itsImageURLSuffix = "";
		
		// 23.05.2013 JWC
		itsListener = theListener;
	}
	
	/**
	 * Constructor setting URL and report XML.
	 * @param theURL the URL of the EasReportService
	 * @param theReportXML the XML document from the Protege repository
	 * @param theListener the EasReportTab object listening for progress update messages
	 */
	public ReportServiceClient(String theURL, String theReportXML, ProgressListener theListener)
	{
		itsURL = theURL;
		itsReportXML = theReportXML;
		itsUID = "";
		itsPassword = "";
		itIsFinished = false;
		itIsSuccess = false;
		itsReturnCode = 0;

		// 19.11.2009 JWC
		itsAutoLayout = "";
		itsImageURL = "";
		itsImageURLSuffix = "";
		
		// 23.05.2013 JWC
		itsListener = theListener;
	}

	/**
	 * @return the itIsFinished
	 */
	public boolean isItIsFinished() {
		return itIsFinished;
	}

	/**
	 * @param itIsFinished the itIsFinished to set
	 */
	public void setItIsFinished(boolean itIsFinished) {
		this.itIsFinished = itIsFinished;
	}

	/**
	 * @return the itsReportXML
	 */
	public String getItsReportXML() {
		return itsReportXML;
	}

	/**
	 * @param itsReportXML the itsReportXML to set
	 */
	public void setItsReportXML(String itsReportXML) {
		this.itsReportXML = itsReportXML;
	}

	/**
	 * @return the itsURL
	 */
	public String getItsURL() {
		return itsURL;
	}

	/**
	 * @param itsURL the itsURL to set
	 */
	public void setItsURL(String itsURL) {
		this.itsURL = itsURL;
		
		// 19.11.2009 JWC - Update the Images Servlet URL
		setItsImageURL(itsURL + itsImageURLSuffix);
	}
	
	/**
	 * Start the client invocation of the Service
	 * This starts the SwingWorker thread, connects to the Service and
	 * sends the report XML.
	 */
	public void start()
	{
		// Reset the success and finished flags, cannot assume success
		setItIsSuccess(false);
		setItIsFinished(false);
		
		final SwingWorker aWorker = new SwingWorker()
		{
			public Object construct()
			{
				// Do request here
				HttpReportServiceClient aService = new HttpReportServiceClient(itsListener);
				aService.setItsURL(itsURL);
				aService.setItsReportXML(itsReportXML);
				aService.setItsUID(itsUID);
				aService.setItsPassword(itsPassword);
				boolean isASuccess = aService.sendReportXML();
				
				// 19.11.2009 JWC - Send the images now.
				// 16.05.2013 JWC - version 2.6 Do not send images
				// 13.06.2013 JWC - Version 3.0 Use switch to control
				if(isASuccess && itIsSendingImages)
				{				
					aService.setItsImagesURL(itsImageURL);
					aService.setItsKBRef(itsKBRef);
					aService.setItsAutoLayout(itsAutoLayout);				
					isASuccess = aService.sendImages(); 
				}
				
				setItIsSuccess(isASuccess);
				setItsReturnCode(aService.getItsReturnCode());
				return aService;
			}
			
			public void finished()
			{
				// Finish up...
				itIsFinished = true;
			}
		};
		aWorker.start();
	}

	/**
	 * @return the itIsSuccess
	 */
	public boolean isItIsSuccess() {
		return itIsSuccess;
	}

	/**
	 * @param itIsSuccess the itIsSuccess to set
	 */
	public void setItIsSuccess(boolean itIsSuccess) {
		this.itIsSuccess = itIsSuccess;
	}

	/**
	 * @return the itsPassword
	 */
	public String getItsPassword() {
		return itsPassword;
	}

	/**
	 * @param itsPassword the itsPassword to set
	 */
	public void setItsPassword(String itsPassword) {
		this.itsPassword = itsPassword;
	}

	/**
	 * @return the itsReturnCode
	 */
	public int getItsReturnCode() {
		return itsReturnCode;
	}

	/**
	 * @param itsReturnCode the itsReturnCode to set
	 */
	public void setItsReturnCode(int itsReturnCode) {
		this.itsReturnCode = itsReturnCode;
	}

	/**
	 * @return the itsUID
	 */
	public String getItsUID() {
		return itsUID;
	}

	/**
	 * @param itsUID the itsUID to set
	 */
	public void setItsUID(String itsUID) {
		this.itsUID = itsUID;
	}

	/**
	 * @return the itsAutoLayout
	 */
	public String getItsAutoLayout() {
		return itsAutoLayout;
	}

	/**
	 * @param itsAutoLayout the itsAutoLayout to set
	 */
	public void setItsAutoLayout(String itsAutoLayout) {
		this.itsAutoLayout = itsAutoLayout;
	}

	/**
	 * @return the itsImageURL
	 */
	public String getItsImageURL() {
		return itsImageURL;
	}

	/**
	 * @param itsImageURL the itsImageURL to set
	 */
	public void setItsImageURL(String itsImageURL) {
		this.itsImageURL = itsImageURL;
	}

	/**
	 * @return the itsKBRef
	 */
	public KnowledgeBase getItsKBRef() {
		return itsKBRef;
	}

	/**
	 * @param itsKBRef the itsKBRef to set
	 */
	public void setItsKBRef(KnowledgeBase itsKBRef) {
		this.itsKBRef = itsKBRef;
	}
	
	/**
	 * @return the itsImageURLSuffix
	 */
	public String getItsImageURLSuffix() {
		return itsImageURLSuffix;
	}

	/**
	 * @param itsImageURLSuffix the itsImageURLSuffix to set
	 */
	public void setItsImageURLSuffix(String itsImageURLSuffix) {
		this.itsImageURLSuffix = itsImageURLSuffix;
		setItsImageURL(itsURL + itsImageURLSuffix);
	}

	/**
	 * @return the itsProxyHost
	 */
	public String getItsProxyHost() {
		return itsProxyHost;
	}

	/**
	 * @param itsProxyHost the itsProxyHost to set
	 */
	public void setItsProxyHost(String itsProxyHost) {
		this.itsProxyHost = itsProxyHost;
	}

	/**
	 * @return the itsProxyPort
	 */
	public String getItsProxyPort() {
		return itsProxyPort;
	}

	/**
	 * @param itsProxyPort the itsProxyPort to set
	 */
	public void setItsProxyPort(String itsProxyPort) {
		this.itsProxyPort = itsProxyPort;
	}
}
