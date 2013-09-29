package PamUtils;

import java.util.ArrayList;
import java.util.Collections;


public class StatMath{
	
	/**
	 * Calculate the mean of an array of double values, ignoring an 'initialtoIgnorePercentage' percentage of jumps
	 * @param array of float values.
	 * @param InitialtoIgnorePercentage.
	 * @return mean of the array values.
	 */
	 public static double mean(ArrayList<Double> array, double InitialtoIgnorePercentage){
		 
		 double MeanTotal=0;
		 int n=0;
		 int forStart=(int) Math.round((InitialtoIgnorePercentage/100)*array.size());

		 for (int i=forStart; i<array.size();i++){
			MeanTotal+= array.get(i);
			n++;
			}
		 
		 double mean=MeanTotal/n;
		 return mean;
	 }
	 
	 /**
	  * Calculate the standard deviation of an array of doubles, ignoring an 'initialtoIgnorePercentage' percentage of jumps
	  * @param array
	  * @param initialtoIgnorePercentage- percentage of initial values to ignore.
	  * @return
	  */
	 public static double std(ArrayList<Double> array, double initialtoIgnorePercentage){
		 double Std=0.0;
		 double MeanTotal=0.0;
		
		 double StandardDeviation;
		 int n=0;
		 int forStart=(int) Math.round((initialtoIgnorePercentage/100)*array.size());
		
		//work out the mean
		for (int i=forStart; i<array.size();i++){
			MeanTotal+=array.get(i);
			n++;
		}
		MeanTotal=MeanTotal/n;
		
		//calculate standard deviation
		for (int k=forStart;k<array.size(); k++){
			Std+=Math.pow((array.get(k)-MeanTotal),2);
		}
		
		//standard deviation
		StandardDeviation=Math.sqrt(Std/(n-1));
		 
		return StandardDeviation;
		 
	 }
	 

	/**
	 * Find the median of an array of doubles. 
	 * @param array
	 * @return median of the array. 
	 */
	public static  double median(ArrayList<Double> array){
		int Size=array.size();
		//this round about way of sorting is to stop 'sort' from sorting the matrices in the PreMarkovChain part.
		ArrayList<Double> ZSort=new ArrayList<Double>();
		for (int i=0; i<array.size(); i++){
			ZSort.add(array.get(i));
		}
		Collections.sort(ZSort);
		
		double Median=0;
		if (Size%2==0){
			double n1=ZSort.get(Size/2);		
			double n2=ZSort.get((Size/2)-1);
			Median=(n1+n2)/2;
		}
		else{
			Median=ZSort.get((int) ((Size/2)-0.5));
		}
		return Median;
	}
	

	/**
	 * Calculate the mean of an array of float values, ignoring an 'initialtoIgnorePercentage' percentage of jumps
	 * @param array of float values.
	 * @param InitialtoIgnorePercentage.
	 * @return mean of the array values.
	 */
	 public static double meanf(ArrayList<Float> array, double initialtoIgnorePercentage){
		 float MeanTotal=0;
		 int n=0;
		 int forStart=(int) Math.round((initialtoIgnorePercentage/100)*array.size());
		 
		 for (int i=forStart; i<array.size();i++){
			MeanTotal+= array.get(i);
			n++;
			}
		 double mean=MeanTotal/n;
		 return mean;
	 }
	 
	 /**
	  * Calculate the standard deviation of an array of float values, ignoring an 'initialtoIgnorePercentage' percentage of jumps
	  * @param array of float values.
	  * @param initialtoIgnorePercentage- percentage of initial values to ignore.
	  * @return standard deviation of the array values. 
	  */
	 public static double stdf(ArrayList<Float> array, double InitialtoIgnorePercentage){
		 double Std=0.0;
		 float MeanTotal=(float) 0.0;
		 int forStart=(int) Math.round((InitialtoIgnorePercentage/100)*array.size());
		
		 double StandardDeviation;
		 int n=0;
		 
		for (int i=forStart; i<array.size();i++){
			MeanTotal+=array.get(i);
			n++;
		}
		MeanTotal=MeanTotal/n;
		
		for (int k=forStart;k<array.size(); k++){
			Std+=Math.pow((array.get(k)-MeanTotal),2);
		}
		  StandardDeviation=Math.sqrt(Std/(n-1));
		 return StandardDeviation;
		 
	 }
	 


}
