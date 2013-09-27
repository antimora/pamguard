package PamController.command;

import PamController.PamController;

public class StartCommand extends ExtCommand {

	public StartCommand() {
		super("start", true);
	}

	@Override
	public boolean execute() {
		return PamController.getInstance().pamStart();
	}

}
