package com.graph.enums;
public enum SelectionType {
	FULL (0), PARTIAL (1), NONE (2);
	
	public int value;

	SelectionType(int value) {
		this.value = value;
	}
}
