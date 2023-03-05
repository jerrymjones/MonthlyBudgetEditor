/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022-2023, Jerry Jones
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 
package com.moneydance.modules.features.budgeteditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.Account.AccountType;
import com.infinitekind.moneydance.model.AccountUtil;
import com.infinitekind.moneydance.model.Budget;
import com.infinitekind.moneydance.model.BudgetItem;
import com.infinitekind.moneydance.model.BudgetItemList;
import com.infinitekind.moneydance.model.BudgetPeriod;
import com.infinitekind.moneydance.model.PeriodType;
import com.infinitekind.util.DateUtil;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.apps.md.view.gui.MDColors;
import com.moneydance.awt.EditingTable;

/**
* This class extends EditingTable to provide additional features such as pop-up
* menu support to ease editing of budgets based on a variety of methods, and 
* budget initialization from prior year's actuals or budget.
*
* @author  Jerry Jones
*/
public class Table extends EditingTable {
	private final BudgetEditorWindow window;
    private final FeatureModuleContext context;
	private final MDColors colors;
	private final TableModel model;

	// The table instance for the event handles to use
	private final Table table;

	// Popup menu
	private JPopupMenu 	popMenu;
	private int popRow = -1;
	private int popColumn = -1;

	// Save these menu items so that they can be handled specially in the popup
	JMenuItem menuItemPrevious;
	JMenuItem menuItemRolloverPrior;
	JMenuItem menuItemRolloverAll;
	JMenuItem menuItemSettoPriorSpend;

	/**
	* Constructor for our table.
	*
	* @param window - The budget editor window object.
	* @param context - The context for this extension.
	* @param model - The table model for this table.
	* @param colors - Moneydance color scheme.
	*/
	public Table(final BudgetEditorWindow window, final FeatureModuleContext context, final TableModel model, final MDColors colors) {
       	super(model);

		// Save the main window object for later
		this.window = window;

		// Save the context for later
		this.context = context;

		// Save the table model for later use
		this.model = model;

		// Save access to Moneydance colors
        this.colors = colors;

		// Save this table instance for the event handler
		this.table = this;

		// Set selection parameters
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setCellSelectionEnabled(false);

		// Some themes have too small a row height so try to fix that here
		// Also, this makes the overall table look less squished.
		this.setRowHeight(30); 

		// Set column editor and renderer
        for (int i = 1; i < this.getColumnCount(); i++ ) 
			{
            final TableColumn colSelect = this.getColumnModel().getColumn(i);
            colSelect.setCellRenderer(new CurrencyTableCellRenderer());
			if (i != this.getColumnCount() - 1) // Don't set for totals column
				colSelect.setCellEditor(new CurrencyTableCellEditor(this, new JTextField()));
        	}
		
		// Only allow tabbing, etc. to editable cells
		new EditableCellFocusAction(this.table, KeyStroke.getKeyStroke("TAB"));
		new EditableCellFocusAction(this.table, KeyStroke.getKeyStroke("shift TAB"));
		new EditableCellFocusAction(this.table, KeyStroke.getKeyStroke("RIGHT"));
		new EditableCellFocusAction(this.table, KeyStroke.getKeyStroke("LEFT"));
		new EditableCellFocusAction(this.table, KeyStroke.getKeyStroke("UP"));
		new EditableCellFocusAction(this.table, KeyStroke.getKeyStroke("DOWN"));

		// Add a mouse listener to show the pop-up menu
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent me) {
				Table.this.doShowPopup(me);
			}
		});	
	}


	/** 
	 * Prepare the renderer for the cell at the specified row and column.
	 * 
	 * @param renderer - The table cell renderer object.
	 * @param row - The row to prepare.
	 * @param column - The column to prepare.
	 * @return Component - The component being rendered.
	 */
	@Override
	public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) 
	{
		final Component c = super.prepareRenderer(renderer, row, column);

		// Alternate row colors in table for readability
		if ((row % 2) != 0) 
			c.setBackground(this.colors.headerBG);
		else
			c.setBackground(this.colors.listBackground); 

		// Set text color of totals. Other than the category name in column 0, any other 
		// cell could potentially be a total so we color it specially.
		if ((!this.model.isCellEditable(row, column)) && ((column > 0) || (this.model.getBudgetCategoryItem(row).hasChildren())))
			{
			if (row == 0)
            	c.setForeground(new Color(33, 144, 255));	// Medium blue
			else
				c.setForeground(new Color(0, 204, 204));	// Dark Cyan
			}
		else
			{
			// Ensures proper foreground color on all themes
			c.setForeground(this.colors.defaultTextForeground);
			}
		
		// Moneydance doesn't really support negative budgets for some odd reason so highlight those values red
		final Object value = this.table.getValueAt(row, column);
		if (value instanceof Number)
			{
			if (((Double)value < 0) && (this.model.isCellEditable(row, column)))
				c.setForeground(Color.RED);
			}

		// Highlight a right-clicked cell
		if ((row == this.popRow) && (column == this.popColumn))
			c.setBackground(new Color(51, 153, 255));

		// Remove border from the cells. The editor will still set the selection border. This makes uneditable cells also appear unselectable.
		((JComponent) c).setBorder(null);

		// Return the component object being rendered
		return c;   
	}


	/**
	 * Method called by the budget editor window from the Initialize Budget button
	 * to initialize the selected budget and year with a prior year's budget.
	 */
	public void copyPriorBudget() {
		// Get the list of available budgets
		final MyBudgetList budgetList = new MyBudgetList(this.context);
        if (budgetList.getBudgetCount() == 0)
            {
            // Display an error message - No budgets exist!
            JOptionPane.showMessageDialog( this,
            "No monthly style budgets have been created.  Use 'Tools:Budget Manager' to create a monthly budget before using this extension.",
            "Error (Monthly Budget Editor)",
            JOptionPane.ERROR_MESSAGE);
            
            return;
            }

		// Show a dialog to enable selection of the budget to copy from.
		final String strNames[] = budgetList.getBudgetNames();
		final String budgetName = (String) JOptionPane.showInputDialog(this.table.getParent(), 
		"Select the budget you wish to copy from. Note that only monthly type budgets can be copied.",
        "Select Budget", 
		JOptionPane.QUESTION_MESSAGE, null,
        strNames,
        strNames[this.window.getBudgetIndex()]); 	// Initial choice
		
		// Get the Budget object for the budget selected
		final Budget budget = budgetList.getBudget(budgetName);

		// Get our current budget categories list
		final BudgetCategoriesList budgetCategoriesList = this.model.getBudgetCategoriesList();

		// Get the BudgetItemList for the prior year
		final BudgetItemList priorBudgetItemList = budget.getItemList();

		// Iterate through all accounts to find prior year's budget information
        for (final Iterator<Account> iter = AccountUtil.getAccountIterator(this.context.getCurrentAccountBook()); iter.hasNext(); ) 
            {
            // Get the account 
            final Account acct = iter.next();

			// Get the type of this account
			final AccountType acctType = acct.getAccountType();

			// Is the account active
			if ((!acct.getAccountOrParentIsInactive()) && (!acct.getHideOnHomePage()))
				{
				if ((acctType == Account.AccountType.INCOME) || (acctType == Account.AccountType.EXPENSE))
					{
					// Get the BudgetCategoryIem by the key (full name)
					final BudgetCategoryItem item = budgetCategoriesList.getCategoryItem(acct.getUUID());   
					if (item != null)
						{
						// If this is not a roll-up category then we need to get the current budget values for this category
						if (!item.hasChildren())
							{
							for (int month = 1; month <= 12; month++)
								{
								Long value;

								// Find existing budget values for each month
								final BudgetItem i = priorBudgetItemList.getBudgetItemForCategory(acct, new BudgetPeriod(DateUtil.getDate(this.model.getBudgetYear() - 1, month, 1), PeriodType.MONTH));
								if (i != null)
									value = i.getAmount();
								else
									value = 0l;

								// Save the new value if it changed the old value
								if (item.getBudgetValueForMonth(month) != value)
									{
									// Save the new value
									item.setBudgetValueForMonth(this.model, budgetCategoriesList, month, value, acctType); 
						
									// Mark this cell as changed so we know what to update
									item.setDataChangedForMonth(month, true);
						
									// Set the global data changed flag as well
									this.window.setDataChanged(true);
									}
								}
							}
						}
					else
						System.err.println("ERROR: "+acct.getFullAccountName()+" not found in categories list.");
					}
				}
			}
	}


	/**
	 * Method called by the budget editor window from the Initialize Budget button 
	 * to initialize the selected budget and year with a prior year's actuals.
	 */	
	public void copyPriorActuals() {
		// iterate through all rows
		for (int row = 0; row < this.model.getRowCount(); row++) 
			{
			// Get the budget category item
			final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
			if (item != null)
				{
				// Get actual spending by month for last year 
				if (!item.hasChildren())
					{
					// Retrieve the transaction totals for this account from last year
					final TransactionTotals actualSpending = new TransactionTotals(this.context, item.getAccount(), this.model.getBudgetYear() - 1, 1, 12);

					// Set each months budgets equal to actual spending for last year and the same month
					for (int i = 1; i <= 12; i++)
						{
						// Get the actual spending
						Double v = (double)actualSpending.getTotals()[i] / 100d;

						// If this is an income category then the sign has to be changed
						if (item.getCategoryType() == Account.AccountType.INCOME)
						v = v * -1;

						// Save the new value
						this.model.setValueAt(v, row, i);
						}
					}
				}
			else
				System.err.println("ERROR: Item is null in copyPriorActuals.");
			}
	}


	/** 
	 * This method shows a pop-up when the right mouse button is clicked on a cell.
	 * This is used when the cell is not in edit mode. See the CurrencyTableCellEditor
	 * class for the pop-up method used when a cell is in edit mode.
	 * 
	 * @param me - The mouse event that triggered this method.
	 */
	public void doShowPopup(final MouseEvent me) {
		// Create new popup menu
		this.popMenu = new JPopupMenu();

		// is this event a popup trigger?
		if (this.popMenu.isPopupTrigger(me))
			{
			final Point p = me.getPoint();
			final int row = this.rowAtPoint(p);
			final int column = this.columnAtPoint(p);

			this.showPopup(this.popMenu, row, column, me.getX(), me.getY());
			}
	}


	/** 
	 * This method is called from CurrencyTableCellEditor when a cell is right
	 * clicked while in edit mode to bring up the pop-up menu.
	 * 
	 * @param popMenu - The popup menu object created by the cell editor.
	 * @param row - The row where the cell was right clicked.
	 * @param column - The column where the cell was right clicked.
	 */
	public void doShowPopup(final JPopupMenu popMenu, final int row, final int column) {
		// Since the user clicked on the editor for this cell we need to calculate a good place for the popup
		// to be placed. We'll set the location to the mid point of the selected cell.
		final Rectangle r = this.table.getCellRect(row, column, false);
		this.showPopup(popMenu, row, column, r.x+((r.width * 3) / 4), r.y+(r.height / 2));
	}


	/** 
	 * This method shows the pop-up menu when brought up by right clicking on an
	 * editable cell.
	 * 
	 * @param popMenu - The popup menu object previously created.
	 * @param row - The row where the cell was right clicked.
	 * @param column - The column where the cell was right clicked.
	 * @param x - The x location where the mouse was clicked.
	 * @param y - The y location where the mouse was clicked.
	 */
   	public void showPopup(final JPopupMenu popMenu, final int row, final int column, final int x, final int y) {
		// Only process editable cells
		if ((column > 0) && (!this.model.getBudgetCategoryItem(row).hasChildren())) 
			{
			// Stop editing in case a different cell was in edit mode
			final TableCellEditor editor = this.table.cellEditor;
			if (editor != null)
				editor.stopCellEditing();
				
			// Select the cell where the mouse was clicked
			this.table.changeSelection(row, column, false, false);

			// Save the right clicked cell so that we can highlight it 
			this.popRow = row;
			this.popColumn = column;

			// Set up the appropriate pop-up menu items for the cell clicked
			if (column == 13) // Totals
				{
				this.addPopupMenuItem(popMenu, "menuItemDistributeTotal", "Distribute the total across all months", null, this.popListener);
				this.addPopupMenuItem(popMenu, "menuItemSetToActuals", "Set all months to actual spending", null, this.popListener);
				}
			else
				{
				// Enable appropriate menu items based on the month 
				if (column != 1) // There is no previous period in January
					this.addPopupMenuItem(popMenu, "menuItemPrevious", "Apply previous period's budget", null, this.popListener);
				if (column != 12) // Doesn't make sense in December
					this.addPopupMenuItem(popMenu, "menuItemCopytoEOY", "Apply selected cell to end of year", null, this.popListener);
				this.addPopupMenuItem(popMenu, "menuItemCopytoAll", "Apply selected cell to entire year", null, this.popListener);
				popMenu.addSeparator();
				this.addPopupMenuItem(popMenu, "menuItemSettoActualSpend", "Set budget equal to actual spending for the month", null, this.popListener);
				if (column != 1) // There is no prior item in January
					{
					this.addPopupMenuItem(popMenu, "menuItemSettoPriorSpend", "Set budget equal to actual spending from the previous month", null, this.popListener);
					popMenu.addSeparator();
					this.addPopupMenuItem(popMenu, "menuItemRolloverPrior", "Rollover balance from prior month", null, this.popListener);
					this.addPopupMenuItem(popMenu, "menuItemRolloverAll", "Rollover balance from all prior months", null, this.popListener);
					}
				}

				// Add a listener to unset the cell highlight for a right clicked cell if the menu is cancelled 
				popMenu.addPopupMenuListener(new PopupMenuListener() {
					@Override
					public void popupMenuCanceled(final PopupMenuEvent popupMenuEvent) {
						// Remove the cell highlight for the cell that was right clicked
						Table.this.popRow = -1;
						Table.this.popColumn = -1;
						Table.this.model.fireTableCellUpdated(row, column);
					}
					@Override
					public void popupMenuWillBecomeInvisible(final PopupMenuEvent popupMenuEvent) {}
					@Override
					public void popupMenuWillBecomeVisible(final PopupMenuEvent popupMenuEvent) {}
				});

			// Show the popup
			popMenu.show(this.table, x, y);
			}
	}

	
	/** 
	 * This method provides a way to add a pop-up menu item in one line of code,
	 * 
	 * @param menu - The pop-up menu to add the item to.
	 * @param identifier - The identifier that will be used to determine what item was selected.
	 * @param text - The text of the pop-up menu item.
	 * @param tooltip - A tooltip for the menu item or null if none is desired.
	 * @param listener - The action listener for this item.
	 * @return JMenuItem - Returns the new menu item object.
	 */
	private JMenuItem addPopupMenuItem(final JPopupMenu menu, final String identifier, final String text, final String tooltip, final ActionListener listener)
	{
		// Create a new menu item
		final JMenuItem menuItem = new JMenuItem();

		// Add an action listener for the menu item
		menuItem.addActionListener(listener);

		// Add a name to identify the event later
		menuItem.setName(identifier);

		// Add text to display for this menu item
		menuItem.setText(text);

		// Set the tooltip text
		if (tooltip != null)
			menuItem.setToolTipText(tooltip);

		// Add popup menu selections to the menu
		menu.add(menuItem);

		// Return the menu item object
		return menuItem;
	}

	
	/**
	 * Create the action listener to receive menu item events.
	 */
    ActionListener popListener = new ActionListener () {
		@Override
		public void actionPerformed(final ActionEvent event) {
			final String cmd = ((JMenuItem) event.getSource()).getName();
			final int row = Table.this.table.getSelectedRow();
			final int column = Table.this.table.getSelectedColumn();

			switch(cmd) {
				// 	Use previous period Budget (Jan is special)
				case "menuItemPrevious":
					Table.this.previous(row, column);
					break;
				
				// Apply selected cell to end of year
				case "menuItemCopytoEOY":
					Table.this.copytoEOY(row, column);
					break;

				// Apply selected cell to entire year
				case "menuItemCopytoAll":
					Table.this.copytoAll(row, column);
					break;
	
				// Rollover balance from prior month (Jan is special - do not go to previous year)
				case "menuItemRolloverPrior":
					Table.this.rolloverPrior(row, column);
					break;

				// Rollover balance from prior months back to January (Jan is special - do not go to previous year)
				case "menuItemRolloverAll":
					Table.this.rolloverAll(row, column);
					break;
				
				// Set budget equal to actual spending for month
				case "menuItemSettoActualSpend":
					Table.this.settoActualSpend(row, column);
					break;
				
				// Set budget equal to actual spending from previous month (Jan is special)	
				case "menuItemSettoPriorSpend":
					Table.this.settoPriorSpend(row, column);
					break;

				case "menuItemDistributeTotal":
					Table.this.distributeTotal(row, column);
					break;

				case "menuItemSetToActuals":
					Table.this.setToActuals(row, column);
					break;
			}

		// Remove the cell highlight for the cell that was right clicked
		Table.this.popRow = -1;
		Table.this.popColumn = -1;
		Table.this.model.fireTableCellUpdated(row, column);
		}
	};
	

	/** 
	 * Method to copy the previous month's budget value to this month.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void previous(final int row, final int column)
	{
		this.model.setValueAt(this.model.getValueAt(row, column-1), row, column);
	}

	
	/** 
	 * Method to copy the selected cell's value to the end of the year.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void copytoEOY(final int row, final int column)
	{
		final Object cellValue = this.model.getValueAt(row, column);
		for (int i = column + 1; i < 13; i++)
			this.model.setValueAt(cellValue, row, i);
	}

	
	/** 
	 * Method to copy the selected cell's value to the entire year.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void copytoAll(final int row, final int column)
	{
		final Object cellValue = this.model.getValueAt(row, column);
		for (int i = 1; i < 13; i++)
			{
			if (i != column)
				this.model.setValueAt(cellValue, row, i);
			}
	}

	
	/** 
	 * Method to rollover the previous month's value to this month. This will
	 * set the previous month's budget to the actual spending for that month and 
	 * then add or subtract any remainder to the budget for the selected
	 * month.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void rolloverPrior(final int row, final int column)
	{
		// Get the budget category item
		final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
		if (item != null)
			{
			// Get total actual spending last month plus this month
			final TransactionTotals actualSpending = new TransactionTotals(this.context, item.getAccount(), this.model.getBudgetYear(), column - 1, 2);
			
			// Get total amount budgeted last month + this month
			final Long totalBudget = item.getBudgetValueForMonth(column - 1) + item.getBudgetValueForMonth(column);

			// Set last months budget equal to actual spending for the month
			Double v = actualSpending.getTotals()[column - 1] / 100d;

			// If this is an income category then the sign has to be changed
			if (item.getCategoryType() == Account.AccountType.INCOME)
				v = v * -1;

			// Save the new value for the prior month
			this.model.setValueAt(v, row, column - 1);

			// Subtract last month's actual spending from the total amount budgeted and store as this month's new budget
			// If this is an income category then the sign has to be changed
			if (item.getCategoryType() == Account.AccountType.INCOME)
				v = (double)(totalBudget + actualSpending.getTotals()[column - 1]) / 100d;
			else		
				v = (double)(totalBudget - actualSpending.getTotals()[column - 1]) / 100d;

			// Save the new value for the current month
			this.model.setValueAt(v, row, column);
			}
		else
			System.err.println("ERROR: This month's item is null in rolloverPrior.");
	}

	
	/** 
	 * Method to rollover the balance from all prior months back to January of
	 * the current budget year. This will set all the prior months budgets to 
	 * the actual spending for those months and then add or subtract any remainder
	 * from their original budget to this month's budgeted amount.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void rolloverAll(final int row, final int column)
	{
		Long totalBudget = 0l;
		Long totalSpending  = 0l;
		int i;
		Double v;

		// Get the budget category item
		final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
		if (item != null)
			{
			// Get total actual spending since the beginning of the year including this month
			final TransactionTotals actualSpending = new TransactionTotals(this.context, item.getAccount(), this.model.getBudgetYear(), 1, column);
			
			// Get total amount budgeted for the year to date (up to the end of the selected month)
			for (i = 1; i < column + 1; i++)
				totalBudget += item.getBudgetValueForMonth(i);

			// Set prior months budgets equal to actual spending for each month
			for ( int month = 1; month < column; month++)
				{
				// Get prior spending
				Long spend = actualSpending.getTotals()[month];

				// If this is an income category then the sign has to be changed
				if (item.getCategoryType() == Account.AccountType.INCOME)
					spend = spend * -1;

				totalSpending += spend;
				v = (double)spend / 100d;
				this.model.setValueAt(v, row, month);
				}

			// Subtract the prior months actual spending from the total amount budgeted for the period and store as this month's new budget
			v = (totalBudget - totalSpending) / 100d;
			this.model.setValueAt(v, row, column);
			}
		else
			System.err.println("ERROR: Item is null in rolloverAll.");
	}

	
	/** 
	 * Method to set the selected cell's budget equal to actual spending for
	 * the month.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void settoActualSpend(final int row, final int column)
	{
		// Get the budget category item
		final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
		if (item != null)
			{
			// Get this month's actuals
			final TransactionTotals actualTotals = new TransactionTotals(this.context, item.getAccount(), this.model.getBudgetYear(), column, 1);

			// Set the selected cell equal to the prior month actuals
			Double v = actualTotals.getTotals()[column] / 100d;

			// If this is an income category then the sign has to be changed
			if (item.getCategoryType() == Account.AccountType.INCOME)
				v = v * -1;

			// Set the new value
			this.model.setValueAt(v, row, column);
			}
		else
			System.err.println("ERROR: Item is null in settoActualSpend.");
	}

	
	/** 
	 * Method to set the selected cell's budget equal to the actual spending
	 * from the previous month.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void settoPriorSpend(final int row, final int column)
	{
		final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
		if (item != null)
			{
			// Get prior month's actuals
			final Account acct = item.getAccount();
			final TransactionTotals actualTotals = new TransactionTotals(this.context, acct, this.model.getBudgetYear(), column - 1, 1);

			// Set the selected cell equal to the prior month totals
			final Double v = actualTotals.getTotals()[column - 1] / 100d;
			this.model.setValueAt(v, row, column);
			}
		else
			System.err.println("ERROR: Item is null in settoPriorSpend.");	
	}
    
	
	/** 
	 * Method to distribute the total budget for this category evenly across all
	 * months.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void distributeTotal(final int row, final int column)
	{
		// Get the budget category item
		final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
		if (item != null)
			{
			// Get the budget total
			final long cellValue = item.getBudgetTotal();

			// Calculate monthly amount and final December amount which may be different
			final long monthlyValue = cellValue / 12;
			final long decemberValue = cellValue - (monthlyValue * 11);

			// Now spread the value across all months except december
			for (int i = 1; i < 12; i++)
				this.model.setValueAt((Double)(monthlyValue / 100d), row, i);

			// Now set December to whatever is left over
			this.model.setValueAt((Double)(decemberValue / 100d), row, 12);
			}
		else
			System.err.println("ERROR: Item is null in distributeTotal(.");
	}

	/** 
	 * Method to set all months to the actual spending for the month.
	 * 
	 * @param row - The row where the right mouse click occurred.
	 * @param column - The column where the right mouse click occurred.
	 */
	private void setToActuals(final int row, final int column) {
		Double v;

		// Get the budget category item
		final BudgetCategoryItem item = this.model.getBudgetCategoriesList().getCategoryItemByIndex(row);
		if (item != null)
			{
			// Get total actual spending since the beginning of the year including this month
			final TransactionTotals actualSpending = new TransactionTotals(this.context, item.getAccount(), this.model.getBudgetYear(), 1, column);

			// Set prior months budgets equal to actual spending for each month
			for ( int month = 1; month <= 12; month++)
				{
				// Get prior spending
				Long spend = actualSpending.getTotals()[month];

				// If this is an income category then the sign has to be changed
				if (item.getCategoryType() == Account.AccountType.INCOME)
					spend = spend * -1;

				v = (double)spend / 100d;
				this.model.setValueAt(v, row, month);
				}
			}
		else
			System.err.println("ERROR: Item is null in setToActuals.");
	}
}
