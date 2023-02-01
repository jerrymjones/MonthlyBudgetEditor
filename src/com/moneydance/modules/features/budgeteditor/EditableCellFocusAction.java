/*
    This code provided by Rob Camick (See https://tips4java.wordpress.com/2008/11/04/table-tabbing/)
    We assume no responsibility for the code. You are free to use and/or modify and/or distribute any or all code posted on the Java Tips Weblog without 
    restriction. A credit in the code comments would be nice, but not in any way mandatory.

    The default Tab Action in a JTable causes focus to move to the next cell. When the end of line is reached focus move to the first cell of the next line. 
    When the end of the table is reached focus moves to the top of the table. There may be times when you want to change this default behaviour.

    For example, maybe you want focus to move to the next editable cell in the table. Do you really need to write a custom Tab Action from scratch or can you 
    leverage the functionality of the default Tab Action? If you have read my previous entry on Wrapping Actions then you know the answer, if not then maybe 
    now is a good time to take a quick read. We will attempt to use the functionality of the default Tab Action to satisfy this requirement.

    All we really need to do is create a loop and keep invoking the default Tab Action until focus is on an editable cell. The EditableCellFocusAction class 
    does exactly this. It is easily installed with a single line of code:

    new EditableCellFocusAction(table, KeyStroke.getKeyStroke("TAB"));

    Why stop there we can use this Action for other KeyStrokes as well:

    new EditableCellFocusAction(table, KeyStroke.getKeyStroke("shift TAB"));
    new EditableCellFocusAction(table, KeyStroke.getKeyStroke("RIGHT"));
    new EditableCellFocusAction(table, KeyStroke.getKeyStroke("LEFT"));
    new EditableCellFocusAction(table, KeyStroke.getKeyStroke("UP"));
    new EditableCellFocusAction(table, KeyStroke.getKeyStroke("DOWN"));
 */
package com.moneydance.modules.features.budgeteditor;

import java.awt.event.ActionEvent;

import javax.swing.JTable;
import javax.swing.KeyStroke;

/**
* Class to extend WrappedAction to enhance tabbing action across a table
*
* The default Tab Action in a JTable causes focus to move to the next cell. When
* the end of line is reached focus move to the first cell of the next line. 
* When the end of the table is reached focus moves to the top of the table. 
* There may be times when you want to change this default behaviour so that
* a tab moves to the next editable cell.
*
* @author  Rob Camick
*/
public class EditableCellFocusAction extends WrappedAction
{
	private final JTable table;

	/**
	 * Constructor to specify the component and KeyStroke for the Action we want
	 * to wrap.
	 * 
	 * @param table - The JTable this action applies to.
	 * @param keyStroke - The keystroke to act on.
	 */
	public EditableCellFocusAction(final JTable table, final KeyStroke keyStroke)
	{
		super(table, keyStroke);
		this.table = table;
	}

	
	/** 
	 * This method performs the enhanced action of moving to the next editable cell.
	 * 
	 * @param e - The action event that brought us here.
	 */
	public void actionPerformed(final ActionEvent e)
	{
		final int originalRow = this.table.getSelectedRow();
		final int originalColumn = this.table.getSelectedColumn();

		// Invike the original action for this event
		this.invokeOriginalAction( e );

		// Get the currently selected row and column
		int row = this.table.getSelectedRow();
		int column = this.table.getSelectedColumn();

		//  Keep invoking the original action until we find an editable cell
		while (! this.table.isCellEditable(row, column))
			{
			// Perform the original action again
			this.invokeOriginalAction( e );

			//  We didn't move anywhere, reset cell selection and get out.
			if (row == this.table.getSelectedRow() &&  column == this.table.getSelectedColumn())
				{
				this.table.changeSelection(originalRow, originalColumn, false, false);
				break;
				}

			// Get the new row and column
			row = this.table.getSelectedRow();
			column = this.table.getSelectedColumn();

			//  Back to where we started, get out.
			if (row == originalRow &&  column == originalColumn)
				break;
			}
	}
}