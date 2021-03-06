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

package org.brailleblaster.perspectives.braille.views.wp;

import java.util.ArrayList;
import java.util.Map.Entry;

import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;

import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.Styles;
import org.brailleblaster.perspectives.braille.document.BBSemanticsTable.StylesType;
import org.brailleblaster.perspectives.braille.mapping.elements.BrailleMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.PageMapElement;
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement;
import org.brailleblaster.perspectives.braille.mapping.maps.MapList;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.braille.views.wp.formatters.WhiteSpaceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Rectangle;

public class BrailleView extends WPView {
	private ViewStateObject stateObj;
	private ArrayList<BrailleMapElement> pageRanges = new ArrayList<BrailleMapElement>();
	private String charAtOffset;
	
	private VerifyKeyListener verifyListener;
	private FocusListener focusListener;
	private MouseAdapter mouseListener;
	private CaretListener caretListener;
	private SelectionAdapter selectionListener;
	private TraverseListener traverseListener;

	//Added this line to save previous indicator 
	ArrayList <Bullet> indications; 
	boolean flag=true;
	int counter;
	
	public BrailleView(Manager manager, SashForm sash, BBSemanticsTable table) {
		super(manager, sash, table);
		stateObj = new ViewStateObject();
		this.total = 0;
		this.spaceBeforeText = 0;
		this.spaceAfterText = 0;
		indications=new ArrayList<Bullet>();
	}
	
	@Override
	public void initializeListeners(){
		view.addTraverseListener(traverseListener = new TraverseListener(){
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.stateMask == SWT.MOD1 + SWT.MOD2 && e.keyCode == SWT.TAB)
					manager.setStyleTableFocus(e);
			}
			
		});
		
		view.addVerifyKeyListener(verifyListener = new VerifyKeyListener(){
			@Override
			public void verifyKey(VerifyEvent e) {
				stateObj.setCurrentChar(e.keyCode);

				//Handles single case where page is on last line and text is selected to last line and arrow down is pressed which does not move cursor
				if(manager.inBraillePageRange(view.getCaretOffset()) && e.keyCode == SWT.ARROW_DOWN && view.getLineAtOffset(view.getCaretOffset()) == view.getLineCount() - 1)
					view.setCaretOffset(stateObj.getNextStart());
				
				stateObj.setOldCursorPosition(view.getCaretOffset());
			}
			
		});
		
		view.addFocusListener(focusListener = new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				Message message = Message.createGetCurrentMessage(Sender.BRAILLE, view.getCaretOffset());
				manager.dispatch(message);
				setViewData(message);
				if(stateObj.getOldCursorPosition() == -1 && positionFromStart  == 0){
					view.setCaretOffset((Integer)message.getValue("brailleStart"));
				}
				sendStatusBarUpdate(view.getLineAtOffset(view.getCaretOffset()));

			}

			@Override
			public void focusLost(FocusEvent e) {
				setPositionFromStart();
				Message message = Message.createUpdateCursorsMessage(Sender.BRAILLE);
				manager.dispatch(message);
			}
		});
		
		view.addMouseListener(mouseListener = new MouseAdapter(){
			@Override
			public void mouseDown(MouseEvent e) {
				if(!getLock()){
					if(view.getCaretOffset() > stateObj.getCurrentEnd() || view.getCaretOffset() < stateObj.getCurrentStart()){
						setCurrent();
				
					}
					sendStatusBarUpdate(view.getLineAtOffset(view.getCaretOffset()));
				}
			}	
		});
		
		view.addCaretListener(caretListener = new CaretListener(){
			@Override
			public void caretMoved(CaretEvent e) {
				if(!getLock()){
					if(stateObj.getCurrentChar() == SWT.ARROW_DOWN || stateObj.getCurrentChar() == SWT.ARROW_LEFT || stateObj.getCurrentChar() == SWT.ARROW_RIGHT || stateObj.getCurrentChar() == SWT.ARROW_UP || stateObj.getCurrentChar() == SWT.PAGE_DOWN || stateObj.getCurrentChar() == SWT.PAGE_UP){
						if(e.caretOffset >= stateObj.getCurrentEnd() || e.caretOffset < stateObj.getCurrentStart()){						
							setCurrent();	
							stateObj.setCurrentChar(' ');
						}
						//if(view.getLineAtOffset(view.getCaretOffset()) != currentLine){
							sendStatusBarUpdate(view.getLineAtOffset(view.getCaretOffset()));
						//}
					}
				}
			}
		});
		
		view.getVerticalBar().addSelectionListener(selectionListener = new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkStatusBar(Sender.BRAILLE);
				if(!getLock() & scrollBarPos != view.getVerticalBar().getSelection()){
					scrollBarPos = view.getVerticalBar().getSelection();
					if(view.getVerticalBar().getSelection() == (view.getVerticalBar().getMaximum() - view.getVerticalBar().getThumb()))
						manager.incrementView();
					else if(view.getVerticalBar().getSelection() == 0)
						manager.decrementView();
				}
			}
		});
		
		view.addPaintListener(new PaintListener(){
			@Override
			public void paintControl(PaintEvent e) {
				checkStatusBar(Sender.BRAILLE);
			}			
		});
		view.addModifyListener(viewMod);
		setListenerLock(false);
	}
	
	@Override
	public void removeListeners(){
		view.removeModifyListener(viewMod);
		view.removeTraverseListener(traverseListener);
		view.removeVerifyKeyListener(verifyListener);
		view.removeFocusListener(focusListener);
		view.removeMouseListener(mouseListener);
		view.removeCaretListener(caretListener);
		view.getVerticalBar().removeSelectionListener(selectionListener);
	}
	
	private void setCurrent(){
		Message message = Message.createSetCurrentMessage(Sender.BRAILLE, view.getCaretOffset(), true);
		
		if(charAtOffset != null)
			message.put("char", charAtOffset);
		
		manager.dispatch(message);
		setViewData(message);
		charAtOffset = null;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void setViewData(Message message){
		stateObj.setCurrentStart((Integer)message.getValue("brailleStart"));
		stateObj.setCurrentEnd((Integer)message.getValue("brailleEnd"));
		stateObj.setNextStart((Integer)message.getValue("nextBrailleStart"));
		this.pageRanges.clear();
		setPageRange((ArrayList<BrailleMapElement>)message.getValue("pageRanges"));
	}
	
	private void setPageRange(ArrayList<BrailleMapElement> list){
		this.pageRanges.clear();
		for(int i = 0; i < list.size(); i++){
			this.pageRanges.add(list.get(i));
		}
	}
	
	public void setBraille(TextMapElement t, MapList list, int index){
		setListenerLock(true);
		for(int i = 0; i < t.brailleList.size(); i++){
			Styles style = stylesTable.makeStylesElement(t.parentElement(), t.brailleList.get(i).n);
			Styles prevStyle;
			if(list.size() > 1 && index != 0 &&  list.get(index - 1).n!=null)
				prevStyle = stylesTable.makeStylesElement(list.get(index - 1).parentElement(),list.get(index - 1).n);
			else
				prevStyle = null;
		
			String textBefore = "";
			String text = t.brailleList.get(i).n.getValue();
			int textLength = text.length();
			
	
			if(insertNewLine(t.brailleList.get(i).n)){
				textBefore = "\n";
				
				spaceBeforeText++;
			}
			
		
			view.append(textBefore + text);
		
			
			handleStyle(prevStyle, style, t.brailleList.get(i).n, t.parentElement());
		
			t.brailleList.get(i).setOffsets(spaceBeforeText + total, spaceBeforeText + total + textLength);
			total += spaceBeforeText + textLength + spaceAfterText;
			
		
			spaceBeforeText = 0;
			spaceAfterText = 0;
		}
		setListenerLock(false);
	}

	
	public void prependBraille(TextMapElement t, MapList list, int index){
		setListenerLock(true);
		for(int i = 0; i < t.brailleList.size(); i++){
			Styles style = stylesTable.makeStylesElement(t.parentElement(), t.brailleList.get(i).n);
			Styles prevStyle;
			if(list.size() > 1 && index != 0)
				prevStyle = stylesTable.makeStylesElement(list.get(index - 1).parentElement(),list.get(index - 1).n);
			else
				prevStyle = null;
		
			String textBefore = "";
			String text = t.brailleList.get(i).n.getValue();
			int textLength = text.length();
	
			if(insertNewLine(t.brailleList.get(i).n)){
				textBefore = "\n";
				spaceBeforeText++;
			}
		
			view.insert(textBefore + text);
			handleStyle(prevStyle, style, t.brailleList.get(i).n, t.parentElement());
		
			t.brailleList.get(i).setOffsets(spaceBeforeText + total, spaceBeforeText + total + textLength);
			total += spaceBeforeText + textLength + spaceAfterText;
			spaceBeforeText = 0;
			spaceAfterText = 0;
			view.setCaretOffset(total);
		}
		setListenerLock(false);
	}
	
	public void setBraille(MapList list, Node n, TextMapElement t){
		setListenerLock(true);
		Styles style = stylesTable.makeStylesElement(t.parentElement(), n);
		Styles prevStyle;
		if(list.size() > 1 && list.get(list.size() - 2).n!=null)
			prevStyle = stylesTable.makeStylesElement(list.get(list.size() - 2).parentElement(),list.get(list.size() - 2).n);
		else
			prevStyle = null;
		
		String textBefore = "";
		String text = n.getValue();
		int textLength = text.length();
	
		if(insertNewLine(n)){
			textBefore = "\n";
			spaceBeforeText++;
		}
		
		view.append(textBefore + text);
		handleStyle(prevStyle, style, n, t.parentElement());
		
		t.brailleList.add(new BrailleMapElement(spaceBeforeText + total, spaceBeforeText + total + textLength, n));
		total += spaceBeforeText + textLength + spaceAfterText;
		spaceBeforeText = 0;
		spaceAfterText = 0;
		setListenerLock(false);
	}
	
	private boolean insertNewLine(Node n){
		Element parent = (Element)n.getParent();
		int index = parent.indexOf(n);
		if(index > 0){
			if(((Element)parent.getChild(index - 1)).getLocalName().equals("newline"))
				return true;
		}
		
		return false;
	}
	
	/*
	private void checkFinalNewline(Node n){
		Element parent = (Element)n.getParent();
		int childCount = parent.getChildCount();
		
		if(parent.indexOf(n) == childCount - 2){
			if(parent.getChild(childCount - 1) instanceof Element && ((Element)parent.getChild(childCount - 1)).getLocalName().equals("newline")){
				view.append("\n");
				this.spaceAfterText++;
			}
		}
	}
	*/
	
	private void handleStyle(Styles prevStyle, Styles style, Node n, Element parent){
		boolean isFirst = isFirst(n);
		String viewText = n.getValue();

		for (Entry<StylesType, Object> entry : style.getEntrySet()) {
			switch(entry.getKey()){
				case linesBefore:
					if(isFirst && (prevStyle == null || !prevStyle.contains(StylesType.linesAfter)))
						setLinesBefore(total + spaceBeforeText, style);
					break;
				case linesAfter:
					if(isLast(n))
						setLinesAfter(spaceBeforeText + total + viewText.length() + spaceAfterText, style);
					break;
				case firstLineIndent: 
					if(isFirst && (Integer.valueOf((String)entry.getValue()) > 0 || style.contains(StylesType.leftMargin)))
						setFirstLineIndent(spaceBeforeText + total, style);
					break;
				case format:
					setAlignment(spaceBeforeText + total, spaceBeforeText + total + n.getValue().length(), style);
					break;	
				case emphasis:
			//		 setFontRange(this.total, this.spaceBeforeText + n.getValue().length(), Integer.valueOf(entry.getValue()));
					 break;
				case leftMargin:
					if(followsNewLine(n)){
						if(isFirst && !style.contains(StylesType.firstLineIndent))
							view.setLineIndent(view.getLineAtOffset(spaceBeforeText + total), 1, (Integer.valueOf((String)entry.getValue()) * charWidth));
						else if(!isFirst)
							view.setLineIndent(view.getLineAtOffset(spaceBeforeText + total), 1, (Integer.valueOf((String)entry.getValue()) * charWidth));
					}
					break;
				case name:
					break;
				case topBoxline:
				case bottomBoxline:
					break;
				default:
					System.out.println(entry.getKey());
			}
		}
	}
	
	public void adjustStyle(Message m, ArrayList<TextMapElement>list){
		int start = list.get(0).brailleList.getFirst().start;
		int end = list.get(list.size() - 1).brailleList.getLast().end;
		int length = 0;
		int spaces = 0;
		int offset = 0;
		getBounds(m, list);	
	
		String textBefore = "";
		Styles style = (Styles)m.getValue("Style");
		Styles previousStyle = (Styles)m.getValue("previousStyle");
		
		setListenerLock(true);	
		view.setLineIndent(view.getLineAtOffset(start), getLineNumber(start, view.getTextRange(start, (end - start))), 0);
		view.setLineAlignment(view.getLineAtOffset(start), getLineNumber(start, view.getTextRange(start, (end - start))), SWT.LEFT);
		
		if(!style.contains(StylesType.linesBefore)  && previousStyle.contains(StylesType.linesBefore))
			removeLinesBefore(m);
		
		if(!style.contains(StylesType.linesAfter) &&  previousStyle.contains(StylesType.linesAfter))
			removeLinesAfter(m);

		start = (Integer)m.getValue("start");
		end = (Integer)m.getValue("end");
		int prev = (Integer)m.getValue("braillePrev");
		int next = (Integer)m.getValue("brailleNext");
		
		for (Entry<StylesType, Object> entry : style.getEntrySet()) {
			switch(entry.getKey()){
				case linesBefore:
					if(start != prev){
						view.replaceTextRange(prev, (start - prev), "");
						length = start - prev;	
					}
					
					spaces = Integer.valueOf((String)entry.getValue());
					textBefore = makeInsertionString(spaces,'\n');
					offset = spaces - length;
									
					insertBefore(start - (start - prev), textBefore);
					start += offset;
					end += offset;
					if(next != -1)
						next += offset;
					break;
				case linesAfter:
					length = 0;
					if(end != next && next != 0){
						view.replaceTextRange(end, (next - end), "");
						length = next - end;	
					}
					spaces = Integer.valueOf((String)entry.getValue());
					textBefore = makeInsertionString(spaces,'\n');
					insertBefore(end, textBefore);
					offset = spaces - length;
					break;
				case format:
					setAlignment(start, end, style);
					break;
				case firstLineIndent:
					if(Integer.valueOf((String)entry.getValue()) > 0 || style.contains(StylesType.leftMargin))
						setFirstLineIndent(start, style);
					break;
				case leftMargin:
					if(style.contains(StylesType.firstLineIndent))
						handleLineWrap(start, view.getTextRange(start, (end - start)), Integer.valueOf((String)entry.getValue()), true);
					else
						handleLineWrap(start, view.getTextRange(start, (end - start)), Integer.valueOf((String)entry.getValue()), false);
					break;
				default:
					break;
			}
		}
		setListenerLock(false);
	}
	
	public void getBounds(Message m, ArrayList<TextMapElement>list){
		m.put("start", list.get(0).brailleList.getFirst().start);
		m.put("end", list.get(list.size() - 1).brailleList.getLast().end);
		m.put("braillePrev", getPrev(m));
		m.put("brailleNext", getNext(m));
		m.put("offset", 0);	
	}
	
	private int getPrev(Message m){
		int prev = (Integer)m.getValue("braillePrev");
		
		if(-1 != prev)
			prev++;
		else
			prev = 0;
		
		return prev;
	}
	
	private int getNext(Message m){
		int next = (Integer)m.getValue("brailleNext");
		
		if(-1 != next)
			next--;
		else 
			next = view.getCharCount();
		
		return next;
	}
	
	private void removeLinesBefore(Message m){
		int prev = (Integer)m.getValue("braillePrev");
		int start = (Integer)m.getValue("start");
		int end = (Integer)m.getValue("end");
		int next = (Integer)m.getValue("brailleNext");
		int offset = (Integer)m.getValue("offset");
		int length = 0;
		
		if(start != prev){
			view.replaceTextRange(prev, (start - prev), "");
			length = start - prev;	
			offset -= length;
		}
	
		m.put("linesBeforeOffset", offset);
		m.put("offset", offset);
		start += offset;
		end += offset;
		m.put("start", start);
		m.put("end", end);
		m.put("prev", prev);
		if(next != -1)
			m.put("brailleNext", next + offset);
	}
	
	public void removeLinesAfter(Message m){
		int end = (Integer)m.getValue("end");
		int next = (Integer)m.getValue("brailleNext");
		int offset = (Integer)m.getValue("offset");
		
		if(end != next){
			int removedSpaces;
			removedSpaces = next - end;
		
			view.replaceTextRange(end, removedSpaces, "");
		}

		if(next != -1){
			//m.put("linesAfterOffset", offset);
			m.put("brailleNext", next + offset);
		}
	}
	
	
	private boolean followsNewLine(Node n){
		Element parent = (Element)n.getParent();
		int index = parent.indexOf(n);
		
		if(index > 0 && isElement(parent.getChild(index - 1))){
			if(((Element)parent.getChild(index - 1)).getLocalName().equals("newline"))
				return true;
		}
		return false;
	}
	
	private boolean isFirst(Node n){	
		int i = 0;
		Element parent = (Element)n.getParent();
		
		if(parent.getAttribute("modifiers") != null){
			if(parent.indexOf(n)  < 3 && parent.getChild(0) instanceof Element){
				if(parent.indexOf(n) == 1 && ((Element)parent.getChild(0)).getLocalName().equals("newline")){
					return isFirstElement((Element)parent.getParent().getChild(parent.getParent().indexOf(parent) - 1));
				}
				else if(parent.indexOf(n) == 2 && ((Element)parent.getChild(0)).getLocalName().equals("newpage")){
					if(parent.getChild(1) instanceof Element && ((Element)parent.getChild(1)).getLocalName().equals("newline"))
						return isFirstElement((Element)parent.getParent().getChild(parent.getParent().indexOf(parent) - 1));
					else
						return false;
				}
				else
					return false;
			}
			else
				return false;
		}
		
		while(!(isText(parent.getChild(i)))){
			i++;
		}
		
		if(parent.indexOf(n) == i){
			Element grandParent = (Element)parent.getParent();
			Elements els = grandParent.getChildElements();
			
		if(!els.get(0).getLocalName().equals("brl") || !els.get(0).equals(parent))
			return false;
			
			if(grandParent.getAttributeValue("semantics").contains("action") && !grandParent.getLocalName().equals("lic")){
				return isFirstElement(grandParent);
			}
			else {
				i = 0;
				while((isText(grandParent.getChild(i)))){
					i++;
				}
				if(grandParent.indexOf(parent) == i)
					return true;
				else 
					return false;
			}
		}
		else {
			return false;
		}
	}
	
	private boolean isFirstElement(Element child){
		Element parent = (Element)child.getParent();
		
		while(parent.getAttributeValue("semantics").contains("action")){	
			if(parent.indexOf(child) != 0)
				return false;
			
			child = parent;
			parent = (Element)parent.getParent(); 
		}
		
		if(parent.indexOf(child) == 0)
			return true;
		else
			return false;
	}
	
	private boolean isLast(Node n){
		boolean isLast = false;
		Element parent = (Element)n.getParent();
		
		for(int i = 0; i < parent.getChildCount(); i++){
			if(isText(parent.getChild(i))){
				if(parent.getChild(i).equals(n)){
					isLast = true;
				}
				else{
					isLast = false;
				}
			}
			else if(isElement(n)){
				if(parent.getChild(i).equals(n)){
					isLast = true;
				}
				else{
					isLast = false;
				}
			}
		}
		
		if(isLast){
			Element grandParent = (Element)parent.getParent();
			for(int i = 0; i < grandParent.getChildCount(); i++){
				if(isElement(grandParent.getChild(i))){
					if(grandParent.getChild(i).equals(parent)){
						isLast = true;
					}
					else if(grandParent.getLocalName().equals("li") && isElement(grandParent.getChild(i))){
						if(!((Element)grandParent.getChild(i)).getLocalName().equals("list") && !((Element)grandParent.getChild(i)).getLocalName().equals("p"))
							isLast = false;
					}
					else if(!((i == grandParent.getChildCount() - 1) && ((Element)grandParent.getChild(i)).getLocalName().equals("br"))) {
						isLast = false;
					}
				}
			}
			
			if(isLast && (grandParent.getAttributeValue("semantics").contains("action")))
				isLast = isLast(parent);
		}
		
		return isLast;
	}
	
	public void updateBraille(TextMapElement t, Message message){
		Styles style = stylesTable.makeStylesElement(t.parentElement(), t.n);
		int total = (Integer)message.getValue("brailleLength");
		int margin = 0;
		int pos = view.getCaretOffset();
		System.out.println("Value: " + t.value());
		String insertionString = (String)message.getValue("newBrailleText");
		
		if(t.brailleList.getFirst().start != -1){
			setListenerLock(true);			
			
			view.replaceTextRange(t.brailleList.getFirst().start, total, insertionString);
		
		    //Add new one then Remove previous indicators 
		
			flag=true;
			removeIndicator();
			addIndicator();

			if(style.contains(StylesType.format) && t.brailleList.size() > 0)
				setAlignment(t.brailleList.getFirst().start, t.brailleList.getLast().end, style);
			
			//reset margin in case it is not applied
			if(t.brailleList.getFirst().start == view.getOffsetAtLine(view.getLineAtOffset(t.brailleList.getFirst().start)))
				handleLineWrap(t.brailleList.getFirst().start, insertionString, 0, false);
			
			if(style.contains(StylesType.leftMargin)) {
				margin = Integer.valueOf((String)style.get(StylesType.leftMargin));
				handleLineWrap(t.brailleList.getFirst().start, insertionString, margin, style.contains(StylesType.firstLineIndent));
			}
				
			if(isFirst(t.brailleList.getFirst().n) && style.contains(StylesType.firstLineIndent)&& insertionString.length() > 0)
				setFirstLineIndent(t.brailleList.getFirst().start, style);
			view.setCaretOffset(pos);
			setListenerLock(false);	
		}
	}
	
	public void refreshStyle(TextMapElement t){
		Styles style = stylesTable.makeStylesElement(t.parentElement(), t.n);
		String text = view.getTextRange(t.brailleList.getFirst().start, t.brailleList.getLast().end - t.brailleList.getFirst().start);
		int margin = 0;
		if(style.contains(StylesType.format) && t.brailleList.size() > 0)
			setAlignment(t.brailleList.getFirst().start, t.brailleList.getLast().end, style);
		else
			setAlignment(t.brailleList.getFirst().start, t.brailleList.getLast().end, SWT.LEFT);
		
		//reset margin in case it is not applied
		if(t.brailleList.getFirst().start == view.getOffsetAtLine(view.getLineAtOffset(t.brailleList.getFirst().start)))
			handleLineWrap(t.brailleList.getFirst().start, text, 0, false);
		
		if(style.contains(StylesType.leftMargin)) {
			margin = Integer.valueOf((String)style.get(StylesType.leftMargin));
			handleLineWrap(t.brailleList.getFirst().start, text, margin, style.contains(StylesType.firstLineIndent));
		}
			
		if(isFirst(t.brailleList.getFirst().n) && style.contains(StylesType.firstLineIndent))
			setFirstLineIndent(t.brailleList.getFirst().start, style);
	}
	
	public void removeMathML(TextMapElement t){
		int total = t.brailleList.getLast().end - t.brailleList.getFirst().start;
		view.replaceTextRange(t.brailleList.getFirst().start, total, "");
	}
	
	public void removeWhitespace(int start, int length){
		setListenerLock(true);
		view.replaceTextRange(start, Math.abs(length), "");
		setListenerLock(false);
	}
	
	public void changeAlignment(int startPosition, int alignment){
		view.setLineAlignment(view.getLineAtOffset(startPosition), 1, alignment);
	}
	
	public void changeIndent(int start, Message message){
		view.setLineIndent(view.getLineAtOffset(start), 1, (Integer)message.getValue("indent"));
	}
	
	public void changeMargin(int start, int end, Message message){
		int startLine = view.getLineAtOffset(start);
		int endLine = view.getLineAtOffset(end);
		int lines = (endLine - startLine) + 1;
		view.setLineIndent(startLine, lines, (Integer)message.getValue("margin"));
	}
	
	public void updateCursorPosition(Message message){
		setListenerLock(true);
		setViewData(message);
		setCursorPosition(message);
		setPositionFromStart();
		setListenerLock(false);
	}
	
	public void setPositionFromStart(){
		int count = 0;
		positionFromStart = view.getCaretOffset() - stateObj.getCurrentStart();
		if(positionFromStart > 0 && stateObj.getCurrentStart() + positionFromStart <= stateObj.getCurrentEnd()){
			String text = view.getTextRange(stateObj.getCurrentStart(), positionFromStart);
			count = text.length() - text.replaceAll("\n", "").length();
			positionFromStart -= count;
			positionFromStart -= checkPageRange(stateObj.getCurrentStart() + positionFromStart);
			cursorOffset = count;
		}
		else if(positionFromStart > 0 && stateObj.getCurrentStart() + positionFromStart > stateObj.getCurrentEnd()){
			String text = view.getTextRange(stateObj.getCurrentStart(), positionFromStart);
			count = text.length() - text.replaceAll("\n", "").length();
			cursorOffset = (stateObj.getCurrentStart() + positionFromStart) - stateObj.getCurrentEnd();
			positionFromStart = 99999;
		}	
		else {
			positionFromStart -= count;
			cursorOffset = count;
		}
	}
	
	private void setCursorPosition(Message message){
		int offset = (Integer)message.getValue("offset");
		if(message.contains("element")){
			Element e = getBrlNode((Node)message.getValue("element"));
			int pos;
			if(e != null){
				int [] arr = getIndexArray(e);
				if(arr == null){
					if((Integer)message.getValue("lastPosition") == 0)
						pos = stateObj.getCurrentStart();
					else
						pos = stateObj.getCurrentEnd();
				}
				else {
					if((Integer)message.getValue("lastPosition") < 0 && stateObj.getCurrentStart() > 0)
						pos = stateObj.getCurrentStart() + (Integer)message.getValue("lastPosition");	
					else if((Integer)message.getValue("lastPosition") == 99999)
						pos = stateObj.getCurrentEnd() + offset;
					else {
						pos = stateObj.getCurrentStart() + findCurrentPosition(arr, (Integer)message.getValue("lastPosition")) + offset;
						pos += checkPageRange(pos);
					}
				}
				view.setCaretOffset(pos);
			}
			else {
				view.setCaretOffset(stateObj.getCurrentStart());
			}
		}
	}
	
	private int checkPageRange(int position){
		int offset = 0;
		for(int i = 0; i < this.pageRanges.size(); i++){
			if(position + offset > this.pageRanges.get(i).start){
				offset += this.pageRanges.get(i).end - this.pageRanges.get(i).start;
			}
		}
			
		return offset;	
	}
	
	private int findCurrentPosition(int [] indexes, int textPos){
		for(int i = 0; i < indexes.length; i++){
			if(textPos == indexes[i])
				return i;
			else if(textPos < indexes[i])
				return i - 1;
		}
		
		return indexes.length;
	}
	
	public void setWords(int words){
		this.words = words;
	}

	@Override
	public void resetView(SashForm sashform) {
		setListenerLock(true);
		recreateView(sashform);
		total = 0;
		spaceBeforeText = 0;
		spaceAfterText = 0;
		stateObj.setOldCursorPosition(-1);
		setListenerLock(false);
	}
	
	public void insert(TextMapElement t, Node n, int pos){
		Styles style = stylesTable.makeStylesElement(t.parentElement(), t.n);
		int margin = 0;
		int originalPosition = view.getCaretOffset();
		Element parent = (Element)n.getParent();
		int start = pos;
		int index = parent.indexOf(n);
		
		setListenerLock(true);
		view.setCaretOffset(pos);
		if(index > 0 && isElement(parent.getChild(index - 1)) && ((Element)parent.getChild(index - 1)).getLocalName().equals("newline") && t.brailleList.size() > 0){
			view.insert("\n");
			start++;
			view.setCaretOffset(pos + 1);
		}
		view.insert(n.getValue());
		t.brailleList.add(new BrailleMapElement(start, start + n.getValue().length(), n));
		
		//reset margin in case it is not applied
		if(t.brailleList.getLast().start == view.getOffsetAtLine(view.getLineAtOffset(t.brailleList.getLast().start)))
			handleLineWrap(t.brailleList.getLast().start, n.getValue(), 0, false);
				
		if(style.contains(StylesType.leftMargin)) {
			margin = Integer.valueOf((String)style.get(StylesType.leftMargin));
			handleLineWrap(t.brailleList.getLast().start, n.getValue(), margin, false);
		}
					
		if(isFirst(n) && style.contains(StylesType.firstLineIndent))
			setFirstLineIndent(t.brailleList.getFirst().start, style);
		
		if(style.contains(StylesType.format))
			setAlignment(start,start + n.getValue().length(),style);
		
		view.setCaretOffset(originalPosition);
		setListenerLock(false);
	}
	
	public void resetElement(Message m, MapList list, TextMapElement t, BrailleMapElement b, int pos){
		Styles style = stylesTable.makeStylesElement(t.parentElement(), t.n);
		boolean isFirst = t instanceof PageMapElement || t instanceof BrlOnlyMapElement || isFirst(b.n); 
		boolean isLast =  t instanceof PageMapElement || t instanceof BrlOnlyMapElement || isLast(b.n);
		int margin = 0;
		int lineBreaks = 0;
		int originalPosition = view.getCaretOffset();
		int start = pos;
		Element parent = (Element)b.n.getParent();
		int index = parent.indexOf(b.n);
		setListenerLock(true);
		view.setCaretOffset(pos);
		
		//checks for newline element before text node if not at the beginning of a block element
		if(!(t instanceof BrlOnlyMapElement || t instanceof PageMapElement) && t.brailleList.indexOf(b) > 0 && index > 0 && isElement(parent.getChild(index - 1)) && ((Element)parent.getChild(index - 1)).getLocalName().equals("newline")){
			view.insert("\n" + b.n.getValue());
			lineBreaks++;
			view.setCaretOffset(pos + 1);
		}
		else
			view.insert(b.n.getValue());
		
		WhiteSpaceManager wsp = new WhiteSpaceManager(manager, this, list);
		int linesBefore = 0;
		if(isFirst)
			linesBefore = wsp.setLinesBeforeBraille(t, b, lineBreaks + start, style);
		
		int linesAfter = 0;
		if(isLast)
			linesAfter = wsp.setLinesAfterBraille(t, b, lineBreaks + start + b.n.getValue().length() + linesBefore, style);
		
		m.put("brailleLength", b.n.getValue().length() + linesBefore + linesAfter + lineBreaks);
		m.put("brailleOffset", start + b.n.getValue().length() + linesBefore + linesAfter + lineBreaks);
		b.setOffsets(lineBreaks + linesBefore + start, lineBreaks + start + b.n.getValue().length() + linesBefore);
		
		//reset margin in case it is not applied
		if(t.brailleList.getLast().start == view.getOffsetAtLine(view.getLineAtOffset(t.brailleList.getLast().start)))
			handleLineWrap(t.brailleList.getLast().start, b.n.getValue(), 0, false);
				
		if(style.contains(StylesType.leftMargin)) {
			margin = Integer.valueOf((String)style.get(StylesType.leftMargin));
			handleLineWrap(t.brailleList.getLast().start, b.n.getValue(), margin, false);
		}
					
		if(isFirst && style.contains(StylesType.firstLineIndent))
			setFirstLineIndent(t.brailleList.getFirst().start, style);
		
		if(style.contains(StylesType.format))
			setAlignment(start + linesBefore,start + b.n.getValue().length(),style);
		
		view.setCaretOffset(originalPosition);
		setListenerLock(false);
	}
	
	public void mergeElement(Message m, MapList list, TextMapElement t, BrailleMapElement b, int pos){
		Styles style = stylesTable.makeStylesElement(t.parentElement(), t.n);
		boolean isFirst = t instanceof PageMapElement || t instanceof BrlOnlyMapElement || isFirst(b.n); 
		boolean isLast =  t instanceof PageMapElement || t instanceof BrlOnlyMapElement || isLast(b.n);
		int margin = 0;
		int lineBreaks = 0;
		int originalPosition = view.getCaretOffset();
		int start = pos;
		Element parent = (Element)b.n.getParent();
		int index = parent.indexOf(b.n);
		setListenerLock(true);
		view.setCaretOffset(pos);
		
		//checks for newline element before text node if not at the beginning of a block element
		if(!(t instanceof BrlOnlyMapElement || t instanceof PageMapElement) && t.brailleList.indexOf(b) > 0 && index > 0 && isElement(parent.getChild(index - 1)) && ((Element)parent.getChild(index - 1)).getLocalName().equals("newline")){
			view.insert("\n" + b.n.getValue());
			lineBreaks++;
			view.setCaretOffset(pos + 1);
		}
		else
			view.insert(b.n.getValue());
		
		b.setOffsets(lineBreaks + start, lineBreaks + start + b.n.getValue().length());
		list.shiftOffsetsFromIndex(list.indexOf(t) + 1, 0, b.n.getValue().length() + lineBreaks);
		
		WhiteSpaceManager wsp = new WhiteSpaceManager(manager, this, list);
		int linesBefore = 0;
		if(isFirst)
			linesBefore = wsp.setLinesBeforeBraille(t, b, lineBreaks + start, style);
		
		int linesAfter = 0;
		if(isLast)
			linesAfter = wsp.setLinesAfterBraille(t, b, lineBreaks + start + b.n.getValue().length() + linesBefore, style);
		
		
		b.setOffsets(lineBreaks + linesBefore + start, lineBreaks + start + b.n.getValue().length() + linesBefore);
		m.put("brailleLength", linesBefore + linesAfter);
		m.put("brailleOffset", start + b.n.getValue().length() + linesBefore + linesAfter + lineBreaks);
		
		//reset margin in case it is not applied
		if(t.brailleList.getLast().start == view.getOffsetAtLine(view.getLineAtOffset(t.brailleList.getLast().start)))
			handleLineWrap(t.brailleList.getLast().start, b.n.getValue(), 0, false);
				
		if(style.contains(StylesType.leftMargin)) {
			margin = Integer.valueOf((String)style.get(StylesType.leftMargin));
			handleLineWrap(t.brailleList.getLast().start, b.n.getValue(), margin, false);
		}
					
		if(isFirst && style.contains(StylesType.firstLineIndent))
			setFirstLineIndent(t.brailleList.getFirst().start, style);
		
		if(style.contains(StylesType.format))
			setAlignment(start + linesBefore,start + b.n.getValue().length(),style);
		
		view.setCaretOffset(originalPosition);
		setListenerLock(false);
	}

	
	public void resetSelectionElement(Message m, MapList list, TextMapElement t, BrailleMapElement b, int pos, boolean format){
		Styles style = stylesTable.makeStylesElement(t.parentElement(), t.n);
		boolean isFirst = t instanceof PageMapElement || t instanceof BrlOnlyMapElement || isFirst(b.n); 
		boolean isLast = t instanceof PageMapElement || t instanceof BrlOnlyMapElement || isLast(b.n); 
		int margin = 0;
		int lineBreaks = 0;
		int originalPosition = view.getCaretOffset();
		int start = pos;
		Element parent = (Element)b.n.getParent();
		int index = parent.indexOf(b.n);
		setListenerLock(true);
		view.setCaretOffset(pos);		
		
		if(t instanceof BrlOnlyMapElement || t instanceof PageMapElement){
			index = list.indexOf(t);
			if(index > 0 && list.get(index - 1).brailleList.getLast().end == pos){
				if(index < list.size() - 1 && list.get(index + 1).brailleList.getFirst().start > start){
					view.insert("\n" + b.n.getValue());
					lineBreaks++;
					view.setCaretOffset(pos + 1);
			
					b.setOffsets(1 + start, 1 + start + b.n.getValue().length());
					list.shiftOffsetsFromIndex(list.indexOf(t) + 1, 0, b.n.getValue().length());
					m.put("brailleLength", 1);
					m.put("brailleOffset", start + b.n.getValue().length() + 1);
				}
				else {
						view.insert("\n" + b.n.getValue() + "\n");
						lineBreaks++;
						view.setCaretOffset(pos + 1);
			
						b.setOffsets(1 + start, 1 + start + b.n.getValue().length());
						list.shiftOffsetsFromIndex(list.indexOf(t) + 1, 0, b.n.getValue().length());
						m.put("brailleLength", 2);
						m.put("brailleOffset", start + b.n.getValue().length() + 2);
				}
			}
			else {
				if((index < list.size() - 1 && list.get(index + 1).brailleList.getFirst().start > start) || index == list.size() - 1){
					view.insert(b.n.getValue());
					view.setCaretOffset(pos + 1);
			
					b.setOffsets(start, start + b.n.getValue().length());
					list.shiftOffsetsFromIndex(list.indexOf(t) + 1, 0, b.n.getValue().length());
					m.put("brailleLength", 0);
					m.put("brailleOffset", start + b.n.getValue().length());
				}
				else {
					view.insert(b.n.getValue() + "\n");
					view.setCaretOffset(pos + 1);
			
					b.setOffsets(start, start + b.n.getValue().length());
					list.shiftOffsetsFromIndex(list.indexOf(t) + 1, 0, b.n.getValue().length());
					m.put("brailleLength", 1);
					m.put("brailleOffset", start + b.n.getValue().length() + 1);
				}
			}
			view.setLineIndent(view.getLineAtOffset(t.brailleList.getFirst().start), 1, 0);
		}
		else {
			//checks for newline element before text node if not at the beginning of a block element
			if(!(t instanceof BrlOnlyMapElement || t instanceof PageMapElement) && t.brailleList.indexOf(b) > 0 && index > 0 && isElement(parent.getChild(index - 1)) && ((Element)parent.getChild(index - 1)).getLocalName().equals("newline")){
				view.insert("\n" + b.n.getValue());
				lineBreaks++;
				view.setCaretOffset(pos + 1);
			}
			else 
				view.insert(b.n.getValue());
		
				b.setOffsets(lineBreaks + start, lineBreaks + start + b.n.getValue().length());
				list.shiftOffsetsFromIndex(list.indexOf(t) + 1, 0, b.n.getValue().length() + lineBreaks);
		
				if(format){
					WhiteSpaceManager wsp = new WhiteSpaceManager(manager, this, list);
					int linesBefore = 0;
					int listIndex = list.indexOf(t);
					if(isFirst){
						linesBefore = wsp.setLinesBeforeBraille(t, b, lineBreaks + start, style);
						list.shiftOffsetsFromIndex(listIndex, 0, linesBefore);
					}
					
					int linesAfter = 0;
					if(isLast) {
						linesAfter = wsp.setLinesAfterBraille(t, b, lineBreaks + start + b.n.getValue().length() + linesBefore, style);
						list.shiftOffsetsFromIndex(listIndex + 1, 0, linesAfter);
					}
					
				//	b.setOffsets(lineBreaks + linesBefore + start, lineBreaks + start + b.n.getValue().length() + linesBefore);
					m.put("brailleLength", 0);
					m.put("brailleOffset", start + b.n.getValue().length() + linesBefore + linesAfter + lineBreaks);
					start += linesBefore;
				}
				else {
					b.setOffsets(lineBreaks + start, lineBreaks + start + b.n.getValue().length());
					m.put("brailleLength", 0);
					m.put("brailleOffset", start + b.n.getValue().length() + lineBreaks);
				}
		
				//reset margin in case it is not applied
				if(t.brailleList.getLast().start == view.getOffsetAtLine(view.getLineAtOffset(t.brailleList.getLast().start)))
					handleLineWrap(t.brailleList.getLast().start, b.n.getValue(), 0, false);
				
				if(style.contains(StylesType.leftMargin)) {
					margin = Integer.valueOf((String)style.get(StylesType.leftMargin));
					handleLineWrap(t.brailleList.getLast().start, b.n.getValue(), margin, false);
				}
					
				if(isFirst && style.contains(StylesType.firstLineIndent))
					setFirstLineIndent(t.brailleList.getFirst().start, style);
		
				if(style.contains(StylesType.format))
					setAlignment(start,start + b.n.getValue().length(),style);
		
				view.setCaretOffset(originalPosition);
		}
		setListenerLock(false);
	}

	@Override
	public void addPageNumber(PageMapElement p, boolean insert) {
		String text = p.brailleList.getFirst().value();
		
		spaceBeforeText++;
		if(insert){
			view.insert("\n" + text);
			view.setCaretOffset(spaceBeforeText + text.length() + total);
		}
		else
			view.append("\n" + text);
		
		p.setBrailleOffsets(spaceBeforeText + total, spaceBeforeText + total + text.length());
		total += spaceBeforeText + text.length();
		spaceBeforeText = 0;
	}

	public void setBRLOnlyBraille(BrlOnlyMapElement b,boolean insert) {
		String brailleSidebar=b.n.getValue();
		spaceBeforeText++;
		if(insert){
		    view.insert("\n"+brailleSidebar);
		    view.setCaretOffset(spaceBeforeText+brailleSidebar.length()+total);
		}
		else{
			view.append("\n"+brailleSidebar);
		}
		b.setBrailleOffsets(spaceBeforeText+total, spaceBeforeText+total+brailleSidebar.length());
		total += spaceBeforeText+brailleSidebar.length();
		spaceBeforeText = 0;
	}
	
	
	/**
	 * Resets the braille page indicator. Helpful for when 
	 * we're refreshing.
	 */
	public void resetIndicator() {
		 flag = true;
	}
	
	/**
	 * Add indicator at input line number
	 * Indicator is a bullet and add all indicator at indication array list
	 */
	public void addIndicator(){
//		int lineNumber=manager.getDocument().getIndicatorLocation();
//		if (lineNumber<view.getLineCount()){
//			
//			if (flag==true)
//			{
//			   counter=lineNumber - 1;
//			   flag=false;
//			}
//			System.out.println(counter + " - out while... " + view.getLineCount() + " - out while.");
//			while(counter < view.getLineCount()){
//				if (view.getLineBullet(counter)==null){
//					StyleRange indicatorStyle = new StyleRange();
//					indicatorStyle.underline=true;
//					indicatorStyle.underlineStyle=SWT.UNDERLINE_SINGLE;	
//					indicatorStyle.metrics = new GlyphMetrics(5, 0, 50);
//					indicatorStyle.foreground = view.getDisplay().getSystemColor(SWT.COLOR_BLACK);
//					Bullet bullet = new Bullet (ST.BULLET_TEXT, indicatorStyle);
//					bullet.text = "                                                 ";
//					view.setLineBullet(counter, 1, null);
//					view.setLineBullet(counter, 1, bullet);
//					indications.add(bullet);
//				}
//				counter=counter+lineNumber;
//				System.out.println(counter + " - in while... " + view.getLineCount() + " - in while.");
//			}
//	   }
		// Only do once.
		int linesPerPage = manager.getDocument().getLinesPerPage();
		counter = linesPerPage - 1;
		if(indications.isEmpty() == false)
			indications.clear();
		while( counter < view.getLineCount() ) {
			
			// Make sure the current line isn't wrapping to the next line. This can happen 
			// if the user's screen resolution is too small, or if the application is 
			// not fullscreen.
			
			String curLine = view.getLine(counter);
			Rectangle viewClientArea = view.getClientArea();
			int charWidth = manager.getBraille().getFontWidth();
			int stringWidth = charWidth * curLine.length();
			int indentAmt = 13;
			viewClientArea.width += indentAmt;
			if( viewClientArea.width < stringWidth )
				counter++;
		    
			StyleRange indicatorStyle = new StyleRange();
			indicatorStyle.underline=true;
			indicatorStyle.underlineStyle=SWT.UNDERLINE_SINGLE;
			indicatorStyle.metrics = new GlyphMetrics(0, 0, 0);
			indicatorStyle.foreground = view.getDisplay().getSystemColor(SWT.COLOR_BLACK);
			Bullet bullet = new Bullet (ST.BULLET_TEXT, indicatorStyle);
			bullet.text = "                                                 ";
			view.setLineBullet(counter, 1, null);
			view.setLineBullet(counter, 1, bullet);
			indications.add(bullet);
			
			counter = counter + linesPerPage;
		} // while()
	}
	
	/**
	 * Remove indicator at all lines except input given line
	 */
	private void removeIndicator(){
		int lineNumber;
		if(stateObj.getCurrentStart() > view.getCharCount())
			lineNumber = view.getLineAtOffset(view.getCharCount());
		else
			lineNumber = view.getLineAtOffset(stateObj.getCurrentStart());
		
		for (int i = lineNumber; i < view.getLineCount(); i++) {
			// Check to find bullet which are in indication array list
			if (indications.contains(view.getLineBullet(i))) {	
				indications.remove(view.getLineBullet(i));
				view.setLineBullet(i, 1, null);
			}
		}
	}
}