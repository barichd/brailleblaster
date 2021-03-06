package org.brailleblaster.perspectives.imageDescriber.UIComponents;

import org.brailleblaster.BBIni;
import org.brailleblaster.perspectives.Controller;
import org.brailleblaster.perspectives.imageDescriber.ImageDescriberController;
import org.brailleblaster.perspectives.imageDescriber.views.ImageDescriberView;
import org.brailleblaster.wordprocessor.BBFileDialog;
import org.brailleblaster.wordprocessor.BBMenu;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ImageDescriberMenu extends BBMenu {
	private final int MENU_INDEX = 1;
	ImageDescriberController currentController;
	ImageDescriberView idv;
	
	//file menu
	MenuItem openItem, saveItem, saveAsItem, closeItem;

	//editMenu
	Menu editMenu;
	MenuItem editItem, prevItem, nextItem, applyItem, undoItem, applyToAllItem, clearAllItem;
	
	// Menu items for font sizing of our widgets.
	MenuItem fontSizeItem;
	Menu fontSizeMenu;
	MenuItem fontButtonItem;
	Menu btnFntSizeMenu;
	MenuItem fontBtnSize[] = new MenuItem[10];
	//////////////
	MenuItem fontEditBoxItem;
	Menu editBoxSizeMenu;
	MenuItem fontEditBoxSize[] = new MenuItem[10];
	
	public ImageDescriberMenu(final WPManager wp, ImageDescriberController idc) {
		super(wp);
		setPerspectiveMenuItem(MENU_INDEX);
		currentController = idc;
		idv = idc.getImageDescriberView();
		
		openItem = new MenuItem(fileMenu, SWT.PUSH, 0);
		openItem.setText(lh.localValue("&Open") + "\t" + lh.localValue("Ctrl + O"));
		openItem.setAccelerator(SWT.MOD1 + 'O');
		openItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				String filePath = fileOpenDialog();
				int index = wp.getFolder().getSelectionIndex();
				if(index == -1 && filePath != null){
					wp.addDocumentManager(filePath);
				}
				else if(filePath != null){
					if(currentController.canReuseTab())
						currentController.reuseTab(filePath);
					else
						wp.addDocumentManager(filePath);
				}
			}		
		});
		
		saveItem = new MenuItem(fileMenu, SWT.PUSH, 2);
		saveItem.setText(lh.localValue("&Save") + "\t" + lh.localValue("Ctrl + S"));
		saveItem.setAccelerator(SWT.MOD1 + 'S');
		saveItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				if(wp.getFolder().getItemCount() != 0)
					currentController.save();
			}
		});
		
		saveAsItem = new MenuItem(fileMenu, SWT.PUSH, 3);
		saveAsItem.setText(lh.localValue("Save&As") + "\t" + lh.localValue("F12"));
		saveAsItem.setAccelerator(SWT.F12);
		saveAsItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				if(wp.getFolder().getItemCount() != 0)
					currentController.saveAs();
			}	
		});
		
		closeItem = new MenuItem(fileMenu, SWT.PUSH, 5);
		closeItem.setText(lh.localValue("&Close") + "\t" + lh.localValue("Ctrl + W"));
		closeItem.setAccelerator(SWT.MOD1 + 'W');
		closeItem.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void widgetSelected(SelectionEvent e) {	
				int count = wp.getFolder().getItemCount();				
				
				if(count > 0)
					currentController.close();
	
				if(wp.getList().size() == 0)
					setCurrent(null);
			}
		});
		
		editItem = new MenuItem(menuBar, SWT.CASCADE, 1);
		editItem.setText(lh.localValue("&Edit"));
		
		editMenu = new Menu(wp.getShell(), SWT.DROP_DOWN);
		prevItem = new MenuItem(editMenu, SWT.PUSH);
		prevItem.setText(lh.localValue("Pre&vious"));
		prevItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentController.setImageToPrevious();
			} // widgetSelected()
		});
		
		nextItem = new MenuItem(editMenu, SWT.PUSH);
		nextItem.setText(lh.localValue("&Next"));
		nextItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentController.setImageToNext();
			} // widgetSelected()

		}); // nextBtn.addSelectionListener...)
		
		undoItem = new MenuItem(editMenu, SWT.PUSH);
		undoItem.setText(lh.localValue("&Undo"));
		undoItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Undo current element changes.
				currentController.undo();
			} // widgetSelected()

		}); // undoAllItem.addSelectionListener...
		
		applyToAllItem = new MenuItem(editMenu, SWT.PUSH);
		applyToAllItem.setText(lh.localValue("A&pply to All"));
		applyToAllItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentController.applyToAll();
			} // widgetSelected()
			
		}); // applyToAll.addSelectionListener...
		
		clearAllItem = new MenuItem(editMenu, SWT.PUSH);
		clearAllItem.setText(lh.localValue("&Clear All"));
		clearAllItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentController.clearAll();
			} // widgetSelected()

		}); // clearAllBtn.addSelectionListener
		
		editItem.setMenu(editMenu);
		
		//////////////
		// Font Sizes.
			
			// Item for view menu option in BBMenu.
			fontSizeItem = new MenuItem(this.viewMenu, SWT.CASCADE);
			fontSizeItem.setText(lh.localValue("Font Sizes"));
			// Attach a new dropdown menu to our font size item.
			fontSizeMenu = new Menu(wordProc.getShell(), SWT.DROP_DOWN);
			fontSizeItem.setMenu(fontSizeMenu);
			
			/////////////////////
			// Button Font Sizes.
			
				// Add button dropdown to font size dropdown menu.
				fontButtonItem = new MenuItem(fontSizeMenu, SWT.CASCADE);
				fontButtonItem.setText(lh.localValue("Button Font Size"));
				btnFntSizeMenu = new Menu(wordProc.getShell(), SWT.DROP_DOWN);
				fontButtonItem.setMenu(btnFntSizeMenu);
				
					// Check box for button size AUTO.
					fontBtnSize[0] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[0].setText(lh.localValue("Auto"));
					fontBtnSize[0].setData(ImageDescriberController.class);
					fontBtnSize[0].setSelection(true);
					fontBtnSize[0].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_AUTO);
							_setBtnFntCheck(0, true);
						}
					});
				
					// Check box for button size 12.
					fontBtnSize[1] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[1].setText(lh.localValue("12"));
					fontBtnSize[1].setData(ImageDescriberController.class);
					fontBtnSize[1].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_12);
							_setBtnFntCheck(1, true);
						}
					});
					
					// Check box for button size 11.
					fontBtnSize[2] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[2].setText(lh.localValue("11"));
					fontBtnSize[2].setData(ImageDescriberController.class);
					fontBtnSize[2].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_11);
							_setBtnFntCheck(2, true);
						}
					});
					
					// Check box for button size 10.
					fontBtnSize[3] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[3].setText(lh.localValue("10"));
					fontBtnSize[3].setData(ImageDescriberController.class);
					fontBtnSize[3].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_10);
							_setBtnFntCheck(3, true);
						}
					});
					
					// Check box for button size 9.
					fontBtnSize[4] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[4].setText(lh.localValue("9"));
					fontBtnSize[4].setData(ImageDescriberController.class);
					fontBtnSize[4].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_09);
							_setBtnFntCheck(4, true);
						}
					});
					
					// Check box for button size 8.
					fontBtnSize[5] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[5].setText(lh.localValue("8"));
					fontBtnSize[5].setData(ImageDescriberController.class);
					fontBtnSize[5].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_08);
							_setBtnFntCheck(5, true);
						}
					});
					
					// Check box for button size 7.
					fontBtnSize[6] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[6].setText(lh.localValue("7"));
					fontBtnSize[6].setData(ImageDescriberController.class);
					fontBtnSize[6].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_07);
							_setBtnFntCheck(6, true);
						}
					});
					
					// Check box for button size 6.
					fontBtnSize[7] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[7].setText(lh.localValue("6"));
					fontBtnSize[7].setData(ImageDescriberController.class);
					fontBtnSize[7].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_06);
							_setBtnFntCheck(7, true);
						}
					});
					
					// Check box for button size 5.
					fontBtnSize[8] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[8].setText(lh.localValue("5"));
					fontBtnSize[8].setData(ImageDescriberController.class);
					fontBtnSize[8].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_05);
							_setBtnFntCheck(8, true);
						}
					});
					
					// Check box for button size 4.
					fontBtnSize[9] = new MenuItem(btnFntSizeMenu, SWT.CHECK);
					fontBtnSize[9].setText(lh.localValue("4"));
					fontBtnSize[9].setData(ImageDescriberController.class);
					fontBtnSize[9].addSelectionListener(new SelectionAdapter(){
						@Override
						public void widgetSelected(SelectionEvent e) {
							idv.setButtonsFont(idv.IDV_FONTSIZE_04);
							_setBtnFntCheck(9, true);
						}
					});

				// Button Font Sizes.
				/////////////////////
					
				///////////////////////
				// Edit Box Font Sizes.
					
					// Add dropdown to font size dropdown menu.
					fontEditBoxItem = new MenuItem(fontSizeMenu, SWT.CASCADE);
					fontEditBoxItem.setText(lh.localValue("Edit Box Font Size"));
					editBoxSizeMenu = new Menu(wordProc.getShell(), SWT.DROP_DOWN);
					fontEditBoxItem.setMenu(editBoxSizeMenu);
					
						fontEditBoxSize[0] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[0].setText(lh.localValue("Auto"));
						fontEditBoxSize[0].setSelection(true);
						fontEditBoxSize[0].setData(ImageDescriberController.class);
						fontEditBoxSize[0].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_AUTO);
								_setEboxFntCheck(0, true);
							}
						});
						
						fontEditBoxSize[1] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[1].setText(lh.localValue("12"));
						fontEditBoxSize[1].setData(ImageDescriberController.class);
						fontEditBoxSize[1].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_12);
								_setEboxFntCheck(1, true);
							}
						});
						
						fontEditBoxSize[2] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[2].setText(lh.localValue("11"));
						fontEditBoxSize[2].setData(ImageDescriberController.class);
						fontEditBoxSize[2].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_11);
								_setEboxFntCheck(2, true);
							}
						});
						
						fontEditBoxSize[3] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[3].setText(lh.localValue("10"));
						fontEditBoxSize[3].setData(ImageDescriberController.class);
						fontEditBoxSize[3].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_10);
								_setEboxFntCheck(3, true);
							}
						});
						
						fontEditBoxSize[4] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[4].setText(lh.localValue("9"));
						fontEditBoxSize[4].setData(ImageDescriberController.class);
						fontEditBoxSize[4].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_09);
								_setEboxFntCheck(4, true);
							}
						});
						
						fontEditBoxSize[5] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[5].setText(lh.localValue("8"));
						fontEditBoxSize[5].setData(ImageDescriberController.class);
						fontEditBoxSize[5].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_08);
								_setEboxFntCheck(5, true);
							}
						});
						
						fontEditBoxSize[6] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[6].setText(lh.localValue("7"));
						fontEditBoxSize[6].setData(ImageDescriberController.class);
						fontEditBoxSize[6].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_07);
								_setEboxFntCheck(6, true);
							}
						});
						
						fontEditBoxSize[7] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[7].setText(lh.localValue("6"));
						fontEditBoxSize[7].setData(ImageDescriberController.class);
						fontEditBoxSize[7].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_06);
								_setEboxFntCheck(7, true);
							}
						});
						
						fontEditBoxSize[8] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[8].setText(lh.localValue("5"));
						fontEditBoxSize[8].setData(ImageDescriberController.class);
						fontEditBoxSize[8].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_05);
								_setEboxFntCheck(8, true);
							}
						});
						
						fontEditBoxSize[9] = new MenuItem(editBoxSizeMenu, SWT.CHECK);
						fontEditBoxSize[9].setText(lh.localValue("4"));
						fontEditBoxSize[9].setData(ImageDescriberController.class);
						fontEditBoxSize[9].addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								idv.setEditBoxesFont(idv.IDV_FONTSIZE_04);
								_setEboxFntCheck(9, true);
							}
						});

				// Edit Box Font Sizes.
				///////////////////////
					
		// Font Sizes.
		//////////////
	}

	// Buttons.
	// Sets all font-size checkboxes to true/false/on/off, then flips the 
	// checkbox at given index to the given bool.
	private void _setBtnFntCheck(int checkIndex, boolean onOrOff)
	{
		// Turn them all on or off.
		for(int curBtn = 0; curBtn < fontBtnSize.length; curBtn++)
			fontBtnSize[curBtn].setSelection(!onOrOff);
		
		// Flip the one we want.
		fontBtnSize[checkIndex].setSelection(onOrOff);
		
	} // _setBtnFntCheck()
	
	// Edit boxes.
	// Sets all font-size checkboxes to true/false/on/off, then flips the 
	// checkbox at given index to the given bool.
	private void _setEboxFntCheck(int checkIndex, boolean onOrOff)
	{
		// Turn them all on or off.
		for(int curB = 0; curB < fontEditBoxSize.length; curB++)
			fontEditBoxSize[curB].setSelection(!onOrOff);
		
		// Flip the one we want.
		fontEditBoxSize[checkIndex].setSelection(onOrOff);
		
	} // _setEboxFntCheck()
	
	protected String fileOpenDialog(){
		String tempName = null;

		if(!BBIni.debugging()){
			String[] filterNames = new String[] { "XML", "XML ZIP", "EPUB", "XHTML", "HTML","HTM","UTDML working document"};
			String[] filterExtensions = new String[] { "*.xml", "*.zip", "*.epub", "*.xhtml","*.html", "*.htm", "*.utd"};
			BBFileDialog dialog = new BBFileDialog(wordProc.getShell(), SWT.OPEN, filterNames, filterExtensions);
			tempName = dialog.open();
		}
		else
			tempName = BBIni.getDebugFilePath();
		
		return tempName;
	}
	
	@Override
	public void setCurrent(Controller controller) {
		this.currentController = (ImageDescriberController)controller;
	}

	@Override
	public Controller getCurrent() {
		return currentController;
	}
}
