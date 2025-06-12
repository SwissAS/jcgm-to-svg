package com.jpprade.jcgmtosvg;


import com.jpprade.jcgmtosvg.commands.PolyBezierV2;
import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;

import net.sf.jcgm.core.BeginApplicationStructure;
import net.sf.jcgm.core.BeginFigure;
import net.sf.jcgm.core.CGM;
import net.sf.jcgm.core.CGMDisplay;
import net.sf.jcgm.core.Command;
import net.sf.jcgm.core.EdgeColour;
import net.sf.jcgm.core.EdgeWidth;
import net.sf.jcgm.core.EndApplicationStructure;
import net.sf.jcgm.core.EndFigure;
import net.sf.jcgm.core.FillColour;
import net.sf.jcgm.core.LineColour;
import net.sf.jcgm.core.LineWidth;
import net.sf.jcgm.core.PolyBezier;
import net.sf.jcgm.core.RectangleElement;
import net.sf.jcgm.core.RestrictedText;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;


public class CGM4SVG extends CGM {

	static final String RESTRICTED_TEXT_HOTSPOT_ID = "rt_";

	static final String OPTION_HOTSPOT_ENABLED = "hotSpotEnabled";
	static final String OPTION_HOTSPOT_IN_APPLICATION_STRUCTURE_ONLY = "hotSpotInApplicationStructureOnly";
	static final String OPTION_HOTSPOT_PADDING = "hotSpotPadding";
	static final String OPTION_HOTSPOT_REGEX = "hotSpotRegex";
	static final String OPTION_HOTSPOT_LINK = "hotSpotLink";
	static final String OPTION_HOTSPOT_COLOR = "hotSpotColor";
	
	SVGPainter painter;

	Map<String,Object> options = new HashMap<String,Object>();
	
	private final Stack<BeginApplicationStructure> basStack = new Stack<>();
	
	private final HashMap<BeginApplicationStructure, PaintHolder> mapping = new HashMap<>();
	
	private BeginFigure currentFigure = null;
	
	private final ConcurrentHashMap<BeginFigure, List<PolyBezierV2>> figurePolyBezier = new ConcurrentHashMap<>();

	public CGM4SVG(InputStream is, SVGPainter painter, Map<String,Object> options) throws IOException {
		super();
		this.painter = painter;
		if(options != null){
			this.options = options;
		}
		DataInputStream in = new DataInputStream(new BufferedInputStream(is));
		read(in);
		in.close();
	}
	
	public CGM4SVG(File cgmFile, SVGPainter painter) throws IOException {
		super(cgmFile);
		this.painter = painter;
	}
	
	@Override
	public void paint(CGMDisplay d) {
		int restrictedTextCounter = 0;
		for (Command c : getCommands()) {
			if (c == null) {
				continue;
			}
			
			BeginApplicationStructure currentAPS;
			switch (c) {
 				case RestrictedText rt -> {
					//Draw hotSpot on Restricted text if hotSpotEnabled option is set to true
					if(options.get(OPTION_HOTSPOT_ENABLED) != null && (boolean) options.get(OPTION_HOTSPOT_ENABLED) == true) {
						drawRestrictedTextHotspot(d,rt,RESTRICTED_TEXT_HOTSPOT_ID+restrictedTextCounter);
						restrictedTextCounter++;
					}
					rt.paint(d);
				}
				case BeginApplicationStructure bas -> {
					c.paint(d);
					
					currentAPS = bas;
					this.basStack.add(currentAPS);
					PaintHolder ph = new PaintHolder();
					ph.setApsid(currentAPS.getIdentifier());
					this.mapping.put(currentAPS, ph);
				}
				case BeginFigure bf -> {
					c.paint(d);
					this.currentFigure = bf;
					this.figurePolyBezier.put(this.currentFigure, new ArrayList<>());
				}
				case EndApplicationStructure ignored1 -> {
					c.paint(d);
					
					this.basStack.pop();
					if (!this.basStack.empty()) {
						this.basStack.peek();
					}
				}
				case EndFigure ignored2 -> {
					List<PolyBezierV2> toPaint = this.figurePolyBezier.get(this.currentFigure);
					
					if (!toPaint.isEmpty()) {
						PolyBezierV2 c2 = mergePB(toPaint);
						
						if (!this.basStack.isEmpty()) {
							BeginApplicationStructure top = this.basStack.peek();
							
							c2.paint(d, this.currentFigure,
									this.mapping.get(top).getCurrentFC(),
									this.mapping.get(top).getCurrentEC(),
									this.mapping.get(top).getCurrentEW());
							
						} else {
							c2.paint(d, this.currentFigure);
							
						}
					}
					this.currentFigure = null;
				}
				// all polybezier will be merged into a single shape
				default -> {
					switch (c) {
						case PolyBezier pb -> {
							PolyBezierV2 c2 = new PolyBezierV2(pb);
							if (!this.basStack.isEmpty()) {
								BeginApplicationStructure top = this.basStack.peek();
								if (this.currentFigure == null) {
									c2.paint(d, null,
											this.mapping.get(top).getCurrentFC(),
											this.mapping.get(top).getCurrentEC(),
											this.mapping.get(top).getCurrentEW());
								} else {
									this.figurePolyBezier.get(this.currentFigure).add(c2); // all polybezier will be merged into a single shape
								}
							} else {
								if (this.currentFigure == null) {
									c2.paint(d, null);
								} else {
									this.figurePolyBezier.get(this.currentFigure).add(c2);
								}
							}
						}
						case FillColour fc -> {
							if (!this.basStack.isEmpty()) {
								BeginApplicationStructure top = this.basStack.peek();
								this.mapping.get(top).setCurrentFC(fc);
							}
							c.paint(d);
						}
						case EdgeColour ec -> {
							if (!this.basStack.isEmpty()) {
								BeginApplicationStructure top = this.basStack.peek();
								this.mapping.get(top).setCurrentEC(ec);
							}
							c.paint(d);
						}
						case EdgeWidth ew -> {
							if (!this.basStack.isEmpty()) {
								BeginApplicationStructure top = this.basStack.peek();
								this.mapping.get(top).setCurrentEW(ew);
							}
							c.paint(d);
						}
						case LineColour lc -> {
							if (!this.basStack.isEmpty()) {
								BeginApplicationStructure top = this.basStack.peek();
								this.mapping.get(top).setCurrentLC(lc);
							}
							c.paint(d);
						}
						case LineWidth lw -> {
							if (!this.basStack.isEmpty()) {
								BeginApplicationStructure top = this.basStack.peek();
								this.mapping.get(top).setCurrentLW(lw);
							}
							c.paint(d);
						}
						case RectangleElement re -> {
							if (d.isWithinApplicationStructureBody()) {
								// FIXME: Airbus provides CGMs where rectangles are printed for the entire size of the screen
								//  in the content of an APS; we therefore skip it for the moment
								//  → get rid of this workaround once the "ApplicationStructure"-related commands are supported; see related to do for CGMDisplay#isWithinApplicationStructureBody in jcgm-core
								continue;
							} 
							c.paint(d);
						}
						default -> {
							c.paint(d);
						}
					}
				}
			}
		}
	}
	
	private PolyBezierV2 mergePB(List<PolyBezierV2> tomerge) {
		PolyBezierV2 ret = tomerge.getFirst();
		if (tomerge.size() == 1) {
			return ret;
		}
		for (int i = 1; i < tomerge.size(); i++) {
			ret.mergeShape(tomerge.get(i));
		}
		return ret;
	}

	private Rectangle2D.Double addPadding(CGMDisplay d, Rectangle2D.Double shape) {
		AffineTransform cgmTransform = d.getGraphics2D().getTransform();
		if(options.get(OPTION_HOTSPOT_PADDING) != null) {
			Double paddingX = (Double) options.get(OPTION_HOTSPOT_PADDING) / Math.abs(cgmTransform.getScaleX());
			Double paddingY = (Double) options.get(OPTION_HOTSPOT_PADDING) / Math.abs(cgmTransform.getScaleX());
			shape.setFrame(shape.x - paddingX, shape.y - paddingY, 
			shape.width + 2 * paddingX, shape.height + 2 * paddingY);
		}
		return shape;
	}

	private Rectangle2D.Double addOffset(CGMDisplay d, Rectangle2D.Double shape, Point2D offset) {
		shape.setFrame(shape.x+offset.getX(),shape.y-offset.getY(),shape.width,shape.height);
		return shape;
	}

	private String getApsID(RestrictedText rt) {
		boolean isWithinApplicationStructure = !this.basStack.isEmpty();

		//hotSpotInApplicationStructureOnly option filter the Restricted text commands
		//will put hotSpot only for Restricted text within an ApplicationStructure when option is set to true.
		if(options.get(OPTION_HOTSPOT_IN_APPLICATION_STRUCTURE_ONLY) != null &&
		 (boolean) options.get(OPTION_HOTSPOT_IN_APPLICATION_STRUCTURE_ONLY) == true &&
		  !isWithinApplicationStructure) {
			return null;
		}

		if(isWithinApplicationStructure){
			BeginApplicationStructure top = this.basStack.peek();
			return top.getIdentifier();
		}

		return rt.getText();
	}

	private void drawRestrictedTextHotspot(CGMDisplay d, RestrictedText rt, String id) {

		String apsid = getApsID(rt);

		if(apsid == null) {
			return;
		}
	
		String text = rt.getText();
		//apply a regex to select specific restricted text to draw hotSpot on. 
		//hotSpotRegex contains the regex String.
		if(options.get(OPTION_HOTSPOT_REGEX) == null || text.matches((String) options.get(OPTION_HOTSPOT_REGEX))){
			
			Rectangle2D.Double shape = rt.getTextBox();
			//apply Restricted Text offset on the hotSpot shape
			Point2D offset = rt.getTextOffset(d);
			shape = addOffset(d,shape,offset);	
			//add padding to rectangle hotSpot shape if hotSpotPadding is set (double value)
			shape = addPadding(d,shape);						

			SVGGraphics2DHS g2d = (SVGGraphics2DHS) d.getGraphics2D();
			g2d.drawHotSpot(shape, id, apsid, text, (String)options.get(OPTION_HOTSPOT_LINK), (String)options.get(OPTION_HOTSPOT_COLOR));
		}	
	}

}
