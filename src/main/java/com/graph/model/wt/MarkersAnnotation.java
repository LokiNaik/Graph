package com.graph.model.wt;

public class MarkersAnnotation {

	private String fillColor;
	private String strokeColor;
	private Float strokeWidth;
	private String strokeDasharray;
	private Float opacity;
	private Float left;
	private Float top;
	private Float width;
	private Float height;
	private Float rotationAngle;
	private String drawingImgUrl;
	private VisualTransformMatrix visualTransformMatrix;
	private ContainerTransformMatrix containerTransformMatrix;
	private String typeName;
	private String state;
	private String bgColor;
	private TipPosition tipPosition;
	private String arrowType;
	private Float x1;
	private Float x2;
	private Float y1;
	private Float y2;
	private String color;
	private String fontFamily;
	private Float padding;
	private String text;
	

    public Float getStrokeWidth() {
        return strokeWidth;
    }
    public void setStrokeWidth(Float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }
    public Float getOpacity() {
        return opacity;
    }
    public void setOpacity(Float opacity) {
        this.opacity = opacity;
    }
    public Float getLeft() {
        return left;
    }
    public void setLeft(Float left) {
        this.left = left;
    }
    public Float getTop() {
        return top;
    }
    public void setTop(Float top) {
        this.top = top;
    }
    public Float getWidth() {
        return width;
    }
    public void setWidth(Float width) {
        this.width = width;
    }
    public Float getHeight() {
        return height;
    }
    public void setHeight(Float height) {
        this.height = height;
    }
    public Float getRotationAngle() {
        return rotationAngle;
    }
    public void setRotationAngle(Float rotationAngle) {
        this.rotationAngle = rotationAngle;
    }
    public Float getX1() {
        return x1;
    }
    public void setX1(Float x1) {
        this.x1 = x1;
    }
    public Float getX2() {
        return x2;
    }
    public void setX2(Float x2) {
        this.x2 = x2;
    }
    public Float getY1() {
        return y1;
    }
    public void setY1(Float y1) {
        this.y1 = y1;
    }
    public Float getY2() {
        return y2;
    }
    public void setY2(Float y2) {
        this.y2 = y2;
    }
    public Float getPadding() {
        return padding;
    }
    public void setPadding(Float padding) {
        this.padding = padding;
    }
    public VisualTransformMatrix getVisualTransformMatrix() {
		return visualTransformMatrix;
	}
	public void setVisualTransformMatrix(VisualTransformMatrix visualTransformMatrix) {
		this.visualTransformMatrix = visualTransformMatrix;
	}
	public ContainerTransformMatrix getContainerTransformMatrix() {
		return containerTransformMatrix;
	}
	public void setContainerTransformMatrix(ContainerTransformMatrix containerTransformMatrix) {
		this.containerTransformMatrix = containerTransformMatrix;
	}
	public TipPosition getTipPosition() {
		return tipPosition;
	}
	public void setTipPosition(TipPosition tipPosition) {
		this.tipPosition = tipPosition;
	}
	public String getBgColor() {
		return bgColor;
	}
	public void setBgColor(String bgColor) {
		this.bgColor = bgColor;
	}

	public String getFillColor() {
		return fillColor;
	}
	public void setFillColor(String fillColor) {
		this.fillColor = fillColor;
	}
	public String getStrokeColor() {
		return strokeColor;
	}
	public void setStrokeColor(String strokeColor) {
		this.strokeColor = strokeColor;
	}
	public String getStrokeDasharray() {
		return strokeDasharray;
	}
	public void setStrokeDasharray(String strokeDasharray) {
		this.strokeDasharray = strokeDasharray;
	}
	
	
	
	public String getDrawingImgUrl() {
		return drawingImgUrl;
	}
	public void setDrawingImgUrl(String drawingImgUrl) {
		this.drawingImgUrl = drawingImgUrl;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getArrowType() {
		return arrowType;
	}
	public void setArrowType(String arrowType) {
		this.arrowType = arrowType;
	}
	
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getFontFamily() {
		return fontFamily;
	}
	public void setFontFamily(String fontFamily) {
		this.fontFamily = fontFamily;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}


}
