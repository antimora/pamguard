package PamView;

import java.awt.FlowLayout;

import javax.swing.JToolBar;

public class PamStatusBar {

	private JToolBar statusBar;
	
	//private JLabel daqStatus;
	
	//private JLabel loggingStatus;
	
	private static PamStatusBar pamStatusBar;
	
	private PamStatusBar (){
		statusBar = new JToolBar("Pamguard");
		statusBar.setFloatable(true);
//		statusBar.setAutoscrolls(true);
//		FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
//		flow.setAlignment(FlowLayout.)
		statusBar.setLayout(new FlowLayout(FlowLayout.LEFT));
//		statusBar.setLayout(new BorderLayout());
		// the PCU's are responsible for putting these in now
		// since there may be multiple instances.
	}
	public static PamStatusBar getStatusBar() {
		if (pamStatusBar == null) {
			pamStatusBar = new PamStatusBar();
		}
		return pamStatusBar;
	}
	public JToolBar getToolBar() {
		return statusBar;
	}
	
	public void resize() {
		statusBar.invalidate();
//		statusBar.pack();
//		System.out.println("Resize status bar");
	}
	
	
//	public JLabel getDaqStatus() {
//		return daqStatus;
//	}
//	public JLabel getLoggingStatus() {
//		return loggingStatus;
//	}
}
