package com.graph.model.vdi;

import java.util.List;

public class WaferArea {
	private List<AllWaferAreas> allWaferAreas;
	private List<Area1> areas;
	private List<Area1> discardedAreas;
	private SelectedOptions selectedOptions;
	private int selectionType;
	private int sampleSize;
	
	public int getSampleSize() {
		return sampleSize;
	}

	public List<AllWaferAreas> getAllWaferAreas() {
		return allWaferAreas;
	}

	public List<Area1> getAreas() {
		return areas;
	}

	public List<Area1> getDiscardedAreas() {
		return discardedAreas;
	}

	public SelectedOptions getSelectedOptions() {
		return selectedOptions;
	}

	public int getSelectionType() {
		return selectionType;
	}

}
