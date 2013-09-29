package loggerForms;

import java.util.ArrayList;

import loggerForms.controlDescriptions.ControlDescription;
import loggerForms.controls.LoggerControl;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;

public class FormsLogging extends SQLLogging {

	FormDescription formDescription;
	
	protected FormsLogging(FormDescription formDescription, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.formDescription = formDescription;
		setUpdatePolicy(SQLLogging.UPDATE_POLICY_OVERWRITE);
		setTableDefinition(formDescription.getOutputTableDef());
	}

//	@Override
//	public void setTableData(PamDataUnit pamDataUnit) {
//		
//		FormsDataUnit formDataUnit = (FormsDataUnit) pamDataUnit;
//		int dataLen = formDataUnit.getFormData().length;
//		int tableLen= getTableDefinition().getTableItemCount();
//		
////		for (int j=0;j<tableLen;j++){
////			System.out.println(getTableDefinition().getTableItem(j).getName());
////		}
//		
//		int diff = tableLen-dataLen;
//		// put number of standard items as static is pamtable item/table(ie diff ~3-5)
//		
////		System.out.printf("Save Forms Data "+PamCalendar.formatDateTime(formDataUnit.getTimeMilliseconds())+"\n");
//		
//		for (int i=0;i<dataLen;i++){
////			System.out.printf("INTS.. %d dataLen:%d, tableLen:%d, diff:%d \n",i,dataLen,tableLen,diff);
////			if ((formDataUnit.getFormData()[i]==null)){
////				System.out.println("Object "+i+" to save: ~null");
////				getTableDefinition().getTableItem(i+diff).setValue(null);  //be null;
////			}else{
////				System.out.println("Object "+i+" to save: "+formDataUnit.getFormData()[i].toString());
////				System.out.println("Name: "+getTableDefinition().getTableItem(i+diff).getName());
////				System.out.println("Data: "+formDataUnit.getFormData()[i]);
//				getTableDefinition().getTableItem(i+diff).setValue(formDataUnit.getFormData()[i]);//object
//				
////			}
//			
//		}
//		
//	}
	
	@Override
	public void setTableData(PamDataUnit pamDataUnit) {
		FormsDataUnit formDataUnit = (FormsDataUnit) pamDataUnit;
		
		Object[] datas = formDataUnit.getFormData();
//		for(LoggerControl lc:formDataUnit.getLoggerForm().getInputControls()){
//			
//			lc.moveDataToTableItems();
////			for (FormsTableItem fti:lc.getControlDescription().getFormsTableItems()){
////				
////				getTableDefinition().getTableItem(fti).setValue(lc.getData());
////				
////			}
//		}
		
		ArrayList<ControlDescription> inputCtrls = formDescription.getInputControlDescriptions();
		ControlDescription cd;
		for (int i = 0; i < inputCtrls.size(); i++) {
			cd = inputCtrls.get(i);
			cd.moveDataToTableItems(datas[i]);
			
		}
		
//		for (ControlDescription controlDescription:formDescription.getInputControlDescriptions()){
//			controlDescription.moveDataTo
//		}
	}
	
	
	
	

	/* (non-Javadoc)
	 * @see generalDatabase.SQLLogging#createDataUnit(long, int)
	 */
	@Override
	protected PamDataUnit createDataUnit(long timeMilliseconds,int databaseIndex) {

		ArrayList<ControlDescription> inputCtrls = formDescription.getInputControlDescriptions();
		Object[] formData = new Object[inputCtrls.size()];
		ControlDescription cd;
		for (int i = 0; i < inputCtrls.size(); i++) {
			cd = inputCtrls.get(i);
			formData[i] = cd.moveDataFromTableItems();
			
		}
		
//		
//		int dataLen = formDescription.getInputControlDescriptions().size();
//		int tableLen= getTableDefinition().getTableItemCount();
//		int diff = tableLen-dataLen;
//		Object[] formData = new Object[dataLen];
//		int tableIndex;
//		for (int j=0;j<dataLen;j++){
//			tableIndex = j+diff;
//			formData[j] = getTableDefinition().getTableItem(j+diff).getValue();
//			if (formData[j] != null && formData[j].getClass() == String.class) {
//				formData[j] = ((String) formData[j]).trim();
//			}
//		}
		
		
		
		FormsDataUnit formsDataUnit = new FormsDataUnit(null, timeMilliseconds, formDescription, formData);
		formsDataUnit.setDatabaseIndex(databaseIndex);
		getPamDataBlock().addPamData(formsDataUnit);
		
		return formsDataUnit;
		
		
		
		
		
		
		
		
		
//		LoggerForm.
//		return new FormsDataUnit(null, timeMilliseconds, formDescription, formData);
	}

}
