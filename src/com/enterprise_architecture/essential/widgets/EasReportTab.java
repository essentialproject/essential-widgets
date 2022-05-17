/**
 * Copyright (c)2006-2020 Enterprise Architecture Solutions Ltd.
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
 * 16.11.2006	JWC	Added support for basic HTTP authentication on the service
 * 12.09.2007	JWC	Refactored name of this class
 * 20.12.2007	JWC	version 1.1. Fixed property file reading problem
 * 15.02.2008	JWC	version 1.2. Re-worked XML rendering of repository snapshot to
 * 					resolve client/server bug in XMLStorer
 * 10.12.2008	JWC	Refactored package name and enhanced report service URL control
 * 30.05.2009	JWC Added additional error codes from ReportService. Fixes issue with using
 * 					Apache webserver in front of Tomcat.
 * 19.11.2009	JWC	Added controls to export images from GraphWidgets
 * 02.11.2010	JWC	Added property to control date/time format of timestamp tag in XML snapshot
 * 16.05.2013	JWC Removed action on select of URL from drop-down. Default /reportService in URL
 * 					so that only the hostname and Viewer environment need be specified. 
 * 23.05.2013	JWC Improved progress tracking for sending XML and images.
 * 29.07.2013	JWC Version 3 with re-worked UI and new capabilities.
 * 01.05.2015	JWC Start to phase out graph widget images
 */
package com.enterprise_architecture.essential.widgets;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.enterprise_architecture.essential.xml.XMLRenderer;
import com.enterprise_architecture.essential.xml.XMLRendererListener;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.plugin.PluginUtilities;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

/**
 * EasReportTab provides a tab for Protege that should be included in projects
 * that wish to export the Knowledgebase containing the EAS meta-model so that it
 * can be sent to the reporting service.<br>
 * The format of the timestamp tag that is added to the XML reporting snapshot (from version 2.4.) can be controlled
 * by the reporttab.xml.datetimeformat property in the reporttab.properties file
 * @author Jonathan W. Carter
 * @version 4.2.1
 * @since v1.0
 * @see com.enterprise_architecture.essential.report.EasReportService EasReportService
 *
 */
public class EasReportTab extends AbstractTabWidget implements ActionListener, ProgressListener, XMLRendererListener
{	
	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 4L;
	private static final String PROPERTY_FILE="reporttab.properties";
	private static final String PLUGIN_CLASS_NAME = "com.enterprise_architecture.essential.widgets.EasReportTab";
	private static final String URL_PROP="reporttab.url.default";
	private static final String URL_HISTORY_FILE="history.xml";
	private static final String TAB_LABEL_PROP="reporttab.tab.label";
	private static final String COLUMN_PROP="reporttab.url.columns";
	private static final String DATE_TIME_FORMAT_PROP = "reporttab.xml.datetimeformat";
	private static final String DEFAULT_URL_SERVICE_PROP = "reporttab.url.defaultservice";
	
	private static final int UID_FIELD_LENGTH = 10;
	public static final int NO_SERVER = 0;
	public static final int INTERNAL_SERVER_ERROR = 500;
	public static final int BAD_URL = 404;
	public static final int BAD_PASSWORD = 401;
	public static final int ACCESS_FORBIDDEN = 403;
	public static final int BAD_REQUEST = 400;
	private static final int STATUS_ROWS = 5;
	
	/**
	 * Default width and height of the panel
	 */
	private static final int DEFAULT_TAB_HEIGHT = 300;
	private static final int DEFAULT_TAB_WIDTH = 500;
	private static final int DEFAULT_FONT_SIZE = 12;
	private static final int DEFAULT_STATUS_ROWS = 15;
	private static final int DEFAULT_STATUS_COLS = 120;
	
	
	public final static int ONE_SECOND = 1000;
		
	// Messages and User Interface content controlled by properties with default values
	private static String READY_STATUS_MESSAGE = "Ready to create and send repository snapshot";
	private static String GETTING_KB_XML_MESSAGE = "Rendering repository...";
	private static String SENDING_TO_REPORT_SERVICE = "Sending repository snapshot...";
	private static String SUCCESS_MESSAGE = "Success. Repository snapshot generated and sent";
	private static String FAILED_GENERATION = "Repository rendering failed";
	private static String FAILED_SEND = "Failed to send snapshot to the Report Service";
	private static String TEXT_FIELD_LABEL = "Report Service URL: ";

	private static String BUTTON_TEXT="Publish Repository";
	private static String USER_NAME_LABEL = "User: ";
	private static String PASSWORD_LABEL = "Password: ";
	private static String USER_PWD_OPT = "User credentials (if required)";
	private static String NO_SERVER_MESSAGE = "No response from the Report Service at this URL.";
	private static String INTERNAL_SERVER_ERROR_MESSAGE = "Essential Viewer ReportService encountered an internal error while receiving your repository snapshot. Contact your system administrator and check Essential Viewer server logs for errors, e.g. memory exceptions.";
	private static String BAD_URL_MESSAGE = "Essential Viewer ReportService could not be found at this URL.";
	private static String BAD_PASSWORD_MESSAGE = "User name and password required; invalid user name and password supplied";
	private static String SERVER_ERROR_MESSAGE = "Server error. The URL appears to OK but sending the repository snapshot could not be completed. Contact your system administrator and check Essential Viewer server logs for errors, e.g. memory exceptions.";
	private static String BAD_REQUEST_MESSAGE = "Client error. An exception occurred while sending graph images to the report service. Check the Protege console logs for more details.";
	private static String EAS_LOGO_PATH = "essentialAMLogo.jpg";
	private static String EAS_LOGO_DESCRIPTION = "Essential Architecture Manager. (c)2006 - 2013 Enterprise Architecture Solutions Ltd.";
	
	// Progress Message defaults
	public static String SENDING_XML_MSG = "Sending XML snapshot...";
	public static String SENDING_IMAGES_MSG = "Sending graphical model images...";
	public static String SENDING_IMAGES_UPDATE = "Sending..";
	public static String SENDING_IMAGES_DONE = "Sent images: ";	
	public static String SENDING_EXCEPTION = "Exception encountered during send";
	public static String FORM_LOGIN_FAILURE = "Exception encountered during form login";
	public static String PROGRESS_PREFIX_STRING_1 = "Step ";
	public static String PROGRESS_PREFIX_STRING_2 = " of ";
	public static String PROGRESS_PREFIX_STRING_3 = ": ";
	public static String FINISHING_SEND_MSG = "Finishing send to server...";
	public static String CHECKING_LOGIN_MSG = "Checking for access...";
	public static String ACCESS_FORBIDDEN_MESSAGE = "Access forbidden. Your user name and password were correct but you do not have access to publish to this URL.";

	// End of Messages and UI content controlled by properties.
	
	// The XML for the report
	private String itsURL = "";
	private String itsTabLabel = "Essential Viewer Tab";
	private int itsURLFieldColumns = 20;
	private String itsReportXML;
	private boolean itIsTaskComplete = false;
	private ReportServiceClient itsServiceClient;
	private LinkedHashSet<String> itsURLHistory = null;
	
	/** 02.11.2010	JWC
	 * DateTime format for timestamp tag in report XML snapshot.
	 * Format must be a java.text.SimpleDateFormat format that produces a valid XSD DateTime value.
	 * Controlled by the reporttab.xml.datetimeformat property
	 * @see java.text.SimpleDateFormat
	 */	
	private String itsTimeStampFormat;
	
	// GUI components
	// 08.12.2008	JWC ComboBox control
	private JComboBox itsURLEntry;
	private JTextArea itsStatus;
	private JProgressBar itsProgress;
	private Timer itsTimer;
	private JButton itsButton;
	private JTextField itsUserName;
	private JPasswordField itsPassword;
	
	/** 
	 * Scroll pane for the output text area 
	 * @since version 3.0
	 */
	private JScrollPane itsScrollPane;
	
	
	/**
	 * Checkbox to ask user whether or not to send the graph images to Viewer.
	 * @since version 3.0
	 */
	private JCheckBox itsSendImage;
	
	// 19.11.2009 JWC Get layout choice history
	private String itsLastLayout = "";
	private boolean isMultiUser = false;
	private static final String IMAGES_URL = "reporttab.images.url";
	private String itsImagesURLSuffix = "";
	private ImageAutoLayoutPanel itsLayout = null;
	private KnowledgeBase itsKB = null;
	
	// 16.05.2013 JWC Define the default Report Service name
	protected static String itsDefaultURLService = "/reportService";
	
	/**
	 * Keep a track of how many steps to the process:
	 * 1. Render XML
	 * 2. Send XML
	 * 3. (Optional) Send graphical model images
	 */
	protected int itsProcessStepNumber = 3;
	protected int itsProcessStepCount = 0;
	
	/**
	 * Initialise the tab widget. Read in properties and the Service URL history.
	 * Create the Swing components and lay them out.
	 * Re-implemented in version 3 to use properties for all GUI text and messages.
	 */
	public void initialize()
	{
		// Intialise with the properties
		Properties aProperties = new Properties();
		
		// Create a service request client
		itsServiceClient = new ReportServiceClient(this);
		itsURLHistory = new LinkedHashSet<String>();
		String anIconURL = "";
		String aSendImageLabel = "";
		String aSendImageDefault = "";
		String anIntroText = "";
		String aSendImageTooltip = "";
		
		int aDefaultTabWidth = DEFAULT_TAB_WIDTH;
		int aDefaultTabHeight = DEFAULT_TAB_HEIGHT;
		int aFontSize = DEFAULT_FONT_SIZE;
		int aStatusRows = DEFAULT_STATUS_ROWS;
		int aStatusColumns = DEFAULT_STATUS_COLS;		
		
		// Read the properties that might have been set
		FileInputStream aPropFile = null;
		try
		{
			/** 20.12.2007 JWC fix file not found issue */
			File aPluginDir = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			File aPropertyFile = new File(aPluginDir, PROPERTY_FILE);
			aPropFile = new FileInputStream(aPropertyFile);
			aProperties.load(aPropFile);
			
			// Read and use the properties
			itsTabLabel = aProperties.getProperty(TAB_LABEL_PROP);
			itsURLFieldColumns = Integer.parseInt(aProperties.getProperty(COLUMN_PROP));
			itsURL = aProperties.getProperty(URL_PROP);
			
			// 19.11.2009 JWC
			itsImagesURLSuffix = aProperties.getProperty(IMAGES_URL);
			
			// 02.11.2010 JWC
			itsTimeStampFormat = aProperties.getProperty(DATE_TIME_FORMAT_PROP);
			
			// 16.05.2013 JWC Define the default Report Service name
			itsDefaultURLService = aProperties.getProperty(DEFAULT_URL_SERVICE_PROP);
			
			anIconURL = aProperties.getProperty("reporttab.tab.icon");	
			aSendImageLabel = aProperties.getProperty("reporttab.ui.sendimagelabel");
			aSendImageDefault = aProperties.getProperty("reporttab.ui.sendimagedefault");
			
			// Read in all the UI and messages from properties
			READY_STATUS_MESSAGE = aProperties.getProperty("reporttab.message.READY_STATUS_MESSAGE");
			GETTING_KB_XML_MESSAGE = aProperties.getProperty("reporttab.message.GETTING_KB_XML_MESSAGE");
			SENDING_TO_REPORT_SERVICE = aProperties.getProperty("reporttab.message.SENDING_TO_REPORT_SERVICE");
			SUCCESS_MESSAGE = aProperties.getProperty("reporttab.message.SUCCESS_MESSAGE");
			FAILED_GENERATION = aProperties.getProperty("reporttab.message.FAILED_GENERATION");
			FAILED_SEND = aProperties.getProperty("reporttab.message.FAILED_SEND");
			TEXT_FIELD_LABEL = aProperties.getProperty("reporttab.ui.TEXT_FIELD_LABEL");
			BUTTON_TEXT = aProperties.getProperty("reporttab.ui.BUTTON_TEXT");
			USER_NAME_LABEL = aProperties.getProperty("reporttab.ui.USER_NAME_LABEL");
			PASSWORD_LABEL = aProperties.getProperty("reporttab.ui.PASSWORD_LABEL");
			USER_PWD_OPT = aProperties.getProperty("reporttab.ui.USER_PWD_OPT");
			anIntroText = aProperties.getProperty("reporttab.ui.introText");
			aSendImageTooltip = aProperties.getProperty("reporttab.ui.sendimagelabel_tooltip");
			aDefaultTabWidth = Integer.parseInt(aProperties.getProperty("reporttab.ui.tabwidth"));
			aDefaultTabHeight = Integer.parseInt(aProperties.getProperty("reporttab.ui.tabheight"));
			aFontSize = Integer.parseInt(aProperties.getProperty("reporttab.ui.fontsize"));
			aStatusRows = Integer.parseInt(aProperties.getProperty("reporttab.ui.statusrows"));
			aStatusColumns = Integer.parseInt(aProperties.getProperty("reporttab.ui.statuscolumns"));
			NO_SERVER_MESSAGE = aProperties.getProperty("reporttab.message.NO_SERVER_MESSAGE");
			INTERNAL_SERVER_ERROR_MESSAGE = aProperties.getProperty("reporttab.message.INTERNAL_SERVER_ERROR_MESSAGE");
			BAD_URL_MESSAGE = aProperties.getProperty("reporttab.message.BAD_URL_MESSAGE");
			BAD_PASSWORD_MESSAGE = aProperties.getProperty("reporttab.message.BAD_PASSWORD_MESSAGE");
			SERVER_ERROR_MESSAGE = aProperties.getProperty("reporttab.message.SERVER_ERROR_MESSAGE");
			BAD_REQUEST_MESSAGE = aProperties.getProperty("reporttab.message.BAD_REQUEST_MESSAGE");
			EAS_LOGO_PATH = aProperties.getProperty("reporttab.ui.EAS_LOGO_PATH");
			EAS_LOGO_DESCRIPTION = aProperties.getProperty("reporttab.ui.EAS_LOGO_DESCRIPTION");
			SENDING_XML_MSG = aProperties.getProperty("reporttab.message.SENDING_XML_MSG");
			SENDING_IMAGES_MSG = aProperties.getProperty("reporttab.message.SENDING_IMAGES_MSG");
			SENDING_IMAGES_UPDATE = aProperties.getProperty("reporttab.message.SENDING_IMAGES_UPDATE");
			SENDING_IMAGES_DONE = aProperties.getProperty("reporttab.message.SENDING_IMAGES_DONE");
			SENDING_EXCEPTION = aProperties.getProperty("reporttab.message.SENDING_EXCEPTION");
			FORM_LOGIN_FAILURE = aProperties.getProperty("reporttab.message.FORM_LOGIN_FAILURE");
			PROGRESS_PREFIX_STRING_1 = aProperties.getProperty("reporttab.message.PROGRESS_PREFIX_STRING_1");
			PROGRESS_PREFIX_STRING_2 = aProperties.getProperty("reporttab.message.PROGRESS_PREFIX_STRING_2");
			PROGRESS_PREFIX_STRING_3 = aProperties.getProperty("reporttab.message.PROGRESS_PREFIX_STRING_3");
			FINISHING_SEND_MSG = aProperties.getProperty("reporttab.message.FINISHING_SEND_MSG");
			CHECKING_LOGIN_MSG = aProperties.getProperty("reporttab.message.CHECKING_LOGIN_MSG");
			ACCESS_FORBIDDEN_MESSAGE = aProperties.getProperty("reporttab.message.ACCESS_FORBIDDEN_MESSAGE");
			
		}
		catch(IOException ioEx)
		{
			/** 20.12.2007 JWC. Add some helpful trace */
			File aCurrentFile = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			System.out.println("Attempting to open file in directory: ");
			System.out.println("User directory: " + System.getProperty("user.dir"));
			System.out.println("Absolute path: " + aCurrentFile.getAbsolutePath());
			try
			{
				System.out.println("Canonical path: " + aCurrentFile.getCanonicalPath());
			}
			catch(IOException aCanonicalEx)
			{
				System.out.println("Couldn't get canonical path.");
			}
			System.out.println("Exception reading properties file:");
			ioEx.printStackTrace();
		}
		finally // Tidy up and close the file
		{
			try
			{
				aPropFile.close();
			}
			catch(Exception ex)
			{
				// just continue
			}
		}
		
		
		// Parse history.xml and load the results into itsURLHistory
		FileInputStream aHistoryFile = null;
		try
		{
			File aPluginDir = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			File aURLFile = new File(aPluginDir, URL_HISTORY_FILE);
			aHistoryFile = new FileInputStream(aURLFile);
			XMLReader aReader = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");

	        // Set the ContentHandler...
	        ServiceHistoryParser aParser = new ServiceHistoryParser();
	        aReader.setContentHandler(aParser);

	        // Parse the file...
	        aReader.parse(new InputSource(aHistoryFile));
	        
	        // Set the history object to what has been parsed.
	        itsURLHistory.addAll(aParser.getItsURLHistory());
	        itsLastLayout = aParser.getItsLastLayout();
		}
		catch(SAXException aParseError)
		{
			System.out.println("Error encountered while parsing URL history file");
			aParseError.printStackTrace();
		}
		catch(FileNotFoundException noFile)
		{
			// If no history.xml found, continue - it will be created later
			// Nothing to do, itsURLHistory has already been initialised
		}
		catch(IOException xmlReaderEx)
		{
			// Handle an IOExceptions from the parse call
			// Any FileNotFound should have been caught by the above catch block.
			File aCurrentFile = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			System.out.println("Attempting to open file in directory: ");
			System.out.println("User directory: " + System.getProperty("user.dir"));
			System.out.println("Absolute path: " + aCurrentFile.getAbsolutePath());
			try
			{
				System.out.println("Canonical path: " + aCurrentFile.getCanonicalPath());
			}
			catch(IOException aCanonicalEx)
			{
				System.out.println("Couldn't get canonical path.");
			}
			System.out.println("Exception reading URL history file:");
			xmlReaderEx.printStackTrace();
		}
		catch(NullPointerException aNullEx)
		{
			File aCurrentFile = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			System.out.println("Attempting to open file in directory: ");
			System.out.println("User directory: " + System.getProperty("user.dir"));
			System.out.println("Absolute path: " + aCurrentFile.getAbsolutePath());
			try
			{
				System.out.println("Canonical path: " + aCurrentFile.getCanonicalPath());
			}
			catch(IOException aCanonicalEx)
			{
				System.out.println("Couldn't get canonical path.");
			}
			System.out.println("Exception reading URL history file:");
			aNullEx.printStackTrace();
		}
		finally
		{
			try
			{
				aHistoryFile.close();
			}
			catch(Exception ex)
			{
				// just continue, it doesn't matter if the file can't be closed.
			}
		}
		
		// Set the tab label
		setLabel(itsTabLabel);
		ImageIcon anIcon = createImageIcon(anIconURL, itsTabLabel);
		setIcon(anIcon);
		
		// Create the GUI components
		createURLField();
		itsButton = createButton();
		JLabel aTextFieldLabel = new JLabel(TEXT_FIELD_LABEL);
		JLabel aUserNameLabel = new JLabel(USER_NAME_LABEL);
		JLabel aPasswordLabel = new JLabel(PASSWORD_LABEL);
		
		// Banner and intro
		Icon aBanner = createImageIcon(EAS_LOGO_PATH, EAS_LOGO_DESCRIPTION);
		JLabel anEASLogo = new JLabel(aBanner);
				
		createStatusTextField();
		createProgressBar();
		createProgressTimer();
		
		// Add checkbox for the sending of images
		boolean isChecked = true;
		if(!aSendImageDefault.equalsIgnoreCase("true"))
		{
			isChecked = false;
		}
		itsSendImage = new JCheckBox(aSendImageLabel, isChecked);	
		
		// Lay out the components

        itsUserName = new JTextField(UID_FIELD_LENGTH);
        itsPassword = new JPasswordField(UID_FIELD_LENGTH);
        
        // add the components to the tab widget
        // Create a panel and layout the controls
        FlowLayout aTabLayout = new FlowLayout(FlowLayout.LEFT);
        setLayout(aTabLayout);
        JPanel aMainPanel = new JPanel();
        
        // Add the main components to the main panel.
        Dimension aDimensions = new Dimension(aDefaultTabWidth, aDefaultTabHeight);
        aMainPanel.setMaximumSize(aDimensions);
        
        BoxLayout aBoxLayout = new BoxLayout(aMainPanel, BoxLayout.PAGE_AXIS);
        aMainPanel.setLayout(aBoxLayout);
        aMainPanel.setAlignmentX(LEFT_ALIGNMENT);        
        Border aLoweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
                
        aMainPanel.setBorder(aLoweredetched);
        aMainPanel.setPreferredSize(aDimensions);
               
        // Add components to the panel		
		// If the logo has been loaded successfully, add it
        if(anEASLogo != null)
        {
        	add(anEASLogo);
        }
        
        // Ensure that the next goes on the line below
        JPanel aSpacer = new JPanel();
        aSpacer.setPreferredSize(new Dimension(300, 1));
        add(aSpacer);
       
        // Header done, now add all the controls       
        // Introduction text to explain the tab.
        Font aTextFont = new Font("SansSerif", (Font.PLAIN), aFontSize);
    	JPanel anIntroTextPanel = new JPanel();
    	anIntroTextPanel.setLayout(new BoxLayout(anIntroTextPanel, BoxLayout.LINE_AXIS));
    	JLabel anIntroTextLabel = new JLabel(anIntroText);
    	anIntroTextLabel.setFont(aTextFont);
    	anIntroTextPanel.setAlignmentX(LEFT_ALIGNMENT);    	
    	anIntroTextPanel.add(anIntroTextLabel);
    	anIntroTextPanel.add(Box.createHorizontalGlue());
    	aMainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    	aMainPanel.add(anIntroTextPanel);    	
    	aMainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    	   	
    	// Viewer URL and user credentials
    	JPanel aViewerPanel = new JPanel();
    	aViewerPanel.setLayout(new BoxLayout(aViewerPanel, BoxLayout.LINE_AXIS));
    	aViewerPanel.setAlignmentX(LEFT_ALIGNMENT);
    	aViewerPanel.add(aTextFieldLabel);
    	aViewerPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    	aViewerPanel.add(itsURLEntry);
    	aViewerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    	aMainPanel.add(aViewerPanel);
    	aMainPanel.add(Box.createRigidArea(new Dimension(0,10)));
    	
    	// Add user credentials panel
    	JPanel aUserPanel = new JPanel();
    	aUserPanel.setLayout(new BoxLayout(aUserPanel, BoxLayout.LINE_AXIS));
    	aUserPanel.setAlignmentX(LEFT_ALIGNMENT);    	
    	aUserPanel.add(aUserNameLabel);
    	aUserPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    	aUserPanel.add(itsUserName);
    	aUserPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    	aUserPanel.add(aPasswordLabel);
    	aUserPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    	aUserPanel.add(itsPassword);
    	itsUserName.setToolTipText(USER_PWD_OPT);
    	aUserNameLabel.setToolTipText(USER_PWD_OPT);
    	itsPassword.setToolTipText(USER_PWD_OPT);
    	aPasswordLabel.setToolTipText(USER_PWD_OPT);
    	aMainPanel.add(aUserPanel);
    	aMainPanel.add(Box.createRigidArea(new Dimension(0,10)));
    	    	
    	// Panel for Progress and publish button
    	JPanel aPublishPanel = new JPanel();
    	aPublishPanel.setLayout(new BoxLayout(aPublishPanel, BoxLayout.LINE_AXIS));
    	aPublishPanel.setAlignmentX(LEFT_ALIGNMENT);
    	aPublishPanel.add(itsProgress);
    	aPublishPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    	aPublishPanel.add(itsSendImage);
    	itsSendImage.setToolTipText(aSendImageTooltip);
    	aPublishPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    	aPublishPanel.add(itsButton);    	
        aMainPanel.add(aPublishPanel);
    	aMainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    	
    	// 19.11.2009 JWC Add the auto layout panel for the GraphWidget images
        // But only if the Protege client is a multi-user client
        isMultiUser = getKnowledgeBase().getProject().isMultiUserClient();  	
        
        if(isMultiUser)
        {
	        itsLayout = new ImageAutoLayoutPanel(itsLastLayout);
	        itsLayout.getItsPanel().setAlignmentX(LEFT_ALIGNMENT);
	        aMainPanel.add(itsLayout.getItsPanel());
        }
                
        // Now add the status panel
    	JPanel aStatusPanel = new JPanel();
    	aStatusPanel.setLayout(new BoxLayout(aStatusPanel, BoxLayout.LINE_AXIS));
    	itsStatus = new JTextArea(READY_STATUS_MESSAGE, aStatusRows, aStatusColumns);
    	itsScrollPane = new JScrollPane(itsStatus);		
    	itsStatus.setAutoscrolls(true);		
    	itsStatus.setLineWrap(true);
    	itsStatus.setWrapStyleWord(true);
    	itsStatus.setFont(aTextFont);
    	itsStatus.setEditable(false);
    	itsStatus.setBorder(aLoweredetched);
    	aStatusPanel.setAlignmentX(LEFT_ALIGNMENT);
    	aStatusPanel.add(itsScrollPane);
    	aMainPanel.add(aStatusPanel);
    	aMainPanel.add(Box.createVerticalGlue());
    	
    	// Add the main panel to the tab    	
    	// Putting it in a scroll pane to support smaller displays
    	JScrollPane aMainScrollPane = new JScrollPane(aMainPanel);
    	aMainScrollPane.setBorder(null);
    	
    	// Add the scroll pane, not the main panel.   	
    	add(aMainScrollPane);
        
		// Initialise the report
		itsReportXML = new String();
	}
	
	/**
	 * Is the project suitable for this tab?
	 * @param project the current project
	 * @param errors a set of errors that were found
	 * @return true if the current project is suitable for this tab.
	 */
	public static boolean isSuitable(Project project, Collection errors) 
	{
		return true;
	}
	
	// Handle the Swing events
	
	/**
	 * Handle the action event.
	 * Generate the XML for the report and send it to the report service
	 * @param theEvent the event that happened
	 */
	public void actionPerformed(ActionEvent theEvent)
	{		
		resetProgress();
		itIsTaskComplete = false;
		// If we're sending images, then count = 3, else it's 2
		if(itsSendImage.isSelected())
		{
			itsProcessStepNumber = 3;
		}
		else
		{
			itsProcessStepNumber = 2;
		}
		
		// Prevent double clicks
		itsButton.setEnabled(false);
		itsButton.paint(itsButton.getGraphics());
		
		// Generate the XML
		// Invoke the report generating actions
		// First, update the GUI widgets...
		itsStatus.setText(GETTING_KB_XML_MESSAGE);
		itsStatus.update(itsStatus.getGraphics());
		itsURLEntry.hidePopup();
		itsURLEntry.update(itsURLEntry.getGraphics());
		
		boolean isReportReady = generateReport();
    	if(isReportReady)
    	{  		
     		updateProgress(SENDING_TO_REPORT_SERVICE);
    		sendReportXML();
    	}
    	else
    	{
    		failedMessage(FAILED_GENERATION);
    		itIsTaskComplete = true;
    		updateProgress(100);
    	}
    	
	}
	
	/**
	 * Overrides the close() method of the AbstractTabWidget to save the URL history 
	 * on close of this tab. Saves to a file, the name of which is defined by the URL_HISTORY_FILE
	 * field located in this classes Plugin Installation Directory.
	 * @since version 2.0
	 */
	public void close()
	{
		String aHistory = renderXML();
		FileWriter aHistoryOut = null;
		try
		{
			File aPluginDir = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			File aHistoryFile = new File(aPluginDir, URL_HISTORY_FILE);
			aHistoryOut = new FileWriter(aHistoryFile);
			aHistoryOut.write(aHistory);
			aHistoryOut.flush();
		}
		catch(IOException ioEx)
		{
			File aCurrentFile = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			System.out.println("Attempting to store file in directory: ");
			System.out.println("User directory: " + System.getProperty("user.dir"));
			System.out.println("Absolute path: " + aCurrentFile.getAbsolutePath());
			try
			{
				System.out.println("Canonical path: " + aCurrentFile.getCanonicalPath());
			}
			catch(IOException aCanonicalEx)
			{
				System.out.println("Couldn't get canonical path.");
			}
			System.out.println("Exception saving URL history file:");
			ioEx.printStackTrace();
		}
		finally
		{
			try
			{
				aHistoryOut.close();
			}
			catch(Exception ex)
			{
				// just continue
			}
		}
	}
	
	/**
	 * Receive a progress update. 
	 * @param theMessage if not empty, add theMessage to the status panel.
	 * @param theProgressPercentage use this value to update the overall progress on the progress bar.
	 */
	public void updateProgress(String theMessage, int theProgressPercentage)
	{
		if(!theMessage.isEmpty())
		{
			//itsStatus.setText(theMessage);
			itsStatus.append("\n" + theMessage);
			itsStatus.paint(itsStatus.getGraphics());
		}
		
		// Calculate the overall progress, based on itsProcessStep count and the percentage.		
		int anOverallProgress = ((100 * itsProcessStepCount) + theProgressPercentage) / itsProcessStepNumber;
		itsProgress.setValue(anOverallProgress);
		if(theProgressPercentage > 0)
		{
			int aRenderedStepCount = itsProcessStepCount + 1;
			if(aRenderedStepCount > itsProcessStepNumber)
			{
				aRenderedStepCount = itsProcessStepNumber;
			}
			int aRenderedProcessNumber = itsProcessStepNumber;
			String aProgressString = PROGRESS_PREFIX_STRING_1 + aRenderedStepCount;
			aProgressString = aProgressString + PROGRESS_PREFIX_STRING_2 + aRenderedProcessNumber;
			aProgressString = aProgressString + PROGRESS_PREFIX_STRING_3 + theProgressPercentage + "%";
			itsProgress.setString(aProgressString);			
		}
		itsProgress.paint(itsProgress.getGraphics());
		
		// Update the process step counter if we've just completed a step
		if (theProgressPercentage == 100)
		{
			itsProcessStepCount++;
		}		
	}
	
	/**
	 * Override to allow update of just the message, where no progress has been made
	 * @param theMessage if not empty, add theMessage to the status panel.
	 */
	public void updateProgress(String theMessage)
	{
		if(!theMessage.isEmpty())
		{
			itsStatus.append("\n" + theMessage);
			itsStatus.paint(itsStatus.getGraphics());
		}
	}
	
	/**
	 * Receive a progress update from the XMLRenderer
	 * @param theMessage current output from the execution of the XMLRenderer.
	 * @param theProgressPercentage the percentage of the overall task that has been
	 * completed.
	 */
	public void updateRenderProgress(String theMessage, int theProgressPercentage)
	{
		// Leverage the updateProgress method
		updateProgress(theMessage, theProgressPercentage);
	}
	
	/** 
	 * Returns an ImageIcon, or null if the path was invalid. 
	 * @param theFile the filename of the image resource file
	 * @param theDescription a description of the Icon.
	 */
	protected static ImageIcon createImageIcon(String theFile, String theDescription) 
	{
   		try
		{
			File aPluginDir = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			File anImageFile = new File(aPluginDir, theFile);
			String anImageFilename = anImageFile.getCanonicalPath();
		   
	        return new ImageIcon(anImageFilename, theDescription);
	    } 
	    catch(FileNotFoundException noFile) 
	    {
	        System.out.println("Couldn't find image file: " + theFile);
	        return new ImageIcon();
	    }
	    // Handle an IOExceptions from the parse call
		// Any FileNotFound should have been caught by the above catch block.
	    catch (IOException anIOEx)
	    {
			File aCurrentFile = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			System.out.println("Attempting to open file in directory: ");
			System.out.println("User directory: " + System.getProperty("user.dir"));
			System.out.println("Absolute path: " + aCurrentFile.getAbsolutePath());
			try
			{
				System.out.println("Canonical path: " + aCurrentFile.getCanonicalPath());
			}
			catch(IOException aCanonicalEx)
			{
				System.out.println("Couldn't get canonical path.");
			}
			System.out.println("Exception reading URL history file:");
			anIOEx.printStackTrace();
			return new ImageIcon();
	    }    
	    catch(NullPointerException aNullEx)
		{
			File aCurrentFile = PluginUtilities.getInstallationDirectory(PLUGIN_CLASS_NAME);
			System.out.println("Attempting to open file in directory: ");
			System.out.println("User directory: " + System.getProperty("user.dir"));
			System.out.println("Absolute path: " + aCurrentFile.getAbsolutePath());
			try
			{
				System.out.println("Canonical path: " + aCurrentFile.getCanonicalPath());
			}
			catch(IOException aCanonicalEx)
			{
				System.out.println("Couldn't get canonical path.");
			}
			System.out.println("Exception reading icon image file:");
			aNullEx.printStackTrace();
			return new ImageIcon();
		}
	}
	
	/**
	 * Make sure that the selected publishing URL has the required report service
	 * defined in the URL. If not, append the value of the reporttab.url.defaultservice property, 
	 * which holds the default report service URL suffix, to the selected / specified URL
	 * @param theSelectedURL the selected URL identifying the target Essential Viewer environment
	 * @return the valid, full URL for the selected Viewer
	 */
	protected String buildPublishURL(String theSelectedURL)
	{
		String aPubServiceURL = "";
		
		// Check to see if the default service identifier has been included or not
		if(theSelectedURL.endsWith(itsDefaultURLService))
		{
			aPubServiceURL = theSelectedURL;
		}
		// If not, append it to the URL
		else
		{
			// Remove any trailing '/' characters
			String aCleanURL = theSelectedURL.trim();
			if(aCleanURL.endsWith("/"))
			{
				aCleanURL = aCleanURL.substring(0, (aCleanURL.length()-1));
			}
					
			aPubServiceURL = aCleanURL.concat(itsDefaultURLService);
		}
		// And return it
		return aPubServiceURL;
	}
	
	/**
	 * Create a button with which to invoke the report generation
	 * @return a reference to the button
	 */
	private JButton createButton()
	{
		// Add a button
		JButton aButton = new JButton(BUTTON_TEXT);
        
		// Add the action listener
		aButton.addActionListener(this);
		return aButton;
	}
	
	/**
	 * Create the text field in which the URL to which the report should
	 * be sent to is sent.
	 * 
	 */
	private void createURLField()
	{
		// Add the URL ComboBox
		itsURLEntry = new JComboBox();
		itsURLEntry.setEditable(true);
		setURLChoices();
		
		// Add the action listener
		// 16.05.2013 JWC Stop sending action on select URL
		//itsURLEntry.addActionListener(this);
	}
	
	/**
	 * Create the status textfield to show the status of the reporting
	 */
	private void createStatusTextField()
	{
		// Add a JLabel for the status
		itsStatus = new JTextArea(READY_STATUS_MESSAGE, STATUS_ROWS, itsURLFieldColumns);
		itsStatus.setLineWrap(true);
		itsStatus.setWrapStyleWord(true);
	}
	
	/**
	 * Generate the XML for the report.
	 * @return true if the XML was generated successfully, false otherwise.
	 */
	private boolean generateReport()
	{
		boolean isSuccess = false;
		
		// Use the XMLRenderer class to get the repository in XML
		// Create a Writer for XMLRenderer to use.
		StringWriter anXMLString = new StringWriter();
		
		try
		{
			// 19.11.2009 JWC - Support for the GraphWidget image sending
			itsKB = getKnowledgeBase();
			
			// Get the XML representation
			// Render the Instances in the KnowledgeBase as XML
			XMLRenderer anXMLRender = new XMLRenderer(itsKB, anXMLString);
			
			// 23.05.2013 JWC - set the listener
			anXMLRender.setItsListener(this);
			
			// 02.11.2010	JWC - set the datetime format for the timestamp tag
			if(itsTimeStampFormat != null)
			{
				anXMLRender.setItsTimeStampFormatString(itsTimeStampFormat);
			}
			anXMLRender.render();
			
			// Check for errors and read the XML.
			// Make sure to encode the String correctly as UTF-8
			byte[] aRenderedXML = anXMLString.toString().getBytes("UTF8");
			itsReportXML = new String(aRenderedXML, "UTF8");
			
			if(itsReportXML != null)
			{
				isSuccess = true;
			}
			else
			{
				isSuccess = false;
			}
		}
		catch(Exception ex)
		{
			System.out.println("Caught the exception:\n" + ex.toString());
			ex.printStackTrace(System.out);
			isSuccess = false;
		}
		
		return isSuccess;
	}
	
	/**
	 * Send the XML for the report to the reporting service. This kicks off a thread to
	 * manage the service request for sending the XML, whilst allowing the UI to update.
	 */
	private void sendReportXML()
	{
		String aReportServiceURL = (String)itsURLEntry.getSelectedItem();
		String aUser = itsUserName.getText();
		String aPassword = new String(itsPassword.getPassword());
		
		// 16.05.2013 JWC Check that the URL is valid
		String aValidReportServiceURL = buildPublishURL(aReportServiceURL);
				
		// Create the [potentially] long running process
		itsServiceClient.setItsURL(aValidReportServiceURL);
		itsServiceClient.setItsReportXML(itsReportXML);
		itsServiceClient.setItsUID(aUser);
		itsServiceClient.setItsPassword(aPassword);
		
		// 26.07.2013 JWC - Switch on or off image sending as required		
		itsServiceClient.setItIsSendingImages(itsSendImage.isSelected());
		
		// 19.11.2009 JWC - Add variable for sending the images from GraphWidget
		if(itsLayout != null)
		{
			itsServiceClient.setItsAutoLayout(itsLayout.getLayout());
			itsLastLayout = itsLayout.getLayout();
		}
		else
		{
			itsServiceClient.setItsAutoLayout("");
		}
		itsServiceClient.setItsKBRef(itsKB);
		itsServiceClient.setItsImageURLSuffix(itsImagesURLSuffix);		
		
		// Start the request
		itsServiceClient.start();		
	}
	
	/**
	 * Update the status message to notify of the failure
	 * @param theFailure a message containing the reason for the failure
	 */
	private void failedMessage(String theFailure)
	{
		updateProgress(theFailure, itsProgress.getMaximum());
	}
	
	/**
	 * Create the progress bar UI control
	 *
	 */
	private void createProgressBar()
	{
		itsProgress = new JProgressBar();
	}
	
	/**
	 * Reset the progress indicator
	 *
	 */
	private void resetProgress()
	{
		itsStatus.setText(READY_STATUS_MESSAGE);
		itsProgress.setBorderPainted(true);
		itsProgress.setStringPainted(true);
		itsProgress.setString("");
		itsProgress.setValue(0);
		itsProcessStepCount = 0;
		itsProgress.paint(itsProgress.getGraphics());
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        itsTimer.start();
	}
	
	/**
	 * Start the progress bar
	 * Set it to max of 100%
	 */
	private void startProgress()
	{
		// Do nothing 
	}
	
	/**
	 * Override to show progress where there is no message to report.
	 * Update the progress bar to show progress
	 * @param theProgressPercent what percent of the report sending has been
	 * completed.
	 */
	public void updateProgress(int theProgressPercent)
	{
		itsProgress.setValue(theProgressPercent);
		itsProgress.paint(itsProgress.getGraphics());
		
		// Is it complete?
		if(theProgressPercent == itsProgress.getMaximum())
		{
			// Do nothing now 29.07.2013 JWC
			//itsProgress.setIndeterminate(false);
			//itsProgress.setString(PROGRESS_COMPLETE);
			//itsProgress.setValue(itsProgress.getMinimum());
		}
	}
	
	/**
	 * Create the timer that will poll the service to see if it is finished. If it is
	 * finished trying to send the repository snapshot XML, the ReportService is interrogated
	 * to see if it was successful or not.
	 * @see com.enterprise_architecture.essential.widgets.ReportServiceClient
	 */
	private void createProgressTimer()
	{
//		Create a timer.
		itsTimer = new Timer(ONE_SECOND, new ActionListener() 
        {
            public void actionPerformed(ActionEvent evt) 
            {         
                startProgress();
                if(itsServiceClient.isItIsFinished())
                {
                	itIsTaskComplete = true;
                	if(itsServiceClient.isItIsSuccess())                		
                	{
                		//itsStatus.setText(SUCCESS_MESSAGE);
                		updateProgress(SUCCESS_MESSAGE, 100);
                	}
                	else
                	{            		
                		String anErrorReason = "";
                		switch(itsServiceClient.getItsReturnCode())
                		{
                			// JWC 30.05.2009 Added additional error message.
                			case NO_SERVER : anErrorReason = NO_SERVER_MESSAGE; break;
                			case INTERNAL_SERVER_ERROR : anErrorReason = INTERNAL_SERVER_ERROR_MESSAGE; break;
                			case BAD_URL : anErrorReason = BAD_URL_MESSAGE; break;
                			case BAD_PASSWORD : anErrorReason = BAD_PASSWORD_MESSAGE; break;
                			case BAD_REQUEST : anErrorReason = BAD_REQUEST_MESSAGE; break;
                			case ACCESS_FORBIDDEN : anErrorReason = ACCESS_FORBIDDEN_MESSAGE; break;
                			default : anErrorReason = SERVER_ERROR_MESSAGE; break;
                		}
                		failedMessage(FAILED_SEND + " : \n" + anErrorReason);            		
                	}
                }
                            	
                if (itIsTaskComplete) 
                {
                    itsTimer.stop();
                    setCursor(null); //turn off the wait cursor
                    itsButton.setEnabled(true);
                    updateProgress(itsProgress.getMaximum());
                    
                    if(itsServiceClient.isItIsSuccess())
                    {
                    	saveURLChoices();
                    }
                }
            }
        });
	}
	
	/**
	 * Load up the URL ComboBox with the previously successful URLs held
	 * in the URL history field. If the history is empty, start with the default
	 * from the properties file.
	 * @since version 2.0
	 */
	private void setURLChoices()
	{
		// Load all the options into the combox box from the current
		// URL history
		Iterator<String> anIt = itsURLHistory.iterator();
		while(anIt.hasNext())
		{
			itsURLEntry.addItem(anIt.next());
		}
		
		// If there is nothing in position 1, use the default
		if(itsURLEntry.getItemCount() == 0)
		{
			itsURLEntry.setSelectedItem(itsURL);
		}
		else
		{
			itsURLEntry.setSelectedIndex(0);
		}
	}
	
	/**
	 * Save the history of selected report URLs.
	 * This method should only be used when the URL has been used successfully.
	 * @since version 2.0
	 */
	private void saveURLChoices()
	{	
		// Find the selected value
		String aSelection = (String)itsURLEntry.getSelectedItem();
		// Add last successful URL to the history set - to avoid duplicate URLs
		// Work on a new set to get the last entry first in the list
		LinkedHashSet<String> aNewSet = new LinkedHashSet<String>();
		aNewSet.add(aSelection);
		aNewSet.addAll(itsURLHistory);
		
		// Reset the overall URL history
		itsURLHistory.clear();
		itsURLHistory.addAll(aNewSet);
		
		// Reflect the URL history set in the combo box widget
		itsURLEntry.removeAllItems();
		Iterator<String> anIt = itsURLHistory.iterator();
		while(anIt.hasNext())
		{
			itsURLEntry.addItem(anIt.next());
		}
		itsURLEntry.setSelectedIndex(0);
		
	}
		
	/**
	 * Render the contents of the itsURLSet LinkedHashSet as an XML document
	 * this can then be persisted.
	 * @return the XML document representing the URL history
	 * @since version 2.0
	 */
	private String renderXML()
	{
		StringWriter anXMLString = new StringWriter();
		
		PrintWriter aPrintWriter = new PrintWriter(anXMLString);
		aPrintWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		aPrintWriter.println("<history>");
		Iterator<String> anIt = itsURLHistory.iterator();
		int anIndex = 0;
		while(anIt.hasNext())
		{
			String aURL = anIt.next();
			aPrintWriter.println("\t<url index=\"" + anIndex + "\">" + aURL + "</url>");
			anIndex++;
		}
		// 19.11.2009 JWC - Added the autolayout selection to the history
		if((itsLastLayout != null) && (itsLastLayout.length() > 0))
		{
			aPrintWriter.println("\t<layout>" + itsLastLayout + "</layout>");
		}
		
		aPrintWriter.println("</history>");
		
		return anXMLString.toString();
	}
}
