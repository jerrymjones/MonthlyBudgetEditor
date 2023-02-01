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

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
* Subclass the default cell editor to add additional capability.
*
* This class sets alignment as well as popup support for the currency values in
* the table.
*
* @author  Jerry Jones
*/
public class CurrencyTableCellEditor extends DefaultCellEditor { 
    // The table using this editor
    Table table;    

    public CurrencyTableCellEditor(final Table table, final JTextField textField) {
        // Construct the default cell editor
        super(textField);

        // Save the table for the mouse event
        this.table = table;

        // Set cell alignment during edit
        textField.setHorizontalAlignment(JTextField.RIGHT);

        // Click each cell only once to enter edit mode
        this.setClickCountToStart(1);

        // Provide popup menu support even when the editor is active
        this.getComponent().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent me) {
				CurrencyTableCellEditor.this.showPopup(me);
			}
		});	
    }

    
    /** 
     * This medhod overrides the default function to provide justification 
     * of the text as well as selecting the existing text when the editor
     * starts so that any typing will replace the existing text.
     * 
     * @param table - The table object for the main window.
     * @param value - The value of the table cell editor.
     * @param isSelected - true if this cell is currently selected.
     * @param row - The table row.
     * @param column - The table column. 
     * @return Component - The cell editor component.
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
        final Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);

        // Set text field properties
        final JTextField tf = (JTextField)this.getComponent();
        tf.setHorizontalAlignment(JTextField.RIGHT);

        // Select the text in the editor so that typing will replace the value
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tf.selectAll();
                }
            });

        return c;
    }

    /**
     * This method pops up a menu when a right-click occurs on a cell being 
     * edited.
     * 
     * @param me - The mouse event that got us here
     */
    private void showPopup (final MouseEvent me) {
		// Create new popup menu
		final JPopupMenu popMenu = new JPopupMenu();		

        // is this event a popup trigger?
        if (popMenu.isPopupTrigger(me)) 
            {
            // Get row and column of the cell in the table
            final int row = this.table.getEditingRow();
            final int col = this.table.getEditingColumn();


            // Stop cell editing to close the editor
            this.stopCellEditing();
       
            // Go do the popup
            this.table.doShowPopup(popMenu, row, col);
            }
    }
}