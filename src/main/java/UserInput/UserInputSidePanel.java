/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package UserInput;

/**
 * 
 * @author David McLaren
 */

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import PamUtils.PamCalendar;
import PamView.PamBorderPanel;
import PamView.PamSidePanel;
import PamguardMVC.PamDataBlock;


public class UserInputSidePanel implements PamSidePanel{

	UserInputController userInputController;
	SidePanel sidePanel;
	TitledBorder titledBorder;


	public UserInputSidePanel(UserInputController userInputController){
		this.userInputController = userInputController;
		sidePanel = new SidePanel();
	}

	public JComponent getPanel() {
		// TODO Auto-generated method stub
		return sidePanel;
	}

	public void rename(String newName) {
		// TODO Auto-generated method stub

	}

	private class SidePanel extends PamBorderPanel {


		PamDataBlock<UserInputDataUnit> uiDataBlock;
		PamDataBlock userEntryDataBlock;
		UserInputLogger userInputLogger;
		private JTextField textInputField;
		TitledBorder titledBorder;

		public SidePanel(){ 
			super();
			setBorder(titledBorder = new TitledBorder(userInputController.getUnitName()));
			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			textInputField = new JTextField(10);
			textInputField.addKeyListener(new TextListener());
			textInputField.setFocusable(true);
			this.add(textInputField);
		}


		class TextListener implements KeyListener {

			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_ENTER){
					submitText();			
				}
			}

			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_ENTER){
					textInputField.setText(null);
					textInputField.setCaretPosition(0);
				}

			}

			public void keyTyped(KeyEvent e) {
			}

		}

		private void submitText(){

			long timeMS = PamCalendar.getTimeInMillis();
			String dateStr = PamCalendar.formatDateTime(timeMS);

			// set data in user input process/model
			// may need to split the data up into several datablocks to fit in into 
			// the dataabse - max length for a line is UserInpuController.maxCommentLength
			String text = textInputField.getText();
			
			userInputController.getUserInputPanel().appendStringToOutputField(text);
			
			
			String subText;
			int lastSpace, nextSpace;
			while (text.length() > 0) {
				if (text.length() < UserInputController.maxCommentLength) {
					createDataUnit(timeMS, text);
					break;
				}
				// find the last space before the maximum character length
				lastSpace = 0;
				nextSpace = 0;
				int startChar;
				while (true) {
					nextSpace = text.indexOf(' ', lastSpace+1);
					if (nextSpace > 0 && nextSpace < UserInputController.maxCommentLength) {
						lastSpace = nextSpace;
						continue;
					}
					if (lastSpace <= 0){
						lastSpace = Math.min(UserInputController.maxCommentLength, text.length());
					}
					startChar = 0;
					while(text.charAt(startChar) == ' ') {
						startChar++;
					}
					subText = text.substring(startChar, lastSpace);
					createDataUnit(timeMS, subText);
					text = text.substring(lastSpace);	
					text = text.trim();
					break;
				}
			}


		}

		private void createDataUnit(long timeMS, String data) {
			UserInputDataUnit nd = new UserInputDataUnit(timeMS, data);
			userInputController.getUserInputProcess().uiDataBlock.addPamData(nd);
		}



	}


}
