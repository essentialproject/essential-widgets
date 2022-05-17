/**
 * Copyright (c)2006-2009 Enterprise Architecture Solutions Ltd.
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
 * 09.12.2006	JWC	1st coding. 
 * 19.11.2009	JWC	Added history item for the last selected auto layout
 * 
 */
package com.enterprise_architecture.essential.widgets;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parse the XML file that contains the history of successful Report Service URLs. Also included
 * in version 1.1 is the history of the last selected auto layout for the graph widget images.
 * @author Jonathan Carter <jonathan.carter@e-asolutions.com>
 * @version 1.1
 *
 */
public class ServiceHistoryParser extends DefaultHandler 
{
	private String itsCurrentElement;
	private String itsCurrentCharacters;
	private Vector<String> itsURLHistory;
	private String itsCurrentAttribute;
	private String itsLastLayout;
	
	public ServiceHistoryParser()
	{
		itsCurrentCharacters = new String();
		itsURLHistory = new Vector<String>();
		itsLastLayout = new String();
	}

    /**
	 * @return the itsLastLayout
	 */
	public String getItsLastLayout() {
		return itsLastLayout;
	}

	/**
	 * @param itsLastLayout the itsLastLayout to set
	 */
	public void setItsLastLayout(String itsLastLayout) {
		this.itsLastLayout = itsLastLayout;
	}

	// Override methods of the DefaultHandler class
    // to gain notification of SAX Events.
    //
    // See org.xml.sax.ContentHandler for all available events.
    //
    public void startDocument( ) throws SAXException 
    {
    	itsURLHistory = new Vector<String>();
    	itsLastLayout = new String();
    }

    public void endDocument( ) throws SAXException 
    {
       //System.out.println( "SAX Event: END DOCUMENT" );
    }

    /**
     * Handle the startElement event.
     */
    public void startElement(String theNamespaceURI,
            				 String theLocalName,
            				 String theQName,
            				 Attributes theAttributes ) throws SAXException 
    {
    	
    	// Only need the URL tag or the LAYOUT tag
    	if(theLocalName.equals("url")) 
    	{
    		//set the current element
            itsCurrentElement = "URL";
            itsCurrentAttribute = theAttributes.getValue(0);
    	}
    	else if(theLocalName.equals("layout"))
    	{
    		itsCurrentElement = "LAYOUT";
    	}
    }

    /**
     * Handle the end element. 
     * Finish the element value, save it to the Vector and reset
     * working variables
     */
    public void endElement(String theNamespaceURI,
            			   String theLocalName,
            			   String theQName ) throws SAXException 
    {      
    	if(itsCurrentElement != null) 
        {
    		if(itsCurrentElement.equals("URL"))
    		{
    			String aURL = itsCurrentCharacters;
    			
    			// If we know the index, then insert this into the Vector at that
    			// location.
    			if(itsCurrentAttribute != null)
    			{
    				int anIndex = Integer.parseInt(itsCurrentAttribute);
    				itsURLHistory.insertElementAt(aURL, anIndex);
    			}
    			else // add it at the end
    			{
    				itsURLHistory.add(aURL);
    			}
            }
    		else if(itsCurrentElement.equals("LAYOUT"))
    		{
    			String aLayout = itsCurrentCharacters;
    			itsLastLayout = aLayout;
    		}
        }
    	itsCurrentElement = null;
    	itsCurrentCharacters = new String();  	
    }
    
    /**
     * handle the stream of characters produced by parser.
     */
    public void characters( char[] ch, int start, int length )
    						throws SAXException 
    {

		if(itsCurrentElement != null) 
		{
			try 
			{	       
				String aCharacterStream;
  
				aCharacterStream = new String(ch, start, length);
				itsCurrentCharacters = itsCurrentCharacters.concat(aCharacterStream);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
    }

    
	/**
	 * Get a Vector of strings, each containing the URL of a successfully accessed
	 * Reporting Service
	 * @return the itsURLHistory
	 */
	public Vector<String> getItsURLHistory() {
		return itsURLHistory;
	}

	/**
	 * @param itsURLHistory the itsURLHistory to set
	 */
	public void setItsURLHistory(Vector<String> itsURLHistory) 
	{
		this.itsURLHistory = itsURLHistory;
	}
	
}
