package com.graph.model.wt;

import java.util.Date;

import org.bson.types.ObjectId;

public class WaferListLineGraph {
	ObjectId _id;
	private String LotId;
	private int SlotNumber;
	private int waferNumber;
	private String  ProgramName;
	private String  ProcessCode;
	private String deviceName;
	private Date WaferStartTime;
	private Boolean ISACTIVE;
	
	
	public Boolean getISACTIVE() {
		return ISACTIVE;
	}
	public void setISACTIVE(Boolean iSACTIVE) {
		ISACTIVE = iSACTIVE;
	}
	public Date getWaferStartTime() {
		return WaferStartTime;
	}
	public void setWaferStartTime(Date waferStartTime) {
		WaferStartTime = waferStartTime;
	}
	public String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public String get_id() 
	{ 
		return _id.toHexString(); 
		}
	public void set_id(ObjectId _id) { 
		this._id = _id; 
		}
	public String getLotId() {
		return LotId;
	}
	public void setLotId(String lotId) {
		LotId = lotId;
	}
	
	public int getWaferNumber() {
		return waferNumber;
	}
	public void setWaferNumber(int waferNumber) {
		this.waferNumber = waferNumber;
	}
	public String getProgramName() {
		return ProgramName;
	}
	public void setProgramName(String programName) {
		ProgramName = programName;
	}
	public String getProcessCode() {
		return ProcessCode;
	}
	public void setProcessCode(String processCode) {
		ProcessCode = processCode;
	}
	
	public int getSlotNumber() {
		return SlotNumber;
	}
	public void setSlotNumber(int slotNumber) {
		SlotNumber = slotNumber;
	}
	
}
