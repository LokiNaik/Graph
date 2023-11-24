package com.graph.model.wt;

public class RangeModel {
    private String rangeCalculationType;
    private float min;
    private float max;
    private boolean autoCalculationMax;
    private boolean autoCalculationMin;

    public boolean isAutoCalculationMax() {
	   return autoCalculationMax;
    }

    public void setAutoCalculationMax(boolean autoCalculationMax) {
	   this.autoCalculationMax = autoCalculationMax;
    }

    public boolean isAutoCalculationMin() {
	   return autoCalculationMin;
    }

    public void setAutoCalculationMin(boolean autoCalculationMin) {
	   this.autoCalculationMin = autoCalculationMin;
    }

    public String getRangeCalculationType() {
	   return rangeCalculationType;
    }

    public void setRangeCalculationType(String rangeCalculationType) {
	   this.rangeCalculationType = rangeCalculationType;
    }

    public float getMin() {
	   return min;
    }

    public void setMin(float min) {
	   this.min = min;
    }

    public float getMax() {
	   return max;
    }

    public void setMax(float max) {
	   this.max = max;
    }

}
