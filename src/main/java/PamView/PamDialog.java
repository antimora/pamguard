package PamView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import PamController.PamController;
import PamController.PamguardVersionInfo;

/**
 * General  functionality for PamGuard dialogs.
 * Should be subclassed.
 * @author Doug Gillespie
 *
 */
abstract public class PamDialog extends JDialog {

	public static final long serialVersionUID = 1;
	private JButton okButton, cancelButton, defaultButton, helpButton;
	private JPanel contentPane;
	private String helpPoint = null;
	private JPanel buttonPanel;
	//	private String helpTarget;
	//	static private PamDialog singleInstance;
	private boolean smruDev;
	private ClipboardCopier clipboardCopier;
	private boolean sendGeneralSettingsNotification = true;
	private String warningTitle;
	private boolean warnDefaultSetting = true;
	private CancelObserver cancelObserver;
	
	public JPanel getButtonPanel() {
		return buttonPanel;
	}

	/**
	 * Dialogs are always constructed with OK and Cancel buttons.
	 * Help and Set Defaults buttons are optional.
	 * @param parentFrame owner frame - can be null, but preferably the owner frame of the 
	 * menu that calls this dialog
	 * @param title dialog title
	 * @param hasDefault display a default button
	 */
	public PamDialog(Window parentFrame, String title, boolean hasDefault) {

		super(parentFrame == null ? 
				PamController.getInstance().getGuiFrameManager().getFrame(0) : parentFrame, title);
		
		warningTitle = title;

		smruDev = (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU);

		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());
		//		((JPanel) contentPane).setBorder(new EmptyBorder(10, 10, 10, 10));

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		okButton = new JButton("  Ok  ");
		okButton.addActionListener(new OkButtonPressed());
		buttonPanel.add(okButton);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new CancelButtonPressed());
		buttonPanel.add(cancelButton);

		helpButton = new JButton("Help ...");
		helpButton.addActionListener(new HelpButtonPressed());
		helpButton.setVisible(false);
		buttonPanel.add(helpButton);
		
		if (smruDev) {
			clipboardCopier = new ClipboardCopier(this.getRootPane());
			buttonPanel.addMouseListener(new PopupListener());
		}

		if (hasDefault) {
			defaultButton = new JButton("Set Defaults");
			defaultButton.addActionListener(new DefaultButtonPressed());
			buttonPanel.add(defaultButton);
		}
		contentPane.add(BorderLayout.SOUTH, buttonPanel);

		getRootPane().setDefaultButton(okButton);

		addWindowListener(new DialogWindowAdapter());

		setLocation(300, 200);

		this.setModal(true);

		this.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

		this.setResizable(false);
		setAlwaysOnTop(parentFrame == null);

		setIconImage(new ImageIcon(ClassLoader
				.getSystemResource("Resources/pamguardIcon.png")).getImage());
		

//		setIconImages(parentFrame.getIconImages());
		
		positionInFrame(parentFrame);
		
	}

	protected void positionInFrame(Window parentFrame) {
		if (parentFrame == null) {
			return;
		}
		Rectangle frameBounds = parentFrame.getBounds();
		setLocation(frameBounds.x + 200, frameBounds.y + 150);
	}
	

	class DialogWindowAdapter extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			// get's called when close button is pressed
			// but not when Ok button sets window
			// visibile to false.
			cancelButtonPressed();
			super.windowClosing(e);
		}		
	}

	/**
	 * Used to set the main panel containing dialog controls - 
	 * @param component - usually a jPanel
	 * @see JPanel
	 */
	public void setDialogComponent(JComponent component) {
		contentPane.add(BorderLayout.CENTER, component);
		pack();
	}

	@Override
	public void setVisible(boolean visible) {
		/*
		 * Override so that it can call the Color Manager 
		 * just before the item becomes visible since
		 * notifications may not have been sent to dialogs
		 * which are not in part of the swing container / component 
		 * tree. 
		 */
		if (visible) {
			PamColors.getInstance().notifyContianer(this.getContentPane());
		}
		
		super.setVisible(visible);
	}

	/**
	 * Reschedule closing of the window to happen
	 * on the AWT thread using SwingUtilities.invokeLater(...)
	 */
	public void closeLater() {
		SwingUtilities.invokeLater(new CloseLater());
	}
	
	/**
	 * Swing worker runnable to close the dialog. 
	 * @author Doug Gillespie
	 *
	 */
	class CloseLater implements Runnable {
		@Override
		public void run() {
			closeNow();
		}
	}
	
	/**
	 * Function to close dialog called from CloseLater swing thead. 
	 */
	private void closeNow() {
		super.setVisible(false);
	}

	//	
	//	public static Object showDialog(JFrame parentFrame) {
	//		if (singleInstance == null || singleInstance.getOwner() != parentFrame) {
	//			singleInstance = new PamDialog(null, "Pam Dialog", false);
	//		}
	//	}

	/**
	 * called when the Ok button is pressed. This must return true in
	 * order that the dialog may close. It should also copy all parameters
	 * into an object that will be returned by showDialog.
	 */
	abstract public boolean getParams();

	/**
	 * called when the cancel button is pressed before the
	 * dialog closes. Generally you should set the 
	 * parameters returned by the dialog to null or some
	 * default value, or in some other way indicate that
	 * Cancel was pressed.
	 *
	 */
	abstract public void cancelButtonPressed();

	/**
	 * only closes the dialog if getParams returns true.
	 * <p> getParams should copy all data from the dialog
	 * controls to appropriate classes and return them
	 * to the calling function. 
	 */
	class OkButtonPressed implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			okButtonPressed();
		}
	}

	protected void okButtonPressed() {

		if (getParams()) {
			setVisible(false);
		}
		PamController pamController = PamController.getInstance();
		if (pamController != null & sendGeneralSettingsNotification) {
			pamController.dialogOKButtonPressed();
		}
		
	}

	class CancelButtonPressed implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (cancelObserver != null) {
				boolean ans = cancelObserver.cancelPressed();
				if (ans == false) {
					return;
				}
			}
			cancelButtonPressed();
			setVisible(false);
		}
	}


	class HelpButtonPressed implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			//startHelp("mapHelp.hs", "mapControlPanel", "Context sensitive help frame");
			PamHelp.getInstance().displayContextSensitiveHelp(helpPoint);
		}
	}



	class DefaultButtonPressed implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			restoreDefaultSettingsQ();
		}
	}

	public String getHelpPoint() {
		return helpPoint;
	}

	/**
	 * restore default setting ? Check with user before calling restoreDefaultSettings();
	 * Thsi may be overridden if you want to ask a more complex question or offer
	 * options at this point. 
	 *
	 */
	public void restoreDefaultSettingsQ() {
		int ans = JOptionPane.YES_OPTION;
		if (warnDefaultSetting) {
			ans = JOptionPane.showConfirmDialog(getContentPane(), 
				"Are you sure you want to restore all default settings", 
				getTitle(), JOptionPane.YES_NO_OPTION);
		}
		if (ans == JOptionPane.YES_OPTION) {
			restoreDefaultSettings();
		}
	}

	/**
	 * standard function which should us used to copy default parameters into
	 * the dialog controls. 
	 *
	 */
	public abstract void restoreDefaultSettings();

	/**
	 * Sets the starting point for Java help. If this is null, then the 
	 * help button is hidden, if it is not null, then the help button
	 * is displayed. 
	 * @param helpPoint
	 */
	public void setHelpPoint(String helpPoint) {
		this.helpPoint = helpPoint;
		enableHelpButton(helpPoint != null);
	}

	public void enableHelpButton(boolean helpOn) {

		if(helpOn){
			helpButton.setVisible(true); // should really be true - removed for DEc 06 release
		}
		else{
			helpButton.setVisible(false);
		}

	}


	/**
	 * getter for cancel button so that you 
	 * can change it's name from the default value
	 * @return reference to the cancel button
	 */
	public JButton getCancelButton() {
		return cancelButton;
	}

	/**
	 * getter for ok button so that you 
	 * can change it's name from the default value
	 * @return reference to the ok button
	 */
	public JButton getOkButton() {
		return okButton;
	}

	public static void addComponent(JPanel panel, Component p, GridBagConstraints constraints){
		((GridBagLayout) panel.getLayout()).setConstraints(p, constraints);
		panel.add(p);
	}

	public JButton getDefaultButton() {
		return defaultButton;
	}

	public JButton getHelpButton() {
		return helpButton;
	}
	/**
	 * Only activated in the smruDev version to provide a quick
	 * way of copying dialog displays for help file authoring. 
	 * @author Doug Gillespie
	 *
	 */
	public class PopupListener extends MouseAdapter {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		
		private void maybeShowPopup(MouseEvent e) {
//			if (mouseDown) return; 
			if (e.isPopupTrigger()) {
				JPopupMenu menu = getPopupMenu();
				if (menu != null) {
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	}
	
	private JPopupMenu getPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		JMenuItem menuItem = clipboardCopier.getCopyMenuItem("Copy dialog image");
		menu.add(menuItem);
		return menu;
	}

	/**
	 * Tell the Pamguard Controller to send a notification message around ALL
	 * program modules, processes and data blocks indicating that some settings
	 * have changed. 
	 * @param sendGeneralSettingsNotification the sendGeneralSettingsNotification to set
	 */
	public void setSendGeneralSettingsNotification(
			boolean sendGeneralSettingsNotification) {
		this.sendGeneralSettingsNotification = sendGeneralSettingsNotification;
	}

	/**
	 * @return the sendGeneralSettingsNotification
	 */
	public boolean isSendGeneralSettingsNotification() {
		return sendGeneralSettingsNotification;
	}
	
	public String getWarningTitle() {
		return warningTitle;
	}

	public void setWarningTitle(String warningTitle) {
		this.warningTitle = warningTitle;
	}

	/**
	 * Display a warning message with a default title
	 * @param warningText text
	 * @return false so these can be a single return line in dialog getParams funcs. 
	 */
	public boolean showWarning(String warningText) {
		return showWarning(warningTitle, warningText);
	}
		
	public static boolean showWarning(Window owner, String warningTitle, String warningText) {
		JOptionPane.showMessageDialog(owner, warningText, warningTitle, JOptionPane.ERROR_MESSAGE);
		return false;
	}
	/**
	 * Display a warning message with given title and text
	 * @param warningTitle title of warning dialog
	 * @param warningText message of warning dialog
	 * @return false so these can be a single return line in dialog getParams funcs. 
	 */
	public boolean showWarning(String warningTitle, String warningText) {
		return showWarning(getOwner(), warningTitle, warningText);
	}

	/**
	 * Use the standard internal Double formatting to 
	 * print Double numbers with a minimum, but sensible number
	 * of decimal places. 
	 * @param val value to format 
	 * @return formatted string. 
	 */
	public static String formatDouble(double val) {
		return (new Double(val)).toString();
	}
	
	/**
	 * @param warnDefaultSetting the warnDefaultSetting to set
	 */
	public void setWarnDefaultSetting(boolean warnDefaultSetting) {
		this.warnDefaultSetting = warnDefaultSetting;
	}

	/**
	 * @return the warnDefaultSetting
	 */
	public boolean isWarnDefaultSetting() {
		return warnDefaultSetting;
	}

	/**
	 * @return the cancelObserver
	 */
	public CancelObserver getCancelObserver() {
		return cancelObserver;
	}

	/**
	 * @param cancelObserver the cancelObserver to set
	 */
	public void setCancelObserver(CancelObserver cancelObserver) {
		this.cancelObserver = cancelObserver;
	}
}
