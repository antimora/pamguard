package PamController;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import com.sun.org.apache.xerces.internal.parsers.SAXParser;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class XMLTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLTest t = new XMLTest();
		t.xmlTest();
	}
	void xmlTest() {
		XMLReader parser = new SAXParser();
		System.out.println(parser.toString());
//		new xmldocument
		Element el;
		Document doc = new DocumentImpl();
		Element root = doc.createElement("Modules");
		Element module = doc.createElement("Module");
		module.setAttribute("Name", "Click Detector");
		module.setAttribute("JavaClass", "something.else.clickControl");
		
		module.appendChild(el = doc.createElement("Trigger"));
		el.setAttribute("Threshold", ((Double) Math.PI).toString());
		el.setAttribute("SHORTFILTER", ((Double) .1).toString());
		el.setAttribute("LONGFILTER", ((Double) .000001).toString());
		el.setAttribute("LONGFILTER2", ((Double) .000001).toString());
		
		module.appendChild(el = doc.createElement("PreFilter"));
		el.setAttribute("Type", "Butterworth");
		el.setAttribute("Band", "BANDPASS");
		el.setAttribute("LOFREQ", ((Double) 1.245).toString());
		el.setAttribute("HIFREQ", ((Double) 1.245).toString());
		el.setAttribute("ORDER", ((Integer) 3).toString());
		el.setAttribute("RIPPLE", ((Double) 3.).toString());
		
//		Object tstc = new TestClass();
//		XMLEncoder en = new xm
//		Node n = new 
		
		root.appendChild(module);
		doc.appendChild(root);
		

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("test.xml");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		// XERCES 1 or 2 additionnal classes.
		OutputFormat of = new OutputFormat("XML","ISO-8859-1",true);
		of.setIndent(1);
		of.setLineSeparator("\r\n");
		of.setIndenting(true);
		of.setDoctype(null,"pamguard.dtd");
		XMLSerializer serializer = new XMLSerializer(fos,of);
		// As a DOM Serializer
		try {
			serializer.asDOMSerializer();
			serializer.serialize( doc.getDocumentElement() );
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class TestClass {
		double a = 1.0;
		int b = 2;
		String c = "catfish";
		double[] smallArray = {2., 5., 6., 8.};
	}

}
