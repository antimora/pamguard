package clickDetector;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;


public class ClickSpectrumTemplateParams implements Serializable, Cloneable {
	
	File templateFile; 
	
	ArrayList<String> previousFiles=null;
	
	ArrayList<ClickTemplate> clickTemplateArray=new ArrayList<ClickTemplate>();
	
	ArrayList<Boolean> clickTempVisible=new ArrayList<Boolean>();
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	protected ClickSpectrumTemplateParams clone()  {
		try {
			return (ClickSpectrumTemplateParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
