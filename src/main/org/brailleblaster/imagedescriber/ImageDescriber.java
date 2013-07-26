/* BrailleBlaster Braille Transcription Application
  *
  * Copyright (C) 2010, 2012
  * ViewPlus Technologies, Inc. www.viewplus.com
  * and
  * Abilitiessoft, Inc. www.abilitiessoft.com
  * All rights reserved
  *
  * This file may contain code borrowed from files produced by various 
  * Java development teams. These are gratefully acknoledged.
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
  * Maintained by John J. Boyer john.boyer@abilitiessoft.com
*/

package org.brailleblaster.imagedescriber;

import java.util.ArrayList;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import org.brailleblaster.document.BBDocument;

public class ImageDescriber {

	// The document with images we want to add descriptions to.
	private BBDocument doc;
	// Current image element.
	private Element curImgElement;
	// Root element.
	private Element rootElement;
	// List of <img> elements.
	ArrayList<Element> imgList = null;
	// The current element we're working on.
	int curElement = -1;
	// The number of img elements we have in this document.
	int numImgElms = 0;
	
	///////////////////////////////////////////////////////////////////////////
	// Call ImageDescriber with this Constructor to initialize everything.
	public ImageDescriber(BBDocument document){
		
		// Init variables.
		doc = document;
		rootElement = doc.getRootElement();
		curImgElement = rootElement;
		imgList = new ArrayList<Element>();
		curElement = -1;

		// Fill list of <img>'s.
		fillImgList(rootElement);
		
		// Get size of <img> list.
		numImgElms = imgList.size();
		
		// Only init the current element if there are <img>'s.
		if(numImgElms > 0)
			curElement = 0;
		
		for( int asdf = 0; asdf < numImgElms; asdf++ ) {
			if( hasImgGrpParent(imgList.get(asdf)) == false) {
				wrapInImgGrp(imgList.get(asdf));
				System.out.println( imgList.get(asdf) + " has been wrapped!" );
//			System.out.println( imgList.get(asdf).getAttribute("src") );
			}
//			imgList.get(asdf).getAttribute("src").setValue("Rubber Chicken");
		}
		
	} // ImageDescriber(BBDocument document)
	
	///////////////////////////////////////////////////////////////////////////
	// Searches forward in the xml tree for an element named <img>
	public Element nextImageElement()
	{
		// Make sure there are images.
		if(numImgElms == 0)
			return null;
		
		// Move to next element, then return it.
		curElement++;
		if(curElement >= numImgElms)
			curElement = 0;
			
		// Return current <img> element.
		return imgList.get(curElement);
		
	} // NextImageElement()
	
	///////////////////////////////////////////////////////////////////////////
	// Searches backward in the xml tree for an element named <img>
	public Element prevImageElement()
	{
		// Make sure there are images.
		if(numImgElms == 0)
			return null;
		
		// Move to previous element, then return it.
		curElement--;
		if(curElement < 0)
			curElement = numImgElms - 1;
			
		// Return current <img> element.
		return imgList.get(curElement);
	
	} // PrevImageElement()
	
	///////////////////////////////////////////////////////////////////////////
	// Recursively moves through xml tree and adds <img> nodes to list.
	public void fillImgList(Element e)
	{
		// Is this element an <img>?
		if( e.getLocalName().compareTo("img") == 0 )
			imgList.add(e);
		
		// Get children.
		Elements childElms = e.getChildElements();
		
		// Get their children, and so on.
		for(int curChild = 0; curChild < childElms.size(); curChild++)
			fillImgList( childElms.get(curChild) );
		
	} // FillImgList(Element e)
	
	///////////////////////////////////////////////////////////////////////////
	// Returns true if the current image has an <imggroup> parent.
	// False otherwise.
	public boolean hasImgGrpParent(Element e)
	{
		// If the parent is <imggroup>, return true.
		if( ((nu.xom.Element)(e.getParent())).getLocalName().compareTo("imggroup") == 0 )
			return true;
		else
			return false;
		
	} // HasImgGrpParent(Element e)
	
	///////////////////////////////////////////////////////////////////////////
	// Encapsulates given element into <imggroup>, and adds 
	// <prodnote> in the group with it.
	public void wrapInImgGrp(Element e)
	{
		// Create all elements.
		String ns = e.getDocument().getRootElement().getNamespaceURI();
		Element imgGrpElm = new Element("imggroup", ns);
		Element prodElm = new Element("prodnote", ns);
		Element copyElm = (nu.xom.Element)e.copy();
		
		// Add <prodnote> attributes.
		prodElm.addAttribute( new Attribute("id", "TODO!") );
		prodElm.addAttribute( new Attribute("imgref", copyElm.getAttributeValue("id")) );
		prodElm.addAttribute( new Attribute("render", "required") );
		
		// Arrange child hierarchy.
		imgGrpElm.appendChild(copyElm);
		imgGrpElm.appendChild(prodElm);
		
		// Replace given element with this updated one.
		e.getParent().replaceChild(e, imgGrpElm);
		
	} // wrapInImgGrp(Element e)
		
} // public class ImageDescriber {
