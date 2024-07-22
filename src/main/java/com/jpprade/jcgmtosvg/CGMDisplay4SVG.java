package com.jpprade.jcgmtosvg;

import net.sf.jcgm.core.CGM;
import net.sf.jcgm.core.CGMDisplay;
import net.sf.jcgm.core.DashType;

public class CGMDisplay4SVG extends CGMDisplay {
	
	public CGMDisplay4SVG(CGM cgm) {
		super(cgm);
		this.lineDashes.put(DashType.SOLID, new float[]{100, 0}); // solid
		this.lineDashes.put(DashType.DASH, new float[]{2, 1}); // dash
		this.lineDashes.put(DashType.DOT, new float[]{0.01f, 0.6f}); // dot
		this.lineDashes.put(DashType.DASH_DOT, new float[]{2f, 1, 0.5f, 1}); // dash-dot
		this.lineDashes.put(DashType.DASH_DOT_DOT, new float[]{2, 1, 0.5f, 1, 0.5f, 1}); // dash-dot-dot
		//seems visualy good :
		this.lineDashes.put(DashType.SINGLE_ARROW, new float[]{10, 0});
		this.lineDashes.put(DashType.STITCH_LINE, new float[]{2, 2});
		this.lineDashes.put(DashType.CENTER_LINE, new float[]{4, 1, 1.3f, 1}); //  https://www.aircraftsystemstech.com/2019/11/lines-and-drawing-symbols-aircraft.html
	}
}
