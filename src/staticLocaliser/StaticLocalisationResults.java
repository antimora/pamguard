package staticLocaliser;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import PamDetection.AbstractLocalisation;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;

/**
 * Store the results for static localisation
 * <p>
 * (PamDataUnit)
 *  
 * 
 * @author spn1
 *
 */
public class StaticLocalisationResults extends PamDataUnit{
	
	//SAVABLE FIELDS
	private Long time; 
	private String algorithmName;
	//loc 0-x 1-y 2-z(x=East,y=North,z=height)
	private Double[] location=new Double[3];
	private Double range;
	//errors arranged as locs
	private Double[] errors=new Double[3];
	private Double rangeError;
	private Double chi;
	private Integer referenceHydrophone;
	private LatLong beamLatLong;
	private Double probability;
	private Integer nDegreesFreedom;
	private Double aic;
	private int ambiguity=0;
	private int timeDelay=0;
	private Integer tDPos;
	
	
	//WORKING FIELDS
	private AbstractStaticLocaliserAlgorithm algorithm;
	private ArrayList<ArrayList<Point3f>> jumps;
	private Long runTimeMillis;
	
	
	public StaticLocalisationResults(Long time, AbstractStaticLocaliserAlgorithm algorithm) {
		super(time);
		this.time=time;
		this.algorithm=algorithm;
		algorithmName=algorithm.getName();
	}
	
	public Long getTimeMillis(){
		return time;
	}
	
	public void setTimeMillis(Long time){
		this. time=time;
	}

	
	public String getAlgorithmName() {
		return algorithmName;
	}
	
	
	public String getDataBlockName(){
		return "";
	}
		 
	
	// For clustering algorithms. In many cases there exists degeneracy in a result i.e. there are multiple possibilities for a set of time delays.
	//If an algorithm utilises a clustering algorithm then it can return multiple results, these have to be tagged with a different ambiguity value. Note
	//this is not the same as time delay possibilities. This is one set of time delays creating multiple possibilities. 
	public int getAmbiguity(){
		return ambiguity;
	}
	
	public void setAmbiguity(int ambiguity){
		this.ambiguity=ambiguity;
	}
	
	//localisation//
	public void setX(double x){
		location[0]=x;
	}
	
	public void setY(double y){
		location[1]=y;
	}
	
	public void setZ(double z){
		location[2]=z;
	}
	
	public Double getX(){
		return location[0];
	}
	
	public Double getY(){
		return location[1];
	}
	
	public Double getZ(){
		return location[2];
	}
	
	public Double getDepth(){
		return -location[2];
	}
	
	public void setRange(Double range) {
		this.range=range;	
	}
	
	public Double getRange() {
		return range;	
	}
	
	public Double[] getlocalisation(){
		return location;
	}
	
	//localisation errors
	public void setXError(double x){
		errors[0]=x;
	}
	
	public void setYError(double y){
		errors[1]=y;
	}
	
	public void setZError(double z){
		errors[2]=z;
	}
	
	public void setRangeError(Double rangeError) {
		this.rangeError=rangeError;	
	}
	
	public Double getRangeError() {
		return rangeError;	
	}
	
	
	public Double getXError(){
		return errors[0];
	}
	
	public Double getYError(){
		return errors[1];
	}
	
	public Double getZError(){
		return errors[2];
	}
	
	public 	Double getDepthError(){
		return errors[2];
	}
	
	public Double[] getlocalisationError(){
		return errors;
	}
	
	
	//MCMC specific
	public void setJumps(ArrayList<ArrayList<Point3f>> jumps){
		this.jumps= jumps;
	}
	
	public ArrayList<ArrayList<Point3f>> getJumps(){
		return jumps;
	}
	
	//SimplexSpecific
	
	//LeastSquares Specifc
	
	
	
	public Double  getChi2( ){
		return chi;
	}
	
	public Integer getReferenceHydrophone(){
		return referenceHydrophone;
	}
	
	public void setReferenceHydrophone(Integer referenceHydrophone){
		this.referenceHydrophone=referenceHydrophone;
	}
	
	//Stats
	public void setChi2(double chi){
		this.chi=chi;
	}
	public void setProbability(Double prob) {
		this.probability=prob;
	} 
	
	public Double getProbability() {
		return probability;
	}
	
	//usually the  number of time delays?
	public void setnDegreesFreedom(Integer nDegreesFreedom) {
		this.nDegreesFreedom=nDegreesFreedom;
	}
	
	public Integer getnDegreesFreedom() {
		return nDegreesFreedom;
	}

	public void setAic(Double aic) {
		// TODO Auto-generated method stub
		this.aic=aic;
	}

	public Double getAic() {
		// TODO Auto-generated method stub
		return aic;
	}


	//Algorithm info
	
	public void setAlgorithm(AbstractStaticLocaliserAlgorithm algorithm){
		this.algorithm=algorithm;
	}
	
	public AbstractStaticLocaliserAlgorithm getAlgorithm(){
		return this.algorithm;
	}

	
	//Real world Co-Ordinates and data 
	public LatLong getLatLong() {
		return beamLatLong;
	}

	
	//Performance Info
	public Long getRunTimeMillis() {
		return runTimeMillis;
	}
	
	public void setRunTimeMillis(Long runTime) {
		this.runTimeMillis=runTime;
	}
	
	/**
	 * The time delay out of the group of time delays which was used for this result. 
	 * @return
	 */
	public Integer getTimeDelay() {
		return timeDelay;
	}


	public void setTimeDelay(Integer tdSel) {
		this.timeDelay=tdSel;
		
	}
	
	/**
	 * How many possible time delays there were for this data unit
	 *
	 */
	public Integer getNTimeDelayPossibilities() {
		return tDPos;
	}
	
	public void setNTimeDelayPossibilities(Integer tDPos) {
		this. tDPos=tDPos;
	}
	

}
