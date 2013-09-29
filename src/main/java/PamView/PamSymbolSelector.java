package PamView;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Component for including in dialogs. The 
 * PmSymbolSelector has a small square window in which 
 * the current selected symbol is displayed and a button
 * you can press to change it. 
 * 
 * @author Douglas Gillespie
 *
 */
public class PamSymbolSelector extends JPanel {

	private JButton changeButton;
	
	private SymbolPanel symbolPanel;
	
	private PamSymbol currentSymbol;
	
	public PamSymbolSelector(Frame guiFrame) {
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, symbolPanel = new SymbolPanel());
		add(BorderLayout.EAST, changeButton = new JButton("Change"));
		changeButton.addActionListener(new ChangeAction(guiFrame));
	}
	
	public void setSymbol() {
		symbolPanel.repaint();
	}

	public PamSymbol getCurrentSymbol() {
		return currentSymbol;
	}

	public void setCurrentSymbol(PamSymbol currentSymbol) {
		this.currentSymbol = currentSymbol;
		setSymbol();
	}
	
	class SymbolPanel extends JPanel {

		public SymbolPanel() {
			super();
			setPreferredSize(new Dimension(20,20));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			if (currentSymbol == null) {
				return;
			}
			Point pt = new Point(getWidth()/2, getHeight()/2);
			currentSymbol.draw(g, pt);
		}
		
	}
	
	class ChangeAction implements ActionListener {
		
		private Frame guiFrame;
		

		public ChangeAction(Frame guiFrame) {
			super();
			this.guiFrame = guiFrame;
		}


		public void actionPerformed(ActionEvent e) {
			
			PamSymbol newSymbol = PamSymbolDialog.show(guiFrame, currentSymbol);
			if (newSymbol != null) {
				setCurrentSymbol(newSymbol.clone());
				
			}
		}
		
	}
	
}
