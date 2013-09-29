package PamGraph3D.spectrogram3D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3b;
import javax.vecmath.Point3d;

import PamView.ColourArray;
import PamView.ColourArray.ColourArrayType;
import PamView.PamColors.PamColor;
import PamView.PamPanel;

/**
 * Produces a 3D view of a spectrogram. Requires FFT data in ArrayList<ArrayList<Format>. The option of creating a 2D or 3D spectrogram exists. The 3D spectrogram undergoes a compression algorithm to 
 * increase the speed of the 3D display. 
 * @author Jamie Macaulay
 */
public class Surface3D extends Shape3D {
	
	private static final long serialVersionUID = 1L;

	ArrayList<ArrayList<Float>> surfaceData;
	
	Point3d[] points;
	Color3b[] colours;
	
	double SpectrogramSizex=1;
	double SpectrogramSizey=1;
	
	double xlength; //time
	double ylength; //freq
	
	double    x1,x2,x3,x4;
    double    y1,y2,y3,y4;
    double    z1,z2,z3,z4;
    
    boolean threeD;
	
	
	/**
	 * 
	 * @param surfaceData-	NORMALISED data
	 * @param threeD
	 */
	public Surface3D(ArrayList<ArrayList<Float>> surfaceData, boolean threeD){
		
		
		this.surfaceData=surfaceData;
		this.threeD=threeD;
		 generatePamColours(ColourArrayType.GREY);
		this.setAppearance(surfaceSpectroAppearance());
	    this.setGeometry  (createSpectroSurface(threeD));
	   
	    
	}
	

	
	public Surface3D(ArrayList<ArrayList<Float>> FFTData, boolean threeD, double xSize, double ySize, ColourArrayType colours) {
		this.SpectrogramSizex=xSize;
		this.SpectrogramSizey=ySize;
		this.surfaceData=FFTData;
		this.threeD=threeD;
		generatePamColours(colours);
		this.setAppearance(surfaceSpectroAppearance());
	    this.setGeometry  (createSpectroSurface(threeD));
	}
	

	/**
	 * Generate a spectrogram of random data/noise. This is purely for testing purposes
	 * @param bins
	 * @param fftSize
	 * @return. 2D ArrayList of random float values.
	 */
	public ArrayList<ArrayList<Float>> generateTestData(int bins, int fftSize){
		
		ArrayList<ArrayList<Float>> specData=new ArrayList<ArrayList<Float>>();
		
		for (int i=0; i<bins; i++){
		ArrayList<Float> fftData=new ArrayList<Float>();
			for (int j=0; j<fftSize; j++){
				Double rand=Math.random();
				fftData.add(rand.floatValue());
			}
			specData.add(fftData);
		}
		
		return specData;
	}
	
	
	  private Color3b[] colors = new Color3b[256];
	  
	  /**
	   * Generate colour gradients for the spectrogram;
	   */
	   private void generateColors() {
	        for (int i=0;i<256;i++) {
	        	Integer iI=i;
	        	  colors[i] = new Color3b(iI.byteValue(),iI.byteValue(),iI.byteValue());
	        
	        }
	    }
	   
	   private void generatePamColours(ColourArrayType colourType){
		  ColourArray colourArray = ColourArray.createStandardColourArray(256, colourType);
		  Color[] colours= colourArray.getColours();
		  for (int i=0;i<colours.length;i++) {
			  Integer r=colours[i].getRed();
			  Integer g=colours[i].getGreen();
			  Integer b=colours[i].getBlue();
			  colors[i] = new Color3b(r.byteValue(),g.byteValue(),b.byteValue());
		  } 
	   }
	
	/**
	 * Convert the FFT data to a 3D surface;
	 * 
	 * @return QuadArray, either flat or 3D 
	 */
	   public QuadArray createSpectroSurface(boolean threeD){
		   
		  	QuadArray spectroSurf = null;
		
			int freqbins=surfaceData.get(0).size();
			int timebins=surfaceData.size();
			int count=0;
			
			int numberofPoints=(freqbins-1)*(timebins-1)*4;
						
			points   = new Point3d[numberofPoints];
			colours  = new Color3b[numberofPoints];
			 
			xlength=SpectrogramSizex/timebins;
			ylength=SpectrogramSizey/freqbins;
			
		
			for (int i=1; i<surfaceData.size(); i++){
				for (int j=1; j<surfaceData.get(0).size(); j++){
					
					x1=x2=	(i-1)*xlength-SpectrogramSizex/2;
					x3=x4=	i*xlength-SpectrogramSizex/2;
					y1=y4=	(j-1)*ylength-SpectrogramSizey/2;
					y2=y3=	j*ylength-SpectrogramSizey/2;
					z1=		surfaceData.get(i-1).get(j-1);
					z2=		surfaceData.get(i-1).get(j);
					z3=		surfaceData.get(i).get(j);
					z4=		surfaceData.get(i).get(j-1);
					
					colours[count]=colors[(int) Math.round(z1*(colors.length-1))];
					colours[count+1]=colors[(int) Math.round(z2*(colors.length-1))];
					colours[count+2]=colors[(int) Math.round(z3*(colors.length-1))];
					colours[count+3]=colors[(int) Math.round(z4*(colors.length-1))];
					
					if (threeD==true){
					z1=z1*(SpectrogramSizey/2);
					z2=z2*(SpectrogramSizey/2);
					z3=z3*(SpectrogramSizey/2);
					z4=z4*(SpectrogramSizey/2);
					}
					else{
						z1=z2=z3=z4=-0.4;
					}
					
					points[count]=new Point3d(x1,y1,z1);
					points[count+1]=new Point3d(x2,y2,z2);
					points[count+2]=new Point3d(x3,y3,z3);
					points[count+3]=new Point3d(x4,y4,z4);
					count=count+4;
					
				}
			}
			
			if (threeD==true){
				//System.out.println("Points Length Original: "+points.length);
				compress(points,colours);
			//System.out.println("Points Length Compressed: "+points.length);
				spectroSurf = new QuadArray (points.length,GeometryArray.COORDINATES | GeometryArray.COLOR_3);
				spectroSurf.setCoordinates(0, points);
				spectroSurf.setColors(0, colours);
			}
				
			else{
				//System.out.println("Points Length: "+points.length);
				spectroSurf = new QuadArray (points.length,GeometryArray.COORDINATES | GeometryArray.COLOR_3);
				spectroSurf.setCoordinates(0, points);
				spectroSurf.setColors(0, colours);
			}
			
			//here we can include an algorithm to reduce the number of points
			//System.out.println("Count: "+count);
			//System.out.println("Points length: "+points.length);
		
			return spectroSurf;
		}
	   
	   /**
	    * Use this function to reduce the number of polygons by around half for large wire frame 3D spectrograms. Note do use for 2D spectrograms as it will appear half the 
	    * data is missing. Significantly increases the performance of large 3D spectrograms
	    * 
	    * @return Point3d and Color3d 
	    */
	   public void compress(Point3d[] points, Color3b[] colours){
		   int count=0;
		   int sel=0;
		   ArrayList<Point3d> pointssel=new ArrayList<Point3d>();
		   ArrayList<Color3b> colourssel=new ArrayList<Color3b>();
		   
		   int N=0;
		   
		   int fftN=surfaceData.get(0).size();
		   int specN=surfaceData.size();
		   
			for (int i=1; i<specN; i++){
				for (int j=1; j<fftN; j++){
			   
					if (i>1 && i<specN && j>1 && j<fftN){
						
						if (j % 2 == N){
							pointssel.add(points[count]);
							pointssel.add(points[count+1]);
							pointssel.add(points[count+2]);
							pointssel.add(points[count+3]);
							colourssel.add(colours[count]);
							colourssel.add(colours[count+1]);
							colourssel.add(colours[count+2]);
							colourssel.add(colours[count+3]);
							sel=sel+4;
							
						}
					}
					else{
							pointssel.add(points[count]);
							pointssel.add(points[count+1]);
							pointssel.add(points[count+2]);
							pointssel.add(points[count+3]);
							colourssel.add(colours[count]);
							colourssel.add(colours[count+1]);
							colourssel.add(colours[count+2]);
							colourssel.add(colours[count+3]);
							sel=sel+4;
					}
					
			   count=count+4;
			   
			   }  
				N=1-N;
		   }
			
			Point3d[] pointsel=new Point3d[pointssel.size()];
			Color3b[] colorssel=new Color3b[pointssel.size()];
		   
			for (int n=0; n<pointssel.size(); n++){
				pointsel[n]=pointssel.get(n);
				colorssel[n]=colourssel.get(n);
			}
			
			this.points=pointsel;
			this.colours=colorssel;
	   }
	   
	   public void setSpectrogramY(double Y){
		   this.SpectrogramSizey=Y;
		   createSpectroSurface(threeD);
		   this.setGeometry  (createSpectroSurface(threeD));
		   
	   }
	   
	   public void setSpectrogramX(double X){
		   this.SpectrogramSizex=X;
		   createSpectroSurface(threeD);
		   this.setGeometry  (createSpectroSurface(threeD));
		   
	   }
	   
	   public void setColor(ColourArrayType colour){
		   generatePamColours(colour);
		   createSpectroSurface(threeD);
	   }
	   
	   
	   
	   private Appearance surfaceSpectroAppearance () {

	        Appearance appearance = new Appearance();

	        PolygonAttributes polyAttrib = new PolygonAttributes();
	        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);

	  
	        appearance.setPolygonAttributes(polyAttrib);
	        
	        TransparencyAttributes transparencyAttrib;
	        
		if (threeD == false) {
	        polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		    appearance.setPolygonAttributes(polyAttrib);
		   // transparencyAttrib = new TransparencyAttributes(TransparencyAttributes.FASTEST,0.25f);
		    //appearance.setTransparencyAttributes(transparencyAttrib);
		}

		else {	    
		    polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	   	    LineAttributes lineAttrib = new LineAttributes(1.0f,LineAttributes.PATTERN_SOLID,false);
	   	    appearance.setLineAttributes(lineAttrib);
	   	    transparencyAttrib = new TransparencyAttributes(TransparencyAttributes.FASTEST,0.25f);
		    appearance.setTransparencyAttributes(transparencyAttrib);
	  	}

	        return appearance;
	    }
	
	
	
	

	
	

}
