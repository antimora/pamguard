package loggerForms.controls;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import NMEA.NMEADataBlock;
import NMEA.NMEADataUnit;
import PamView.PamCheckBox;
import PamView.PamPanel;

import loggerForms.LoggerForm;
import loggerForms.controlDescriptions.ControlDescription;
import loggerForms.controls.LoggerControl.ComponentFocusListener;

public class CheckboxControl extends LoggerControl {
	
	private JCheckBox checkBox;
	
	public CheckboxControl(ControlDescription controlDescription,
			LoggerForm loggerForm) {
		super(controlDescription, loggerForm);
		
		component.add(new LoggerFormLabel(loggerForm,controlDescription.getTitle()));
		component.add(checkBox = new PamCheckBox(""));
		component.add(new LoggerFormLabel(loggerForm,controlDescription.getPostTitle()));
		checkBox.addFocusListener(new ComponentFocusListener());
		setToolTipToAllSubJComponants(component);
		setDefault();
	}
	
	
	@Override
	public Object getData() {
		return Boolean.valueOf(checkBox.isSelected());
	}

	@Override
	public void setData(Object data) {
		if (data==null){
			checkBox.setSelected(false);
			return;
		}
		try {
			checkBox.setSelected((Boolean) data);
		}
		catch(ClassCastException e) {
			e.printStackTrace();
		}
	}
	

	/* (non-Javadoc)
	 * @see loggerForms.controls.LoggerControl#setDefault()
	 */
	@Override
	public void setDefault() {
		String defVal = getControlDescription().getDefaultValue();
		if (defVal == null || defVal.length()==0||defVal.equals("0") || defVal.equalsIgnoreCase("false")) {
			setData(false);
		}
		else {
			setData(true);
		}
	}


	@Override
	public String getDataError() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int fillNMEAControlData(NMEADataUnit dataUnit) {
		String subStr = NMEADataBlock.getSubString(dataUnit.getCharData(), 
				controlDescription.getNmeaPosition());
		if (subStr == null){
			return AUTO_UPDATE_FAIL;
		}
		else if (subStr.equalsIgnoreCase("FALSE") || subStr.equals("0")) {
			checkBox.setSelected(false);
		}
		else {
			checkBox.setSelected(true);
		}
		return AUTO_UPDATE_SUCCESS;
	}

	
	
}
