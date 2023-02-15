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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;

import com.infinitekind.moneydance.model.Budget;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.apps.md.view.gui.MDColors;
import com.moneydance.awt.AwtUtil;
import com.moneydance.awt.GridC;

/**
* Create the main window of the Budget Editor extension.
* This class creates the main window of the Budget Editor including the controls
* and table used to enter budget data. 
*
* @author  Jerry Jones
*/
public class BudgetEditorWindow extends JFrame
{
  // Display Constants
  private static final int TOP_HEIGHT = 75;
  private static final int BOTTOM_HEIGHT = 75;

  // Calculated frame width and height
  private int frameWidth;
  private int frameHeight;

  // Flag used to return error status back to Main
  public boolean bError = false;

  // Storage for the Main extension object
  private Main extension;

  // Extension context
  private FeatureModuleContext context;

  // The budget selection control
  private JComboBox<String> budgetSelector;

  // List of available monthly budgets that can be edited
  private MyBudgetList budgetList;
  private int budgetIndex;

  // Budget map
  private final Map<String,Budget> mapBudgets = new HashMap<String,Budget>();

  // The year selector control
  private JComboBox<String> yearSelector;
  private int yearIndex;

  // Storage for the table used to edit budget data
  private Table table;
  TableModel tableModel = null;

  // Panels used to display information
  JPanel topLtPanel;
  JPanel topRtPanel;

  // Clickable User Guide link
  JLabel helpLink;

  // Global data changed flag
  private boolean dataChanged;

  /** 
   * Default constructor for the BudgetEditorWindow.
   */
  public BudgetEditorWindow(final Main extension) 
  {
    // Title for main window
    super("Monthly Budget Editor");

    // Save the extension for later
    this.extension = extension;

    // Get FeatureModule context
    this.context = extension.getUnprotectedContext();

    // Get the colors for the current Moneydance theme
    final MDColors colors = com.moneydance.apps.md.view.gui.MDColors.getSingleton();

    // Set the global data changed flag to indicate the data has not changed 
    this.dataChanged = false;

    /*
    * Configure the frame for our data entry screen
    */

    // Set the frame size based on the screen size of the primary display
    this.setFrameSize();

    // Set what to do on close
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.enableEvents(WindowEvent.WINDOW_CLOSING);

    // Center the frame on the screen
    AwtUtil.centerWindow(this);

    /*
    * Add the Top Panel - Configuration Options
    */
    final JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setBackground(colors.headerBG);
    this.add( topPanel, BorderLayout.NORTH );

    /*
    ** Create a center panel at the top for the selectors for budget to edit
    ** and budget year.
    */
    final JPanel topCtrPanel = new JPanel(new GridBagLayout());
    topCtrPanel.setBackground(colors.headerBG);
    topPanel.add( topCtrPanel, BorderLayout.CENTER );

    /*
    ** Budget selector - Get a list of monthly style budgets to select from
    */
    this.budgetList = new MyBudgetList(this.context);
    final String strNames[] = this.budgetList.getBudgetNames();

    // If there are no budgets then we have to inform the user then exit
    if (strNames.length < 1)
      {
      // Display an error message - No budgets exist!
      JOptionPane.showMessageDialog( this,
      "No Budgets have been created.  Use 'Tools:Budget Manager' to create a monthly budget before using this extension.",
      "Error",
      JOptionPane.ERROR_MESSAGE);

      // Tell main that window initialization failed
      this.bError = true;

      // Exit from the constructor
      return;
      }

    // Create the selector
    final JLabel budgetLabel = new JLabel("Budget:");
    topCtrPanel.add(budgetLabel,GridC.getc(0, 0).insets(10, 0, 10, 15));
    this.budgetSelector = new JComboBox<String>(strNames);
    this.budgetIndex = 0;  // Save the currently selected item in case we need to revert to it.
    this.budgetSelector.setSelectedIndex(this.budgetIndex);    
    this.budgetSelector.setToolTipText("Select the budget to edit");  
    topCtrPanel.add(this.budgetSelector, GridC.getc(1, 0).insets(10, 0, 10, 15));
    
    // Create an action listener to dispatch perform the action when this control is changed
    this.budgetSelector.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(final ActionEvent e) 
      {
      if (BudgetEditorWindow.this.tableModel != null)
        {
        if (!BudgetEditorWindow.this.checkValuesEdited())
          {
          // Budget values have not changed or the user doesn't care so update the table and current index
          BudgetEditorWindow.this.tableModel.Update(BudgetEditorWindow.this.budgetList.getBudget((String)BudgetEditorWindow.this.budgetSelector.getSelectedItem()), (String)BudgetEditorWindow.this.yearSelector.getSelectedItem());
          BudgetEditorWindow.this.budgetIndex = BudgetEditorWindow.this.budgetSelector.getSelectedIndex();
          }
        else
          // Reset the selector back to the previous index since the user does not want to change it 
          BudgetEditorWindow.this.budgetSelector.setSelectedIndex(BudgetEditorWindow.this.budgetIndex);
        }
      }
    });

    /*
    ** Budget year selector - Select budget year - last year, this year, and next year
    */
    final JLabel budgetYrLabel = new JLabel("Budget Year:");
    topCtrPanel.add(budgetYrLabel,GridC.getc(2, 0).insets(10, 0, 10, 15));

    // Get the current year then allow selection of that year plus/minus 1 year
    final Calendar c = Calendar.getInstance();
    final int thisYear = c.get(Calendar.YEAR);
    final String years[]={Integer.toString(thisYear-1), Integer.toString(thisYear), Integer.toString(thisYear+1)};
    this.yearSelector = new JComboBox<String>(years);
    this.yearIndex = 1;  // Save the currently selected item in case we need to revert to it.
    this.yearSelector.setSelectedIndex(this.yearIndex);  // Set the current year as the default selection
    this.yearSelector.setToolTipText("Select the budgeting year to edit");
    topCtrPanel.add(this.yearSelector, GridC.getc(3, 0).insets(10, 0, 10, 15));
    
    // Create an action listener to dispatch perform the action when this control is changed
    this.yearSelector.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) 
        {
        if (BudgetEditorWindow.this.tableModel != null)
          {
          if (!BudgetEditorWindow.this.checkValuesEdited())
            {
            // Budget values have not changed or the user doesn't care so update the table and current index
            BudgetEditorWindow.this.tableModel.Update(BudgetEditorWindow.this.budgetList.getBudget((String)BudgetEditorWindow.this.budgetSelector.getSelectedItem()), (String)BudgetEditorWindow.this.yearSelector.getSelectedItem());
            BudgetEditorWindow.this.yearIndex = BudgetEditorWindow.this.yearSelector.getSelectedIndex();
            }
          else
            // Reset the selector back to the previous index since the user does not want to change it 
            BudgetEditorWindow.this.yearSelector.setSelectedIndex(BudgetEditorWindow.this.yearIndex);
          }
        }
      });


      /*
      ** Top right panel - Initialize Button
      */
      // Create a panel in the upper right corner of the window
      this.topRtPanel = new JPanel(new GridBagLayout());
      this.topRtPanel.setBackground(colors.headerBG);
      topPanel.add( this.topRtPanel, BorderLayout.EAST);

      // Add a button to initialize the budget from either prior year's actuals or prior year's budget
      final JButton initButton = new JButton("Initialize Budget");
      initButton.setToolTipText("Initialize the selected budget with the prior year budget or actuals");
      this.topRtPanel.add(initButton, GridC.getc(0, 0).insets(10, 0, 10, 15));   
 
      // Create an action listener to dispatch the action when this button is clicked
      initButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          BudgetEditorWindow.this.initBudget();
        }
      });

      /*
      ** Top left panel - Help Item
      */
      // Create a panel in the upper left corner of the window
      this.topLtPanel = new JPanel(new GridBagLayout());
      this.topLtPanel.setBackground(colors.headerBG);
      topPanel.add( this.topLtPanel, BorderLayout.WEST);

      // Add a clickable text link to request help
      this.helpLink = new JLabel("User Guide", JLabel.LEFT);
      this.helpLink.setForeground(new Color(33, 144, 255));

      // Set the preferred size of this item so that the center panel actually is
      // centered on the frame.
      final Dimension d = this.topRtPanel.getPreferredSize();
      d.setSize(d.width - 30, d.height);
      this.helpLink.setPreferredSize(d);
      
      // Add the help link
      this.topLtPanel.add(this.helpLink, GridC.getc(0, 0).insets(10, 15, 10, 15));

      // Create an action listener to dispatch the action when this label is clicked
      this.helpLink.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
          BudgetEditorWindow.this.showHelp();
        }
      });

    /*
    * Add the Middle Panel - Budget Editor Table
    */
    final JPanel middlePanel = new JPanel(new BorderLayout());
    middlePanel.setPreferredSize(new Dimension( this.frameWidth, this.frameHeight - BudgetEditorWindow.TOP_HEIGHT - BudgetEditorWindow.BOTTOM_HEIGHT ));
    middlePanel.setForeground(colors.defaultTextForeground);
    middlePanel.setBorder(new EmptyBorder(15, 15, 0, 15));
    this.add( middlePanel, BorderLayout.CENTER ); 

    // Create a table to use to edit the budget values
    this.table = new Table(this, this.context, this.tableModel = new TableModel(this, this.context, this.budgetList.getBudget((String)this.budgetSelector.getSelectedItem()), (String)this.yearSelector.getSelectedItem() ), colors);

    // Do not allow selection of an entire row
    this.table.setRowSelectionAllowed(false);

    // Do not allow columns to be reordered by dragging them
    this.table.getTableHeader().setReorderingAllowed(false);


    // Set the minimum width of the category column
    final TableColumn colSelect = this.table.getColumn("Category");
		colSelect.setMinWidth(240);

    // Create the scroll pane and add the table to it. 
    // Note: If this doesn't work on low width screens I might want to allow horizontal scrollbars
    // and resize the columns same as I did for the report window. Would have to turn off auto 
    // resizing of the table too. this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    final JScrollPane scrollPane = new JScrollPane(this.table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    // Add the scroll pane to this panel.
    middlePanel.add(scrollPane, BorderLayout.CENTER);

    /*
    * Add the Bottom Panel - Action Buttons
    */  
    final JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setForeground(colors.defaultTextForeground);
    this.add( bottomPanel, BorderLayout.SOUTH ); 
    /*
    ** Create a center panel at the bottom for the save and cancel buttons.
    */
    final JPanel bottomCtrPanel = new JPanel(new GridBagLayout());
    bottomPanel.add( bottomCtrPanel, BorderLayout.CENTER );

      /*
      ** Save Button
      */
      final JButton saveButton = new JButton("Save");
      saveButton.setToolTipText("Save the current budget and exit");
      bottomCtrPanel.add(saveButton,GridC.getc(0,0).insets(15,15,15,15));

      // Create an action listener to dispatch the action when this button is clicked
      saveButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          BudgetEditorWindow.this.save();
        }
      });

      /*
      ** Cancel Button
      */
      final JButton cancelButton = new JButton("Cancel");
      cancelButton.setToolTipText("Cancel budget editing and exit");
      bottomCtrPanel.add(cancelButton,GridC.getc(1,0).insets(15,15,15,15));
      
      // Create an action listener to dispatch the action when this button is clicked
      cancelButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          BudgetEditorWindow.this.cancel();
        }
      });

      /*
      ** Create a panel in the lower right corner of the window
      */
/*
      this.bottomLtPanel = new JPanel(new GridBagLayout());
//      this.bottomLtPanel.setSize(new Dimension( this.frameWidth/4, BudgetEditorWindow.TOP_HEIGHT ));
      bottomPanel.add( this.bottomLtPanel, BorderLayout.WEST);
*/
      /*
      ** Create a panel in the lower right corner of the window
      */
/*
      this.bottomRtPanel = new JPanel(new GridBagLayout());
      bottomPanel.add( this.bottomRtPanel, BorderLayout.EAST);
*/
      /*
      ** Import Button
      */
/*
      final JButton importButton = new JButton("Import");
      importButton.setToolTipText("Import budget data from a CSV file");
      this.bottomRtPanel.add(importButton,GridC.getc(0,0).insets(15,15,15,15)); 

      // Create an action listener to dispatch the action when this button is clicked
      importButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          BudgetEditorWindow.this.importCSV();
        }
      });
*/
      /*
      ** Export Button
      */
/*
      final JButton exportButton = new JButton("Export");
      exportButton.setToolTipText("Export the current budget data to a CSV file");
      this.bottomRtPanel.add(exportButton,GridC.getc(1,0).insets(15,15,15,15)); 

      // Create an action listener to dispatch the action when this button is clicked
      exportButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          BudgetEditorWindow.this.exportCSV();
        }
      });
*/
  }

  /** 
   * Processes events on this window.
   * 
   * @param e - The event that sent us here.
   */
  public final void processEvent(final AWTEvent e) 
  {
    if(e.getID() == WindowEvent.WINDOW_CLOSING) 
      {
      // Default close to cancel. If it returns true the budget values have changed
      // and the user does not want to cancel
      if (this.cancel())
        return;
      }
    super.processEvent(e);
  }

  
  /** 
   * Get a budget key given the budget name.
   * 
   * @param strName - The name of the budget to retrieve the key for.
   * @return String - The key for the budget name or null if the name wasn't found.
   */
	public String getBudgetKey (final String strName) {
		final Budget objBud = this.mapBudgets.get(strName);
		if (objBud != null)
			return objBud.getKey();
		else
			return null;
	}

	
  /** 
   * Get the index of the selected budget item.
   * 
   * @return int - Selected budget index.
   */
  public int getBudgetIndex() {
    return this.budgetIndex;
  }

  
  /** 
   * Returns the status of the global data changed flag.
   * 
   * @return boolean - true if any budget data has been changed, false otherwise.
   */
  public boolean isDataChanged() {
    return this.dataChanged;
  }

  
  /** 
   * Set the state of the global data changed flag.
   * 
   * @param value - true if data has changed, false otherwise.
   */
  public void setDataChanged(final boolean value) {
    this.dataChanged = value;
  }


  /** 
   * Get the table model for the table.
   * 
   * @return BudgetEditorTableModel - The table model.
   */
  public TableModel getModel() {
    return this.tableModel;
  }


  /**
   * Set the frame size based on the width and height of the display.
   */
  private void setFrameSize()
    {
    final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    this.frameWidth  = ( gd.getDisplayMode().getWidth() * 95 ) / 100;
    this.frameHeight = ( gd.getDisplayMode().getHeight() * 95 ) / 100;
    this.setSize( this.frameWidth, this.frameHeight );
    }

    
  /** 
   * Check to see if any data has changed and prompt the user for what to do if
   * so.
   * 
   * @return boolean - Returns true if data has changed and the user doesn't want
   * to leave after all (Cancel). Returns false if data hasn't changed or if the
   * user doesn't care or if the data was saved first (Yes or No).
   */
  private boolean checkValuesEdited()
    {
    // Prompt with a warning if the data has changed else just close the console
    if (this.isDataChanged()) 
      {
      final int response = JOptionPane.showConfirmDialog( this,
      "Budget values have been edited. Would you like to save the changes first?",
      "Budget Edited",
      JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
      if (response == 0)  // Yes
        {
          this.tableModel.saveData();
        return false;
        }
      else if (response == 2) // Cancel
        return true;
      }

    // Either the data wasn't changed or the user selected No
    this.setDataChanged(false);
    return false;
    }

  /**
   * This is the handler for the "Initialize Budget" button. It prompts the 
   * user to see what should be done then cancels, copies last year's budget
   * or copies last year's actuals.
   */
  private void initBudget() {
    // Prompt user to get what is to be done
    final String[] options = new String[] {"Cancel", "Copy prior year's budget", "Use prior year's actuals"};
    final int response = JOptionPane.showOptionDialog( this,
    "WARNING! Any existing budget values for the selected year will be overwritten if you continue.",
    "Initialize Budget",
    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
    if (response == 1)      // Copy prior year's budget
      this.table.copyPriorBudget();
    else if (response == 2) // Use prior year's actuals
      this.table.copyPriorActuals();

    return;
  }
  
  /** 
   * Action method called when the Cancel button is pressed. It first checks
   * to see if any data was changed and then asks the user what to do. If the 
   * data hasn't changed or the user doesn't care then the console will be
   * closed. Otherwise, return without closing the console.
   * 
   * @return boolean - true if the user doesn't want to cancel, false if we
   * cancelled and the console was closed.
   */
  private boolean cancel() {
    // Prompt with a warning if the data has changed. If true is returned then the user doesn't want to cancel
    if (this.checkValuesEdited())
        return true;
    else
      {
      // Otherwise, close the console and return false
      this.extension.closeConsole();
      return false;
      }
  }
  
  /**
   * Action method called when the Save button is pressed. If data was changed it
   * will be saved. If nothing was changed the user will be alerted to that fact.
   * The console will then be closed.
   */
  private void save() 
  {
    if (this.isDataChanged()) 
      // Save the changes then exit
      this.tableModel.saveData();
    else
      {
      JOptionPane.showMessageDialog(this,
      "No budget values were changed. There is nothing to save!",
      "Save Budget",
      JOptionPane.PLAIN_MESSAGE);
      }

    // Close the console and exit
    this.extension.closeConsole();
  }


  /**
   * Action method called when the Import button is pressed. This method imports
   * data from a CSV file into the selected budget and year.
   */
  /*
  private void importCSV() 
  {
    JOptionPane.showMessageDialog(this,
    "This feature will be implemented in the future.",
    "CSV Import",
    JOptionPane.PLAIN_MESSAGE);
  }
*/

  /**
   * Action method called when the Export button is pressed. This method exports
   * data from the selected budget and year to a CSV file.
   */
/*
   private void exportCSV() 
  {
    JOptionPane.showMessageDialog(this,
    "This feature will be implemented in the future.",
    "CSV Export",
    JOptionPane.PLAIN_MESSAGE);
  }
*/

  /**
   * Action method called when the User Guide label is clicked. This method
   * displays a brief help message for the extension.
   */
  private void showHelp() 
  {
    final String url = "https://github.com/jerrymjones/MonthlyBudgetEditor/wiki";
    final String myOS = System.getProperty("os.name").toLowerCase();
    try 
      {
      if (myOS.contains("windows"))
        { // Windows
        if (Desktop.isDesktopSupported())
          {
          final Desktop desktop = Desktop.getDesktop();
          desktop.browse(new URI(url));
          }
        else
          this.showHelpFailed(url); // Copy URL to clipboard
        } 
      else 
        { // Not-windows
          final Runtime runtime = Runtime.getRuntime();
          if (myOS.contains("mac")) // Apple
            runtime.exec("open " + url);
          else if (myOS.contains("nix") || myOS.contains("nux")) // Linux
            runtime.exec("xdg-open " + url);
          else 
            this.showHelpFailed(url); // Copy URL to clipboard
        }
      }
    catch(IOException | URISyntaxException e) 
      {
        this.showHelpFailed(url); // Copy URL to clipboard
      }
  }

  /**
   * This method is called when we could not open a browser window to show help.
   * We'll tell the user and copy the URL to the clipboard so it can be manually
   * opened.
   */
  private void showHelpFailed(final String url)
  {
    // Create a transferrable of the url to copy to the clipboard
    final StringSelection sel  = new StringSelection(url); 
  
    // Get an instance of the system clipboard
    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 

    // Copy the url to the clipboard
    clipboard.setContents(sel, sel); 

    // Now tell the user what happened
    JOptionPane.showMessageDialog(this,
    "I could not open the help URL on this system. The URL was copied to the clipboard for you to manually open it.",
    "Browser Open Failed",
    JOptionPane.INFORMATION_MESSAGE);
  }
}