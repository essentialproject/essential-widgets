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
 * 19.11.2009	JWC	1st coding.
 */
package com.enterprise_architecture.essential.widgets;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

/**
 * Class to provide controls for the automatic layout of the images from the 
 * GraphWidgets. To be included on the EasReportTab when the client is a multiuser client
 * because in multi-user mode, the GraphWidget is not laid out as per the last user arrangement
 * @author Jonathan Carter
 * @version 1.0
 *
 */
public class ImageAutoLayoutPanel implements ActionListener
{
	public final static String LAYOUT_RIGHT = "Left-to-Right";
	public final static String LAYOUT_DOWN = "Top-Down";
	private JRadioButton itsRight = null;
	private JRadioButton itsDown = null;
	private ButtonGroup itsButtonGroup = null;
	private JPanel itsPanel = null;
	private String itsSelection = "";
	private final static String LABEL_STRING = "Image Auto-Layout";
	
	/**
	 * Constructor. Initialise the GUI components and add them to the Panel.
	 */
	public ImageAutoLayoutPanel()
	{
		this("");
	}
	
	/**
	 * Constructor that creates the layout panel and sets the selected button
	 * according to the input parameter (e.g. from the EasReportTab history)
	 * @param theLastSelection the last button that was selected, either 
	 * ImageAutoLayoutPanel.LAYOUT_RIGHT or ImageAutoLayoutPanel.LAYOUT_DOWN
	 */
	public ImageAutoLayoutPanel(String theLastSelection)
	{		
		itsDown = new JRadioButton(LAYOUT_DOWN);
		itsRight = new JRadioButton(LAYOUT_RIGHT);
	
		// Create the buttons, selected as per theLastSelection
		if(theLastSelection.equals(LAYOUT_RIGHT))
		{
			itsRight.setSelected(true);
			itsSelection = LAYOUT_RIGHT;
		}
		else
		{
			itsDown.setSelected(true);
			itsSelection = LAYOUT_DOWN;
		}
		
		// Call the default constructor
		buildPanel();
	}
	
	/**
	 * Build the GUI panel and set it all up
	 */
	private void buildPanel()
	{
		// Radio buttons have already been created, so create rest
		JLabel aControlLabel = new JLabel(LABEL_STRING);
		
		// Button Group
		itsButtonGroup = new ButtonGroup();
		itsButtonGroup.add(itsDown);
		itsButtonGroup.add(itsRight);
		
		// Panel
		itsPanel = new JPanel();
		itsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		Border aLoweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        itsPanel.setBorder(aLoweredetched);
        itsPanel.add(aControlLabel);
        itsPanel.add(itsDown);      
        itsPanel.add(itsRight);
        
        // Register action listeners
        itsDown.addActionListener(this);
        itsRight.addActionListener(this);
	}
	
	/**
	 * Return the selected layout approach.
	 * @return ImageAutoLayoutPanel.LAYOUT_RIGHT for the left-to-right arrangement and 
	 * ImageAutoLayoutPanel.LAYOUT_DOWN for the top-down arrangement.
	 */
	public String getLayout()
	{
		return itsSelection;
	}
	
	/**
	 * Return a reference to the panel that holds the radio buttons
	 * @return the panel for this control
	 */
	public JPanel getItsPanel()
	{
		return itsPanel;
	}
	
	/**
	 * Handle the actions of either button being pressed and update the selection
	 */
	public void actionPerformed(ActionEvent theEvent)
	{
		itsSelection = theEvent.getActionCommand();
	}
}
