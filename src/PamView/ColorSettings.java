package PamView;

import java.io.Serializable;

public class ColorSettings implements Serializable {

		static public final long serialVersionUID = 1;
		
		boolean nightTime;

		public boolean isNightTime() {
			return nightTime;
		}

		public void setNightTime(boolean nightTime) {
			this.nightTime = nightTime;
		}
		
}
