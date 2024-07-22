package com.jpprade.jcgmtosvg;

import java.util.ArrayList;
import java.util.List;

import net.sf.jcgm.core.ApplicationStructureAttribute;
import net.sf.jcgm.core.EdgeColour;
import net.sf.jcgm.core.EdgeWidth;
import net.sf.jcgm.core.FillColour;
import net.sf.jcgm.core.LineColour;
import net.sf.jcgm.core.LineWidth;
import net.sf.jcgm.core.Member;
import net.sf.jcgm.core.StructuredDataRecord;

public class PaintHolder {
	
	private FillColour currentFC = null;
	private EdgeColour currentEC = null;
	private EdgeWidth currentEW = null;
	private LineColour currentLC = null;
	private LineWidth currentLW = null;
	private String apsid = "";
	
	private final List<ApplicationStructureAttribute> curentAPS = new ArrayList<>();
	
	public FillColour getCurrentFC() {
		return this.currentFC;
	}
	
	public void setCurrentFC(FillColour currentFC) {
		this.currentFC = currentFC;
	}
	
	public EdgeColour getCurrentEC() {
		return this.currentEC;
	}
	
	public void setCurrentEC(EdgeColour currentEC) {
		this.currentEC = currentEC;
	}
	
	public EdgeWidth getCurrentEW() {
		return this.currentEW;
	}
	
	public void setCurrentEW(EdgeWidth currentEW) {
		this.currentEW = currentEW;
	}
	
	public LineColour getCurrentLC() {
		return this.currentLC;
	}
	
	public void setCurrentLC(LineColour currentLC) {
		this.currentLC = currentLC;
	}
	
	public LineWidth getCurrentLW() {
		return this.currentLW;
	}
	
	public void setCurrentLW(LineWidth currentLW) {
		this.currentLW = currentLW;
	}
	
	
	public void addAPS(ApplicationStructureAttribute aps) {
		this.curentAPS.add(aps);
	}
	
	public ApplicationStructureAttribute getRegionAPS() {
		for (ApplicationStructureAttribute aps : this.curentAPS) {
			String attributeType = aps.getApplicationStructureAttributeType();
			if ("region".equals(attributeType)) {
				return aps;
			}
		}
		return null;
	}
	
	public ApplicationStructureAttribute getVCAPS() {
		for (ApplicationStructureAttribute aps : this.curentAPS) {
			String attributeType = aps.getApplicationStructureAttributeType();
			if ("viewcontext".equals(attributeType)) {
				return aps;
			}
		}
		return null;
	}
	
	public String getName() {
		for (ApplicationStructureAttribute aps : this.curentAPS) {
			String attributeType = aps.getApplicationStructureAttributeType();
			StructuredDataRecord structuredDataRecord = aps.getSdr();
			if ("name".equals(attributeType)) {
				List<Member> members = structuredDataRecord.getMembers();
				if (members != null
						&& members.size() == 1
						&& members.getFirst().getCount() > 0
						&& members.getFirst().getData() != null
						&& !members.getFirst().getData().isEmpty()
						&& members.getFirst().getData().getFirst() instanceof String s) {
					return s;
				}
			}
		}
		return "";
	}
	
	public String getApsid() {
		return this.apsid;
	}
	
	public void setApsid(String apsid) {
		this.apsid = apsid;
	}
	
}
