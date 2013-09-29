package PamModel;

import java.awt.Frame;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import PamController.PamController;
import PamController.PamControllerInterface;
import PamguardMVC.PamDataBlock;


public class DependencyManager {

	PamModel pamModel;

	public DependencyManager(PamModel pamModel) {
		
		this.pamModel = pamModel;
		
	}
	
	/**
	 * 
	 * Checks through the data model and tries to find an 
	 * appropriate PamDataBlock. Returns a reference to the 
	 * datablock if it can find one, null otherwise. 
	 * @param parentComponent
	 * @param pamDependent
	 * @return reference to a PamDataBlock satisfying the dependency
	 */
	public PamDataBlock checkDependency(Frame parentFrame, PamDependent pamDependent) {

		return checkDependency(parentFrame, pamDependent, false);
	}

	/**
	 * 
	 * Checks through the data model and tries to find an 
	 * appropriate PamDataBlock. Returns a reference to the 
	 * datablock if it can find one, null otherwise. 
	 * @param parentComponent
	 * @param pamDependent
	 * @param create create set to true if you want checkDependency to automatically
	 * create required dependencies.
	 * @return reference to a PamDataBlock satisfying the dependency
	 */
	public PamDataBlock checkDependency(Frame parentFrame, PamDependent pamDependent, boolean create) {

		PamDependency pamDependency = pamDependent.getDependency();
		
		PamDataBlock dataBlock = findDependency(pamDependency);
		
		if (dataBlock != null || create == false) return dataBlock;
		
		/*
		 * No data block of the correct type / data name exists, so 
		 * find the appropriate module info for the default
		 * data provider and create one. 
		 */
		PamModuleInfo moduleInfo = PamModuleInfo.findModuleInfo(pamDependency.getDefaultProvider());
		if (moduleInfo == null) {
			String str = "Cannot find " + pamDependency.getDefaultProvider() + "dependent module information";
			System.out.println(str);
		}
		
		/*
		 * Have found the module that's needed, check with the user that they 
		 * want to go ahead and create it.
		 */
		String str = "The " + pamDependent.getDependentUserName() + " you are trying to create requires a \n" +
		moduleInfo.getDescription() + " before it can operate \n\n" +
		"Do you wish to go ahead and create a " + moduleInfo.getDescription();
		if (JOptionPane.showConfirmDialog(parentFrame, str, "Module dependencey manager", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
			return null;
		}
		/*
		 * Check dependencies for the module we're about to create
		 */
		if (moduleInfo.getDependency() != null)	checkDependency(parentFrame, moduleInfo, create);
		
		/*
		 * Then go ahead and create it.
		 */
		PamController.getInstance().addModule(parentFrame, moduleInfo);
		
		return findDependency(pamDependency);
		
	}

	/**
	 * Checks through the data model and tries to find an 
	 * appropriate PamDataBlock. Returns a reference to the 
	 * datablock if it can find one, null otherwise. 
	 * @param pamDependency
	 * @return reference to a PamDataBlock satisfying the dependency
	 */
	public PamDataBlock findDependency(PamDependency pamDependency)
	{
		PamControllerInterface pamController = PamController.getInstance();
		
		ArrayList<PamDataBlock> pamDataBlocks = pamController.getDataBlocks(pamDependency.getRequiredDataType(), true);
		
		if (pamDataBlocks == null || pamDataBlocks.size() == 0) return null;
		
		/*
		 * If the dependency name is not set, this is as far as it is necessary
		 * to go - otherwise check through the list trying to find the specifically
		 * named DataBlock
		 */
		if (pamDependency.getDataBlockName() == null) return pamDataBlocks.get(0);
		
		PamDataBlock dataBlock;
		for (int i = 0; i < pamDataBlocks.size(); i++) {
			dataBlock = pamDataBlocks.get(i);
			if (dataBlock.getDataName().equals(pamDependency.getDataBlockName())) {
				return dataBlock;
			}
		}
		
		return null;
		
	}
}
