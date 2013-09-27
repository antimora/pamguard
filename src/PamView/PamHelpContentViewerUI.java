package PamView;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.help.JHelpContentViewer;
import javax.help.plaf.basic.BasicContentViewerUI;
import javax.swing.event.HyperlinkEvent;

public class PamHelpContentViewerUI extends BasicContentViewerUI {

	private static final long serialVersionUID = 1L;

	public PamHelpContentViewerUI(JHelpContentViewer arg0) {
		super(arg0);
//		System.out.println("Construct PamHelpContentViewerUI");
	}

//	@Override
//	public void idChanged(HelpModelEvent arg0) {
//		System.out.println("PamHelpContentViewerUI - idChanged " + arg0.getURL());
//		/*
//		 * Decide if it's a file or a external web link
//		 */
//		URL url = arg0.getURL();
////		System.out.println(url.getAuthority() + " " + url.getFile() + " " + url.getHost());
////		System.out.println(url.getPath() + " " + url.getDefaultPort() + " " + 
//		System.out.println("protocol " + url.getProtocol());
//		
////		if (url.getProtocol().equalsIgnoreCase("http")) {
////			try {
////				Desktop.getDesktop().browse(url.toURI());
////			} catch (IOException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			} catch (URISyntaxException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
////		}
////		else {
//			super.idChanged(arg0);
////		}
//	} 

	@Override
	public void hyperlinkUpdate(HyperlinkEvent arg0) {
		/*
		 * If it's activated and if it's 
		 */
//		System.out.println("hyperlinkUpdate " + arg0.getDescription() + " " + arg0.getEventType() +
//				" " + arg0.getURL());
		
		URL url = arg0.getURL();
		boolean isHttp = url.getProtocol().equalsIgnoreCase("http");
		if (arg0.getEventType() == HyperlinkEvent.EventType.ACTIVATED && isHttp) {
			openBrowserURL(url);
		}
		else {
			super.hyperlinkUpdate(arg0);
		}
	}
	
	private void openBrowserURL(URL url) {
		if (url == null) {
			return;
		}
		try {
			Desktop.getDesktop().browse(url.toURI());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

}
