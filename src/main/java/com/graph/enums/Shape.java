package com.graph.enums;

public enum Shape {
CIRCLE(0), CIRCLE_SECTOR (1), PARTIAL_CIRCLE_SECTOR (2);
	
	public int value;

	Shape(int value) {
		this.value = value;
	}

}
