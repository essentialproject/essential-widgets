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
 * 01.12.2006	JWC	1st coding.
 * 08.12.2006	JWC version 1.0 working.
 * 18.03.2007	JWC Fixed issue with InstanceFieldWidgets. On creating a new instance
 * 					from an InstanceFieldWidget and that field contributes to an AutoText
 * 					the change to the InstanceFieldWidget browser text wasn't picked up.
 * 12.09.2007	JWC	Refactored package name. Investigating bug with Protege 3.3
 * 15.10.2007	JWC	Added the dispose() method to release frame listeners in response
 * 					to an email to the user group from Tania Tudorache that points out that
 * 					failing to do so could create memory leaks.
 * 15.01.2008	JWC Fixed dropped events in client-server-mode projects. In such projects
 * 					some events were being dropped, causing the AutoText widget to not
 * 					update as it should.
 * 08.12.2008	JWC	Refactored package name.
 * 20.04.2009	JWC	Fixed the widget to work with Protege 3.4. Revised how the references 
 * 					to the Instances of SlotWidgets on the Form. Thanks to Tania Tudorache and
 * 					Timothy Redmond of the Protege team for their help. 
 */
package com.enterprise_architecture.essential.widgets;

import java.util.ArrayList;
import java.util.Collection;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Facet;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.ValueType;
import edu.stanford.smi.protege.util.CollectionUtilities;
import edu.stanford.smi.protege.widget.TextFieldWidget;
import edu.stanford.smi.protege.widget.WidgetConfigurationPanel;

/**
 * Text widget that can be used to automatically create slot values
 * based on the values of other slots in the class. In addition, static 
 * text prefixes, suffixes and separators can be included in the 
 * text that is automatically created. <br>
 * Works with Protege version 3.1 to 3.4
 * @author Jonathan W. Carter <jonathan.carter@e-asolutions.com>
 * @since version 1.0
 * @version 1.1   - 18.03.2007. Fixed issue where using an Instance as an AutoText slot didn't work when
 * creating a new instance.
 * @version 1.1.1 - 15.10.2007. Added the <tt>dispose()</tt> method to release listeners to other
 * slots
 * @version 1.1.2 - 15.01.2008. Resolved missing update events in client-server mode.
 * @version 1.2	  - 08.12.2008. Refactored package name.
 * @version 1.3   - 20.04.2009. Revised getSlotWidgetInstance() to work with Protege 3.4
 *
 */
public class AutoTextWidget extends TextFieldWidget 
{
	protected final static String NO_VALUE_SET = "< NONE >";
	private final static String STRING_COMPONENT = "AutoText.Component.String_";
	private final static String SLOT_COMPONENT = "AutoText.Component.Slot_";

	/** Array holding auto text components in order
	 * 
	 */
	private ArrayList itsAutoText = new ArrayList();
	private AutoTextListener itsListener = null;
	
	/** Member variables to hold the AutoText config
	 * 
	 */
	private String itsPrefix = "";
	private Slot itsSlot1 = null;
	private String itsSeparator1 = "";
	private Slot itsSlot2 = null;
	private String itsSeparator2 = "";
	private Slot itsSlot3 = null;
	private String itsSeparator3 = "";
	private Slot itsSlot4 = null;
	private String itsSeparator4 = "";
	private Slot itsSlot5 = null;
	private String itsSuffix = "";
	
	
	/**
	 * Constructor.
	 * Call the super() constructor and then get the PropertyList.
	 */
	public AutoTextWidget() 
	{
		super();
		
		// Create the listener to pick up changes to the AutoText source Slots
		itsListener = new AutoTextListener(this);
		
	}
	
	/**
	 * Initialise the AutoText widget.
	 */
	public void initialize()
	{
		super.initialize();
		
		// Initialise the autotext from the properties.
		loadFromProperties();
	}
	
	/** Is this slot widget suitable for use with the selected slot?
	 * 
	 * @param theClass the class that contains the slot
	 * @param theSlot the slot that wants to use this widget
	 * @param facet historical, always null.
	 * @return true if theSlot in question is a String text slot that has single 
	 * cardinality.
	 */
	public static boolean isSuitable(Cls theClass, Slot theSlot, Facet facet) 
	{
    	boolean isSuitable;
    	if ((theClass == null) || (theSlot == null)) 
    	{
      		isSuitable = false;
      	} 
    	else 
    	{
      		boolean isString = theClass.getTemplateSlotValueType(theSlot) == ValueType.STRING;
      		boolean isMultiple = theClass.getTemplateSlotAllowsMultipleValues(theSlot);
      		isSuitable = isString && !isMultiple;
    	}
		return isSuitable;
	}

	/** 
	 * Set the value for the slot that this widget is representing.
	 * Register FrameListeners on this ClassWidget of this widget and also
	 * on the Instances of Slots that contain Instance types to get updates of
	 * the text to be used in the creation of the auto text.
	 * The browser text for this widget (the text that appears in this widget on 
	 * the user interface) is updated and then the value of the instance that
	 * this widget is displaying is updated with the new auto text.
	 * @param values the list of values that this slot contains.
	 * Take the 1st and compare it to the newly created auto-text. If they differ,
	 * take the new value.
	 * This method only executes if this widget is showing, to handle cases such
	 * as where auto text appears in InstanceLists.
	 * 
	 */
	public void setValues(final Collection values) 
	{
		String anExistingAutoText = (String) CollectionUtilities.getFirstItem(values);
		
		// 14.01.2008 JWC
		// Now register the listeners - this will have been created
		registerAutoTextListeners();
			
		String aNewAutoText = "";
			
		// If there was no existing value, create a new value.
		if (anExistingAutoText == null) 
        {
	         aNewAutoText = createAutoText();	
        }
		else
		{
			// Read from knowledge base and leave as is if the widgets haven't
			// been created yet on the form.
			// Test to see if the widgets are there...
			if(slotsHaveInstances())
			{
				// If so, reset the auto text to find any new values
				aNewAutoText = createAutoText();
			}
			else 
			{
				aNewAutoText = anExistingAutoText;
			}
		}
	
		// Set the widget text value.
		setText(aNewAutoText);
						
		// Update the auto text Instance value (in the knowledge base) contents 
		// only if the widget is showing
		if(isShowing())
		{
			// If the value has changed, update the Instance.
			if(!aNewAutoText.equals(anExistingAutoText))
			{
				//valueChanged();
				setInstanceValues();
			}
		}
	
	  	// otherwise leave it unchanged.
  	}
	
	/**
	 * Create an the configuration panel for this widget.
	 * @return a new instance of AutoTextConfigurationPanel
	 */
	public WidgetConfigurationPanel createWidgetConfigurationPanel()
	{
	    AutoTextConfigurationPanel aConfigPanel = new AutoTextConfigurationPanel(this);
		return aConfigPanel;
	}
	
	/**
	 * Create the auto-text string that will be used to set the
	 * slot value.
	 * @return the new string created from the specified sibling Slots 
	 * and static strings.
	 */
	protected String createAutoText()
	{
		StringBuffer aResultText = new StringBuffer();
	
		// Build the string from the contents of the autotext
		// format in AutoText template members
		aResultText.append(itsPrefix);
		
		if(itsSlot1 != null)
		{
			String aSlotWidgetText = getSlotText(itsSlot1);
			aResultText.append(aSlotWidgetText);
		}
		aResultText.append(itsSeparator1);

		if(itsSlot2 != null)
		{
			String aSlotWidgetText = getSlotText(itsSlot2);
			aResultText.append(aSlotWidgetText);
		}
		aResultText.append(itsSeparator2);
		
		if(itsSlot3 != null)
		{
			String aSlotWidgetText = getSlotText(itsSlot3);
			aResultText.append(aSlotWidgetText);
		}
		aResultText.append(itsSeparator3);
		
		if(itsSlot4 != null)
		{
			String aSlotWidgetText = getSlotText(itsSlot4);
			aResultText.append(aSlotWidgetText);
		}
		aResultText.append(itsSeparator4);
		
		if(itsSlot5 != null)
		{
			String aSlotWidgetText = getSlotText(itsSlot5);
			aResultText.append(aSlotWidgetText);
		}
		aResultText.append(itsSuffix);
				
		return aResultText.toString();
	}
	
	
	/**
	 * Load in the AutoText configuration from the properties
	 * for this widget.
	 */
	protected void loadFromProperties()
	{
		// Get each named property and load it accordingly.
		
		// Get the prefix string
		itsPrefix = loadStringComponent(getPropertyList().getString(STRING_COMPONENT + "0"));
		
		// Get the slot
		itsSlot1 = loadSlotFromName(getPropertyList().getString(SLOT_COMPONENT + "1"));
		
		// Get the separator string
		itsSeparator1 = loadStringComponent(getPropertyList().getString(STRING_COMPONENT + "1"));
		
		// Get the slot
		itsSlot2 = loadSlotFromName(getPropertyList().getString(SLOT_COMPONENT + "2"));
		
		// Get the separator string
		itsSeparator2 = loadStringComponent(getPropertyList().getString(STRING_COMPONENT + "2"));
		
		// Get the slot
		itsSlot3 = loadSlotFromName(getPropertyList().getString(SLOT_COMPONENT + "3"));
		
		// Get the separator string
		itsSeparator3 = loadStringComponent(getPropertyList().getString(STRING_COMPONENT + "3"));
		
		// Get the slot
		itsSlot4 = loadSlotFromName(getPropertyList().getString(SLOT_COMPONENT + "4"));
		
		// Get the separator string
		itsSeparator4 = loadStringComponent(getPropertyList().getString(STRING_COMPONENT + "4"));
		
		// Get the slot
		itsSlot5 = loadSlotFromName(getPropertyList().getString(SLOT_COMPONENT + "5"));
		
		// Get the separator string
		itsSuffix = loadStringComponent(getPropertyList().getString(STRING_COMPONENT + "5"));
		
	}
	
	/**
	 * Save the AutoText config to the properties
	 *
	 */
	protected void saveToProperties()
	{
		// Set the Strings and Slots properties
		saveConfigItem(itsPrefix, 0);
		saveConfigItem(itsSlot1, 1);
		saveConfigItem(itsSeparator1, 1);
		saveConfigItem(itsSlot2, 2);
		saveConfigItem(itsSeparator2, 2);
		saveConfigItem(itsSlot3, 3);
		saveConfigItem(itsSeparator3, 3);
		saveConfigItem(itsSlot4, 4);
		saveConfigItem(itsSeparator4, 4);
		saveConfigItem(itsSlot5, 5);
		saveConfigItem(itsSuffix, 5);
	}
	
	/**
	 * Is the specified slot - typically a Slot received in a
	 * FrameEvent - used by this AutoText?
	 * @param theChangedSlot the Slot to test
	 * @return true if theChangedSlot matches one of the Slots used in 
	 * the auto-text template; false otherwise or if theChangedSlot is NULL.
	 * @see AutoTextListener
	 * @see edu.stanford.smi.protege.event.FrameEvent
	 */
	protected boolean isSlotUsed(Slot theChangedSlot)
	{
		boolean isUsed = false;
		
		// Filter out checking against empty slots.
		if(theChangedSlot != null)
		{
			// If theChangedSlot matches one of the slots
			// used in the AutoText
			if((theChangedSlot == itsSlot1) ||
			   (theChangedSlot == itsSlot2) ||
			   (theChangedSlot == itsSlot3) ||
			   (theChangedSlot == itsSlot4) ||
			   (theChangedSlot == itsSlot5))
			{
				isUsed = true;
			}
		}
		return isUsed;
	}
	
	/**
	 * Get the value displayed on the class form for theSourceSlot 
	 * and return it.
	 * @param theSourceSlot the slot to use the value of - specified in 
	 * the configuration of this widget.
	 * @return the String value of the specified slot
	 */
	private String getSlotText(Slot theSourceSlot)
	{
		String aSlotText = "";
				
		// Need to make sure we've got the right instance of the Class as context
		// for the slotwidget instances.
		Instance aSlotInstance = getSlotWidgetInstance();
		if(aSlotInstance != null)
		{
			aSlotText = findSlotInstanceText(aSlotInstance, theSourceSlot);
		}
		else
		{
			// The slot instance was null, can do nothing
		}
		
		return aSlotText;
	}
	
	/**
	 * See if the slots have been filled out with instances yet.
	 * @return true if Slots to be used in the auto-text have instances available
	 */
	private boolean slotsHaveInstances()
	{
		boolean isInstance = false;
		
		// Test each widget in the AutoText config.
		if(isSlotAndInstanceConsistent(itsSlot1) &&
		   isSlotAndInstanceConsistent(itsSlot2) &&
		   isSlotAndInstanceConsistent(itsSlot3) &&
		   isSlotAndInstanceConsistent(itsSlot4) &&
		   isSlotAndInstanceConsistent(itsSlot5))
		{
			isInstance = true;
		}
				
		return isInstance;
	}
	
	/**
	 * Check to see that the specified slot is in the AutoText pattern for
	 * this widget instance and that this is consistent with whether or not 
	 * there is an instance of the SlotWidget for 
	 * the specified Slot in the ClsWidget that is the parent of this AutoText
	 * widget. Therefore, if the Slot is being used, there should be an Instance.
	 * If the Slot is not being used, there shouldn't be an Instance.
	 * In addition, during this check, listeners are added to the Instances of the
	 * Slots that have Instances.
	 * @param theSlot the slot that is being tested for having an instance
	 * @return true if there is an instance, false if either the Slot is NULL (it 
	 * is not being used in this AutoText pattern OR false if there is no Instance
	 * of a SlotWidget for theSlot, e.g. because it has not been created yet.
	 */
	private boolean isSlotAndInstanceConsistent(Slot theSlot)
	{
		boolean isSlotAndInstance = false;
		if(theSlot != null)
		{
			Instance anInstance = getSlotWidgetInstance();
			if(anInstance != null)
			{
				isSlotAndInstance = true;
			}
		}
		// the Slot is NULL and not being used, so return true.
		else
		{
			isSlotAndInstance = true;
		}
			
		return isSlotAndInstance;
	}
	
	/**
	 * Add a listener for changes to an instance of a slot that is 
	 * in the AutoText configuration. If the listener is already listening to this
	 * instance no action is taken.
	 * @param theSlotInstance the instance of the Slot
	 */
	private void addSlotTextListener(Instance theSlotInstance)
	{
		// If the instance exists and the listener is NOT already listening to it
		if((theSlotInstance != null) && 
		   !(itsListener.isListeningAlready(theSlotInstance)))
		{
			theSlotInstance.addFrameListener(itsListener);
			itsListener.addListening(theSlotInstance);
		}
	}
	
	/**
	 * Return a handle to the Instance of the AutoText SlotWidget, using a call
	 * to <tt>getInstance();</tt><br>
	 * This is a revised version of the original method and is required to work with
	 * Protege 3.4. <br>
	 * Thanks to Tania Tudorace of the Protege Team for pointing this out and to 
	 * Timothy Redmond (also of the Protege Team) for his help in tracing this problem.
	 * @return a reference to Instance of this AutoText SlotWidget.
	 * @since version 1.3 - 20.04.2009 refactored to remove parameter
	 */
	private Instance getSlotWidgetInstance()
	{
		// Apply Tania's recommendation, 
		// just return the Instance for this AutoTextWidget.
		return getInstance();
	}
	
	/**
	 * Find the text shown in the slot. 
	 * @param theInstanceFrame the slot/instance that needs to be used
	 * @param theTemplateSlot the slot that theInstanceFrame is an instance of.
	 * @return the text value displayed for the instance of the slot
	 */
	private String findSlotInstanceText(Instance theInstanceFrame, Slot theTemplateSlot)
	{
		String anInstanceText = "";
		Cls aCls = getCls();
			
		// Find out the type of the Slot
		ValueType aType = aCls.getTemplateSlotValueType(theTemplateSlot);
		if(aType == ValueType.BOOLEAN)
		{
			Boolean aBool = (Boolean)theInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
			if(aBool != null)
				anInstanceText = aBool.toString();
		}
		else if(aType == ValueType.CLS)
		{
			Cls aClsInst = (Cls)theInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
			if(aClsInst != null)
				anInstanceText = aClsInst.getBrowserText();
		}
		else if(aType == ValueType.FLOAT)
		{
			Float aFloat = (Float)theInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
			if(aFloat != null)
				anInstanceText = aFloat.toString();
		}
		else if(aType == ValueType.INSTANCE)
		{
			Instance anInst = (Instance)theInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
			if(anInst != null)
				anInstanceText = anInst.getBrowserText();
		}
		else if(aType == ValueType.INTEGER)
		{
			Integer anInt = (Integer)theInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
			if(anInt != null)
				anInstanceText = anInt.toString();
		}
		else if((aType == ValueType.STRING) || (aType == ValueType.SYMBOL))
		{
			String aString = (String)theInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
			if(aString != null)
				anInstanceText = aString;
		}
	
		return anInstanceText;
	}
	
	/**
	 * Get a reference to the specified Slot
	 * @param theSlotName the name of the slot that is being loaded
	 * @return a reference to the slot OR NULL if the named slot is not
	 * found
	 */
	private Slot loadSlotFromName(String theSlotName)
	{
		Slot aSelectedSlot = null;
		
		if((theSlotName != null) && !(theSlotName.equals(NO_VALUE_SET)))
		{
			aSelectedSlot = getKnowledgeBase().getSlot(theSlotName);
		}
		
		return aSelectedSlot;
	}
	
	/**
	 * Return the reference to the specified String from the properties
	 * The property value for nothing selected is translated to the empty
	 * string.
	 * @param theStringName the value of the property for the string 
	 * configuration item
	 * @return the configuration string that will be used in the AutoText
	 */
	private String loadStringComponent(String theStringName)
	{
		String aSelectedString = "";
		if((theStringName != null) && (!theStringName.equals(NO_VALUE_SET)))
		{
			aSelectedString = theStringName;
		}
		
		return aSelectedString;
	}
	
	/**
	 * Save the configuration String item to the specified position
	 * in the AutoText array template. E.g. saving to position 2 will
	 * set the AutoText.Component.String_2 property to the specfied String
	 * value
	 * @param theStringComponent the String value that has been configured
	 * @param thePosition the position in the AutoText template for this String.
	 * Note, the prefix (if used) is a position 0, the suffix at position 5.
	 */
	private void saveConfigItem(String theStringComponent, int thePosition)
	{
		// Save the string into AutoText.Component.String_n
		String aPropName = STRING_COMPONENT + thePosition;

		// A string has been specified
		if((theStringComponent != null) && (theStringComponent.length() > 0))
		{
			getPropertyList().setString(aPropName, theStringComponent);

		}
		else // It's an empty value
		{
			getPropertyList().setString(aPropName, NO_VALUE_SET);
		}
	}
	
	/**
	 * 
	 * Save the configuration Slot item to the specified position
	 * in the AutoText array template. E.g. saving to position 2 will
	 * set the AutoText.Component.Slot_2 property to the name of the 
	 * specfied Slot.
	 * @param theSlotComponent the Slot value that has been configured
	 * @param thePosition thePosition the position in the AutoText template for this Slot.
	 */
	private void saveConfigItem(Slot theSlotComponent, int thePosition)
	{
		// Save the Slot name into AutoText.Component.Slot_n
		String aPropName = SLOT_COMPONENT + thePosition;
		String aSlotName = NO_VALUE_SET;
		
		if(theSlotComponent != null)
		{
			aSlotName = theSlotComponent.getName();
		}
		
		getPropertyList().setString(aPropName, aSlotName);
	}
	
	/**
	 * Register the listeners for this widget. These listeners catch events
	 * when the slot values change on the Frame in which this widget exists.
	 * In addition, for Slots that are Instances of classes, an additional listener
	 * is registered to make sure that changes to slots in the Instance's Class are
	 * also received. E.g. the CreateNewInstance case.
	 * @since v1.1
	 */
	private void registerAutoTextListeners()
	{
		// Use an instance of this Widget to set the listener
		// Listen to browser text changes on the parent frame of this slot.
		// i.e. any updates on this form.
		Instance aWidgetInstance = getInstance();
		addSlotTextListener(aWidgetInstance);
		
		// 15.03.2007 - Catch events on the other widgets if they
		// are used.
		
		// 14.01.2008 JWC - Add SlotText listener to all slots
		// and instead add it in the if statements, below.
		if(isSlotUsed(itsSlot1))
		{
			addSlotTextListener(itsSlot1);
			addSlotInstanceListener(itsSlot1);
		}
		if(isSlotUsed(itsSlot2))
		{
			addSlotTextListener(itsSlot2);
			addSlotInstanceListener(itsSlot2);
		}		
		if(isSlotUsed(itsSlot3))
		{
			addSlotTextListener(itsSlot3);
			addSlotInstanceListener(itsSlot3);
		}
		if(isSlotUsed(itsSlot4))
		{
			addSlotTextListener(itsSlot4);
			addSlotInstanceListener(itsSlot4);
		}
		if(isSlotUsed(itsSlot5))
		{
			addSlotTextListener(itsSlot5);
			addSlotInstanceListener(itsSlot5);
		}
		
	}
	
	/**
	 * Add listeners to Slots that hold instances. This is required to ensure
	 * that changes to the Slots in a Class Instance are picked up when they
	 * are used as an Instance field on the Form (of this AutoTextWidget) and are used 
	 * as part of an AutoText template. A listener is only added to Slots that hold Instances.
	 * @param theTemplateSlot a template slot on the AutoText's parent Form.
	 */
	private void addSlotInstanceListener(Slot theTemplateSlot)
	{
		// Get a reference to the Instance of the specified Template Slot.
		Instance aSlotInstanceFrame = null;
		
		if(theTemplateSlot != null)
		{		
			aSlotInstanceFrame = getSlotWidgetInstance();
			if(aSlotInstanceFrame != null)
			{
				// if it's an instance type
				Cls aCls = getCls();
				
				// Find out the type of the Slot
				ValueType aType = aCls.getTemplateSlotValueType(theTemplateSlot);
				if(aType == ValueType.INSTANCE)
				{
					Instance aSlotInstance = (Instance)aSlotInstanceFrame.getDirectOwnSlotValue(theTemplateSlot);
					
					if(aSlotInstance != null)
					{
						// Add FrameListener to that Instance.
						if(!itsListener.isListeningAlready(aSlotInstance))
						{
							aSlotInstance.addFrameListener(itsListener);
							itsListener.addListening(aSlotInstance);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Reset the ArrayList holding the autotext configuration to an
	 * empty configuration.
	 *
	 */
	public void resetAutoText()
	{
		itsAutoText.clear();
		
		itsPrefix = "";
		itsSlot1 = null;
		itsSeparator1 = "";
		itsSlot2 = null;
		itsSeparator2 = "";
		itsSlot3 = null;
		itsSeparator3 = "";
		itsSlot4 = null;
		itsSeparator4 = "";
		itsSlot5 = null;
		itsSuffix = "";
		
	}

	/**
	 * Get a handle to the AutoText configuration ArrayList. The AutoText
	 * is stored as components in order. Elements in the ArrayList are 
	 * either String or Slot types.
	 * @return the itsAutoText
	 */
	public ArrayList getItsAutoText() {
		return itsAutoText;
	}

	/**
	 * @return the itsPrefix
	 */
	public String getItsPrefix() {
		return itsPrefix;
	}

	/**
	 * @param itsPrefix the itsPrefix to set
	 */
	public void setItsPrefix(String itsPrefix) {
		this.itsPrefix = itsPrefix;
	}

	/**
	 * @return the itsSeparator1
	 */
	public String getItsSeparator1() {
		return itsSeparator1;
	}

	/**
	 * @param itsSeparator1 the itsSeparator1 to set
	 */
	public void setItsSeparator1(String itsSeparator1) {
		this.itsSeparator1 = itsSeparator1;
	}

	/**
	 * @return the itsSeparator2
	 */
	public String getItsSeparator2() {
		return itsSeparator2;
	}

	/**
	 * @param itsSeparator2 the itsSeparator2 to set
	 */
	public void setItsSeparator2(String itsSeparator2) {
		this.itsSeparator2 = itsSeparator2;
	}

	/**
	 * @return the itsSeparator3
	 */
	public String getItsSeparator3() {
		return itsSeparator3;
	}

	/**
	 * @param itsSeparator3 the itsSeparator3 to set
	 */
	public void setItsSeparator3(String itsSeparator3) {
		this.itsSeparator3 = itsSeparator3;
	}

	/**
	 * @return the itsSeparator4
	 */
	public String getItsSeparator4() {
		return itsSeparator4;
	}

	/**
	 * @param itsSeparator4 the itsSeparator4 to set
	 */
	public void setItsSeparator4(String itsSeparator4) {
		this.itsSeparator4 = itsSeparator4;
	}

	/**
	 * @return the itsSlot1
	 */
	public Slot getItsSlot1() {
		return itsSlot1;
	}

	/**
	 * @param itsSlot1 the itsSlot1 to set
	 */
	public void setItsSlot1(Slot itsSlot1) {
		this.itsSlot1 = itsSlot1;
	}

	/**
	 * @return the itsSlot2
	 */
	public Slot getItsSlot2() {
		return itsSlot2;
	}

	/**
	 * @param itsSlot2 the itsSlot2 to set
	 */
	public void setItsSlot2(Slot itsSlot2) {
		this.itsSlot2 = itsSlot2;
	}

	/**
	 * @return the itsSlot3
	 */
	public Slot getItsSlot3() {
		return itsSlot3;
	}

	/**
	 * @param itsSlot3 the itsSlot3 to set
	 */
	public void setItsSlot3(Slot itsSlot3) {
		this.itsSlot3 = itsSlot3;
	}

	/**
	 * @return the itsSlot4
	 */
	public Slot getItsSlot4() {
		return itsSlot4;
	}

	/**
	 * @param itsSlot4 the itsSlot4 to set
	 */
	public void setItsSlot4(Slot itsSlot4) {
		this.itsSlot4 = itsSlot4;
	}

	/**
	 * @return the itsSlot5
	 */
	public Slot getItsSlot5() {
		return itsSlot5;
	}

	/**
	 * @param itsSlot5 the itsSlot5 to set
	 */
	public void setItsSlot5(Slot itsSlot5) {
		this.itsSlot5 = itsSlot5;
	}

	/**
	 * @return the itsSuffix
	 */
	public String getItsSuffix() {
		return itsSuffix;
	}

	/**
	 * @param itsSuffix the itsSuffix to set
	 */
	public void setItsSuffix(String itsSuffix) {
		this.itsSuffix = itsSuffix;
	}
	
	/**
	 * Release any resources, in particular the frame listeners
	 * to avoid memory leaks.
	 * @since version 1.1.1
	 */
	public void dispose()
	{
		// Release all the listeners
		itsListener.stopListening();
		
		// Call the overridden method.
		super.dispose();
	}
	
}
