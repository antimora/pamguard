package userDisplay;

import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;

/**
 * Frames for general purpose user displays. 
 * Doesn't do much apart from use the normal frame functions, but
 * does store a reference to the source of the plot so that
 * it can be remembered and recreated when PAMGuard restarts.  
 * @author Doug Gillespie
 *
 */
public class UserDisplayFrame extends JInternalFrame{
	
	private UserDisplayProvider userDisplayProvider;

	public UserDisplayFrame(UserDisplayProvider userDisplayProvider) {
		super(userDisplayProvider.getName(), true, true, true);
		this.userDisplayProvider = userDisplayProvider;
		add(userDisplayProvider.getComponent());
		setFrameIcon(new ImageIcon(ClassLoader
				.getSystemResource("Resources/pamguardIcon.png")));
		setSize(900, 400);
		setVisible(true);
	}

	/**
	 * @return the userDisplayProvider
	 */
	public UserDisplayProvider getUserDisplayProvider() {
		return userDisplayProvider;
	}
	
	
}
