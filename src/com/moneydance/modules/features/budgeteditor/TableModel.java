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

import java.text.NumberFormat;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

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

/**
* This class implements the table model for the budget editor table. The 
* table model supplies the data for the table.
*
* @author  Jerry Jones
*/
public class TableModel extends AbstractTableModel  {
    // The column names for the table
    private final String[] columnNames = {"Category","Jan","Feb","Mar","Apr","May","June","July","Aug","Sep","Oct","Nov","Dec","Totals"};
       
    // Main budget editor window
    BudgetEditorWindow window;

    // The context of the extension
    private final FeatureModuleContext context;

    // Budget object for the selected budget
    private Budget budget;

    // The budget year to edit
    private int year;

    // Budget Categories list
    private BudgetCategoriesList budgetCategoriesList;

    public TableModel(final BudgetEditorWindow window, final FeatureModuleContext context, final Budget budget, final String year) {
        // Save main window for later
        this.window = window;
        
        // Save context for later
        this.context = context;

        // Save the budget name for later use
        this.budget = budget;

        // Save the budget year for later
        this.year = Integer.parseInt(year);

        // Load the category and budget data from Moneydance
        this.LoadData();
    }
    
    
    /** 
     * Method to reload the data after the budget or budget year changes.
     * 
     * @param budget - The budget to load.
     * @param year - The budget year to edit.
     */
    public void Update(final Budget budget, final String year) {
        // The selected budget changed, update the table model
        this.budget = budget;
        // Save the year
        this.year = Integer.parseInt(year);

        // Update the table
        this.LoadData ();
    }

    
    /**
     * Method to load the data for the table.
     */
    public void LoadData () {
        // Create a new budget categories list
        this.budgetCategoriesList = new BudgetCategoriesList(); 

        // Create a special category for the Income - Expenses total row
        this.budgetCategoriesList.add("Income-Expenses", Account.AccountType.ROOT, 0);

        // Add a special category to the table for "Income"
        this.budgetCategoriesList.add("Income", Account.AccountType.INCOME, 1);

        // Iterate through the accounts to find all active Income categories
        // Note that accounts and categories are the same, they are all Accounts. 
        for (final Iterator<Account> iter = AccountUtil.getAccountIterator(this.context.getCurrentAccountBook()); iter.hasNext(); ) 
            {
            // Get the account 
            final Account acct = iter.next();

            // Go add category if it's the right type and if it's an income category
            this.addIf(acct, Account.AccountType.INCOME);
            }

        // Add a special category to the table for "Expenses"
        this.budgetCategoriesList.add("Expenses", Account.AccountType.EXPENSE, 1);

        // Iterate through the accounts to find all active Expense categories
        for (final Iterator<Account> iter = AccountUtil.getAccountIterator(this.context.getCurrentAccountBook()); iter.hasNext(); ) 
            {
            // Get the account 
            final Account acct = iter.next();

            // Go add category if it's the right type and if it's an expense category
            this.addIf(acct, Account.AccountType.EXPENSE);
            }

        // Update the table
        this.fireTableDataChanged();
    }


    /**
     * This method adds an account (category) to the budget category list if
     * it meets the right criteria - it must be active and not hidden as well
     * as being the proper type.
     * 
     * @param acct - The account to add 
     * @param type - The account type we're looking for,
     */
    private void addIf(final Account acct, final AccountType type) 
    {
    // Get the budget item list
    final BudgetItemList budgetItemList = this.budget.getItemList();

    // Get the type of this account
    final AccountType acctType = acct.getAccountType();

    // Is the account type that we're looking for?    
    if (acctType == type)
        {
        // Is the account active
        if ((!acct.getAccountOrParentIsInactive()) && (!acct.getHideOnHomePage()))
            {
            // Add this category
            final BudgetCategoryItem item = this.budgetCategoriesList.add(acct);
            if (item == null)
                return;

            // If this is not a roll-up category then we need to get the current budget values for this category
            if (!item.getHasChildren())
                {
                for (int month = 1; month <= 12; month++)
                    {
                    // Find existing budget values for each month
                    final BudgetItem i = budgetItemList.getBudgetItemForCategory(acct, new BudgetPeriod(DateUtil.getDate(this.year, month, 1), PeriodType.MONTH));
                    if (i != null)
                        item.setBudgetValueForMonth(this.window.getModel(), this.budgetCategoriesList, month, i.getAmount(), acctType);
                    }
                }
            }
        }
    }

    /**
     * Method to save the data from the table back to the Moneydance budget.
     */
    public void saveData()
    {
        // Get the budget item list
        final BudgetItemList budgetItemList = this.budget.getItemList();

        // Get the number of items in the budget item list
        final int count = this.budgetCategoriesList.getCategoryCount();

        // Iterate through all BudgetCategoryItem(s)
        for (int index = 0; index < count; index++)
            {
            final BudgetCategoryItem item = this.budgetCategoriesList.getCategoryItemByIndex(index);
            if (item != null)
                {
                // Iterate through all months
                for (int month = 1; month <= 12; month++)
                    {
                    // If the data changed flag for the month in BudgetCategoryItems is true process that month 
                    if (item.getDataChangedForMonth(month))
                        {
                        final BudgetItem i = budgetItemList.getBudgetItemForCategory(item.getAccount(), new BudgetPeriod(DateUtil.getDate(this.year, month, 1), PeriodType.MONTH));
                        if (i != null)
                            {
                            // Save the new budget at the old BudgetItem
                            i.setAmount(item.getBudgetValueForMonth(month));
                            i.syncItem();
                            }
                        else
                            {
                            // Create a new BudgetItem and save the new budget
                            // If value to write is 0 and there is no prior budget for this period then we don't need to create one
                            if (item.getBudgetValueForMonth(month) != 0)
                                {
                                final BudgetItem newItem = this.budget.createItem();
                                newItem.setTransferAccount(item.getAccount());
                                newItem.setIntervalStartDate(DateUtil.getDate(this.year, month, 1));
                                newItem.setIntervalEndDate(DateUtil.lastDayInMonth(DateUtil.getDate(this.year, month, 1)));
                                newItem.setAmount(item.getBudgetValueForMonth(month));
                                newItem.setInterval(BudgetItem.INTERVAL_MONTHLY);
                                newItem.syncItem();
                                }
                            }
                        // Clear each BudgetItem data changed flag as it is processed
                        item.setDataChangedForMonth(month, false);
                        }
                    }
                }
            else
                System.err.println("ERROR: Item is null in saveData.");
            }

        // Clear the global data changed flag
        this.window.setDataChanged(false);
    }
    
    
    /** 
     * Method to return a BudgetCategoryItem object given the row from the table.
     * 
     * @param row - The row from the table which is the index into the category items.
     * @return BudgetCategoryItem - The selected BudgetCategoryItem or null if there isn't one.
     */
    public BudgetCategoryItem getBudgetCategoryItem(final int row) {
        return this.budgetCategoriesList.getCategoryItemByIndex(row);
    }

    
    /** 
     * Method to get the BudgetCategoryList object.
     * 
     * @return BudgetCategoriesLis - The budgetCategoryList object requested.
     */
    public BudgetCategoriesList getBudgetCategoriesList() {
        return this.budgetCategoriesList;
    }

    
    /** 
     * Method to get the budget year.
     * 
     * @return int - The budget year.
     */
    public int getBudgetYear() {
        return this.year;
    }

    
    /** 
     * Method to get the number of columns in the table.
     * 
     * @return int - Number of columns.
     */
    @Override
    public int getColumnCount() {
        return this.columnNames.length;
    }

    
    /** 
     * Method to get the number of rows (number of categories).
     * 
     * @return int - Number of rows.
     */
    @Override
    public int getRowCount() {
        return this.budgetCategoriesList.getCategoryCount();
    }

    
    /** 
     * Method to get the column name.
     * 
     * @param column - The column index [0...13].
     * @return String - The column name.
     */
    @Override
    public String getColumnName(final int column) {
        return this.columnNames[column];
    }

    
    /** 
     * Method to get the value at a specific row and column.
     * 
     * @param row - The row in the table.
     * @param column - The column in the table.
     * @return Object - The value at the specified row and column.
     */
    @Override
    // Returns a Double for editable cells - be careful when saving
    public Object getValueAt(final int row, final int column) {
        // Get the category item
        final BudgetCategoryItem item = this.budgetCategoriesList.getCategoryItemByIndex(row);
        if (item != null)
            {
            // Category names
            if ( column == 0)  
                {
                // Display the category indented per the indent level
                return (item.getIndentLevel() == 0) ? 
                    "     "+item.getShortName() : 
                    String.format(" %1$" + item.getIndentLevel() * 6 + "s%2$s", "", item.getShortName());
                }
            // Budget values and totals
            else 
                {
                if (column < 13)
                    return (Double)(item.getBudgetValueForMonth(column) / 100.0d);
                else
                    // Add spacing to right end of table data
                    return NumberFormat.getCurrencyInstance().format((Double)(item.getBudgetValueForMonth(column) / 100.0d))+"    ";
                }
            }
        else
            {
            System.err.println("ERROR: Item is null in getValueAt.");
            return null;
            }
    }

    
    /** 
     * Method to get the class for the requested column.
     * 
     * @param column - The column in the table.
     * @return Class<?> - The class for the column.
     */
    @Override
    public Class<?> getColumnClass(final int column) {
        return this.getValueAt(0, column).getClass();
    }

    
    /** 
     * Method to determine if a cell is editable,
     * 
     * @param row - The row in the table.
     * @param column - The column in the table.
     * @return boolean - Returns true if the cell is editable, false otherwise.
     */
    @Override
    public boolean isCellEditable(final int row, final int column) {
        if ((column > 0) && (column <= 12)) 
            {
            final BudgetCategoryItem item = this.budgetCategoriesList.getCategoryItemByIndex(row);
            if (item != null)
                {
                if (!item.getHasChildren()) 
                    {
                    // budget cells on rows that do not have children are editable 
                    return true;
                    }
                }
            else
                System.err.println("ERROR: Item is null in isCellEditable.");
            }
        // Column 0 (Category name), column 13 (Totals) and rows that are for categories that 
        // have children are not editable
        return false;    
    }
    
    
    /** 
     * Method to set the value at a specific row and column.
     * 
     * @param value - The new value for the cell.
     * @param row - The row in the table.
     * @param column - The column in the table.
     */
    @Override    
    public void setValueAt(final Object value, final int row, final int column) {
        // Calculated long value to store
        long lv;

        // Get the item to update
        final BudgetCategoryItem item = this.budgetCategoriesList.getCategoryItemByIndex(row);
        if (item != null)
            {
            // Calculate the new edited value
            if (value instanceof Double)
                lv = Math.round((Double)value * 100d);
            else if (value instanceof String)
                lv = Math.round(Double.parseDouble((String)value) * 100d);
            else
                lv = Math.round(Double.parseDouble(value.toString()) * 100d);
                
            // Update the data only if the new value is different than the old value
            if (item.getBudgetValueForMonth(column) != lv)
                {
                // Set the budget value for the month
                item.setBudgetValueForMonth(this.window.getModel(), this.budgetCategoriesList, column, lv, item.getCategoryType());
        
                // Mark this cell as changed so we know what to update
                item.setDataChangedForMonth(column, true);

                // Set the global data changed flag as well
                this.window.setDataChanged(true);

                // Notify all listeners that the value of the cell at [row, column] has been updated.
                this.fireTableCellUpdated(row, column);
                }
            }
        else
            System.err.println("ERROR: Item is null in setValueAt.");
    }
}
