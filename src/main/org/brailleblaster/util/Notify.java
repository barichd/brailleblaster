/* BrailleBlaster Braille Transcription Application
 *
 * Copyright (C) 2014
* American Printing House for the Blind, Inc. www.aph.org
* and
 * ViewPlus Technologies, Inc. www.viewplus.com
 * and
 * Abilitiessoft, Inc. www.abilitiessoft.com
 * All rights reserved
 *
 * This file may contain code borrowed from files produced by various 
 * Java development teams. These are gratefully acknowledged.
 *
 * This file is free software; you can redistribute it and/or modify it
 * under the terms of the Apache 2.0 License, as given at
 * http://www.apache.org/licenses/
 *
 * This file is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE
 * See the Apache 2.0 License for more details.
 *
 * You should have received a copy of the Apache 2.0 License along with 
 * this program; see the file LICENSE.
 * If not, see
 * http://www.apache.org/licenses/
 *
 * Maintained by Keith Creasy <kcreasy@aph.org>, Project Manager
 */

package org.brailleblaster.util;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.MessageBox;
import org.brailleblaster.localization.LocaleHandler;
import org.brailleblaster.wordprocessor.WPManager;

/**
 * Display a message.
 */
public class Notify {

	/**
	 * Show the user a message.
	 */
	public Notify(String message) {
		String realMessage;
		if (message.charAt(0) == '&') {
			realMessage = new LocaleHandler().localValue(message);
		} else {
			realMessage = message;
		}

		if (WPManager.getDisplay() == null) {
			System.out.println(realMessage);
			return;
		}
		Shell shell = new Shell(WPManager.getDisplay(), SWT.DIALOG_TRIM);
		MessageBox mb = new MessageBox(shell, SWT.OK);
		mb.setMessage(realMessage);
		mb.open();
		shell.dispose();
	}
}
