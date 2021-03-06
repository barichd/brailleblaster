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
  * this program; see the file LICENSE.txt
  * If not, see
  * http://www.apache.org/licenses/
  *
  * Maintained by Keith Creasy <kcreasy@aph.org>, Project Manager
*/

package org.brailleblaster.perspectives.imageDescriber;

import java.io.File;

import nu.xom.Document;

import org.brailleblaster.archiver.Archiver;
import org.brailleblaster.archiver.ArchiverFactory;
import org.brailleblaster.archiver.NimasArchiver;
import org.brailleblaster.localization.LocaleHandler;
import org.brailleblaster.perspectives.Controller;
import org.brailleblaster.perspectives.imageDescriber.document.ImageDescriber;
import org.brailleblaster.perspectives.imageDescriber.views.ImageDescriberView;
import org.brailleblaster.util.ImageHelper;
import org.brailleblaster.util.YesNoChoice;
import org.brailleblaster.wordprocessor.BBFileDialog;
import org.brailleblaster.wordprocessor.BBStatusBar;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabItem;

///////////////////////////////////////////////////////////////////////////////////////////
// Controller for Image Describer Perspective.
public class ImageDescriberController extends Controller {	
	// Utils
	LocaleHandler lh = new LocaleHandler();
	// UI Elements.
	ImageDescriberView idv;
	Group group;
	// The image describer.
	ImageDescriber imgDesc;
	
	// Helps with managing and manipulating images.
	ImageHelper imgHelper;
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// Constructor.
	public ImageDescriberController(WPManager wordProcesserManager, String fileName) {
		super(wordProcesserManager);
		
		this.item = new TabItem(wp.getFolder(), 0);
		this.group = new Group(wp.getFolder(), SWT.NONE);
		this.group.setLayout(new FormLayout());
		
		this.imgDesc = new ImageDescriber(this);

		// Start the image describer and build the DOM
		if(fileName != null){
			if(openDocument(fileName))
				item.setText(fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1));
		}
		else {
			if(openDocument(null)){
				docCount++;
				if(docCount > 1)
					item.setText("Untitled #" + docCount);
				else
					item.setText("Untitled");
			}
		}
		
		//Set the views in the tab area
		idv = new ImageDescriberView(this.group, imgDesc, this);
		this.item.setControl(this.group);
		
		// Image helper class. Image helper functions, and such.
		imgHelper = new ImageHelper();
	}
	
	public ImageDescriberController(WPManager wp, Document doc, TabItem tabItem, Archiver arch) {
		super(wp);
		this.arch = arch;
		
		imgDesc = new ImageDescriber(this, doc);
		this.item = tabItem;
		this.group = new Group(wp.getFolder(), SWT.NONE);
		this.group.setLayout(new FormLayout());
		idv = new ImageDescriberView(this.group, imgDesc, this);
		this.item.setControl(this.group);
		// Image helper class. Image helper functions, and such.
		imgHelper = new ImageHelper();
		idv.setTextBox(imgDesc.getCurDescription());
	}
	
	// Returns the Image Describer's View.
	public ImageDescriberView getImageDescriberView() {
		return idv;
	}
	
	public boolean openDocument(String fileName){
		
		// If this is the first document, load a previous session.
		String restorePath = docRestore();
		if(restorePath != null)
			arch = ArchiverFactory.getArchive( restorePath, true);
		else if( restorePath == null && fileName != null )
			arch = ArchiverFactory.getArchive( fileName, false);
		else
			arch = ArchiverFactory.getArchive( templateFile, false);
		
		////////////////
		// Recent Files.
		if(fileName != null)
			addRecentFileEntry(fileName);
		
		// Parse document.
		boolean result = imgDesc.startDocument(arch.getWorkingFilePath(), arch.getCurrentConfig(), null);
		
		// Start the auto-saver.
		if( arch.getWorkingFilePath().contains("textFileTemplate.html") == false )
			arch.resumeAutoSave( imgDesc, arch.getWorkingFilePath() );
		 
		// Return document result.
		return result;
	}
	
	public void save(){
		
		// Before saving, delete the temp html files.
		arch.deleteTempFiles();
		
		if(arch.getOrigDocPath() == null)
			saveAs();
		else {
			if(arch != null) { // Save archiver supported file.
				arch.save(imgDesc, null);
			}
		
			arch.setDocumentEdited(false);
		}
		
		// Recreate the temp HTML file, just in case they need it again.
		if(arch instanceof NimasArchiver) {
			((NimasArchiver) arch).resetThenWrite(arch.getCurSpineIdx());
		}
	}
	
	public void saveAs(){
		BBFileDialog dialog = new BBFileDialog(wp.getShell(), SWT.SAVE, arch.getFileTypes(), arch.getFileExtensions());
		String filePath = dialog.open();
		
		if(filePath != null){
			
			// Before saving, delete the temp html file.
			arch.deleteTempFiles();
			
			String ext = getFileExt(filePath);
			arch.saveAs(imgDesc, filePath, ext);
		   
			arch.setDocumentEdited(false);
			
			// Recreate the HTML file, just in case they need it again.
			if(arch instanceof NimasArchiver) {
				((NimasArchiver) arch).resetThenWrite(arch.getCurSpineIdx());
			}
		}
	}
	
	@Override
	public void restore(WPManager wp) {
		
	}

	@Override
	public void dispose() {
		idv.disposeUI();
	}

	@Override
	public void close() {
		boolean cancel = false;
		if(documentHasBeenEdited()){
			YesNoChoice ync = new YesNoChoice(lh.localValue("hasChanged"), true);
			if (ync.result == SWT.YES) 
				save();
			else if(ync.result == SWT.CANCEL)
				cancel =true;
		}
		
		if(!cancel){
			// Shut down auto-save feature.
			arch.destroyAutoSave();
			// Clear previous session information. Otherwise, we'll be looking 
			// for files in the user's temp folder that aren't there.
			arch.clearPrevSession();
			dispose();
			item.dispose();
			wp.removeController(this);
			
			if(wp.getList().size() == 0)
				wp.getStatusBar().setText("");
		}
	}
	
	// 
	public void setImageGoto(int index)
	{
		// Go to a particular image in the image describer.
		imgDesc.gotoImageElement(index);
		
		// Set image preview.
		idv.setMainImage();
		
		// Get prodnote text/image description.
		idv.setTextBox();

		// Show current image index and name.
		setImageInfo();
		
		// Set alt text.
		idv.setAltBox(imgDesc.getCurElmAttribute("alt"));
		
		// Scroll browser to current image.
//		idv.scrollBrowserToCurImg();
	}
	
	public void setImageToPrevious(){
		// Change main image to previous element image.
		imgDesc.prevImageElement();

		// Set image preview.
		idv.setMainImage();
		
		// Get prodnote text/image description.
		idv.setTextBox();

		// Show current image index and name.
		setImageInfo();
		
		// Set alt text.
		idv.setAltBox(imgDesc.getCurElmAttribute("alt"));
		
		// Scroll browser to current image.
//		idv.scrollBrowserToCurImg();
	}
	
	public void setImageToNext(){
		// Change main image to previous element image.
		imgDesc.nextImageElement();
		
		//Change current image in dialog.
		idv.setMainImage();
			
		// Get prodnote text/image description.
		idv.setTextBox();

		// Show current image index and name.
		setImageInfo();
		
		// Set alt text.
		idv.setAltBox(imgDesc.getCurElmAttribute("alt"));
		
		// Scroll browser to current image.
//		idv.scrollBrowserToCurImg();
	}
	
	// Undo-es current element/image changes.
	public void undo()
	{
		// Undo the changes.
		imgDesc.undoCurDescAndAlt();
		// Get prodnote text/image description.
		idv.setTextBox();
		// Set alt text.
		idv.setAltBox(imgDesc.getCurElmAttribute("alt"));
		
	}
	
	public void apply(){
		imgDesc.setDescription(idv.getTextBoxValue(), null, null, null);
		arch.setDocumentEdited(true);
	}
	
	public void applyToAll(){
		// Message box to let user know that things are to be changed.
		org.eclipse.swt.widgets.MessageBox msgB = new org.eclipse.swt.widgets.MessageBox(group.getShell(), SWT.OK | SWT.CANCEL);
		msgB.setMessage("Image Describer will update every image like this one with the given description. CANNOT UNDO! Continue?");
		
		// Warn user before doing this. It could take a while.
		if( msgB.open() == SWT.OK)
		{
			// Apply what is in the edit box first.
			imgDesc.setDescription(idv.getTextBoxValue(), null, null, null);

			// Current image path.
			String curImgPath = "";

			// Get current image path from src attribute.
			curImgPath = imgDesc.currentImageElement().getAttributeValue("src");

			// Get number of <img> elements.
			int numElms = imgDesc.getNumImgElements();

			// For each element, check if it has the same image path.
			for(int curImg = 0; curImg < numElms; curImg++)
			{
				// Is this <img> just like the current image path?
				if( imgDesc.getElementAtIndex(curImg).getAttributeValue("src").compareTo(curImgPath) == 0 )
				{
					// Change description to current prod text.
					imgDesc.setDescAtIndex(curImg, idv.getTextBoxValue(), null, null, null);

				} // if( imgDesc.getElementAtIndex...

			} // for(int curImg...
			arch.setDocumentEdited(true);
		} // if msgBx == true
	}
	
	public void clearAll(){
		// Message box to let user know that things are to be changed.
		org.eclipse.swt.widgets.MessageBox msgB = new org.eclipse.swt.widgets.MessageBox(group.getShell(), SWT.OK | SWT.CANCEL);
		msgB.setMessage("Image Describer will update every image like this one with NO DESCRIPTION. CANNOT UNDO! Continue?");
		
		// Warn user before doing this. It could take a while.
		if( msgB.open() == SWT.OK)
		{
			// Apply what is in the edit box first.
			imgDesc.setDescription("", null, null, null);
			idv.setTextBox("");

			// Current image path.
			String curImgPath = "";

			// Get current image path from src attribute.
			curImgPath = imgDesc.currentImageElement().getAttributeValue("src");

			// Get number of <img> elements.
			int numElms = imgDesc.getNumImgElements();

			// For each element, check if it has the same image path.
			for(int curImg = 0; curImg < numElms; curImg++)
			{
				// Is this <img> just like the current image path?
				if( imgDesc.getElementAtIndex(curImg).getAttributeValue("src").compareTo(curImgPath) == 0 )
				{
					// Change description to current prod text.
					imgDesc.setDescAtIndex(curImg, "", null, null, null);
					// Change alt text.
					imgDesc.setElementAttributesAtIndex(curImg, null, null, "");

				} // if( imgDesc.getElementAtIndex...

			} // for(int curImg...

		} // if msgBx == true
	}

	//////////////////////////////////////////////////////////////////////
	// Returns Archiver. Could be null if we didn't need it 
	// to open a file.
	@Override
	public Archiver getArchiver() {
		return arch;
	}
	
	/////////////////////////////////////////////////////////////////
	// Returns the image describer "document"
	public ImageDescriber getDocument() {
		return imgDesc;
	}
	
	@Override
	public Document getDoc() {
		return imgDesc.getDOM();
	}

	public void setImageInfo(){
		setStatusBarText(wp.getStatusBar());
	}
	
	@Override
	public void setStatusBarText(BBStatusBar statusBar) {
		String textToSet = "Image #" + imgDesc.getCurrentElementIndex() + " - " + imgDesc.currentImageElement().getAttributeValue("src");
//		textToSet += ;
		statusBar.setText( textToSet );
	}

	@Override
	public boolean canReuseTab() {
		if((arch.getOrigDocPath() != null && imgDesc.getImageList().size() > 0) || arch.getDocumentEdited())
			return false;
		else
			return true;
	}

	public ImageDescriber getImageDescriber(){
		return imgDesc;
	}
	
	@Override
	public void reuseTab(String file) {
		closeUntitledDocument();
		openDocument(file);
		item.setText(file.substring(file.lastIndexOf(File.separatorChar) + 1));
		idv.setMainImage();
		idv.setBrowser();
		idv.setTextBox(imgDesc.getCurDescription());
		idv.setAltBox(imgDesc.getCurElmAttribute("alt"));

//		if(arch != null){
//			if( arch instanceof EPub3Archiver )
//				idv.enableBrowserNavButtons();
//			else
//				idv.disableBrowserNavButtons();
//		}
//		else
//			idv.disableBrowserNavButtons();
//		if(file.endsWith(".zip")){
//			openZipFile(file);
//		}
//		else {
//			workingFilePath = file;
//		}
//		
//		if(imgDesc.startDocument(workingFilePath, BBIni.getDefaultConfigFile(), null)){
//			item.setText(file.substring(file.lastIndexOf(File.separatorChar) + 1));
//			currentConfig = BBIni.getDefaultConfigFile();
//			idv.resetViews(imgDesc);
//		}
//		else {
//			workingFilePath = currentPath;
//			currentConfig = "nimas.cfg";
//			if(currentPath != null)
//				imgDesc.startDocument(currentPath, currentConfig, null);
//			else
//				imgDesc.startDocument(BBIni.getProgramDataPath() + BBIni.getFileSep() + "xmlTemplates" + BBIni.getFileSep() + "dtbook.xml", currentConfig, null);
//		}
	}
	
	private void closeUntitledDocument(){
		imgDesc.deleteDOM();
		imgDesc.resetCurrentIndex();
	}
	
} // public class ImageDescriberDialog extends Dialog
