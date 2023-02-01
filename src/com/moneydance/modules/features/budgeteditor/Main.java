/************************************************************
 *      Copyright (C) 2016 The Infinite Kind, Limited       *
 ************************************************************/
package com.moneydance.modules.features.budgeteditor;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;

public class Main extends FeatureModule {
  private BudgetEditorWindow budgetEditorWindow = null;

  public void init() {
    // the first thing we will do is register this module to be invoked via the application toolbar
    final FeatureModuleContext context = this.getContext();
    try {
      context.registerFeature(this, "showconsole", this.getIcon(), this.getName());
    }
    catch (final Exception e) {
      e.printStackTrace(System.err);
    }
  }

  /** Process an invokation of this module with the given URI */
  public void invoke(final String uri) {
    String command = uri;
    int theIdx = uri.indexOf('?');
    if (theIdx >= 0) {
      command = uri.substring(0, theIdx);
    }
    else {
      theIdx = uri.indexOf(':');
      if (theIdx >= 0) {
        command = uri.substring(0, theIdx);
      }
    }

    if (command.equals("showconsole")) {
      this.showConsole();
    }
  }

  
  /** 
   * @return String - The name of the extension.
   */
  public String getName() {
    return "Monthly Budget Editor";
  }

  
  /** 
   * @return FeatureModuleContext
   */
  FeatureModuleContext getUnprotectedContext() {
    return this.getContext();
  }

  
  /**
   * Called to close the window.
   */
  synchronized void closeConsole() {
    if (this.budgetEditorWindow != null) {
      this.budgetEditorWindow.setVisible(false);
      this.budgetEditorWindow.dispose();
      this.budgetEditorWindow = null;
      System.gc();
    }
  }

  
  /** 
	 * Get Icon is not really needed as Icons are not used. Included as the
	 * register feature method requires it.
	 *
   * @return Image
   */
  private Image getIcon() {
    try {
      final ClassLoader cl = this.getClass().getClassLoader();
      final java.io.InputStream in = cl.getResourceAsStream("/com/moneydance/modules/features/budgeteditor/icon.gif");
      if (in != null) {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
        final byte buf[] = new byte[256];
        int n = 0;
        while ((n = in.read(buf, 0, buf.length)) >= 0)
          bout.write(buf, 0, n);
        return Toolkit.getDefaultToolkit().createImage(bout.toByteArray());
      }
    }
    catch (final Throwable e) {
    }
    return null;
  }


  private synchronized void showConsole() {
    if (this.budgetEditorWindow == null) {
      this.budgetEditorWindow = new BudgetEditorWindow(this);

      if (this.budgetEditorWindow != null) {
        if (!this.budgetEditorWindow.bError)
          this.budgetEditorWindow.showWindow();
        else {
          this.closeConsole();
          return;
        }
      }
    }
    else {
      // Force a reload if the window is already initialized.
      this.budgetEditorWindow.getModel().LoadData();
      this.budgetEditorWindow.setVisible(true);
      this.budgetEditorWindow.toFront();
      this.budgetEditorWindow.requestFocus();
    }
  }
}
