package userDisplay;

import java.awt.Rectangle;
import java.io.Serializable;

abstract public class UserFrameParameters implements Cloneable, Serializable{

	public Rectangle boundingRectangle = new Rectangle();
	
//	@Override
//	protected UserFrameParameters clone()  {
//		try {
//			return (UserFrameParameters) super.clone();
//		}
//		catch (CloneNotSupportedException Ex) {
//			Ex.printStackTrace()
//		}
//		return null;
//	}

}
