package mcc;

public class MccTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MccJniInterface mccJniInterface = new MccJniInterface();
		MccJniInterface.loadLibrary();
//		boolean havLib = MccJniInterface.haveLibrary();
		int nBoard = mccJniInterface.getNumBoards();
		System.out.println("Number of boards = " + nBoard);
//		double v;
		for (int i = 0; i < nBoard; i++) {
			System.out.println(String.format("Board %d = %s", i, mccJniInterface.getBoardName(i)));
		}
		int nC = 2;
		double[] v = new double[nC];
		for (int i = 1; i < 100; i++) {
			System.out.print(String.format("Read voltage"));
			for (int iC = 0; iC < nC; iC++) {
				v[iC] = mccJniInterface.readVoltage(0, iC, MccJniInterface.BIP10VOLTS);
				System.out.print(String.format(" %f", v[iC] ));
			}
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println();
		}
//
//		v = mccJniInterface.readVoltage(i, 0, MccJniInterface.BIP10VOLTS);
//		System.out.println(String.format("REad voltage %f", v ));
		
	}
	

}
