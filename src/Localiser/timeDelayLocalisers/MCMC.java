package Localiser.timeDelayLocalisers;

import java.util.ArrayList;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import PamDetection.AbstractDetectionMatch;
import PamUtils.StatMath;


/**
 * Markov chain Monte Carlo (MCMC) time delay based locaisation algorithm.
 * <p>
 * This is an advanaced and highly computationally intensive localisation algorithm based on MCMC methods. For a good description see;  
 * The Transit Light Curve (TLC) Project.I. Four Consecutive Transits of the Exoplanet XO-1b Matthew J. Holman1
 * <p>
 * Input variables are an ArrayList<ArrayList<Double>> of observed time delays, along with a corresponding error  ArrayList<ArrayList<Double>> and ArrayList<ArrayList<Point3f>> of hydrophone positions. 
 * The time delay and delay errors arraylist will be exactly the same size. 
 * <p>
 * Each row in the time delay ArrayList contains a group time delays calculated from N synchrnoised hydrophones in the corresponding ArrayList<ArrayList<Point3f>> row. This allows potential locaisation between large groups of unsynchronised hydrophones, a situation always encountered during target motions analysis.
 * <p>
 * Multiple MCMC chains can and should be run. These are executed on different threads to take advantage of multi core processing as much as possible. Even so a large number of chains or large number of observed delays can result in significant processing times. 
 * <p>
 * Results are analysed for convergence and final locations packed into an MCMCTDResults class. 
 * 
 *Input
 *Time Delays
 *Time Delay Errors
 *Hydrophone Array
 *SampaleRate 
 *
 * @author Jamie Macaulay
 *
 */
public class MCMC implements TimeDelayLocaliserModel {

	//Variables needed for localisation
	private ArrayList<ArrayList<Double>> timeDelaysObs;
	
	private ArrayList< ArrayList<Double>> timeDelayErrors;
	
	private ArrayList<ArrayList<Point3f>> hydrophoneArray;
	
	private float sampleRate;
	
	private double speedOfSound;
		
	//The final results
	ArrayList<MCMCTDResults> finalResults;

	protected MCMCParams settings;
	
	//MCMC Results
	
	ArrayList<ArrayList<Point3f>> jumps;
	
	ArrayList<ArrayList<Double>> chiValues;
	
	ArrayList<MCMCTDResults> results;
	
	
	public MCMC(ArrayList<ArrayList<Point3f>> hydrophoneArray ,ArrayList<ArrayList<Double>> timeDelays, ArrayList< ArrayList<Double>> timeDelayErrors, MCMCParams settings, float sampleRate, double speedOfSound){
		
		this.settings=settings;
		this.sampleRate=sampleRate;
		this.speedOfSound=speedOfSound;

		this.timeDelaysObs=timeDelays;
		this.hydrophoneArray=hydrophoneArray;
				
		//add cross correlation error to time delay errors		
		this.timeDelayErrors=addCrossCorError(timeDelayErrors, settings.timeError, sampleRate);
		
	}
	
	public MCMC(MCMCParams settings){
		this.settings=settings;
	}

	
	/**
	 * Calculates the time in seconds between sound travelling from a point in space to a hydrophone.
	 * @param Hydrophone
	 * @param ChainPos
	 * @param SpeedofSound
	 * @return Time it takes to travel between ChainPos and Hydrophone.
	 */
	private static double timeToHydrophone(Point3f Hydrophone, Point3f ChainPos, double SpeedofSound){
		return Math.abs(Hydrophone.distance(ChainPos) / SpeedofSound);
		//return Math.sqrt((Math.pow(Hydrophone.getX()-ChainPos.getX(),2)+Math.pow(Hydrophone.getY()-ChainPos.getY(),2)+Math.pow(Hydrophone.getZ()-ChainPos.getZ(),2)))/ SpeedofSound;
	}
	
	
	/**
	 * Calculates a random new Co-Ordinate, 3D Cartesian or Cylindrical space;
	 * @param ChainPos
	 * @return
	 */
	public Point3f getNewJumpPoint(Point3f ChainPos){
		
		//Cylindrical
		if (settings.cylindricalCoOrdinates==true){
			float x=ChainPos.getX();
			float y=ChainPos.getY();
			float z=ChainPos.getZ();
			float r=(float) Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2));
			float Theta=(float) Math.atan(y/x);
			float newTheta=(float) (Theta+(Math.random()-0.5)*2*Math.PI);
			float newr=(float) Math.abs(r+((Math.random()-0.5)*settings.jumpSize));
			float newx=(float) (newr*Math.cos(newTheta));
			float newz=(float) (z+((Math.random()-0.5)*settings.jumpSize));
			float newy=(float) (newr*Math.sin(newTheta));
			
			return new Point3f(newx, newy, newz);
		}
	 
	    //Cartesian
		else{
			return new Point3f((float) (ChainPos.getX()+((Math.random()-0.5)*settings.jumpSize)), (float)  (ChainPos.getY()+((Math.random()-0.5)*settings.jumpSize)),(float)  (ChainPos.getZ()+((Math.random()-0.5)*settings.jumpSize)));
		}
	}
	
	/**
	 * Calculates the ChiSquared value between observed data and theoretical delays calculated for a certain point in three dimensional space.
	 * @param ObservedTimeDelays
	 * @param ChainPosTimeDelays
	 * @return
	 */
	public static double chiSquared(ArrayList<ArrayList<Double>> ObservedTimeDelays, ArrayList<ArrayList<Double>> ChainPosTimeDelays,ArrayList<ArrayList<Double>> errors){
		
		double ChiResult=0;
			for (int k=0;k<ObservedTimeDelays.size(); k++){
				for (int m=0; m<ObservedTimeDelays.get(k).size(); m++){
					//System.out.println("Chi: "+ObservedTimeDelays.get(k).get(m));
					if (ObservedTimeDelays.get(k).get(m)!=null && ObservedTimeDelays.get(k).get(m).isNaN()==false){
						ChiResult+=(Math.pow((ObservedTimeDelays.get(k).get(m)-ChainPosTimeDelays.get(k).get(m)),2))/Math.pow(errors.get(k).get(m),2);
					}
				}
		}
		return ChiResult;
	}
	
	/**
	 * Calculate the theoretical time delays if a source was located at point3d ChainPos.
	 * @param ChainPos- theoretical position of the source.
	 * @param hydrophonePos- the positions of all the hydrophones within the array in Cartesian coOrdinates.
	 * @param speedOfSound- the speed of sound;
	 * @return The time delays that would result from a source at this location. 
	 */
	public static ArrayList<ArrayList<Double>> getTimeDelays(Point3f ChainPos, ArrayList<ArrayList<Point3f>> hydrophonePos, double speedOfSound){
		
		ArrayList<Integer> indexM1;
		ArrayList<Integer> indexM2;
		
		ArrayList<Double> timeDelays;
		ArrayList<Double> sourceTime;
	
		ArrayList<ArrayList<Double>> timeDelaysAll=new ArrayList<ArrayList<Double>>();
		//ArrayList<ArrayList<Point3f>> hydrophonePos=this.hydrophoneArray;
		
		double timedelay;
		
		for (int k=0; k<hydrophonePos.size(); k++){
			
			sourceTime=new ArrayList<Double>();
			
			for (int i=0; i<hydrophonePos.get(k).size();i++){
				sourceTime.add(timeToHydrophone(hydrophonePos.get(k).get(i),ChainPos,speedOfSound));
				//System.out.println("HydrophonePos: "+hydrophonePos.get(k).get(i));
				//System.out.println("Source Time: "+sourceTime);
			}
			
			indexM1=AbstractDetectionMatch.indexM1(hydrophonePos.get(k).size());
			indexM2=AbstractDetectionMatch.indexM2(hydrophonePos.get(k).size());
			
			timeDelays=new ArrayList<Double>();
			
			for (int j=0; j<indexM1.size(); j++){
				 timedelay=sourceTime.get(indexM2.get(j))-sourceTime.get(indexM1.get(j));
				//note this is m2-m1 in order to stay with convention
				timeDelays.add(timedelay); //TODO
			}
			
			timeDelaysAll.add(timeDelays);
		}

		return timeDelaysAll;
	}
	

	/**
	 * Take an ArrayList of the Markov chain jumps and chi-squared values of each jump and calculate source position. In order to make sure the burn in phase is not included, a
	 * certain initial percentage of jumps is ignored.  
	 * @param chainJumps
	 * @param Chi2
	 * @param PercentagetoIgnore
	 * @return
	 */
	public MCMCTDResults AnalyseMCMCResults(ArrayList<Point3f> chainJumps, ArrayList<Double> Chi2, int analysisType){
		
		double range;
		double x;
		double y;
		double z;
		double meanRange;
		double meanX;
		double meanY;
		double meanZ;
		double stdRange;
		double stdX;
		double stdY;
		double stdZ;
		double meanChi;

		ArrayList<ArrayList<Point3f>> jumps =new ArrayList<ArrayList<Point3f>>();
		jumps.add(chainJumps);
		ArrayList<Double> Ranges=new ArrayList<Double>();
		ArrayList<Double> xs=new ArrayList<Double>();
		ArrayList<Double> ys=new ArrayList<Double>();
		ArrayList<Double> zs=new ArrayList<Double>();
		
		for (int i=0; i<chainJumps.size();i++){
			range=Math.sqrt(Math.pow(chainJumps.get(i).getX(),2)+Math.pow(chainJumps.get(i).getY(),2)+Math.pow(chainJumps.get(i).getZ(),2));
			x=chainJumps.get(i).getX();
			y=chainJumps.get(i).getY();
			z=chainJumps.get(i).getZ();
			
			Ranges.add(range);
			xs.add(x);
			ys.add(y);
			zs.add(z);
			
		}
		
		switch (analysisType){
		
			default: {
			
				meanRange=StatMath.mean(Ranges,settings.percentageToIgnore);
				meanX=StatMath.mean(xs,settings.percentageToIgnore);
				meanY=StatMath.mean(ys,settings.percentageToIgnore);
				meanZ=StatMath.mean(zs,settings.percentageToIgnore);
				stdRange=StatMath.std(Ranges, settings.percentageToIgnore);
				stdX=StatMath.std(xs, settings.percentageToIgnore);
				stdY=StatMath.std(ys, settings.percentageToIgnore);
				stdZ=StatMath.std(zs, settings.percentageToIgnore);
					
				meanChi=StatMath.mean(Chi2, settings.percentageToIgnore);
				
			break; 
			
			}
		
		}
		
		MCMCTDResults results=new MCMCTDResults();
		results.setLocation(new Point3d(meanX, meanY, meanZ));
		results.setErrors(new Point3d(stdX, stdY, stdZ));
		results.setRange(meanRange);
		results.setRangeError(stdRange);
		results.setChainJumps(jumps);
		results.setMeanChi(meanChi);

		return results;
	}
	
	
	/**Simple Markov Chain Monte Carlo simulation. A good description of how this process works can be found in:
	 * The Transit Light Curve (TLC) Project.I. Four Consecutive Transits of the Exoplanet XO-1b Matthew J. Holman1, Joshua N. Winn2, David W. Latham1,
		Francis T. O�Donovan3, David Charbonneau1,7....
	 * @param ObservedTimeDelays
	 * @return
	 */
	public Object[] mCMC(ArrayList<ArrayList<Double>> ObservedTimeDelays){
		
	System.out.println("MCMC Start");
		

    Point3f  chainPos=new Point3f((float) ((Math.random()-0.5)*settings.ChainStartDispersion), (float) ((Math.random()-0.5)*settings.ChainStartDispersion),(float)(-(Math.random()*settings.ChainStartDispersion)));
    double currentChi=3000E14;
    double newChi;
    Point3f potentialNewJump;

    ArrayList<Double> successChi=new ArrayList<Double>(settings.numberOfJumps/5);
    ArrayList<Point3f> successJump=new ArrayList<Point3f>(settings.numberOfJumps/5);
	ArrayList<ArrayList<Double>> timedelayR=new ArrayList<ArrayList<Double>>(); 
    
		System.out.println("Start MCMC milliseconds: "+ System.currentTimeMillis());
	
		for (int i=0; i<settings.numberOfJumps;i++){
			
			potentialNewJump=getNewJumpPoint(chainPos);
		
			timedelayR=getTimeDelays(potentialNewJump,this.hydrophoneArray, this.speedOfSound);
		    newChi=chiSquared(ObservedTimeDelays,timedelayR,timeDelayErrors);
		
		    if (newChi<currentChi){
		    
		    	chainPos=potentialNewJump;
		    	currentChi=newChi;
		    	successChi.add(newChi);
		    	successJump.add(chainPos);
		    	//System.out.println(ChainPos);
		    	//System.out.println(NewChi);
		    	//System.out.println(ObservedTimeDelays);
		    	//System.out.println(GetTimeDelays(PotentialNewJump));	 
		    }

		    else if (Math.random()<Math.exp((-0.5*(-currentChi+newChi)))){
		   
				chainPos=potentialNewJump;
				currentChi=newChi;
				successChi.add(newChi);
				successJump.add(chainPos);
				//System.out.println(ChainPos);
		    	//System.out.println(NewChi);
		    	//System.out.println(ObservedTimeDelays);
		    	//System.out.println(GetTimeDelays(PotentialNewJump))
			}
		    
		}
		  
		System.out.println("End MCMC millis: "+ System.currentTimeMillis());
		  
		Object[] jumpResults= new Object[2];
		jumpResults[0]=successJump;
		jumpResults[1]=successChi;
		  
		return jumpResults;
	}
	
	/**
	 * Run MCMC on a single thread.
	 */
	Runnable MarkovChainSequence = new Runnable() {

		@SuppressWarnings("unchecked")
		public void run() {
				Object[] SingleChainJumps=mCMC(timeDelaysObs);
				results.add(AnalyseMCMCResults( (ArrayList<Point3f>)SingleChainJumps[0], (ArrayList<Double>)SingleChainJumps[1],settings.IGNORE_PERCENTAGE));
				jumps.add((ArrayList<Point3f>) SingleChainJumps[0]);
				chiValues.add((ArrayList<Double>) SingleChainJumps[1]);
				//System.out.println("Chain1 completed");
		  }
	};

	
	
	
	
	@Override
	public void runAlgorithm() {
		
		System.out.println("Begin MCMC");
		
		// If there is no data in pamDetection then  don't attempt to localise. 
		if (timeDelaysObs == null) {
			System.out.println("Time delays are null. Cannot initilise MCMC algorithm. ");
			return;
		}
		
		jumps=new ArrayList<ArrayList<Point3f>> ();
		chiValues=new ArrayList<ArrayList<Double>>();
		results=new ArrayList<MCMCTDResults>();

		System.out.println("Number of Chains: "+settings.numberOfChains);
		
		/*Run multiple Markov Chains. Each chain is started on a different thread.*/
		Object[] Threads=new Object[settings.numberOfChains];
		
		for (int i=0; i<settings.numberOfChains; i++){
			Threads[i]=new Thread(MarkovChainSequence);
		}

		for (int j=0; j<settings.numberOfChains;j++){
			
			((Thread) Threads[j]).start();
		}
		
		
		for (int k=0; k<settings.numberOfChains;k++){
			try {
				((Thread) Threads[k]).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//TODO
		//Gelman and Rubin Diagnostic
		//Clustering algorithms- must allow degeneracy. 

		
		this.finalResults=clusterAnalaysis(results);
		
		//this.finalResults=results;
		
		System.out.println("Final Results: "+finalResults);

	}
	
	
	public ArrayList<MCMCTDResults> clusterAnalaysis(ArrayList<MCMCTDResults> meanArray){
		
		ArrayList<MCMCTDResults> finalResults=new ArrayList<MCMCTDResults>();
		
		int clusteringAlgorithm=settings.clusterAnalysis;
		
		switch (clusteringAlgorithm){
			case MCMCParams.NONE:
				finalResults.add(averageChains(meanArray));
			break;
			case MCMCParams.K_MEANS:
				if (settings.nKMeans>1) finalResults=clusterChains(meanArray, kMeansAnalysis(meanArray));
				else finalResults.add(averageChains(meanArray));
			break;
		}
		
		return finalResults; 
		
	}
	
	//TODO
	/**
	 * Sometimes multiple chains will converge to different final distributions. If this is the case the results must be clustered, otherwise the final answer will simply be the mean of the two different distributions. 
	 * Here a 3D k-means clustering algorithm is used in x, y and z . First, nk random points are assigned in space based on the start dispersion. Next, all chains are clusters corresponding to their euclidian distance from these two random points. The random points then move to the centroid of their correpsonding clusters. The process repeats 'int iterations' times by which time it is likely convergence will have
	 * occurred. Clustering is attempted  'int attempts' times from different random positions. 
	 * @param meanArray- the mean values of all chains; 
	 * @return 
	 */
	public ArrayList<Point3d> kMeansAnalysis(ArrayList<MCMCTDResults> meanArray){
				
		ArrayList<Point3d[]> meansAll=new ArrayList<Point3d[]>();
		
		Point3d[] means=new Point3d[settings.nKMeans];
		
		int attempts=10; 
		int iterations=20; 
		
		double minDistance;
		double distance;
		int kVal;
		int[] minDistances=new int[meanArray.size()];
		Point3d[] meansNew; 
		ArrayList<int[]> minDistancesAll=new ArrayList<int[]>(); 
		
		for (int l=0; l<attempts; l++){
			
			
			//initialise the mean positions
			for (int k=0; k<settings.nKMeans; k++){
				means[k]=(new Point3d((Math.random()-0.5)*settings.ChainStartDispersion/10.0, (Math.random()-0.5)*settings.ChainStartDispersion/10.0,-(Math.random()*settings.ChainStartDispersion/10.0)));
			}
			
			
			for (int m=0; m<iterations; m++){
				
				//first find out which k means are closest to which results
				for (int j=0; j<meanArray.size() ; j++){
						
					kVal=0; 
					minDistance=Double.MAX_VALUE;
					for (int i=0; i<settings.nKMeans ;i++){
						distance=meanArray.get(j).location.distance(means[i]);
						//System.out.println("Distance: "+distance);
						if (distance<minDistance){
							minDistance=distance;
							kVal=i; 
						}
					}
					//System.out.println("kVal: "+kVal);
					minDistances[j]=kVal;
				}
				
				//now changes the KMeans to the centroid of their closest results
				ArrayList<Point3d> cluster; 
				meansNew=new Point3d[settings.nKMeans];
				
				for (int i=0; i<settings.nKMeans ;i++){
					cluster= new ArrayList<Point3d>();
					for (int j=0; j<meanArray.size() ; j++){
						if (minDistances[j]==i){
							cluster.add(meanArray.get(j).getLocation());
						}
					}
					
					meansNew[i]=centroid(cluster);
				}
				
				
				means=meansNew.clone(); 
			}
			
//			for (int j=0; j<means.length; j++){
//			System.out.println("Means: "+means[j].getX() + " "+means[j].getY()+" "+means[j].getZ() );
//			}
			minDistancesAll.add(minDistances);
			meansAll.add(means.clone());
			
			
		}
		
		//now must cluster the clusters!
		
//		for (int j=0; j<attempts; j++){
//			for (int i=0; i<meansAll.get(j).length; i++){
//				System.out.println("Cluster mean: attempts: "+j+ " X: "+meansAll.get(j)[i].getX() + " Y: " + meansAll.get(j)[i].getY() +" Z: "+ meansAll.get(j)[i].getZ());
//			}
//		}
		
		ArrayList<Point3d> clusterArray = new ArrayList<Point3d>();
		//now we have n attempts each with different means...first get rid of the NaN: 
		for (int i=0; i<meansAll.size(); i++){
			for (int j=0; j<settings.nKMeans; j++){
				if (Double.isNaN(meansAll.get(i)[j].getX())==false) clusterArray.add(meansAll.get(i)[j]);
			}
		}
	
		
//		///now have an array of cluster points. //sort them in order by X
//		Collections.sort(clusterArray, new SortPoint3dX());
	
		//we need to find the number of clusters. Here we have to make a 'decision', i.e. what we define as a cluster. This comes from a param in MCMC params- let the user decide. 
		ArrayList<Point3d> clusterSorted=new ArrayList<Point3d>();
		// group of corresponding clusters
		ArrayList<Point3d> clusternK;
		// remaining clusters
		ArrayList<Point3d> newClusterArray;
		// cluster to compare distances to. 
		Point3d referenceCluster; 
		Point3d finalCluster; 
		double x;
		double y;
		double z;
		
//		for (int k=0; k<clusterArray.size(); k++){
//			System.out.println("clusterArray: "+clusterArray.get(k).getX()+ " "+clusterArray.get(k).getY() + " "+ clusterArray.get(k).getZ());
//		}
		
		//there maybe three clusters, two corresponding the actual chain locations and one which accisdently clusters the entire dataset. 
		while (clusterArray.size()!=0){
			
//			if (clusterArray.size()==0){
//				continue;
//			}
			
			clusternK=	new ArrayList<Point3d>();
			newClusterArray=new ArrayList<Point3d>();
			
			referenceCluster=clusterArray.get(0);
				
			for (int i=0; i<clusterArray.size(); i++){
				//Check the distance of the clusters from each other and also check the min distance of that cluster from the chains, beceause, there is a chance a cluster will end up between two sets of chains. We want
				//to get rid of these. 
//				System.out.println("clusterArray: "+clusterArray.get(i).getX()+ " "+clusterArray.get(i).getY() + " "+ clusterArray.get(i).getZ());
				if (clusterArray.get(i).distance(referenceCluster)<settings.maxClusterSize) clusternK.add(clusterArray.get(i) );
				else newClusterArray.add(clusterArray.get(i));
				
			}
			
			if (clusternK.size()==0) continue; 
			
			//average clusters in clusternK
			x=0;
			y=0;
			z=0; 
			for (int n=0; n<clusternK.size(); n++){
				x+=clusternK.get(n).getX();
				y+=clusternK.get(n).getY();
				z+=clusternK.get(n).getZ();
			}
			
			finalCluster=new Point3d(x/clusternK.size(),y/clusternK.size(),z/clusternK.size());
			//add only if cluster is near enough some chains; 
			if (checkClusterDistance(finalCluster, meanArray)<Math.pow(settings.maxClusterSize,2)) 	clusterSorted.add(finalCluster);
		
			
			//the new array of clusters omits the clusters that have already been removed. 
			clusterArray=newClusterArray;
				
		}
		
//		for (int j=0; j<clusterSorted.size(); j++){
//			System.out.println("Final Cluster: "+clusterSorted.get(j).getX() + " "  +clusterSorted.get(j).getY()+ " " +clusterSorted.get(j).getZ());
//		}

		return clusterSorted; 
	}
	

	/**
	 * Checks the distance between clusterPoint and the closest chain. 
	 * @param clusterPoint
	 * @param meanArray
	 * @return
	 */
	private double checkClusterDistance(Point3d clusterPoint, ArrayList<MCMCTDResults> meanArray){
		
		
		double minDistance=Double.MAX_VALUE;
		double distance; 
		
		for (int i=0; i<meanArray.size(); i++){
			distance=meanArray.get(i).location.distance(clusterPoint);
			if (distance<minDistance){
				minDistance=distance; 
			}
		}
		
		System.out.println("minDistance: "+minDistance + "cluster Point: "+clusterPoint.getX()+ " "+clusterPoint.getY()+ " "+clusterPoint.getZ());
		return minDistance; 
		
	}

	
	/**
	 *Sorts chains by an array for clusters
	 * @param meanArray
	 * @param clusterSorted- Array of sorted clusters. It can be any size <=nk, not always nK if there are less clusters than nK.
	 * @return
	 */
	private ArrayList<MCMCTDResults> clusterChains(ArrayList<MCMCTDResults> meanArray, ArrayList<Point3d> clusterSorted) {
		
		ArrayList<MCMCTDResults> finalResults= new ArrayList<MCMCTDResults>();
		
		if (clusterSorted.size()==0){
			finalResults.add(averageChains(meanArray));
			return finalResults; 
		}
		
		int iVal;
		double distance;
		double minDistance; 
		int[] minDistances=new int[meanArray.size()]; 
		for (int i=0; i<meanArray.size(); i++){
			iVal=0; 
			minDistance=Double.MAX_VALUE;
			for (int j=0; j<clusterSorted.size(); j++){
				distance=meanArray.get(i).location.distance(clusterSorted.get(j));
				if (distance<minDistance){
					minDistance=distance; 
					iVal=j;
				}
			}
			minDistances[i]=iVal; 
		}		
		
		ArrayList<MCMCTDResults> resultsCluster;
		
		for (int i=0; i<clusterSorted.size(); i++){
			resultsCluster=new ArrayList<MCMCTDResults>();
			for (int j=0; j<meanArray.size(); j++){
				if (minDistances[j]==i){
					resultsCluster.add(meanArray.get(j));
				}
				
			}
			
			if (resultsCluster.size()!=0) finalResults.add(averageChains(resultsCluster));
			
		}

		return finalResults;
	}
	
	
	
	public MCMCTDResults averageChains(ArrayList<MCMCTDResults> data){
		
		 ArrayList<ArrayList<Point3f>> jumps=new ArrayList<ArrayList<Point3f>>();
		 
			double range=0;
			double x=0;
			double y=0;
			double z=0;
			double stdRange=0;
			double stdX=0;
			double stdY=0;
			double stdZ=0;
			double chi=0; 
			
			int n=data.size(); 

		 for (int i=0; i<data.size(); i++){
			 range+=data.get(i).getRange();
			 x+=data.get(i).location.getX();
			 y+=data.get(i).location.getY();
			 z+=data.get(i).location.getZ();
			 
			 stdRange+=data.get(i).getRangeError();
			 stdX+=data.get(i).getErrors().getX();
			 stdY+=data.get(i).getErrors().getY();
			 stdZ+=data.get(i).getErrors().getZ();
			 
			 chi+=data.get(i).getChi();
			 
			 jumps.add(data.get(i).getChainJumps().get(0));
			 
		 }
		 
		 MCMCTDResults averageChainResults=new MCMCTDResults();
		 
		 Point3d location =new Point3d(x/n, y/n, z/n );
		 Point3d errors=new Point3d(stdX/n, stdY/n, stdZ/n);
		 
		 averageChainResults.setLocation(location);
		 averageChainResults.setErrors(errors);
		 averageChainResults.setRange(range/n);
		 averageChainResults.setRangeError(stdRange/n);
		 
		 averageChainResults.setMeanChi(chi/n);
		 averageChainResults.setChainJumps(jumps);
		 
		 
		 return averageChainResults;
	}


//	public class SortPoint3dX implements Comparator<Point3d> {
//	    @Override
//	    public int compare(Point3d o1, Point3d o2) {
//	        return ((Double) o1.getX()).compareTo((Double) o2.getX());
//	    }
//	}
	
	/**
	 * Returns the centroid of a cluster of points; 
	 * @param cluster
	 * @return
	 */
	private Point3d centroid(ArrayList<Point3d> cluster){
		
		double x=0;
		double y=0;
		double z=0;
		
		for (int i=0; i<cluster.size(); i++){
			x+=cluster.get(i).getX();
			y+=cluster.get(i).getY();
			z+=cluster.get(i).getZ();
		}
		
		return new Point3d(x/(double) cluster.size(),y/(double) cluster.size(),z/(double) cluster.size());
	}
	
	
	/**
	 * Adds the cross correlation error to the time delay errors array. 
	 * @param tdErrors
	 * @param timeError
	 * @param sampleRate
	 * @return
	 */
	public static ArrayList<ArrayList<Double>> addCrossCorError(ArrayList<ArrayList<Double>> tdErrors, double timeError, float sampleRate){
		//add cross correlation error to time delay errors
		ArrayList<Double> timeDelayErCC;
		ArrayList<ArrayList<Double>> timeDelayErrors=new ArrayList<ArrayList<Double>>();
		for (int i=0; i<tdErrors.size(); i++){
			timeDelayErCC=new ArrayList<Double>();
			for (int j=0; j<tdErrors.get(i).size(); j++){
				timeDelayErCC.add(Math.sqrt(Math.pow(tdErrors.get(i).get(j),2)+Math.pow((timeError/sampleRate), 2)));
			}
			timeDelayErrors.add(timeDelayErCC);
		}
		return timeDelayErrors;
	}
	
	
	
	
	public void setTimeDelays(ArrayList<ArrayList<Double>> timeDelays){
		this.timeDelaysObs=timeDelays;
	}
	
	/**
	 * Note the sample rate must be set before this function. 
	 * @param timeDelayErrors
	 * @throws Exception 
	 */
	public void setTimeDelaysErrors(ArrayList<ArrayList<Double>> timeDelayErrors) {
		if (sampleRate<=0){
			return;
		}
		this.timeDelayErrors=addCrossCorError(timeDelayErrors, settings.timeError, sampleRate);
	}
	
	public void setHydrophonePos(ArrayList<ArrayList<Point3f>> hydrophonePos){
		this.hydrophoneArray=hydrophonePos;
	}
	
	public ArrayList<ArrayList<Point3f>> getJumps(){
		return jumps;
	}
	
	public ArrayList<ArrayList<Double>> getChiJumps(){
		return chiValues;
	}
	
	public ArrayList<MCMCTDResults> getResultsAllChains(){
		return results;
	}
	
	public ArrayList<MCMCTDResults> getResults(){
		return finalResults;
	}


	@Override
	public Boolean changeSettings() {
		MCMCParamsDialog.showDialog(null, settings);
		return true;
	}


	public MCMCParams getSettings() {
		return settings;
	}

	public void setSampleRate(float sampleRate) {
		this.sampleRate=sampleRate;
	}

	public void setSoundSpeed(double speedOfSound) {
			this.speedOfSound=speedOfSound;
	}
	
	/**
	 * Reduces the number of MCMC jumps in order to reduce memory usage when displaying probability distributions in Java3D. 
	 * @param jumps ArrayList of chain jumps to compress
	 * @param compressSize- the ideal number of points in the new array. Note: unless compressSize exactly divides the jump size then final arrays will be slightly larger than compressSize
	 * @return 
	 */
	public static ArrayList<ArrayList<Point3f>> compressMCMCResults(ArrayList<ArrayList<Point3f>> jumps, int  compressSize){
		ArrayList<ArrayList<Point3f>> jumpsCompressed=new ArrayList<ArrayList<Point3f>>(jumps.size());
		
		int skipSize;
		for (int i=0; i<jumps.size(); i++){
			ArrayList<Point3f> jumpsCompressTemp=new ArrayList<Point3f>(compressSize);
			//round down. 
			//System.out.println((double) jumps.get(i).size());
			//System.out.println(((double) compressSize));
			skipSize=(int) Math.floor(((double) jumps.get(i).size())/((double) compressSize));
			if (skipSize<=1){
				jumpsCompressTemp=jumps.get(i);
			}
			else{
				for (int j=0; j<jumps.get(i).size(); j=j+skipSize){
					if (j>=jumps.get(i).size()) continue;
					jumpsCompressTemp.add(jumps.get(i).get(j));
				}
			}
			jumpsCompressed.add(jumpsCompressTemp);
		}
		
		return jumpsCompressed;
	}
	

}
