package com.jpprade.jcgmtosvg;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SVGUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(SVGUtils.class);
	
	private static final String TRANSFORM = "transform";
	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";
	
	private final DecimalFormat dfCoo;
	
	public SVGUtils() {
		DecimalFormat df = new DecimalFormat("0.00");
		this.dfCoo = new DecimalFormat("0.0000");
		
		DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance();
		sym.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(sym);
		this.dfCoo.setDecimalFormatSymbols(sym);
	}
	
	/**
	 * Moves hotspots SDET to first level.
	 * Can also improve "usual" hotspots in order to prevent blinking to be on top.
	 *
	 * @param sourceF
	 * @param destination
	 */
	public void moveHotspotToRightLayer(File sourceF, File destination) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder db;
		try {
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(sourceF);
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			
			//HS SDET on foreground
			{
				String expressionG = "//svg/g/g";
				NodeList nodeGList = (NodeList) xPath.compile(expressionG).evaluate(doc, XPathConstants.NODESET);
				
				String expression = "//*[@class='hotspot']";
				NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
				
				if (nodeList.getLength() == 0) {
					return;
				}
				
				for (int n = 0; n < nodeList.getLength(); n++) {
					Node node = nodeList.item(n);
					Element hotspot = (Element) node;
					Element gParent = findParentG(hotspot);
					hotspot.getParentNode().removeChild(hotspot);
					
					Element lastG = findLastMatchingG(gParent, nodeGList);
					
					lastG.appendChild(hotspot);
				}
			}
			
			// usual HS in background
			// uncomment this so the HS is in background and blinking is not on top
			/*{
				String expressionfG = "//svg/g/g[2]";
				Element firstg = (Element) xPath.compile(expressionfG).evaluate(doc, XPathConstants.NODE);

				String expressionHS = "//*[@class='hotspot' and not(starts-with(@apsname,'SDET'))]";
				NodeList nodeListHS = (NodeList) xPath.compile(expressionHS).evaluate(doc, XPathConstants.NODESET);

				for (int n = 1; n < nodeListHS.getLength() ; n++) {
					Node node = nodeListHS.item(n);
					Element hotspot = (Element)node;
					hotspot.getParentNode().removeChild(hotspot);

					firstg.appendChild(hotspot);
				}
			}*/
			
			
			DOMSource source = new DOMSource(doc);
			FileOutputStream fos = new FileOutputStream(destination);
			Writer out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			StreamResult result = new StreamResult(out);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(source, result);
			
			
		} catch (SAXException | IOException | ParserConfigurationException |
		         XPathExpressionException | TransformerException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private Element findLastMatchingG(Element gParent, NodeList nodeGList) {
		String parrentTransfo = gParent.getAttribute(TRANSFORM);
		for (int n = nodeGList.getLength() - 1; n >= 0; n--) {
			Element g = (Element) nodeGList.item(n);
			String currentTransfo = g.getAttribute(TRANSFORM);
			if (parrentTransfo.equalsIgnoreCase(currentTransfo)) {
				return g;
			}
		}
		return gParent;
	}
	
	private Element findParentG(Node n) {
		Node parent = n.getParentNode();
		if ("g".equalsIgnoreCase(parent.getNodeName())) {
			return (Element) parent;
		}
		return findParentG(parent);
	}
	
	/**
	 * all possible transformation (found in ietp 19)
	 * <p>
	 * 2980 matrix()<br>
	 * 1 matrix() scale()<br>
	 * 151948 matrix() scale()<br>
	 * 109909 matrix() translate() scale()<br>
	 *
	 * @param sourceF
	 * @param destination
	 */
	public void applyTransformation(File sourceF, File destination) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(sourceF);
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			
			{
				String expressionG = "//g[@transform]";
				NodeList nodeList = (NodeList) xPath.compile(expressionG).evaluate(doc, XPathConstants.NODESET);
				
				for (int n = 0; n < nodeList.getLength(); n++) {
					Node node = nodeList.item(n);
					Element g = (Element) node;
					Matrix[] matrixs = parseTransform(g.getAttribute(TRANSFORM));
					Matrix matrix = calculateMatrix(matrixs);
					g.setAttribute(TRANSFORM, "");
					
					updateStyle(g, matrix.m00);
					
					NodeList childs = g.getElementsByTagName("*");
					
					for (int m = 0; m < childs.getLength(); m++) {
						Node childNode = childs.item(m);
						Element child = (Element) childNode;
						
						if (!child.getAttribute(TRANSFORM).isEmpty()) {
							Matrix[] subMatrixs = parseTransform(child.getAttribute(TRANSFORM));
							matrix = calculateMatrix(matrix, subMatrixs);
							child.setAttribute(TRANSFORM, "");
						}
						
						if ("path".equalsIgnoreCase(child.getNodeName())) {
							recalculatePath(child, matrix);
							updateStyle(child, matrix.m00);
						} else if ("circle".equalsIgnoreCase(child.getNodeName())) {
							recalculateCircle(child, matrix);
							updateStyle(child, matrix.m00);
						} else if ("text".equalsIgnoreCase(child.getNodeName())) {
							recalculateText(child, matrix);
							updateStyle(child, matrix.m00);
						} else if ("rect".equalsIgnoreCase(child.getNodeName())) {
							recalculateRect(child, matrix);
							updateStyle(child, matrix.m00);
						} else {
							logger.warn("unhandled shape " + child.getNodeName());
						}
					}
				}
			}
			
			// transform images
			{
				String expressionG = "//image[@transform]";
				NodeList nodeList = (NodeList) xPath.compile(expressionG).evaluate(doc, XPathConstants.NODESET);
				
				for (int n = 0; n < nodeList.getLength(); n++) {
					Node node = nodeList.item(n);
					Element image = (Element) node;
					Matrix[] matrixs = parseTransform(image.getAttribute(TRANSFORM));
					Matrix matrix = calculateMatrix(matrixs);
					image.setAttribute(TRANSFORM, "");
					
					updateStyle(image, matrix.m00);
					
					recalculateImage(image, matrix);
				}
			}
			
			DOMSource source = new DOMSource(doc);
			FileOutputStream fos = new FileOutputStream(destination);
			Writer out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			StreamResult result = new StreamResult(out);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(source, result);
		} catch (SAXException | IOException | ParserConfigurationException |
		         XPathExpressionException | TransformerException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	
	private void recalculateCircle(Element child, Matrix matrix) {
		float rayon = Float.parseFloat(child.getAttribute("r"));
		String cx = child.getAttribute("cx");
		String cy = child.getAttribute("cy");
		
		Point center = new Point(Float.parseFloat(cx), Float.parseFloat(cy));
		Point newcenter = matrix.apply(center);
		
		child.setAttribute("cx", this.dfCoo.format(newcenter.x));
		child.setAttribute("cy", this.dfCoo.format(newcenter.y));
		child.setAttribute("r", this.dfCoo.format(rayon * matrix.m00));
		
	}
	
	private void recalculateText(Element child, Matrix matrix) {
		String x = child.getAttribute("x");
		String y = child.getAttribute("y");
		
		Point positon = new Point(Float.parseFloat(x), Float.parseFloat(y));
		Point newPosition = matrix.apply(positon);
		
		child.setAttribute("x", this.dfCoo.format(newPosition.x));
		child.setAttribute("y", this.dfCoo.format(newPosition.y));
	}
	
	private void recalculateRect(Element child, Matrix matrix) {
		String x = child.getAttribute("x");
		String y = child.getAttribute("y");
		
		float width = Float.parseFloat(child.getAttribute(WIDTH));
		float height = Float.parseFloat(child.getAttribute(HEIGHT));
		
		Point positon = new Point(Float.parseFloat(x), Float.parseFloat(y));
		
		Point newpositon = matrix.apply(positon);
		
		child.setAttribute("x", this.dfCoo.format(newpositon.x));
		child.setAttribute("y", this.dfCoo.format(newpositon.y));
		
		
		child.setAttribute(WIDTH, this.dfCoo.format(width * matrix.m00));
		child.setAttribute(HEIGHT, this.dfCoo.format(height * matrix.m00));
	}
	
	private void recalculateImage(Element image, Matrix matrix) {
		String x = image.getAttribute("x");
		String y = image.getAttribute("y");
		
		float width = Float.parseFloat(image.getAttribute(WIDTH));
		float height = Float.parseFloat(image.getAttribute(HEIGHT));
		
		Point positon = new Point(Float.parseFloat(x), Float.parseFloat(y));
		Point newPosition = matrix.apply(positon);
		
		image.setAttribute("x", String.valueOf(Math.round(newPosition.x)));
		image.setAttribute("y", String.valueOf(Math.round(newPosition.y)));
		
		image.setAttribute(WIDTH, String.valueOf(Math.round(Math.abs(width * matrix.m00))));
		image.setAttribute(HEIGHT, String.valueOf(Math.round(Math.abs(height * matrix.m11))));
	}
	
	
	private void recalculatePath(Element child, Matrix matrix) {
		String d = child.getAttribute("d").trim();
		StringBuilder newD = new StringBuilder();
		
		final String regex = "([MLQTCSAZVH])([^MLQTCSAZVH]*)";
		
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(d);
		
		boolean first = true;
		while (matcher.find()) {
			if (!first) {
				newD.append(" ");
			} else {
				first = false;
			}
			String letter = matcher.group(1);
			String coordinates = matcher.group(2);
			newD.append(letter);
			if ("A".equals(letter)) {
				String[] coos = coordinates.split(" ");
				float rx = Float.parseFloat(coos[0]);
				float ry = Float.parseFloat(coos[1]);
				float xaxis = Float.parseFloat(coos[2]);
				int laf = Integer.parseInt(coos[3]);
				// int sf = Integer.parseInt(coos[4]);
				float x = Float.parseFloat(coos[5]);
				float y = Float.parseFloat(coos[6]);
				Point rxy = new Point(rx, ry);
				Point xy = new Point(x, y);
				
				rxy = matrix.scale(rxy);
				xy = matrix.apply(xy);
				newD.append(rxy.toString()).append(" ");
				newD.append(xaxis).append(" ");
				newD.append(laf).append(" ");
				// newD.append(sf + " ");
				newD.append("1 ");// need to invert all arcs
				newD.append(xy.toString());
			} else if ("Z".equals(letter)) {
				// do nothing
			} else {
				String[] coos = coordinates.split(" ");
				for (int i = 0; i < coos.length; i = i + 2) {
					if (i > 0) {
						newD.append(" ");
					}
					float x = Float.parseFloat(coos[i]);
					float y = Float.parseFloat(coos[i + 1]);
					Point xy = new Point(x, y);
					xy = matrix.apply(xy);
					newD.append(xy.toString());
				}
			}
			
		}
		
		child.setAttribute("d", newD.toString());
	}
	
	private Matrix[] parseTransform(String transform) {
		String[] transforms = transform.split(" ");
		
		final String regex = "(matrix|scale|translate|rotate)(\\(.*\\))";
		
		Matrix matrix;
		Scale scale;
		Translate translate;
		Rotate rotate;
		
		Matrix[] matrixs = new Matrix[transforms.length];
		
		for (int i = 0; i < transforms.length; i++) {
			final Pattern pattern = Pattern.compile(regex);
			final Matcher matcher = pattern.matcher(transforms[i]);
			
			while (matcher.find()) {
				String type = matcher.group(1);
				String value = matcher.group(2);
				
				if ("matrix".contentEquals(type)) {
					matrix = parseMatrix(value);
					matrixs[i] = matrix;
				}
				if ("scale".contentEquals(type)) {
					scale = parseScale(value);
					matrixs[i] = scale;
				}
				if ("translate".contentEquals(type)) {
					translate = parseTranslate(value);
					matrixs[i] = translate;
				}
				
				if ("rotate".contentEquals(type)) {
					rotate = parseRotate(value);
					matrixs[i] = rotate;
				}
			}
		}
		
		
		return matrixs;
		
	}
	
	Matrix parseMatrix(String value) {
		final String regex = "\\(([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+)\\)";
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(value);
		
		try {
			if (matcher.find()) {
				float m00 = Float.parseFloat(matcher.group(1));
				float m10 = Float.parseFloat(matcher.group(2));
				float m01 = Float.parseFloat(matcher.group(3));
				float m11 = Float.parseFloat(matcher.group(4));
				float m02 = Float.parseFloat(matcher.group(5));
				float m12 = Float.parseFloat(matcher.group(6));
				
				return new Matrix(m00, m10, m01, m11, m02, m12);
			}
			return new Matrix(1, 0, 0, 1, 0, 0);
		} catch (Exception e) {
			return new Matrix(1, 0, 0, 1, 0, 0);
		}
		
	}
	
	Scale parseScale(String value) {
		final String regex = "\\(([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+)\\)";
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(value);
		
		try {
			if (matcher.find()) {
				float sx = Float.parseFloat(matcher.group(1));
				float sy = Float.parseFloat(matcher.group(2));
				
				return new Scale(sx, sy);
			}
			return new Scale(1, 1);
		} catch (Exception e) {
			return new Scale(1, 1);
		}
	}
	
	Translate parseTranslate(String value) {
		final String regex = "\\(([+-]?\\d*\\.?\\d+),([+-]?\\d*\\.?\\d+)\\)";
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(value);
		
		try {
			if (matcher.find()) {
				float tx = Float.parseFloat(matcher.group(1));
				float ty = Float.parseFloat(matcher.group(2));
				
				return new Translate(tx, ty);
			}
			return new Translate(0, 0);
		} catch (Exception e) {
			return new Translate(0, 0);
		}
	}
	
	Rotate parseRotate(String value) {
		final String regex = "\\(([+-]?\\d*\\.?\\d+)\\)";
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(value);
		
		try {
			if (matcher.find()) {
				float a = Float.parseFloat(matcher.group(1));
				return new Rotate(a);
			}
			return new Rotate(0);
		} catch (Exception e) {
			return new Rotate(0);
		}
	}
	
	private void updateStyle(Element g, float m00) {
		String style = g.getAttribute("style");
		if (!style.isEmpty()) {
			style = parseStrokeWidth(style, m00);
			style = parseFontSize(style, m00);
			g.setAttribute("style", style);
		}
	}
	
	public String parseFontSize(String style, float scalling) {
		return parseCssProperty(style, scalling, "font-size");
	}
	
	public String parseStrokeWidth(String style, float scalling) {
		return parseCssProperty(style, scalling, "stroke-width");
	}
	
	public String parseCssProperty(String style, float scalling, String cssProperty) {
		final String regex = cssProperty + ":(\\d*\\.?\\d+)";
		
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(style);
		
		if (!matcher.find()) return style; // pattern not met, may also throw an exception here
		
		float strokeWith = Float.parseFloat(matcher.group(1));
		
		strokeWith = strokeWith * scalling;
		
		strokeWith = Math.max(strokeWith, 0.01f);
		
		return new StringBuilder(style)
				.replace(matcher.start(1), matcher.end(1), this.dfCoo.format(strokeWith))
				.toString();
	}
	
	
	private Matrix calculateMatrix(Matrix[] matrixs) {
		Matrix base = matrixs[0];
		if (matrixs.length == 1) {
			return base;
		}
		for (int i = 1; i < matrixs.length; i++) {
			base = base.multiply(matrixs[i]);
		}
		return base;
		
	}
	
	private Matrix calculateMatrix(Matrix matrix, Matrix[] subMatrixs) {
		Matrix[] matrixsInc = new Matrix[subMatrixs.length + 1];
		matrixsInc[0] = matrix;
		System.arraycopy(subMatrixs, 0, matrixsInc, 1, subMatrixs.length);
		return calculateMatrix(matrixsInc);
	}
	
	
	/*
	 [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
	 [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
	 [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
	 */
	class Matrix {
		private final float m00;
		private final float m10;
		private final float m01;
		private final float m11;
		private final float m02;
		private final float m12;
		
		
		public Matrix(float m00, float m10, float m01, float m11, float m02, float m12) {
			this.m00 = m00;
			this.m10 = m10;
			this.m01 = m01;
			this.m11 = m11;
			this.m02 = m02;
			this.m12 = m12;
			
		}
		
		public Matrix multiply(Matrix subMatrix) {
			return new Matrix(this.m00 * subMatrix.m00 + this.m01 * subMatrix.m10,
					this.m10 * subMatrix.m00 + this.m11 * subMatrix.m10,
					this.m00 * subMatrix.m01 + this.m01 * subMatrix.m11,
					this.m10 * subMatrix.m01 + this.m11 * subMatrix.m11,
					this.m00 * subMatrix.m02 + this.m01 * subMatrix.m12 + this.m02,
					this.m10 * subMatrix.m02 + this.m11 * subMatrix.m12 + this.m12
			);
		}
		
		public Point apply(Point p) {
			float nx = this.m00 * p.x + this.m01 * p.y + this.m02;
			float ny = this.m10 * p.x + this.m11 * p.y + this.m12;
			
			return new Point(nx, ny);
		}
		
		public Point scale(Point p) {
			float nx = Math.abs(this.m00 * p.x + this.m01 * p.y);
			float ny = Math.abs(this.m10 * p.x + this.m11 * p.y);
			
			return new Point(nx, ny);
		}
		
		@Override
		public String toString() {
			String l1 = "[ " + this.m00 + " " + this.m01 + " " + this.m02 + " ]\n";
			String l2 = "[ " + this.m10 + " " + this.m11 + " " + this.m12 + " ]\n";
			String l3 = "[ 0 0 1 ]";
			
			return l1 + l2 + l3;
			
		}
	}
	
	class Scale extends Matrix {
		public Scale(float sx, float sy) {
			super(sx, 0, 0, sy, 0, 0);
			
		}
	}
	
	class Rotate extends Matrix {
		public Rotate(float angle) {
			super((float) Math.cos(angle), (float) Math.sin(angle), (float) (-1 * Math.sin(angle)), (float) Math.cos(angle), 0, 0);
			
		}
	}
	
	class Translate extends Matrix {
		public Translate(float tx, float ty) {
			super(1, 0, 0, 1, tx, ty);
		}
	}
	
	class Point {
		protected float x;
		protected float y;
		
		public Point(float x, float y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString() {
			return SVGUtils.this.dfCoo.format(this.x) + " " + SVGUtils.this.dfCoo.format(this.y);
		}
	}
	
	public static void main(String[] args) {
		String input = "path to the folder containing the CGMs";
		String output = "path to the folder where your want the SVGs to be generated";
		
		final Collection<File> cgms = FileUtils.listFiles(new File(input), new String[]{"cgm"}, false);
		final long begin = System.currentTimeMillis();
		for (File cgmFile : cgms) {
			try {
				new JcgmtosvgApplication().convert(cgmFile.getAbsolutePath(), output);
			} catch (IOException e) {
				logger.error("Error while converting CGM {}", cgmFile);
			}
		}
		final long end = System.currentTimeMillis();
		logger.debug("The conversion took {}ms", end - begin);
	}
	
}
