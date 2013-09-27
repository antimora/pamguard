package whistleClassifier.training;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import whistleClassifier.BasicFragmentStore;
import whistleClassifier.FragmentStore;
import whistleClassifier.WhistleFragment;

public class PairedSequentialTrainingSelector extends
		SequentialTrainingSelector {

	private Vector<FragmentStore> testFragmentStores1;
	private Vector<FragmentStore> testFragmentStores2;

	@Override
	protected int[] createTrainingSection(TrainingDataGroup trainingDataGroup,
			double trainingFraction, int sectionLength, double minFreq,
			double maxFreq, int minContourLength) {
		trainingFraction = 0.5;
		// create as for a single test section
		int[] n = super.createTrainingSection(trainingDataGroup, trainingFraction,
				sectionLength, minFreq, maxFreq, minContourLength);
		// then split the test section
		int[] n2 = new int[3];
		n2[0] = n[0];
		n2[1] = n[1]/2;
		n2[2] = n2[1];
		
		// now split the contents of the testFragmentStores
		List<FragmentStore> allTest = super.getTestStoreList(0);
		testFragmentStores1 = new Vector<FragmentStore>();
		testFragmentStores2 = new Vector<FragmentStore>();
		/*
		 * Each fragment store is a set of data for one classification. Therefore 
		 * don't split these in half, but share them out between the two test sets.
		 */
		for (int i = 0; i < n2[1]; i++) {
			testFragmentStores1.add(allTest.get(i));
			testFragmentStores2.add(allTest.get(i+n2[1]));
		}
//		BasicFragmentStore aFragmentStore;
//		BasicFragmentStore aFragmentStore1;
//		BasicFragmentStore aFragmentStore2;
//		for (int i = 0; i < allTest.size(); i++) {
//			aFragmentStore = (BasicFragmentStore) allTest.get(i);
//			aFragmentStore1 = new BasicFragmentStore(aFragmentStore.getSampleRate());
//			aFragmentStore2 = new BasicFragmentStore(aFragmentStore.getSampleRate());
//			testFragmentStores1.add(aFragmentStore1);
//			testFragmentStores2.add(aFragmentStore2);
//			double nFrag = aFragmentStore.getFragmentCount();
//			int nFrag2 = (int) nFrag / 2;
//			Iterator<WhistleFragment> li = aFragmentStore.getFragmentIterator();
//			for (int f = 0; f < nFrag2; f++) {
//				aFragmentStore1.addFragemnt(li.next());
//			}
//			for (int f = 0; f < nFrag2; f++) {
//				aFragmentStore2.addFragemnt(li.next());
//			}
//		}
		
		
		return n2;
	}

	@Override
	List<FragmentStore> getTestStoreList(int iTest) {
		if (iTest == 0) {
			return testFragmentStores1;
		}
		else {
			return testFragmentStores2;
		}
	}

	@Override
	public int getNumTestSets() {
		return 2;
	}

}
