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
 * 01.12.2006	JWC	1st coding.
 * 08.12.2008	JWC	Refactored package name
 */
package com.enterprise_architecture.essential.widgets;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.widget.WidgetConfigurationPanel;

/**
 * Configuration panel for the AutoTextWidget. 
 * The user of the form editor is presented with a choice of valid slots
 * from the current class that can be used to provide text strings 
 * that will be used to build the name of the slot represented by 
 * this widget. Static strings can also be added for prefixes, suffixes
 * and seperators.
 * @author Jonathan W. Carter <jonathan.carter@e-asolutions.com>
 * @version 1.1
 * @since version 1.0
 * @see AutoTextWidget
 *
 */
public class AutoTextConfigurationPanel extends WidgetConfigurationPanel 
{
	private final static String INTRO_LABEL = "Define the auto text from the following text and Slot values";
	private final static String SEPARATOR_COMP_LABEL = "Separator text ";
	private final static String PREFIX_COMP_LABEL = "Prefix text ";
	private final static String SUFFIX_COMP_LABEL = "Suffix text ";
	private final static String SLOT_COMP_LABEL = "Use Slot value ";
	private final static String TAB_NAME = "Auto Text Specification";
	private final static String NONE_OPTION = "< none >";
	private final static int TEXT_COLUMNS = 45;
	private final static int LABEL_ALIGNMENT = JLabel.RIGHT;
	private final static int WIDGET_PADDING_X = 5;
	private final static int WIDGET_PADDING_Y = 5; 
	
	private Font itsLabelFont = new Font("SansSerif", (Font.ITALIC) + (Font.TRUETYPE_FONT), 11);
	private Font itsIntroFont = new Font("SansSerif", (Font.TRUETYPE_FONT), 12);
	JTextField itsPrefix = new JTextField(TEXT_COLUMNS);
	JTextField itsSeparator_1 = new JTextField(TEXT_COLUMNS);
	JTextField itsSeparator_2 = new JTextField(TEXT_COLUMNS);
	JTextField itsSeparator_3 = new JTextField(TEXT_COLUMNS);
	JTextField itsSeparator_4 = new JTextField(TEXT_COLUMNS);
	JTextField itsSuffix = new JTextField(TEXT_COLUMNS);

	JComboBox itsSlot_1 = new JComboBox();
	JComboBox itsSlot_2 = new JComboBox();
	JComboBox itsSlot_3 = new JComboBox();
	JComboBox itsSlot_4 = new JComboBox();
	JComboBox itsSlot_5 = new JComboBox();
	
	JPanel itsPanel = new JPanel(new GridBagLayout());
	
	HashMap itsAllowedSlots = new HashMap();
	Vector itsSlotOptionList = new Vector();
	
	/**
	 * The widget that is being configured
	 */ 
	private AutoTextWidget itsWidget;
	
	public AutoTextConfigurationPanel(AutoTextWidget theWidget)
	{
		super(theWidget);
		itsWidget = theWidget;
		initialise();
	}

	/**
	 * Persist the configuration to Protege's underlying persistence
	 * mechanism.
	 */
    public void saveContents() 
    {
        super.saveContents();
        
        // Save the auto text config.
        // save properties to the widget
        saveConfigToWidget();
        
        // tell it to save itself.
        itsWidget.saveToProperties();
    }
    
	/**
	 * Initialise the panel.
	 * Build all the GUI components and reset everything.
	 *
	 */
	private void initialise()
	{
		// Create the GUI components and set their values as we go
		JLabel aLabel1 = new JLabel(INTRO_LABEL);
		aLabel1.setFont(itsIntroFont);
		JLabel aLabel2 = new JLabel(PREFIX_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel2.setFont(itsLabelFont);
		JLabel aLabel3 = new JLabel(SLOT_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel3.setFont(itsLabelFont);
		JLabel aLabel4 = new JLabel(SEPARATOR_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel4.setFont(itsLabelFont);
		JLabel aLabel5 = new JLabel(SEPARATOR_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel5.setFont(itsLabelFont);
		JLabel aLabel6 = new JLabel(SEPARATOR_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel6.setFont(itsLabelFont);
		JLabel aLabel7 = new JLabel(SEPARATOR_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel7.setFont(itsLabelFont);
		JLabel aLabel8 = new JLabel(SUFFIX_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel8.setFont(itsLabelFont);
		JLabel aLabel9 = new JLabel(SLOT_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel9.setFont(itsLabelFont);
		JLabel aLabel10 = new JLabel(SLOT_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel10.setFont(itsLabelFont);
		JLabel aLabel11 = new JLabel(SLOT_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel11.setFont(itsLabelFont);
		JLabel aLabel12 = new JLabel(SLOT_COMP_LABEL, LABEL_ALIGNMENT);
		aLabel12.setFont(itsLabelFont);
		int aLabelWidth = GridBagConstraints.RELATIVE;
		int aFieldWidth = GridBagConstraints.REMAINDER;
		
		// Initialise the text fields
		initialiseTextFields();
		
		// Initialise the slot combos
		initialiseSlotFields();
		
		// Lay out the panel
		setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel aMainPanel = new JPanel();
        
        aMainPanel.setLayout(new GridBagLayout());
		GridBagConstraints aConstraintSet = new GridBagConstraints();
	    aConstraintSet.fill = GridBagConstraints.HORIZONTAL;
	    aConstraintSet.ipadx = WIDGET_PADDING_X;
	    aConstraintSet.ipady = WIDGET_PADDING_Y;
	    aConstraintSet.gridwidth = 2;
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 0;
	    aMainPanel.add(aLabel1, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 1;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel2, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 1;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsPrefix, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 2;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel3, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 2;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSlot_1, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 3;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel4, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 3;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSeparator_1, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 4;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel9, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 4;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSlot_2, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 5;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel5, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 5;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSeparator_2, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 6;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel10, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 6;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSlot_3, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 7;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel6, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 7;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSeparator_3, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 8;	    
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel11, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 8;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSlot_4, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 9;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel7, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 9;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSeparator_4, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 10;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel12, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 10;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSlot_5, aConstraintSet);
	    
	    aConstraintSet.gridx = 0;
	    aConstraintSet.gridy = 11;
	    aConstraintSet.gridwidth = aLabelWidth;
	    aMainPanel.add(aLabel8, aConstraintSet);
	    
	    aConstraintSet.gridx = 1;
	    aConstraintSet.gridy = 11;
	    aConstraintSet.gridwidth = aFieldWidth;
	    aMainPanel.add(itsSuffix, aConstraintSet);
	    
	    // Add the controls to the panel
	    itsPanel.add(aMainPanel);
	    
		// Add this tab to the overall config control
		addTab(TAB_NAME, itsPanel);
	}
	
	/**
	 * Initialise the text fields from the persisted Properties
	 * 
	 */
	private void initialiseTextFields()
	{
		// Directly index the AutoText;
		itsPrefix.setText(itsWidget.getItsPrefix());
		itsSeparator_1.setText(itsWidget.getItsSeparator1());
		itsSeparator_2.setText(itsWidget.getItsSeparator2());
		itsSeparator_3.setText(itsWidget.getItsSeparator3());
		itsSeparator_4.setText(itsWidget.getItsSeparator4());
		itsSuffix.setText(itsWidget.getItsSuffix());
	}
	
	/**
	 * Initialise the Slot combo boxes with possible values and then
	 * set the selected values from the persisted properties.
	 */
	private void initialiseSlotFields()
	{
		// Get the list of all allowed Slots.
		loadAllowableSlots();
		
		// Initialise the combos with selected values.
		setSelectedSlots();
	}
	
	/**
	 * Load the set of allowed Slots into itsAllowedSlots
	 *
	 */
	private void loadAllowableSlots()
	{
		TreeSet aSlotList = new TreeSet();
		Cls aCls = itsWidget.getCls();
        Collection aTemplateSlots = aCls.getTemplateSlots();
        Iterator i = aTemplateSlots.iterator();
        while (i.hasNext()) 
        {
            Slot aTemplateSlot = (Slot) i.next();          
            aSlotList.add(aTemplateSlot.getName());
 
            // Map slot name in combo box to actual Slot object.
            itsAllowedSlots.put(aTemplateSlot.getName(), aTemplateSlot);
        }
        
        itsSlotOptionList = new Vector(aSlotList);
        Collections.sort(itsSlotOptionList);
        itsSlotOptionList.add(0, NONE_OPTION);	
	}
	
	/**
	 * Set selected slots
	 * Run through the AutoText setup and set any slot values as required
	 * 
	 */
	private void setSelectedSlots()
	{
		
		// Slot 1
		itsSlot_1 = new JComboBox(itsSlotOptionList);
		Slot aSlot1 = itsWidget.getItsSlot1();
		if(aSlot1 == null)
			itsSlot_1.setSelectedIndex(0);
		else
		{
			String aSlotName = aSlot1.getName();		
			itsSlot_1.setSelectedItem(aSlotName);
		}
		
		// Slot 2
		itsSlot_2 = new JComboBox(itsSlotOptionList);
		Slot aSlot2 = itsWidget.getItsSlot2();
		if(aSlot2 == null)
			itsSlot_2.setSelectedIndex(0);
		else
		{
			String aSlotName = aSlot2.getName();		
			itsSlot_2.setSelectedItem(aSlotName);
		}

		// Slot 3
		itsSlot_3 = new JComboBox(itsSlotOptionList);
		Slot aSlot3 = itsWidget.getItsSlot3();
		if(aSlot3 == null)
			itsSlot_3.setSelectedIndex(0);
		else
		{
			String aSlotName = aSlot3.getName();		
			itsSlot_3.setSelectedItem(aSlotName);
		}
		
		// Slot 4
		itsSlot_4 = new JComboBox(itsSlotOptionList);
		Slot aSlot4 = itsWidget.getItsSlot4();
		if(aSlot4 == null)
			itsSlot_4.setSelectedIndex(0);
		else
		{
			String aSlotName = aSlot4.getName();		
			itsSlot_4.setSelectedItem(aSlotName);
		}

		// Slot 5
		itsSlot_5 = new JComboBox(itsSlotOptionList);
		Slot aSlot5 = itsWidget.getItsSlot5();
		if(aSlot5 == null)
			itsSlot_5.setSelectedIndex(0);
		else
		{
			String aSlotName = aSlot5.getName();		
			itsSlot_5.setSelectedItem(aSlotName);
		}

	}
	
	/**
	 * Save the configuration to the AutoText widget
	 * Run through the panel settings and update the widget
	 */
	private void saveConfigToWidget()
	{
		// Reset the currently stored config.
		itsWidget.resetAutoText();
		
		// Save the prefix.
		itsWidget.setItsPrefix(itsPrefix.getText());
		
		// Save the 1st slot...
		itsWidget.setItsSlot1(getSlotFromSelection(itsSlot_1));
		
		// Save separator
		itsWidget.setItsSeparator1(itsSeparator_1.getText());
		
		// Save the slot...
		itsWidget.setItsSlot2(getSlotFromSelection(itsSlot_2));
		
		// Save separator
		itsWidget.setItsSeparator2(itsSeparator_2.getText());
		
		// Save the slot...
		itsWidget.setItsSlot3(getSlotFromSelection(itsSlot_3));
		
		// Save separator
		itsWidget.setItsSeparator3(itsSeparator_3.getText());
		
		// Save the slot...
		itsWidget.setItsSlot4(getSlotFromSelection(itsSlot_4));

		// Save separator
		itsWidget.setItsSeparator4(itsSeparator_4.getText());
		
		// Save the slot...
		itsWidget.setItsSlot5(getSlotFromSelection(itsSlot_5));

		// Save separator
		itsWidget.setItsSuffix(itsSuffix.getText());
		
	}
	
	/**
	 * Get a reference to the selected slot from the JComboBox.
	 * @param theSlotSelector the combo box that is being used to select
	 * a Slot.
	 * @return the selected Slot or null if nothing is selected - the NONE_OPTION
	 */
	private Slot getSlotFromSelection(JComboBox theSlotSelector)
	{
		Slot aSelectedSlot = null;
		String aSlotName = (String)theSlotSelector.getSelectedItem();
		
		// If a slot has been selected, get a reference to it.
		if(!aSlotName.equals(NONE_OPTION))
		{
			aSelectedSlot = (Slot)itsAllowedSlots.get(aSlotName);
		}
		
		return aSelectedSlot;
	}
	
}
