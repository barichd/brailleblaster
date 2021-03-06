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

package org.brailleblaster.archiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nu.xom.converters.DOMConverter;

import org.brailleblaster.BBIni;
import org.brailleblaster.document.BBDocument;
import org.brailleblaster.util.FileUtils;
import org.brailleblaster.util.Notify;
import org.brailleblaster.util.PropertyFileManager;
import org.brailleblaster.util.Zipper;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

//////////////////////////////////////////////////////////////////////////////////
// Archiver gives methods for opening/handling particular document types. 
// Some of these types have special needs, and therefore, specific 
// implementations. This class is ABSTRACT, and is to be used as a 
// base for other archivers.
abstract public class Archiver {
	protected String[] filterNames;
	protected String[] filterExtensions;
	
	protected String originalDocPath;
	protected String zippedPath;
	protected String workingDocPath; 
	protected String currentConfig;
	
	protected boolean documentEdited;
	
	boolean resumingPrevSession = false;
	
	protected String opfFilePath = null;
	protected Document opfDoc = null;
	protected boolean zip;
	NodeList manifestElements;
	NodeList spineElements;

	// Every file that makes our epub doc.
	ArrayList<String> epubFileList = null;
	
	// This is a list of files that we've created that should 
	// be removed deleted before zipping everything back up.
	ArrayList<String> tempList = null;
	
	// Number of images in each file that makes up our document.
	// For every spine element we have, we're going to count the number of images 
	// in that file. This helps with image traversal.
	ArrayList<Integer> numImages = null;
	
	// Index of current file we're looking at in the browser.
	// Current file we're to load using the spine as a reference.
	// Spine is in .opf file in epub and nimas.
	int curSpineFileIdx = 0;
	
	// The executor helps us with running the runnable every so many seconds. Thanks Brandon.
	public ScheduledExecutorService executor = null;
	
	// Special Archiver Runnable class. So we can pause 
	// thread execution and such.
	public class ArchRunner implements Runnable {
		// True if we're to save. False to not save.
		public boolean shouldWeSave = false;
		// Document and path.
		BBDocument _doc = null;
		String _path = null;
		// Constructor.
		public ArchRunner(BBDocument __doc, String __path) { _doc = __doc; _path = __path; }
		public ArchRunner() {  }
		// Run method.
		@Override
		public void run() { 
	    	// Only save if we're not already doing so.
			if(shouldWeSave)
				autosave(_doc, _path);
	     } // run()
	}; // class ArchRunner
	
	ArchRunner archrun = null;
	
	//////////////////////////////////////////////////////////////////////////////////
	// Constructor.
	Archiver() {}
	
	//////////////////////////////////////////////////////////////////////////////////
	// Constructor. Stores path to document to prepare. Restores previous session's 
	// variables if desired.
	Archiver(String docToPrepare, boolean restore)
	{
		// Store paths.
		originalDocPath = docToPrepare;
		workingDocPath = originalDocPath;
		zippedPath = "";
		documentEdited = false;
		opfFilePath = "";
		epubFileList = new ArrayList<String>();
		tempList = new ArrayList<String>();
		numImages = new ArrayList<Integer>();
		
		// Restore paths to a previous session.
		if(restore)
			restorePrevSession();
			
	} //Archiver(String docToPrepare)
	
	//////////////////////////////////////////////////////////////////////////////////
	// Checks the settings file and restores our paths to a previous session.
	public void restorePrevSession()
	{
		///////////////////////
		// Auto-save RELOAD!!!!
		
			// If the 'prevWorkingFile' property has a value, then we more than likely 
			// had a previous session that didn't close properly. We should load it 
			// for the user.
			PropertyFileManager props = BBIni.getPropertyFileManager();
			String prevWkFilePathStr = props.getProperty("prevWorkingFile");
			String origDocPathStr = props.getProperty("originalDocPath");
			String zpPathStr = props.getProperty("zippedPath");
			String opfPathStr = props.getProperty("opfPath");
			String curConfigStr = props.getProperty("currentConfig");
			
			// Grab the previous working path.
			if( prevWkFilePathStr != null )
				if(prevWkFilePathStr.length() > 0) {
					workingDocPath = prevWkFilePathStr;
					resumingPrevSession = true;
				}
			if( origDocPathStr != null )
				if(origDocPathStr.length() > 0)
					originalDocPath = origDocPathStr;
			if( zpPathStr != null )
				if(zpPathStr.length() > 0)
					zippedPath = zpPathStr;
			if( opfPathStr != null )
				if(opfPathStr.length() > 0)
					opfFilePath = opfPathStr;
			if( curConfigStr != null )
				if(curConfigStr.length() > 0)
					currentConfig = curConfigStr;

		// Auto-save RELOAD!!!!
		///////////////////////
		
	} // restorePrevSession()
	
	//////////////////////////////////////////////////////////////////////////////////
	// Clears the previous session from our settings. Used when we're exiting the 
	// program gracefully.
	public void clearPrevSession() {
		PropertyFileManager props = BBIni.getPropertyFileManager();
		props.save("prevWorkingFile", "");
		props.save("originalDocPath", "");
		props.save("zippedPath", "");
		props.save("opfPath", "");
		props.save("currentConfig", "");
	} // clearPrevSession()
	
	//////////////////////////////////////////////////////////////////////////////////
	// Starts/resumes the autosave feature. 
	public void resumeAutoSave(BBDocument _doc, String _path) {
		if(executor == null && _doc != null && !BBIni.debugging()) {
			archrun = new ArchRunner(_doc, _path);
			archrun.shouldWeSave = true;
			executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(archrun, 0, 60000, TimeUnit.MILLISECONDS);
		}
		else if(executor != null)
			archrun.shouldWeSave = true;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	// Pauses the autosave feature.
	public void pauseAutoSave() {
		if(executor != null)
			archrun.shouldWeSave = false;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	// Stops/destroys the autosave feature. Only do this when the caller for this Archiver is 
	// getting thrown away. This effectively stops the thread. CAN NOT RESTART THREAD 
	// AFTER CALLING THIS.
	public void destroyAutoSave() {
		if(executor != null)
			executor.shutdown();
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	// 
	/** Saves a file
	* @param doc: BBDocument containing the DOM to use to create file
	* @param path: Output path of the file
	*/
	public abstract void save(BBDocument doc, String path);
	
	/** Saves document to new format
	* @param doc:  BBDocument containing the DOM to use to create file
	* @param path: Output path of the file
	* @param ext: The extension is used to determine the course of conversion
	* @return
	*/
	public abstract Archiver saveAs(BBDocument doc, String path, String ext);
	
	public void autosave(BBDocument _doc, String _path) {
		// Save session variables to settings file.
		PropertyFileManager props = BBIni.getPropertyFileManager();
		props.save("prevWorkingFile", workingDocPath);
		props.save("originalDocPath", originalDocPath);
		props.save("zippedPath", zippedPath);
		props.save("opfPath", opfFilePath);
		props.save("currentConfig", currentConfig);

		// Use Archiver-specific backup() function.
		// Varying Archivers save their content differently.
		backup(_doc, _path);
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	// Each Archiver type opens and saves their content in a 
	// different way. They will implement this method to 
	// save their content to the temp folder.
	public abstract void backup(BBDocument __doc, String __path);
	
	//////////////////////////////////////////////////////////////////////////////////
	// Returns path to opf file if we found one with a prior call to findOPF().
	public String getOPFPath() {
		return opfFilePath;
	} // getOPF()
	
	// Get-er for original document path.
	public String getOrigDocPath() { return originalDocPath; }
	
	public String getWorkingFilePath(){
		return workingDocPath;
	}
	
	public String getCurrentConfig(){
		return currentConfig;
	}
	
	public String[]  getFileTypes(){
		return filterNames;
	}
	
	public String[] getFileExtensions(){
		return filterExtensions;
	}
	
	public void setCurrentConfig(String config){
		currentConfig = config;
	}
	
	////////////////////////////////////////////////////////////////
	// Opens our auto config settings file and determines 
	// what file is associated with the given file type.
	// 
	// Appropriate strings to pass so far are: epub, nimas, 
	public String getAutoCfg(String settingStr) {
		
		// Init and load properties.
		Properties props = new Properties();
		try {
			// Load it!
			props.load( new FileInputStream(BBIni.getAutoConfigSettings()) );
		}
		catch (IOException e) { e.printStackTrace(); }

		// Loop through the properties, and find the setting.
		for(String key : props.stringPropertyNames()){
			// Is this the string/setting we're looking for?
			if( key.compareTo(settingStr) == 0 )
				return props.getProperty(key);
		}

		// If we made it here, there was no setting by that name.
		return null;
		
	} // getAutoCfg()
	
	
	/** A convenience method for creating semantics with new file names when saving as a new file
	 * copies to new location specified by user and also to temp folder for backup purposes
	 * @param oldPath
	 * @param newPath
	 */
	protected void copySemanticsForNewFile(String oldPath, String newPath){
		FileUtils fu = new FileUtils();
		String tempSemFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(oldPath) + ".sem";
    	String savedSemFile = fu.getPath(newPath) + BBIni.getFileSep() + fu.getFileName(newPath) + ".sem";   
    	
    	String tempCfgFile = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(oldPath) + ".cfg";
    	String savedCfgFile = fu.getPath(newPath) + BBIni.getFileSep() + fu.getFileName(newPath) + ".cfg";   
    
    	//Save new semantic file to correct location and temp folder for further editing
    	copyUTDFile(tempSemFile, savedSemFile);	
    	copyUTDFile(tempSemFile, BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(newPath) + ".sem");
    	
    	copyUTDFile(tempCfgFile, savedCfgFile);	
    	copyUTDFile(tempCfgFile, BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(newPath) + ".cfg");
	}

	/** Copies a semantic file into a new file; used by save as methods
	 * @param tempSemFile: path to existing file
	 * @param savedFilePath: path to which to create file
	 */
	protected void copyUTDFile(String tempSemFile, String savedFilePath) {
		FileUtils fu = new FileUtils();
		
		if(fu.exists(tempSemFile)){
    		fu.copyFile(tempSemFile, savedFilePath);
    	}
	}
	
	protected void saveBrf(BBDocument doc, String path){
		if(!doc.createBrlFile(path)){
			new Notify("An error has occurred.  Please check your original document");
		}
	}

	public void setDocumentEdited(boolean documentEdited){
		this.documentEdited = documentEdited;
	}
	
	public boolean getDocumentEdited(){
		return documentEdited;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////
	// Traverses the list of upzipped files and attempts to find an opf 
	// file.
	// 
	// Notes: zipper must have already been used to unzip an archive.
	// String is the path to the opf file. We also store it. 
	// Get it with getOPFPath().
	public String findOPF(Zipper zipper)
	{
		// Get paths to all unzipped files.
		ArrayList<String> unzippedPaths = zipper.getUnzippedFilePaths();
		
		// Find the .opf file.
		for(int curFile = 0; curFile < unzippedPaths.size(); curFile++)
		{
			// Does this file have an .opf extension?
			if(unzippedPaths.get(curFile).toLowerCase().endsWith(".opf") == true)
			{
				// Found it!
				return unzippedPaths.get(curFile).toLowerCase();
				
			} // If ends with opf
			
		} // for(int curFile...
		
		// Couldn't find it.
		return null;
		
	} // findOPF()
	
	//////////////////////////////////////////////////////////////////////////////////
	// Uses opf file to build a list of files used by the book/document.
	public ArrayList<String> parseOPFFile(String _opfPath)
	{
		// Build factory, and parse the opf.
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	        factory.setNamespaceAware(true); // Needed, just in case manifest/spine are in a namespace.
			builder = factory.newDocumentBuilder();
			opfDoc = builder.parse(opfFilePath);
        } catch (Exception e) { e.printStackTrace(); }
        
		// Grab the spine elements and manifest elements.
        manifestElements = opfDoc.getElementsByTagNameNS("*", "item");
		spineElements = opfDoc.getElementsByTagNameNS("*", "itemref");
		
		// Filepath to current document.
		String curDocFilePath = null;
		
		// Loop through the spine and find the items in the manifest that we need. 
		for(int curSP = 0; curSP < spineElements.getLength(); curSP++)
		{
			// Get the attributes for this spine element.
			NamedNodeMap spineAtts = spineElements.item(curSP).getAttributes();
			
			// Get the ID of the item we need from the manifest.
			String fileID = spineAtts.getNamedItem("idref").getNodeValue();
			
			// Get the file path from the manifest.
			curDocFilePath = opfFilePath.substring( 0, opfFilePath.lastIndexOf(BBIni.getFileSep()) ) + BBIni.getFileSep();
			curDocFilePath += findHrefById(fileID).replace("/", BBIni.getFileSep());
			
			// Add this path to the list of document paths.
			epubFileList.add(curDocFilePath);
		}
		
		// Return list.
		return epubFileList;
		
	} // buildOPF()
	
	/////////////////////////////////////////////////////////////////////////
	// Finds the manifest element using the given id, 
	// and returns the href attribute value.
	String findHrefById(String id)
	{
		// Loop through the manifest items and find the file with this ID.
		for(int curMan = 0; curMan < manifestElements.getLength(); curMan++)
		{
			// Get the attributes for this manifest item.
			NamedNodeMap manAtts = manifestElements.item(curMan).getAttributes();
			
			// If this manifest item has the id we're looking for, time to open a file.
			if( manAtts.getNamedItem("id").getNodeValue().compareTo(id) == 0 )
			{
				// Get value of href; this is our local file path to the file. Return it.
				return manAtts.getNamedItem("href").getNodeValue();
			
			} // if manifestItem ID == fileID...
		
		} // for(int curMan...
		
		// Couldn't find it.
		return null;
	
	} // findHrefById()
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// Takes in a document(W3C) and adds its image count to the list.
	// ...because of the way we're adding temp documents, we need to know which document 
	// we're loading to associate the right count.
	public void addToNumImgsList(Document addMe, int docIdx)
	{
		// Create space big enough to hold our image integers if we haven't done so already.
		if(numImages == null)
			numImages = new ArrayList<Integer>();
		
		// Grab all <img> elements.
		NodeList imgElements = addMe.getElementsByTagName("img");
		
		// If there are no images, return.
		if(imgElements == null)
			return;
		
		// Add this value to the list.
		
		// If this count value makes our list bigger, resize and copy.
		if(docIdx >= numImages.size()) {
			// Create a new list.
			ArrayList<Integer> tmpStrList = new ArrayList<Integer>();
			// Make it so big.
			for(int curI = 0; curI < docIdx + 1; curI++)
				tmpStrList.add(0);
			// Copy old into new.
			for(int curV = 0; curV < numImages.size(); curV++)
				tmpStrList.set( curV, numImages.get(curV) );
			// Point to new list.
			numImages = tmpStrList;
		}
		
		// Finally, add the new count.
		numImages.set(docIdx, imgElements.getLength());
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// Takes in a document(XOM) and adds its image count to the list.
	public void addToNumImgsList(nu.xom.Document addMe, int docIdx)
	{
		// Convert to DOM.
		Document w3cDoc = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation impl = builder.getDOMImplementation();
			w3cDoc = DOMConverter.convert(addMe, impl);
		}
		catch(Exception e) { e.printStackTrace(); }
		
		// Now add.
		addToNumImgsList(w3cDoc, docIdx);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// Just adds counts sequentially to the image count list.
	public void addToNumImgsList(int value) {
		numImages.add(value);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// Returns the list of documents that make up this book.
	public ArrayList<String> getSpine() {
		return epubFileList;
	}
	///////////////////////////////////////////////////////////////////////////////////////////	
	// Returns a path from a particular spine element.
	public String getSpineFilePath(int idx) {
		if(epubFileList != null)
			if(epubFileList.size() > 0)
				return epubFileList.get(idx);
		return null;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Returns list of image counts for files in spine.
	public ArrayList<Integer> getImgCountList() {
		return numImages;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Return number of spine elements.
	public int getNumSpineElements()
	{
		// Returns size of spine list.
		return getSpine().size();
	
	} // getNumSpineElements()
	
	//////////////////////////////////////////////////////////////////////
	// If working with an epub document, returns current spine file.
	public String getCurSpineFilePath()
	{
		// Get current spine file path.
		return getSpineFilePath(curSpineFileIdx);
	
	} // getCurSpineFile()
	
	//////////////////////////////////////////////////////////////////////
	// Returns the current spine index.
	public int getCurSpineIdx() {
		return curSpineFileIdx;
	} // getCurSpineIdx()
	
	//////////////////////////////////////////////////////////////////////
	// Converts image index to a local index, in reference to 
	// a page.
	// 
	// For example: We could be on the 56th image, but it could be the 
	// second image in this particular spine element/page.
	// 
	// Returns -1 if we can't find it, or if there isn't a supported 
	// archiver.
	public int getImageIndexInSpinePage(int imageIndex)
	{
		// Get image counts for spine.
		ArrayList<Integer> imgCntList = getImgCountList();
		
		// Current image index in the spine we're testing against.
		int curImgIdx = 0;
		
		// Add up the spine/image counts
		for(int curS = 0; curS < curSpineFileIdx; curS++)
			curImgIdx += imgCntList.get(curS);
		
		// Is this image index within range of this particular spine element?
		if( imageIndex >= curImgIdx && imageIndex < curImgIdx + imgCntList.get(curSpineFileIdx) )
		{
			// Move to the spine element that we've found.
			return imageIndex - curImgIdx;
	
		} // if( within range )
	
		// If we're here, we couldn't find the spine or image. Return failure.
		return -1;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Takes an image index, and finds the spine file 
	// that contains this image.
	public String setSpineFileWithImgIndex(int imgIndex)
	{
		
		// Get image counts for spine.
		ArrayList<Integer> imgCntList = getImgCountList();
		
		// Current image index in the spine we're testing against.
		int curImgIdx = 0;
		
		// Loop through the spine/image counts, until we find one that contains this image.
		for(int curS = 0; curS < imgCntList.size(); curS++)
		{
			// Is this image index within range of this particular spine element?
			if( imgIndex >= curImgIdx && imgIndex < curImgIdx + imgCntList.get(curS) )
			{
				// Move to the spine element that we've found.
				return gotoSpineFilePath(curS);
			
			} // if( within range )
			
			// Move forward with the index.
			curImgIdx += imgCntList.get(curS);
		
		} // for(curS)
		
		// If we make it here, we couldn't find that particular spine file/element.
		return null;
	
	} // setSpineFileWithImgIndex()
	
	//////////////////////////////////////////////////////////////////////
	// Moves to a specific spine file using an index into the list.
	public String gotoSpineFilePath(int idx)
	{
		// Go to next file path.
		curSpineFileIdx = idx;
		// If we've gone too far, wrap around.
		if(curSpineFileIdx >= getSpine().size())
			curSpineFileIdx = 0;
		if(curSpineFileIdx < 0 )
			curSpineFileIdx = getSpine().size() - 1;
		
		// Return the current spine file path.
		return getSpineFilePath(curSpineFileIdx);
	
	} // gotoSpineFilePath()
	
	//////////////////////////////////////////////////////////////////////
	// Moves index to current file to the next one we see in the spine.
	public String nextSpineFilePath()
	{
		// Go to next file path.
		curSpineFileIdx++;
		// If we've gone too far, wrap around.
		if(curSpineFileIdx >= getSpine().size())
			curSpineFileIdx = 0;
		
		// Return the current spine file path.
		return getSpineFilePath(curSpineFileIdx);
	
	} // nextSpineFile()
	
	//////////////////////////////////////////////////////////////////////
	// Moves index to current file to the previous one we see in the spine.
	public String prevSpineFilePath()
	{
		// Go to previous file path.
		curSpineFileIdx--;
		// If we've gone too far, wrap around.
		if(curSpineFileIdx < 0 )
			curSpineFileIdx = getSpine().size() - 1;
		
		// Return the current spine file path.
		return getSpineFilePath(curSpineFileIdx);
		
	} // prevSpineFile()
	
	//////////////////////////////////////////////////////////////////////
	// Adds temp file to be deleted later.
	public void addTempFile(String path) {
		tempList.add(path);
	} //addTempFile()
	
	//////////////////////////////////////////////////////////////////////
	// Delete temporary files that we've created in the archiver.
	public void deleteTempFiles()
	{
		// Delete them all.
		for(int curFile = 0; curFile < tempList.size(); curFile++) {
			// If we actually got around to creating this file, delete it.
			if(tempList.get(curFile) != null) {
				File f = new File(tempList.get(curFile));
				f.delete();
			}
		}
		
	} // deleteTempFiles()
	
	/** Copies an file not in an epub or zip file, and it's corresponding semantic file and/or config file, if they exist, to the temp folder
	 * These files are placed in the temp folder so work can saved and recovered due to an unexpected crash
	 * without altering the original file
	 * @param path: path of file to open
	 */
	protected void copyFilesToTemp(String path){
		FileUtils fu = new FileUtils();
		workingDocPath = BBIni.getTempFilesPath() + BBIni.getFileSep() + path.substring(path.lastIndexOf(BBIni.getFileSep()) + 1);
		fu.copyFile(path, workingDocPath);
		
		String sem = fu.getPath(path) + BBIni.getFileSep() + fu.getFileName(path) + ".sem";
		File f = new File(sem);
		if(f.exists()){
			String tempSem = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(path) + ".sem";
			fu.copyFile(sem, tempSem);
		}
		
		String config = fu.getPath(path) + BBIni.getFileSep() + fu.getFileName(path) + ".cfg";
		File configFile = new File(config);
		if(configFile.exists()){
			String tempConfig = BBIni.getTempFilesPath() + BBIni.getFileSep() + fu.getFileName(path) + ".cfg";
			fu.copyFile(config, tempConfig);
		}
	}
	
} // class Archiver