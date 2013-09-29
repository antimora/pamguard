package d3;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Class for unpacking XML metadata files created from Mark Johnson's D3 software
 * 
 * @author Doug Gillespie
 *
 */
public class D3XMLFile {

	private static final String dateFormatString = "yyyy,MM,dd,HH,mm,ss"; //e.g. "2012,4,28,8,41,1"
	SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
	private Document doc;
	private long startTime = Long.MIN_VALUE;
	private long endTime = Long.MIN_VALUE;
	private File xmlFile;
	
	private D3XMLFile(Document doc, File xmlFile) {
		this.doc = doc;
		this.xmlFile = xmlFile;
		findFileTimes();
	}
	
	/**
	 * Open an XML file for a corresponding wav file 
	 * by changing the file end to xml and then trying to
	 * find an xml file in the same directory. 
	 * @param file wav file (or valid xml file) 
	 * @return an XML file, from which additional information can then be extracted. 
	 */
	public static D3XMLFile openXMLFile(File file) {
		String wavFileName = file.getAbsolutePath();
		String xmlFileName = wavFileName.replace(".wav", ".xml");
		xmlFileName = xmlFileName.replace(".WAV", ".xml");
		if (xmlFileName.equals(wavFileName)) {
			// no .wav to replace - possibly an AIF file. 
			return null;
		}
		File xmlFile = new File(xmlFileName);
		if (xmlFile.exists() == false) {
			return null;
		}
		/*
		 * Try reading the document.
		 */
		Document doc;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(xmlFile);
		} catch (ParserConfigurationException e) {
			System.out.println(String.format("Parser Error in XML file %s: %s", xmlFileName, e.getMessage()));
			return null;
		} catch (SAXException e) {
			System.out.println(String.format("SAX Error in XML file %s: %s", xmlFileName, e.getMessage()));
			return null;
		} catch (IOException e) {
			System.out.println(String.format("IO Error in XML file %s: %s", xmlFileName, e.getMessage()));
			return null;
		}
		doc.getDocumentElement().normalize();

		return new D3XMLFile(doc, xmlFile);
	}
	
	/**
	 * Get the start time from the XML file
	 * @param file xml file
	 * @return time in milliseconds or Long.MIN_VALUE if it fails. 
	 */
	public static long getXMLStartTime(File file) {
		D3XMLFile dFile = openXMLFile(file);
		if (dFile == null) {
			return Long.MIN_VALUE;
		}
		return dFile.getStartTime();
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}


	/**
	 * Find the start and end times encoded in the XML file
	 * @return true if two times are found. 
	 */
	private boolean findFileTimes() {
		NodeList events = doc.getElementsByTagName("EVENT");
		String eventName;
		for (int i = 0; i < events.getLength(); i++) {
			Node event = events.item(i);
			eventName = "";
			if (event.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) event;
				String dateTime = eElement.getAttribute("TIME");
				Node fc = eElement.getFirstChild();
				if (fc != null) {
					Node sib = fc.getNextSibling();
					if (sib != null) {// && ((Element) sib).getNodeName()))
						eventName = sib.getNodeName();
					}
				}
				if (eventName.equals("START")) {
					startTime = unpackTimeString(dateTime);
				}
				else if (eventName.equals("END")) {
					endTime = unpackTimeString(dateTime);
				}
			}

		}
		return (startTime != Long.MIN_VALUE && endTime != Long.MIN_VALUE);
	}
	
	/**
	 * Convert the tag date string into something sensible
	 * @param timeString timestring from tag xml file
	 * @return date in milliseconds. 
	 */
	private long unpackTimeString(String timeString) {
		if (timeString == null) {
			return Long.MIN_VALUE;
		}
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d;
		try {
			d = dateFormat.parse(timeString);
		} catch (ParseException e) {
			System.out.println(String.format("Unable to interpet XML string date %s in %s", 
					timeString, xmlFile.getName()));
			return Long.MIN_VALUE;
		}
		
		Calendar cl = Calendar.getInstance();
		cl.setTimeZone(TimeZone.getTimeZone("GMT"));
		cl.setTime(d);
		return cl.getTimeInMillis();
	}
}
