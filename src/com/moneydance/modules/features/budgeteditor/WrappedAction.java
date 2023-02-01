/*
    This code provided by Rob Camick (See https://tips4java.wordpress.com/2008/11/04/table-tabbing/)
    We assume no responsibility for the code. You are free to use and/or modify and/or distribute any or all code posted on the Java Tips Weblog without 
    restriction. A credit in the code comments would be nice, but not in any way mandatory.
*/
package com.moneydance.modules.features.budgeteditor;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 *  The WrappedAction class is a convenience class that allows you to replace
 *  an installed Action with a custom Action of your own. There are two
 *  benefits to using this class:
 *
 *  a) Key Bindings of the original Action are retained for the custom Action.
 *  b) the original Action is retained so your custom Action can invoke the
 *     original Action.
 *
 *  This class is abstract so your custom Action must extend this class and
 *  implement the actionPerformed() method.
 * 
 *  @author  Rob Camick
 */
abstract class WrappedAction implements Action
{
	private Action originalAction;
	private final JComponent component;
	private Object actionKey;

	/**
	 * Constructor to replace the default Action for the given KeyStroke with a custom Action
	 * 
	 * @param component - The component to modify the action on.
	 * @param keyStroke - The key stroke to bind the action to.
	 */
	public WrappedAction(final JComponent component, final KeyStroke keyStroke)
	{
		this.component = component;
		final Object actionKey = this.getKeyForActionMap(component, keyStroke);

		if (actionKey == null)
			{
			final String message = "no input mapping for KeyStroke: " + keyStroke;
			throw new IllegalArgumentException(message);
			}

		this.setActionForKey( actionKey );
	}

	/**
	 *  Method to replace the default Action with a custom Action.
	 * 
	 * @param component - The component to modify the action on.
	 * @param actionKey - The key stroke to act on.
	 */
	public WrappedAction(final JComponent component, final Object actionKey)
	{
		this.component = component;
		this.setActionForKey( actionKey );
	}

	
	/** 
	 * Method that child classes should use to invoke the original Action.
	 * 
	 * @param e - The action event to perform.
	 */
	public void invokeOriginalAction(final ActionEvent e)
	{
		this.originalAction.actionPerformed(e);
	}

	
	/**
	 *  Method to install this class as the default Action.
	 */
	public void install()
	{
		this.component.getActionMap().put(this.actionKey, this);
	}

	
	/**
	 *	Method to restore the original Action as the default Action
	 */
	public void unInstall()
	{
		this.component.getActionMap().put(this.actionKey, this.originalAction);
	}

	/** 
	 * Delegate the Action interface methods to the original Action
	 * 
	 * @param listener - The property change listener to add.
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener)
	{
		this.originalAction.addPropertyChangeListener(listener);
	}

	/** 
	 * Gets one of this object's properties using the associated key.
	 * 
	 * @param key - A String containing the key.
	 * @return Object - The object value requested.`
	 */
	public Object getValue(final String key)
	{
		return this.originalAction.getValue(key);
	}

	/** 
	 * Returns the enabled state of the Action. When enabled, any component 
	 * associated with this object is active and able to fire this object's 
	 * actionPerformed method.
	 * 
	 * @return boolean - Returns true if this action is enabled.
	 */
	public boolean isEnabled()
	{
		return this.originalAction.isEnabled();
	}

	
	/** 
	 * Puts a value at the property associated with the key.
	 * 
	 * @param key - A String containing the key.
	 * @param newValue - An Object to use as the new value of the property.
	 */
	public void putValue(final String key, final Object newValue)
	{
		this.originalAction.putValue(key, newValue);
	}

	
	/** 
	 * Remove a property change listener.
	 * 
	 * @param listener - The listener to remove.
	 */
	public void removePropertyChangeListener(final PropertyChangeListener listener)
	{
		this.originalAction.removePropertyChangeListener(listener);
	}

	
	/**
	 * Sets the enabled state of the Action. When enabled, any component associated 
	 * with this object is active and able to fire this object's actionPerformed 
	 * method. If the value has changed, a PropertyChangeEvent is sent to listeners.
	 *  
	 * @param newValue - true to enable this Action, false to disable it
	 */
	public void setEnabled(final boolean newValue)
	{
		this.originalAction.setEnabled(newValue);
	}

	
	/** 
	 * Returns an array of Objects which are keys for which values have been 
	 * set for this AbstractAction, or null if no keys have values set.
	 * 
	 * @return Object[] - an array of key objects, or null if no keys have values set
	 */
	public Object[] getKeys()
		{
			if (this.originalAction instanceof AbstractAction)
				{
				final AbstractAction abstractAction = (AbstractAction)this.originalAction;
				return abstractAction.getKeys();
				}
	
			return null;
		}

	
	/** 
	 * Returns an array of all the PropertyChangeListeners added to this 
	 * AbstractAction with addPropertyChangeListener().
	 * 
	 * @return PropertyChangeListener[] - All of the PropertyChangeListeners
	 * added or an empty array if no listeners have been added
	 */
	public PropertyChangeListener[] getPropertyChangeListeners()
	{
		if (this.originalAction instanceof AbstractAction)
			{
			final AbstractAction abstractAction = (AbstractAction)this.originalAction;
			return abstractAction.getPropertyChangeListeners();
			}

		return null;
	}


	/** 
	 * Search the InputMaps to find the KeyStroke binding
	 * 
	 * @param component - The component to get.
	 * @param keyStroke - the KeyStroke for which to get the binding.
	 * @return Object
	 */
	private Object getKeyForActionMap(final JComponent component, final KeyStroke keyStroke)
	{
		for (int i = 0; i < 3; i++)
		{
			final InputMap inputMap = component.getInputMap(i);

			if (inputMap != null)
				{
				final Object key = inputMap.get(keyStroke);

				if (key != null)
					return key;
				}
		}

		return null;
	}

	
	/** 
	 * Replace the existing Action for the given action key with a wrapped 
	 * custom Action.
	 * 
	 * @param actionKey - The key stroke to act on.
	 */
	private void setActionForKey(final Object actionKey)
	{
		//  Save the original Action
		this.actionKey = actionKey;
		this.originalAction = this.component.getActionMap().get(actionKey);

		if (this.originalAction == null)
			{
			final String message = "no Action for action key: " + actionKey;
			throw new IllegalArgumentException(message);
			}

		//  Replace the existing Action with this class
		this.install();
	}
}