package com.jpprade.jcgmtosvg;

import com.jpprade.jcgmtosvg.extension.SVGGraphics2DHS;
import net.sf.jcgm.core.CGMDisplay;
import net.sf.jcgm.core.Command;
import net.sf.jcgm.core.ScalingMode;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.batik.svggen.SVGSyntax;
import org.apache.batik.util.SVGConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGSVGElement;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JcgmToSvg {
	
	private static final Logger logger = LoggerFactory.getLogger(JcgmToSvg.class);
	
	/**
	 * Converts a single CGM to an SVG
	 *
	 * @param is the input stream of the CGM
	 * @param os an output stream of the converted SVG
	 */
	public static void convert(InputStream is, OutputStream os) throws SVGGraphics2DIOException {
		convert(is,os,new HashMap<>());
	}

	/**
	 * Converts a single CGM to an SVG
	 *
	 * @param is the input stream of the CGM
	 * @param os an output stream of the converted SVG
	 * @param options Map<String, Object> that contains the conversion options:
	 * - hotSpotEnabled: Enabled RestrictedText hotSpot feature
	 * - hotSpotInApplicationStructureOnly: Enabled RestrictedText hotSpot feature and filter it on ApplicationSTructures only
	 * - hotSpotPadding: Add padding to the hotSpot box in px related to the cgm default size
	 * - hotSpotRegex: Filter Restricted text hotspots based on a regular expression
	 * - hotSpotLink: Add a specific link to the Restricted text hotspot
	 * - hotSpotColor: Specify the color of the hotSpot (format: rgba(r, g, b, a))
	 */
	public static void convert(InputStream is, OutputStream os, Map<String, Object> options) throws SVGGraphics2DIOException {
		logger.info("Start of CGM file to SVG conversion with the options:{}.", options.toString());
		// Get a DOMImplementation.
		DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
		
		// Create an instance of org.w3c.dom.Document.
		Document document = domImpl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
		
		SVGPainter svgPainter = new SVGPainter();
		
		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
		
		CGM4SVG cgm;
		try {
			cgm = loadCgm(is, svgPainter, options);
		} catch (Exception e) {
			logger.error("Error while converting CGM to SVG" + ", " + e.getMessage(), e);
			throw new JcgmToSvgException("Error while converting the CGM to SVG", e.getCause());
		}
		
		if (cgm == null) {
			throw new JcgmToSvgException("Could not load the CGM");
		}
		
		double scale = findScale(cgm);
		if (scale > 0 && scale <= 0.0001) {
			ctx.setPrecision(8);
			logger.info("Precision 8 {}", scale);
		} else if (scale > 0.0001 && scale < 0.01) {
			ctx.setPrecision(4);
			logger.info("Precision 4 {}", scale);
		} else {
			ctx.setPrecision(4);
		}
		
		CDATASection styleSheet = document.createCDATASection("");
		
		// Create an instance of the SVG Generator.
		SVGGraphics2D svgGenerator = new SVGGraphics2DHS(ctx, false);
		
		paint2(svgGenerator, cgm);
		
		svgGenerator.setSVGCanvasSize(cgm.getSize());
		
		Element root = createrCss(document, styleSheet, svgGenerator);
		
		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		Writer out = new OutputStreamWriter(os, StandardCharsets.UTF_8);
		svgGenerator.stream(root, out, useCSS, false);
		
		logger.info("End of CGM file to SVG conversion.");
	}
	
	/**
	 * Converts a single CGM file to an SVG.
	 *
	 * @param fileInput       path to the CGM file
	 * @param directoryOutput path to the SVG directory output
	 * @return the SVG file
	 * @throws IOException
	 */
	public static File convert(String fileInput, String directoryOutput) throws IOException {
		return convert(fileInput, directoryOutput, new HashMap<>());
	}
	
	public static File convert(String fileInput, String directoryOutput, Map<String, Object> options) throws IOException {
		File file = new File(fileInput);

		String fname = getFilenameWithoutExtension(new File(fileInput));
		File dout = new File(directoryOutput);
		File outf = new File(dout.getAbsolutePath() + "/" + fname + ".svg");
		FileOutputStream fos = new FileOutputStream(outf);

        try (InputStream inputStream = new FileInputStream(file)) {
			convert(inputStream, fos, options);           
        } catch (IOException e) {
            logger.error("An error occured during the conversion: {}", e);
        }
		
		return outf;
	}
	
	
	private static double findScale(CGM4SVG cgm) {
		List<Command> commands = cgm.getCommands();
		for (Command c : commands) {
			if (c instanceof ScalingMode sm) {
				return sm.getMetricScalingFactor();
			}
		}
		return 0;
	}
	
	private static Element createrCss(Document document, CDATASection styleSheet, SVGGraphics2D svgGenerator) {
		// Add a stylesheet to the definition section.
		SVGSVGElement root = (SVGSVGElement) svgGenerator.getRoot();
		
		Element defs = root.getElementById(SVGSyntax.ID_PREFIX_GENERIC_DEFS);
		Element style = document.createElementNS(SVGConstants.SVG_NAMESPACE_URI, SVGConstants.SVG_STYLE_TAG);
		style.setAttributeNS(null, SVGConstants.SVG_TYPE_ATTRIBUTE, "text/css");
		style.appendChild(styleSheet);
		defs.appendChild(style);
		styleSheet.appendData("svg { fill-rule: evenodd;pointer-events: none;}");
		
		styleSheet.appendData(".hotspot { cursor: pointer;pointer-events: all;}");
		styleSheet.appendData("@keyframes blink {100%,0% {fill: transparent;}60% {fill: #f00;}}.hotspotBlink {animation: blink 0.25s 3;}");
		//-----------JS
		
		Element javascript = document.createElement(SVGConstants.SVG_SCRIPT_TAG);
		javascript.setAttribute("id", "nativeJSHS");
		defs.appendChild(javascript);
		
		CDATASection javascriptData = document.createCDATASection("");
		javascript.appendChild(javascriptData);
		
		javascriptData.appendData("function clickHS(apsid){var apselement = document.getElementById(apsid);apselement.classList.add('hotspotBlink');setTimeout(function(){apselement.classList.remove('hotspotBlink');},750);}");
		
		return root;
	}
	
	private static CGM4SVG loadCgm(InputStream is, SVGPainter svgPainter, Map<String, Object> options) {
		CGM4SVG cgm;
		try {
			cgm = new CGM4SVG(is, svgPainter, options);
		} catch (IOException e) {
			logger.error("Error while loading the CGM from the input stream: " + e.getMessage(), e);
			return null;
		}
		return cgm;
	}
	
	public static void paint2(Graphics2D g2d, CGM4SVG cgm) {
		final CGMDisplay display = new CGMDisplay4SVG(cgm);
		Dimension size = cgm.getSize();
		int width = size.width;
		int height = size.height;
		display.scale(g2d, width, height);
		display.paint(g2d);
	}
	
	static String getFilenameWithoutExtension(File file) throws IOException {
		String filename = file.getCanonicalPath();
		String filenameWithoutExtension;
		if (filename.contains("."))
			filenameWithoutExtension = filename.substring(filename.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1, filename.lastIndexOf('.'));
		else
			filenameWithoutExtension = filename.substring(filename.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1);
		
		return filenameWithoutExtension;
	}
}

