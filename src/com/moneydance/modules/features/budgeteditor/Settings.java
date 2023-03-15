/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023, Jerry Jones
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

import com.infinitekind.moneydance.model.AccountBook;

/**
 * This class implements the settings for the Monthly Budget Editor.
 *
 * @author  Jerry Jones
 */
public class Settings {
    /**
     * The instance of Settings that this Class is storing
     */
    private static Settings instance = null;

    // The account book in use
    private static AccountBook book;

    /*
     * The settings we support
     */
    // Version number of the settings
    private static int version = Constants.SETTINGS_VERSION_1;

    // The name of the budget to use
    private static String budgetName = "Budget";

    // Use full names when true
    private static Boolean useCategoryCurrency = false;

    /**
     * Default constructor for the settings class.
     * 
     * @param  book - The account book in use
     */
    private Settings(final AccountBook book) 
    {
        Settings.loadSettings(book);
    }

    private static void loadSettings(AccountBook book)
    {
        // Get the default settings. These are stored in a comma separated string.
        final String rawSettings = book.getRootAccount().getPreference(Constants.MBE_SETTINGS, "");
        final String[] rawSplit = rawSettings.split("\\s*,\\s*");

        // Get the version of the stored settings
        try 
            {
            int version = Integer.parseInt(rawSplit[0]);

            try
                {
                if ((version == Constants.SETTINGS_VERSION_1) && (rawSplit.length == Constants.V1_NUM_MBR_SETTINGS))
                    {
                    // Get the V1 parameters
                    Settings.getV1Params(rawSplit);

                    // Upgrade to V2 parameters by setting the defaults
                    Settings.version                = Constants.SETTINGS_VERSION_1;
                    Settings.useCategoryCurrency    = false;
                    return;
                    }
                // else, just go set the defaults
                }
            catch (final Exception e)
                {
                e.printStackTrace();
                System.err.println("ERROR: Cannot parse the configuration settings '"+rawSettings+"'.");
                }
            }
        catch(final Exception e)
            {
            e.printStackTrace();
            System.err.println("ERROR: Cannot invalid configuration settings '"+rawSettings+"'.");
            }

        // Otherwise, we'll use the defaults just to get going
        Settings.version                = Constants.SETTINGS_VERSION_1;
        Settings.budgetName             = "Budget";
        Settings.useCategoryCurrency    = false;
    }

    /**
     * Method to retrieve the V1 parameters
     * 
     * @param rawSplit - The settings from the preferences split into an array
     */
    private static void getV1Params(String[] rawSplit) {
        Settings.version                = Constants.SETTINGS_VERSION_1;
        Settings.budgetName             = rawSplit[1];
        Settings.useCategoryCurrency    = rawSplit[2].equalsIgnoreCase("true");
 
    }

    /**
     * Get the Instance of this class There should only ever be one instance of
     * this class and other classes can use this static method to retrieve the
     * instance
     *
     * @return Configuration the stored Instance of this class
     */
    public static synchronized Settings getInstance(final AccountBook book) {
        // If there is no instance yet then we'll create one here
        if (Settings.instance == null)
            { 
            // Create a new instance
            Settings.instance = new Settings(book);
            }
        // Otherwise, if the book changed we need to reload and save the new book
        else if (!book.equals(Settings.book))
            {
            // Reload the settings for the new book
            Settings.loadSettings(book);
            }
        
        // Save the account book
        Settings.book = book;

        return Settings.instance;
    }

    /**
     * Get the Instance of this class There should only ever be one instance of
     * this class and other classes can use this static method to retrieve the
     * instance
     *
     * @return Configuration the stored Instance of this class or null if it hasn't
     * been initialized with an account book yet. 
     */
    public static synchronized Settings getInstance() {
        return Settings.instance;
    }

    /**
     * Save the settings 
     */
    public void saveSettings() {
        final String settings = Settings.version+","+Settings.budgetName+","+Settings.useCategoryCurrency.toString();
        Settings.book.getRootAccount().setPreference(Constants.MBE_SETTINGS, settings);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Settings [version=" + Settings.version + ", budgetName=" + Settings.budgetName + ", useCategoryCurrency=" + Settings.useCategoryCurrency + "]";
    }

    /**
     * @return the budgetName
     */
    public String getBudgetName() {
        return Settings.budgetName;
    }

    /**
     * @param budgetName the budgetName to set
     */
    public void setBudgetName(final String budgetName) {
        Settings.budgetName = budgetName;
    }

    /**
     * @return the useCategoryCurrency
     */
    public Boolean getUseCategoryCurrency() {
        return Settings.useCategoryCurrency;
    }

    /**
     * @param useCategoryCurrency the useCategoryCurrency to set
     */
    public void setUseCategoryCurrency(Boolean useCategoryCurrency) {
        Settings.useCategoryCurrency = useCategoryCurrency;
    }

    /**
     * @return the settings version
     */
    public int getVersion() {
        return Settings.version;
    }
}
