package PamController.command;

import PamController.PamController;

/**
 * Get the status of PAMGuard 0 = idle, 1 = running
 * @author Doug Gillespie
 *
 */
public class StatusCommand extends ExtCommand {

	public StatusCommand() {
		super("Status", true);
	}

	@Override
	public boolean execute() {
		return true;
	}

	@Override
	public String getReturnString() {
		return String.format("status %d", PamController.getInstance().getPamStatus());
	}
	

}
