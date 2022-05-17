/**
 * Copyright (c)2006-2016 Enterprise Architecture Solutions Ltd.
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
 * 10.11.2006	JWC	1st coding.
 * 17.11.2006	JWC	Added support for HTTP basic authentication
 * 08.12.2008	JWC	Refactored package name
 * 30.05.2009	JWC	Changed HTTP response status codes to use standard HTTP codes.
 * 					Fix for problem when using Apache webserver in front of Tomcat.
 * 22.10.2009	JWC	Migrate to v4 of Apache Commons HTTPClient package.
 * 23.10.2009	JWC	Compress the XML before sending it to the ReportServlet.
 * 24.11.2009	JWC	Send GraphWidget images to the image receiver servlet in Viewer.
 * 02.11.2010	JWC	Fixed empty image bug and included XMLRenderer v1.2
 * 23.05.2013	JWC Added listeners to track transfer progress
 * 15.07.2013	JWC Completed the Form-login code.
 * 04.03.2016	JWC	Tuned behaviour of sending 1000's images
 * 17.08.2016	JWC	Updated form-based login to reflect change in Tomcat 8+
 * 12.09.2016	JWC	More edits to form-based login processing for Tomcat 8+
 */
package com.enterprise_architecture.essential.widgets;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.widget.ClsWidget;
import edu.stanford.smi.protege.widget.SlotWidget;

import com.nwoods.jgo.layout.JGoLayeredDigraphAutoLayout;

/**
 * Encapsulate the actual Service request using HTTP.
 * <br>
 * Used by the ReportServiceClient class to make the request in a worker
 * thread.<br/>
 * From version 3.0, support for HTTP form-based login is provided.
 * As form-login is a very human-user oriented approach, to support implementation,
 * a login failure page, must contain the phrase "Login - Error", e.g. even in a comment.
 * <br/> 
 * Similarly, in order to recognise a Form Login page, the phrase "Login" must appear in the
 * page for the login form. 
 * These phrases are controlled by the itsLoginErrorPhrase and itsFormLoginPhrase attributes that can be controlled by a
 * property in the 'host' application.
 * @author Jonathan W. Carter <jonathan.carter@e-asolutions.com>
 * @version 3.1	- Tuned rendering and transmission of images for large sets<br/>
 * @version 3.0 - Added progress tracking and switches on sending of images<br/>
 * @version 2.3 - Fixed empty image bug and included timestamp in XML snapshot (02.11.2010)<br/>
 * @version 2.2 - Added sending of GraphWidget images to Viewer.<br/>
 * @since v1.0
 *
 */
public class HttpReportServiceClient 
{
	private static final int MIN_IMAGE_DIMS = 1;
	private static final int SC_OK = 200;
	public static final int SC_INTERNAL_SERVER_ERROR = 500;
	public static final int SC_BAD_REQUEST = 400;

	private String itsURL;
	private String itsReportXML;
	private String itsUID;
	private String itsPassword;
	private int itsReturnCode;
	
	// 22.10.2009 JWC - Add new member for compression reportXML to prevent passing
	// 					large amounts of data on the stack.
	private ByteArrayEntity itsCompressedXML;
	
	// 19.11.2009 JWC - Added to send the GraphWidget images.
	private String itsImagesURL;
	private String itsAutoLayout;
	private KnowledgeBase itsKBRef;
	private static final String IMAGE_TYPE = "png";
	private static final String IMAGE_PARAM = "image";
	
	// 23.05.2013 JWC - Added listener for progress updates
	private ProgressListener itsListener = null;
	
	// 13.06.2013 JWC - Added to support proxies
	private String itsProxyHost = "";
	private int itsProxyPort = 8080;
	private String itsFormLoginUsernameField = "j_username";
	private String itsFormLoginPasswordField = "j_password";
	private String itsFormLoginAction = "j_security_check";
	private String itsFormLoginPhrase = "Essential Publishing Login Form";
	private String itsLoginErrorPhrase = "Essential Publishing Login Error Page";
	private String itsLogin403Phrase = "Essential Publishing 403 Error Page";
	protected int itsImageSentCount = 0;

	/**
	 * Default constructor - initialise everything.
	 * 
	 */
	public HttpReportServiceClient()
	{
		itsURL = "";
		itsReportXML = "";
		itsImagesURL = "";
		itsAutoLayout = "";
		itsKBRef = null;
		initialiseCompressedXML();
		initialiseCredentials();
		itsListener = null;
		//itsHttpClient = new DefaultHttpClient();
	}
	
	/**
	 * Simple constructor - initialise everything.
	 * @param theListener the object listening for progress update messages
	 */
	public HttpReportServiceClient(ProgressListener theListener)
	{
		itsURL = "";
		itsReportXML = "";
		itsImagesURL = "";
		itsAutoLayout = "";
		itsKBRef = null;
		initialiseCompressedXML();
		initialiseCredentials();
		itsListener = theListener;
		//itsHttpClient = new DefaultHttpClient();
	}
	
	/**
	 * Construct and intialise with specified parameters
	 * @param theURL the URL of the ReportService
	 * @param theXML the XML document containing the knowledgebase
	 * @param theListener the object listening for progress update messages
	 */
	public HttpReportServiceClient(String theURL, String theXML, ProgressListener theListener)
	{
		itsURL = theURL;
		itsReportXML = theXML;
		initialiseCompressedXML();
		initialiseCredentials();
		itsListener = theListener;
		//itsHttpClient = new DefaultHttpClient();
	}
	
	/**
	 * If there are any open HTTP connections open, close them before 
	 * an object of this class is GC-ed.
	 */
	protected void finalize() throws Throwable
	{
		// Ensure that any open Http connections are closed		
		shutdown();
	}
	
	/**
	 * Call this method when finished with the HttpReportServiceClient to close
	 * all http connections.
	 */
	public void shutdown()
	{
		/*// Once we've finished with the HttpReportServiceClient, release all connections.
		if(itsHttpClient.getConnectionManager() != null)
		{
			itsHttpClient.getConnectionManager().shutdown();
		}*/
	}
	
	/**
	 * Send the XML for the report to the reporting service.
	 * Updated to v4 of Apache HTTP Client pack and to add compression - JWC 22.10.2009
	 * @return true if sending the report was successful, false if there was
	 * a problem.
	 */
	public boolean sendReportXML()
	{
		boolean isSuccess = false;
		String aReportServiceURL = itsURL;
		DefaultHttpClient aClient = null;
		HttpResponse aResult = null;
				
		// Create an HTTP connection using this URL
		try
		{
			// Use the member variable
			aClient = new DefaultHttpClient();
			
			// If set, add proxy settings
			if(itsProxyHost.length() > 0)
			{
				HttpHost aProxyServer = new HttpHost(itsProxyHost, itsProxyPort);
				aClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, aProxyServer);
			}
			
			HttpPost aPost = new HttpPost(aReportServiceURL);
			
			// Compress the XML - 23.10.2009 JWC
			compressXML();
			
			// Put it in the post message
			aPost.setEntity(itsCompressedXML);
						
			// Set authentication if required
			setAuthCredentials(aClient, itsURL);
			
			// Test for and if required, do Form login
			boolean isFormLogin = checkForFormLogin(aClient);
			
			// Assume login success unless otherwise informed or do not need form login
			int isLoginSuccess = SC_OK;
			if(isFormLogin)
			{
				//System.out.println("Form Login required");
				isLoginSuccess = doFormLogin(aClient, itsURL);
			}
			else
			{
				// Continue with sending, using the BASIC AUTH or nothing
				//System.out.println("Form Login NOT required");
			}
			
			if(isLoginSuccess != SC_OK)
			{
				isSuccess = false;
				setItsReturnCode(isLoginSuccess);
				sendProgressUpdate("", 100);
			}
			else
			{
				//Update progress
				sendProgressUpdate(EasReportTab.SENDING_XML_MSG, 0);
				
				// execute the request
				aResult = aClient.execute(aPost);
				StatusLine aStatus = aResult.getStatusLine();
				
				// Service returns an HTTP 200 if success
				if(aStatus.getStatusCode() == SC_OK)
				{
					isSuccess = true;
					aResult.getEntity().consumeContent();
					// Instrumentation trace
					//System.out.println("Success from server:");
					// Should be 0 on success.
					//System.out.println("Content length = " + aResult.getEntity().getContentLength());					
				}
				
				else // or 500 on error
				{
					isSuccess = false;
					setItsReturnCode(aStatus.getStatusCode());
					sendProgressUpdate("", 100);
					
					// Leave trace in to aid with form-login troubleshooting
					System.out.println("In failure HTTP code block. Http Code = " + aStatus.getStatusCode());
					System.out.println("Content length = " + aResult.getEntity().getContentLength());
					System.out.println("Response from server= ");
					aResult.getEntity().writeTo(System.out);
					aResult.getEntity().consumeContent();
				}
			}
		}
		catch(Exception anEx)
		{
			isSuccess = false;
			sendProgressUpdate(EasReportTab.SENDING_EXCEPTION, 100);
			setItsReturnCode(0);
			//System.out.println("In exception catch block");			
		}
		finally
		{
			// Release all the resources
			//sendProgressUpdate(EasReportTab.FINISHING_SEND_MSG, 100);
			aClient.getConnectionManager().shutdown();
		}
		return isSuccess;
	}
	
	/**
	 * Send the set of images, one for each GraphWidget, to the reporting environment.
	 * A separate service is used to receive these (separate to the report XML service).
	 * 
	 * @return true if sending all the images was a success, false if there was a problem.
	 * @since version 2.2
	 */
	public boolean sendImages()
	{
		boolean isSuccess = false;
		int aGraphWCount = 0;
		boolean isSendSuccess = false;
		DefaultHttpClient aClient = null;
		
		itsImageSentCount = 0;
		if(itsKBRef != null)
		{			
			try
			{
				// Update progress
				sendProgressUpdate(EasReportTab.SENDING_IMAGES_MSG, 0);
				
				aClient = new DefaultHttpClient();
				
				// If set, add proxy settings
				if(itsProxyHost.length() > 0)
				{
					HttpHost aProxyServer = new HttpHost(itsProxyHost, itsProxyPort);
					aClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, aProxyServer);
				}
				
				// Set authentication if required
				setAuthCredentials(aClient, itsImagesURL);
				
				// Check login and fail immediately if wrong.
				// Test for and if required, do Form login
				boolean isFormLogin = checkForFormLogin(aClient);
				
				// Assume login success unless otherwise informed or do not need form login
				int isLoginSuccess = SC_OK;
				if(isFormLogin)
				{
					//System.out.println("Images: Form Login required");
					isLoginSuccess = doFormLogin(aClient, itsImagesURL);
				}
				else
				{
					// Continue with sending, using the BASIC AUTH or nothing
					//System.out.println("Images: Form Login NOT required");
				}
				
				if(isLoginSuccess != SC_OK)
				{
					isSuccess = false;
					setItsReturnCode(isLoginSuccess);
					sendProgressUpdate("", 100);
				}
				else
				{
					sendProgressUpdate(EasReportTab.SENDING_IMAGES_UPDATE, 0);
					
					// Next find all the classes that have a customised form				
					Collection<Cls> aClassList = itsKBRef.getCls("EA_Class").getSubclasses();
					Iterator<Cls> aClassListIt = aClassList.iterator();
					float aClassCount = (float)aClassList.size();
					float aProgressCount = 0;
					while(aClassListIt.hasNext())
					{
						Cls aClass = aClassListIt.next();
						aProgressCount++;
						ClsWidget aFormW = itsKBRef.getProject().getDesignTimeClsWidget(aClass);
						Collection<Slot> aSlotList = aClass.getTemplateSlots();
					    
						// Now look at each slotwidget, and if it's a graph get the image.
						Iterator<Slot> aSlotListIt = aSlotList.iterator();
						while(aSlotListIt.hasNext())
						{
							Slot aSlot = aSlotListIt.next();
					     	SlotWidget aSlotW = aFormW.getSlotWidget(aSlot);					     	
					     	if(aSlotW != null)
					     	{
						     	String aType = aSlotW.getDescriptor().getWidgetClassName();
													
								if(aType.equals("edu.stanford.smi.protegex.widget.graph.GraphWidget"))
								{
									aGraphWCount++;
									isSendSuccess = createAndSendImage(aClass, aSlot, aClient);														
								}
					     	}					    					  
						}
						// Update Image sending progress
						float aProgress = (aProgressCount / aClassCount) * 100; 
						sendProgressUpdate("", (int)aProgress);
												
					}
				}
			}
			catch(Exception anEx)
			{
				isSuccess = false;
				sendProgressUpdate(EasReportTab.SENDING_EXCEPTION, 100);
				setItsReturnCode(0);
				//System.out.println("Images: In exception catch block");			
			}
			finally
			{
				// Release all the resources
				//sendProgressUpdate(EasReportTab.FINISHING_SEND_MSG, 100);
				aClient.getConnectionManager().shutdown();
			}
		
		}
		// If no graph widgets are found, success
		if(aGraphWCount == 0)
		{
			isSuccess = true;
		}
		else
		{
			isSuccess = isSendSuccess;
		}
		
		// Inform how many images were sent
		String aSentImagesMsg = EasReportTab.SENDING_IMAGES_DONE + itsImageSentCount;
		sendProgressUpdate(aSentImagesMsg, 100);
		return isSuccess;
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
	}
	
	/**
	 * Set the user authentication credentials if they have been set.
	 * This is only actioned if credentials have been specified.
	 * Updated to v4 of Apache HTTP Client pack - JWC 22.10.2009<br>
	 * Updated to be reusable by image sender - JWC 19.11.2009
	 * @param theClient the HTTP client object that is to be used.
	 * @since version 2.2 in this form.
	 */
	private void setAuthCredentials(DefaultHttpClient theClient, String theURL)
	{
		if(itsUID != "")
		{
			// Work out the target host
			try
			{
				URL aURL = new URL(theURL);
				String aHost = aURL.getHost();
				Credentials aCredential = new UsernamePasswordCredentials(itsUID, itsPassword);
				AuthScope anAuthScope = new AuthScope(aHost, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
				
				theClient.getCredentialsProvider().setCredentials(anAuthScope, aCredential);
			}
			catch (MalformedURLException aMalURL)
			{
				System.out.println("Exception during credential setting. Invalid URL specified.");
				aMalURL.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Compress the report XML using GZIP and send the results to the
	 * itsCompressedXML variable to reduce the load on the stack from passing
	 * large data entities about.
	 * itsCompressedXML is a ByteArrayEntity ready to be loaded into the HttpPost 
	 * body as binary/octet type.
	 * @since version 2.1
	 * <br>Author Jonathan Carter
	 */
	private void compressXML() throws IOException
	{
		ByteArrayOutputStream aByteStreamOut = null;
		ObjectOutputStream anObjectOut = null;
		GZIPOutputStream aCompressFilter = null;
		
		try
		{
			aByteStreamOut = new ByteArrayOutputStream();
			aCompressFilter = new GZIPOutputStream(aByteStreamOut);
			anObjectOut = new ObjectOutputStream(aCompressFilter);
			anObjectOut.writeObject(itsReportXML);
			anObjectOut.flush();
			aCompressFilter.finish();
					
			// We have the compressed XML in aByteStreamOut, so get its contents
			// Use the CountingByteArrayEntity to track progress on sending.
			itsCompressedXML = new CountingByteArrayEntity(aByteStreamOut.toByteArray(), itsListener);
			itsCompressedXML.setContentType("binary/octet-stream");			
		}
		catch(IOException ioEx)
		{
			System.out.println("IOException while compressing report XML.");
			ioEx.printStackTrace();
			throw(ioEx);
		}
		finally // release all the resources
		{
			if(anObjectOut != null)
			{
				anObjectOut.close();
			}
			if(aCompressFilter != null)
			{
				aCompressFilter.close();
			}
			if(aByteStreamOut != null)
			{
				aByteStreamOut.close();
			}		
		}
		
	}
	
	
	/**
	 * Create the image for all instances of the specified GraphWidget and send them to the reporting
	 * service. 27.10.2010 JWC - Fixed empty image bug.
	 * @param theClass the class that has a slot that uses a GraphWidget
	 * @param theSlot the slot that uses a GraphWidget
	 * @param theHttpClient the Http connection that should be used to send the created image @since 3.0
	 * @return true on successfully creating and sending the image, false otherwise.
	 * @since version 2.2
	 */
	private boolean createAndSendImage(Cls theClass, Slot theSlot, DefaultHttpClient theHttpClient)
	{
		boolean isSuccess = true; // unless we hear otherwise	    
	    Iterator<Instance> anInstanceListIt = theClass.getDirectInstances().iterator();
	    while(anInstanceListIt.hasNext())
	    {	    
	    	Instance anInstance = anInstanceListIt.next();
	    	
	    	// Get a runtime Form/Class Widget
	    	ClsWidget aFormWidget = itsKBRef.getProject().createRuntimeClsWidget(anInstance);

	    	// Initialize the FormWidget
	    	aFormWidget.initialize();

			// Get the slot widget, which we know must be a graph widget
	    	SlotWidget aGraphW = aFormWidget.getSlotWidget(theSlot);
	    	
			// Initialize the SlotWidget
			aGraphW.initialize();
			
			BufferedImage anImage = null;
			// Invoke the methods on the GraphWidget using Reflection because of 
			// classpath and classloader issues.
			try
			{
				// Set the graph document
				// aGraphW.addNotify();
				Class aGraphWClass = aGraphW.getClass();
				Method addNotify = aGraphWClass.getMethod("addNotify", null);
				addNotify.invoke(aGraphW, null);		
			
				// Optional - before get image, do a layout if a layout is specified
				if(!itsAutoLayout.equals(""))
				{
					Class[] aParamTypes = new Class[1];
					aParamTypes[0] = Integer.TYPE;
					Object[] anArgList = new Object[1];
					
					Method handleLayout = aGraphWClass.getMethod("handlePerformAutomaticLayout", aParamTypes);
					
					if(itsAutoLayout.equals(ImageAutoLayoutPanel.LAYOUT_DOWN))
					{
						//aGraphW.handlePerformAutomaticLayout(JGoLayeredDigraphAutoLayout.LD_DIRECTION_DOWN);
						anArgList[0] = new Integer(JGoLayeredDigraphAutoLayout.LD_DIRECTION_DOWN);
						handleLayout.invoke(aGraphW, anArgList);
					}
					else if(itsAutoLayout.equals(ImageAutoLayoutPanel.LAYOUT_RIGHT))
					{
						//aGraphW.handlePerformAutomaticLayout(JGoLayeredDigraphAutoLayout.LD_DIRECTION_RIGHT);
						anArgList[0] = new Integer(JGoLayeredDigraphAutoLayout.LD_DIRECTION_RIGHT);
						handleLayout.invoke(aGraphW, anArgList);						
					}			
				}
	
				// Get the Image
				//BufferedImage anImage = aGraphW.getView().getImage();
				Method getView = aGraphWClass.getMethod("getView", null);
				Object aGraphView = getView.invoke(aGraphW, null);
				Method getImage = aGraphView.getClass().getMethod("getImage", null);
				
				// 27.10.2010 JWC handle situation where image is empty - height and width <= 0
				// If height or width <= 0, create an empty BufferedImage and then send it.
				Method getDocument = aGraphWClass.getMethod("getDocument", null);
				Object aDoc = getDocument.invoke(aGraphW, null);
				Method getDocSize = aDoc.getClass().getMethod("getDocumentSize", null);
				Dimension aSize = (Dimension)getDocSize.invoke(aDoc, null);

				if((aSize.width <= 0) || (aSize.height <= 0))
				{
					anImage = new BufferedImage(MIN_IMAGE_DIMS, MIN_IMAGE_DIMS, BufferedImage.TYPE_INT_RGB);
				}
				else
				{
					anImage = (BufferedImage)getImage.invoke(aGraphView, null);
				}			
				// Send the image
				isSuccess = sendGraphImage(anImage, anInstance.getFrameID().getName(), theHttpClient);
			}
			catch (Exception ex)
			{
				// Handle any exceptions from calling the methods. Indicate
				// failure and print the stack trace for debug.
				isSuccess = false;
				ex.printStackTrace();				
			}	
			
			// tidy up
			// DEBUG
			anImage.flush();
			aGraphW.dispose();
			aFormWidget.dispose();			
	    }
		
		return isSuccess;
	}
	    
	/**
	 * Send the supplied BufferedImage to the reporting environment, Essential Viewer, as 
	 * a Multi-part MIME so that the instance name can be send with the compressed PNG image
	 * as a tuple.
	 * @param theImage the image to send to the service
	 * @param theFilename the name of the image file that should be created on the reporting service
	 * * @param theHttpClient the Http connection that should be used to send the created image @since 3.0
	 * @return true if successfully sent the image, false otherwise
	 * @since version 2.2
	 */
	private boolean sendGraphImage(BufferedImage theImage, String theFilename, DefaultHttpClient theHttpClient)
	{
		boolean isSuccess = false;
		String aURL = itsImagesURL;
		String aMIMEType = "image/" + IMAGE_TYPE;
		String aFilename = theFilename + "." + IMAGE_TYPE;
		
		// Create an HTTP connection using this URL
		try
		{		
			HttpPost aPost = new HttpPost(aURL);
						
			// Create a message part with the image in it
			// Render the image to the IMAGE_TYPE
			ByteArrayOutputStream anImageOut = new ByteArrayOutputStream();
			ImageIO.write(theImage, IMAGE_TYPE, anImageOut);
			
            // Load the image into the message body
			ByteArrayBody aBody = new ByteArrayBody(anImageOut.toByteArray(), aMIMEType, aFilename);
			
			// Create the entity
			MultipartEntity aRequestContent = new MultipartEntity();
			aRequestContent.addPart(IMAGE_PARAM, aBody);	
			aPost.setEntity(aRequestContent);
			
			// execute the request
			HttpResponse aResult = theHttpClient.execute(aPost);			
			StatusLine aStatus = aResult.getStatusLine();
			aResult.getEntity().consumeContent();
			
			// Service returns an HTTP 200 if success
			if(aStatus.getStatusCode() == SC_OK)
			{
				isSuccess = true;
				itsImageSentCount++;
			}
			else // or 400 / 500 on error
			{
				isSuccess = false;
				setItsReturnCode(aStatus.getStatusCode());
			}			
		}
		catch(Exception anEx)
		{
			isSuccess = false;
			setItsReturnCode(SC_BAD_REQUEST);
			
			// Report the actual error to the console to aid troubleshooting.
			System.out.println("Exception during send: " + anEx.toString());
			System.out.println("Cause: " + anEx.getCause().getLocalizedMessage());
		}
		finally
		{
			// Release all the resources
			//theHttpClient.getConnectionManager().shutdown();
		}
		
		return isSuccess;
	}
	
	
	/**
	 * Initialise the authentication credentials
	 *
	 */
	private void initialiseCredentials()
	{
		itsUID = "";
		itsPassword ="";
		itsReturnCode = 0;
	}
	
	/**
	 * Initialise the Byte Array that contains the compressed XML
	 * @since version 2.1
	 * <br>Author: Jonathan Carter
	 */
	private void initialiseCompressedXML()
	{
		byte[] anEmptyArray = new byte[0]; 
		itsCompressedXML = new ByteArrayEntity(anEmptyArray);
	}

	/**
	 * Send progress messages to the listener, with a check that there is a listener registered.
	 * @param theMessage the message to send
	 * @param theProgressPercentage the percentage complete.
	 */
	protected void sendProgressUpdate(String theMessage, int theProgressPercentage)
	{
		if (itsListener != null)
		{
			itsListener.updateProgress(theMessage, theProgressPercentage);
		}
	}
	
	/**
	 * Perform a login to the Report Service when the app server is set up to use
	 * form login.
	 * 
	 * @param theHttpClient the Client object that will be making the POST request
	 * @param theURL the URL of the Report Service to login to 
	 * @return TRUE if logged in, false otherwise.
	 * @since version 3.0
	 */
	protected int doFormLogin(DefaultHttpClient theHttpClient, String theURL) throws Exception
	{
		int aReturnCode = EasReportTab.BAD_PASSWORD;
				
		// Perfom a GET on the specified string to get the login form
		HttpGet anHttpGet = new HttpGet(itsURL);

        HttpResponse aResponse = theHttpClient.execute(anHttpGet);
        HttpEntity anEntity = aResponse.getEntity();

        // TRACE CODE INSTRUMENTATION FOR NOW
        //System.out.println("Login form get: " + aResponse.getStatusLine());
        
        // Process it and make sure the cookie is set.		
        if (anEntity != null) 
        {
        	//System.out.println("Entity returned:");
        	//anEntity.writeTo(System.out);
            anEntity.consumeContent();
        }
        
        // Now build the form response
        String aPreActionString = "";
        if(theURL.endsWith("/"))
        {
        	aPreActionString = theURL;
        }
        else
        {
        	aPreActionString = theURL + "/";
        }
        String aLoginURL = aPreActionString + itsFormLoginAction;
		HttpPost anHttpPost = new HttpPost(aLoginURL);

        List <NameValuePair> aNameValuePairList = new ArrayList <NameValuePair>();
        aNameValuePairList.add(new BasicNameValuePair(itsFormLoginUsernameField, itsUID));
        aNameValuePairList.add(new BasicNameValuePair(itsFormLoginPasswordField, itsPassword));

        anHttpPost.setEntity(new UrlEncodedFormEntity(aNameValuePairList, HTTP.UTF_8));
     
        // Post the login
        aResponse = theHttpClient.execute(anHttpPost);
        int aResponseCode = aResponse.getStatusLine().getStatusCode();        
        anEntity = aResponse.getEntity();

		
		// Check the response
        // TRACE INSTRUMENTATION
        //System.out.println("Login form post: " + aResponse.getStatusLine());
        if (anEntity != null) 
        {
            // Assume success is content length = 0
            //anEntity.writeTo(System.out);
        	ByteArrayOutputStream aByteArray = new ByteArrayOutputStream();
        	String anEncoding = "UTF-8";
        	anEntity.writeTo(aByteArray);
        	String aResponsePage = aByteArray.toString(anEncoding);
        	anEntity.consumeContent();
            
        	// Look for the error pages - bad password and access forbidden
            if(aResponsePage.contains(itsLoginErrorPhrase) || aResponseCode == EasReportTab.BAD_PASSWORD)
        	{
        		aReturnCode = EasReportTab.BAD_PASSWORD;
        		System.out.println("Login failed - bad user name / password");
        		return aReturnCode;
        	}
            else if(aResponsePage.contains(itsLogin403Phrase) || aResponseCode == EasReportTab.ACCESS_FORBIDDEN)
            {
            	aReturnCode = EasReportTab.ACCESS_FORBIDDEN;
            	System.out.println("Login failed - forbidden");
            	return aReturnCode;
            }
        }
        
        // Get the status code. If it's > 199 and < 400 then successful login
        System.out.println("Form login response code: " + aResponseCode);
        System.out.println("Form login entity content length: " + anEntity.getContentLength());
        
        // If we're here and the status code is 200-399 then we've authenticated. Just test status code
        if(aResponseCode > 199 && aResponseCode < 400)
        {
        	// Consume any additional pages - get past 302 etc....
        	aReturnCode = SC_OK;
        	System.out.println("Login Success! -- trying additional GET. This response was HTTP " + aResponseCode);
        	
        	// Try an additional GET to pass the 302, as it's going to be the BAD URL from server
        	aResponse = theHttpClient.execute(anHttpGet);
        	System.out.println("Return from following 302 etc. Response was HTTP " + aResponse.getStatusLine().getStatusCode());
        	anEntity = aResponse.getEntity();
        	aResponse.getEntity().consumeContent();
        }
        
//        if(anEntity.getContentLength() <= 0)
//        {
//        	aReturnCode = SC_OK;
//        	//System.out.println("Login Success! -- trying additional GET");
//        	
//        	// Try an additional GET to pass the 302, as it's going to be the BAD URL from server
//        	aResponse = theHttpClient.execute(anHttpGet);
//        	anEntity = aResponse.getEntity();
//        	aResponse.getEntity().consumeContent();
//        }
	
		return aReturnCode;
	}
	
	/**
	 * Check to see if the EasReportService needs a form login
	 * @param theHttpClient
	 * @return
	 * @throws Exception
	 */
	protected boolean checkForFormLogin(DefaultHttpClient theHttpClient) throws Exception
	{
		boolean isFormLoginRequired = false;
		
		sendProgressUpdate(EasReportTab.CHECKING_LOGIN_MSG, 0);
		
		// Build a GET request and check returned content for the form login phrase.
		// Perfom a GET on the specified string to get the login form
		HttpGet anHttpGet = new HttpGet(itsURL);

        HttpResponse aResponse = theHttpClient.execute(anHttpGet);
        HttpEntity anEntity = aResponse.getEntity();
        
        ByteArrayOutputStream aByteArray = new ByteArrayOutputStream();
    	String anEncoding = "UTF-8"; 
    	anEntity.writeTo(aByteArray);
    	String aResponsePage = aByteArray.toString(anEncoding);
    	anEntity.consumeContent();
        
        if(aResponsePage.contains(itsFormLoginPhrase))
    	{
    		isFormLoginRequired = true;
    	}
        
        return isFormLoginRequired;
	}
	
	/**
	 * @return the itsImagesURL
	 */
	public String getItsImagesURL() {
		return itsImagesURL;
	}

	/**
	 * @param itsImagesURL the itsImagesURL to set
	 */
	public void setItsImagesURL(String itsImagesURL) {
		this.itsImagesURL = itsImagesURL;
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
	 * @return the itsFormLoginAction
	 */
	public String getItsFormLoginAction() {
		return itsFormLoginAction;
	}

	/**
	 * @param itsFormLoginAction the itsFormLoginAction to set
	 */
	public void setItsFormLoginAction(String itsFormLoginAction) {
		this.itsFormLoginAction = itsFormLoginAction;
	}

	/**
	 * @return the itsLoginErrorPhrase
	 */
	public String getItsLoginErrorPhrase() {
		return itsLoginErrorPhrase;
	}

	/**
	 * @param itsLoginErrorPhrase the itsLoginErrorPhrase to set
	 */
	public void setItsLoginErrorPhrase(String itsLoginErrorPhrase) {
		this.itsLoginErrorPhrase = itsLoginErrorPhrase;
	}

	/**
	 * @return the itsFormLoginPhrase
	 */
	public String getItsFormLoginPhrase() {
		return itsFormLoginPhrase;
	}

	/**
	 * @param itsFormLoginPhrase the itsFormLoginPhrase to set
	 */
	public void setItsFormLoginPhrase(String itsFormLoginPhrase) {
		this.itsFormLoginPhrase = itsFormLoginPhrase;
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
	public int getItsProxyPort() {
		return itsProxyPort;
	}

	/**
	 * @param itsProxyPort the itsProxyPort to set
	 */
	public void setItsProxyPort(int itsProxyPort) {
		this.itsProxyPort = itsProxyPort;
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
	
}
