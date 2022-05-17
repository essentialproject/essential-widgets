/**
 * Copyright (c)2006-2008 Enterprise Architecture Solutions Ltd.
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
 * 07.12.2006	JWC	1st coding and working.
 * 18.03.2007	JWC	Fixed issue with using Instance Slots for Autotext.
 * 					Picked up the browserTextChanged() event.
 * 12.09.2007	JWC	Refactored package name. Investigating bug-fix for Protege 3.3
 * 12.09.2007	JWC	Fixed bug with Protege 3.3. The nature of the FrameEvents (in fact
 * 					all events) has changed in version 3.3 of Protege to include username
 * 					and timestamp. Therefore, duplicate FrameEvents look different due to
 * 					timestamps and the AutoTextListener gets caught in a loop in browserTextChanged().
 * 15.01.2008	JWC Fixed dropped events in client-server-mode projects. In such projects
 * 					some events were being dropped, causing the AutoText widget to not
 * 					update as it should.
 * 08.12.2008	JWC	Refactored package name
 */
package com.enterprise_architecture.essential.widgets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import edu.stanford.smi.protege.event.FrameAdapter;
import edu.stanford.smi.protege.event.FrameEvent;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.Slot;

/**
 * Listener class to listen for changes to the Slots and SlotWidgets
 * that are being used to create auto text in an AutoTextWidget.
 * The listener is registered to each Instance of the Slot that is
 * defined as part of the AutoText configuration and then updates
 * the parent AutoTextWidget when changes occur.
 * @author Jonathan W. Carter <jonathan.carter@e-asolutions.com>
 * @see AutoTextWidget
 * @version 1.1.1 - 12.09.2007. Fixed to work with Protege 3.3+.
 * @version 1.1.2 - 15.01.2008. Resolved missing update events in client-server mode.
 * @version 1.2	 -  08.12.2008. Refactored package name.
 * @since version 1: 07.12.2006
 */
public class AutoTextListener extends FrameAdapter 
{
	private AutoTextWidget itsWidget;
	private HashSet itsFrames = new HashSet();
	private String itsLastSlotName = "";
	private String itsLastSlotText = "";
	private String itsLastBrowserText = "";
	
	/**
	 * Default constructor
	 */
	public AutoTextListener() 
	{
		super();
		
		// Initialise itsWidget
		itsWidget = null;
	}

	/**
	 * Constructor that initialises the parent AutoText relationship
	 * @param theWidget the parent AutoText widget that this listener
	 * will inform of changes to the things that it is listening to.
	 */
	public AutoTextListener(AutoTextWidget theWidget)
	{
		// Call the base class constructor
		super();
		itsWidget = theWidget;
		
	}
	
	/**
	 * Set the relationship to the parent AutoText widget. 
	 * @param theWidget the parent that this
	 * listener will inform of changes to the thing that it is listening
	 * to.
	 */
	public void setItsWidget(AutoTextWidget theWidget)
	{
		itsWidget = theWidget;
	}
	
	/**
	 * Handle updates to the browser text of Slot instances by
	 * informing the parent AutoText to re-set its values to pick up 
	 * the change. However only do this if the browserText has really changed.
	 * @param theEvent events including the creation of new instances in InstanceFieldWidgets.
	 * @since version 1.1.2 - Updated to catch all relevent updates but stop looping.
	 */
	public void browserTextChanged(FrameEvent theEvent)
	{
		super.browserTextChanged(theEvent);
		
		if(theEvent != null)
		{		
			// If the browserText has actually changed then handle the change
			// 14.01.2008 - JWC
			// If it's not the AutoText for this listener then process
			// otherwise drop this event to stop recursion/loop.
			String aNewAutoText = itsWidget.createAutoText();
			
			if(!itsLastBrowserText.equals(aNewAutoText))
			{
				itsLastBrowserText = aNewAutoText;
				handleBrowserTextChange(theEvent);
			}
			
			// Don't process events where this AutoText widget's browser text
			// has changed. Stop any recursion but process all other browser
			// text changes.
		}
	}
	
	/**
	 * Handle changes to the ownSlotValue of Slot instances by
	 * informing the parent AutoText to re-set its values to pick up 
	 * the change
	 * @param theEvent an containing details of the slot that has changed in this
	 * Frame.
	 */
	public void ownSlotValueChanged(FrameEvent theEvent)
	{	
		super.ownSlotValueChanged(theEvent);
		
		if(theEvent != null)
		{			
			handleSlotValueChange(theEvent);
		}
	}
	
	/**
	 * Handle changes to the own facet value of Slot instances by
	 * informing the parent AutoText to re-set its values to pick up 
	 * the change
	 * @param theEvent the event is ignored by this listener.
	 */
	public void ownFacetValueChanged(FrameEvent theEvent)
	{
		super.ownFacetValueChanged(theEvent);
	}
			 
	/**
	 * Record the fact that this Listener is listening to the specified Frame
	 * Instance.
	 * @param theFrame the instance of the Frame that is being listened to. This
	 * is a Protege Instance object and is intended to listen to the instance of
	 * a SlotWidget used in an AutoTextWidget auto-text template.
	 * @see AutoTextWidget
	 */
	public void addListening(Instance theFrame)
	{
		// Add theFrame to the list of Frames that this is listening to.	
		itsFrames.add(theFrame);
	}
	
	/**
	 * Is this listener already listening to the specified Instance?
	 * @param theFrame an Instance (the instance of a SlotWidget that is being
	 * used in an AutoTextWidget auto-text template) that is either already being
	 * listened to or not.
	 * @return true if this listener is already listening to the specified Instance.
	 * false otherwise.
	 */
	public boolean isListeningAlready(Instance theFrame)
	{
		boolean isListening = false;
		if(itsFrames.contains(theFrame))
		{
			isListening = true;
		}
		
		return isListening;
	}
	
	/**
	 * Stop listenting to all the Frames that are registered with this 
	 * listener.
	 */
	public void stopListening()
	{
		Iterator anIt = itsFrames.iterator();
		
		while(anIt.hasNext())
		{
			Instance aFrame = (Instance)anIt.next();
			aFrame.removeFrameListener(this);
		}
	}
	
	/**
	 * Do the main work for handling the event.
	 * This is called by ownSlotValueChanged()
	 * Handle changes to the ownSlotValue of Slot instances by
	 * informing the parent AutoText to re-set its values to pick up 
	 * the change
	 * @param theEvent an containing details of the slot that has changed in this
	 * Frame.
	 */
	private void handleSlotValueChange(FrameEvent theEvent)
	{
		if((itsWidget != null) && (theEvent != null))
		{
			Slot aChangedSlot = theEvent.getSlot();
			
			// Dodge any Null pointers...
			if(aChangedSlot != null)		
			{
				// If the Slot in question is being used
				if(itsWidget.isSlotUsed(aChangedSlot))
				{
					// send a notice to update
					// Force a re-generation of entire auto-text as a value has
					// changed 
					if(!isRepeatedEvent(aChangedSlot))
					{
						storeLastSlotUpdate(aChangedSlot);
						itsWidget.setValues(Collections.EMPTY_LIST);
					}
				}
			}
			// 14.01.2008 - JWC. Mirror browserText change to make sure
			// that if an event happens, we update the AutoText
			else
			{
				// Catch events from newly created Instances - no slot is
				// passed in these events.
				itsWidget.setValues(Collections.EMPTY_LIST);
			}
		}
	}

	/**
	 * Handle events notifying of browserTextChange(). 
	 * The event only triggers a change to the AutoText when the Slot in theEvent is
	 * null or if it's a Slot that isn't used in the AutoText template. This avoids 
	 * duplication with ownSlotValueChange() events.
	 * @param theEvent the event that was raised when the browser text was changed.
	 * only processed if the slot in the event is null or it's not an event from a slot 
	 * that is being monitored already.
	 */
	private void handleBrowserTextChange(FrameEvent theEvent)
	{
		if((itsWidget != null) && (theEvent != null))
		{
			Slot aChangedSlot = theEvent.getSlot();
			
			// Dodge any Null pointers...
			if(aChangedSlot != null)		
			{	
				// Only if the Slot in question is being used
				// SlotValueChanged handles the local Slots...
				if(itsWidget.isSlotUsed(aChangedSlot))
				{
					// send a notice to update
					// Force a re-generation of entire auto-text as a value has
					// changed 
					if(!isRepeatedEvent(aChangedSlot))
					{
						storeLastSlotUpdate(aChangedSlot);
						itsWidget.setValues(Collections.EMPTY_LIST);
					}
				}
			}
			else
			{
				// Catch events from newly created Instances - no slot is
				// passed in these events.
				itsWidget.setValues(Collections.EMPTY_LIST);
			}
		}
	}
	
	/**
	 * Determine whether the event notifying of a change to the specified slot 
	 * has already been handled. The name and text value of the slot are tested
	 * 
	 * @param theUpdatedSlot the slot that has fired a value changed event.
	 * @return true if the criteria match the last recorded Slot update, false 
	 * otherwise (a new update).
	 */
	private boolean isRepeatedEvent(Slot theUpdatedSlot)
	{
		boolean isRepeat = false;
		if(itsLastSlotName.equals(theUpdatedSlot.getName()))
		{
			// Names match, check text values
			if(itsLastSlotText.equals(theUpdatedSlot.getBrowserText()))
			{
				isRepeat = true;
			}
		}
		
		// For now always fire every event, to ensure that all slot updates are
		// caught.
		isRepeat = false;
		return isRepeat;
	}

	/**
	 * Save the relevent details of a Slot that has changed its value.
	 * These details are used to determine whether a new slot changed event
	 * is in fact a repeat, e.g. when a Frame dialog is open.
	 * @param theLastChangedSlot the Slot that was updated last.
	 */
	private void storeLastSlotUpdate(Slot theLastChangedSlot)
	{
		itsLastSlotName = theLastChangedSlot.getName();
		itsLastSlotText = theLastChangedSlot.getBrowserText();
	}
}
