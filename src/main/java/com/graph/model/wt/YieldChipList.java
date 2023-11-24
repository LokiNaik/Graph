package com.graph.model.wt;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class YieldChipList {	
	@Id
	ObjectId _id;
	private String LotId;
	private double Yield;
	private String  ProcessCode;
	private String ArticleName;
	private Integer TestChipCount;
	private List<?> BinCount;
	private Integer Wafer;
	private Integer GoodChipCount;
	private Double FailRatio;
	private Integer SlotNumber;
	private String ProgramName;
	private String StepId;
	private Date WaferStartTime;
	private Date WaferEndTime;
	private Date LotStartTime;
	private Date LotEndTime;
	private Boolean ISACTIVE;
	private String OriginalLotId;
	private String DeviceName;
	private String DeviceGroup;
	private String CareerID;
	private String DeviceID;
	private String Tester;
	private String Prober;
	private Integer ChipArea;
	private Integer PassChips;
	private double FinalYield;
	private Integer FailChips;
	private String ProbeCard;
	private Integer NumberOfChipsPerBin;
	private Integer TestPassBINNumber;
	private String LoadBoard;
	private String Equipment;
	
	public Double getFailRatio() {
		return FailRatio;
	}

	public void setFailRatio(Double failRatio) {
		FailRatio = failRatio;
	}

	public Integer getGoodChipCount() {
		return GoodChipCount;
	}

	public void setGoodChipCount(Integer goodChipCount) {
		GoodChipCount = goodChipCount;
	}

	public Integer getFailChips() {
		return FailChips;
	}

	public Integer getNumberOfChipsPerBin() {
		return NumberOfChipsPerBin;
	}

	public void setNumberOfChipsPerBin(Integer numberOfChipsPerBin) {
		NumberOfChipsPerBin = numberOfChipsPerBin;
	}

	public void setFailChips(Integer failChips) {
		FailChips = failChips;
	}

	public String getDeviceID() {
		return DeviceID;
	}

	public void setDeviceID(String deviceID) {
		DeviceID = deviceID;
	}

	public String getCareerID() {
		return CareerID;
	}

	public void setCareerID(String careerID) {
		CareerID = careerID;
	}

	public String getProbeCard() {
		return ProbeCard;
	}

	public void setProbeCard(String probeCard) {
		ProbeCard = probeCard;
	}

	public Date getWaferEndTime() {
		return WaferEndTime;
	}

	public void setWaferEndTime(Date waferEndTime) {
		WaferEndTime = waferEndTime;
	}

	public Date getLotStartTime() {
		return LotStartTime;
	}

	public void setLotStartTime(Date lotStartTime) {
		LotStartTime = lotStartTime;
	}

	public Date getLotEndTime() {
		return LotEndTime;
	}

	public void setLotEndTime(Date lotEndTime) {
		LotEndTime = lotEndTime;
	}

	public double getFinalYield() {
		return FinalYield;
	}

	public void setFinalYield(double finalYield) {
		FinalYield = finalYield;
	}

	public Integer getPassChips() {
		return PassChips;
	}

	public void setPassChips(Integer passChips) {
		PassChips = passChips;
	}

	public String getOriginalLotId() {
		return OriginalLotId;
	}

	public Integer getWafer() {
		return Wafer;
	}

	public void setWafer(Integer wafer) {
		Wafer = wafer;
	}

	public void setOriginalLotId(String originalLotId) {
		OriginalLotId = originalLotId;
	}

	public String getDeviceName() {
		return DeviceName;
	}

	public void setDeviceName(String deviceName) {
		DeviceName = deviceName;
	}

	public String getDeviceGroup() {
		return DeviceGroup;
	}

	public void setDeviceGroup(String deviceGroup) {
		DeviceGroup = deviceGroup;
	}

	public Integer getTestPassBINNumber() {
		return TestPassBINNumber;
	}

	public void setTestPassBINNumber(Integer testPassBINNumber) {
		TestPassBINNumber = testPassBINNumber;
	}


	public String getArticleName() {
		return ArticleName;
	}

	public void setArticleName(String articleName) {
		ArticleName = articleName;
	}

	public String getLotId() {
		return LotId;
	}

	public void setLotId(String lotId) {
		LotId = lotId;
	}

	public double getYield() {
		return Yield;
	}

	public void setYield(double yield) {
		Yield = yield;
	}

	public Integer getSlotNumber() {
		return SlotNumber;
	}

	public void setSlotNumber(Integer slotNumber) {
		SlotNumber = slotNumber;
	}

	public String getProgramName() {
		return ProgramName;
	}

	public void setProgramName(String programName) {
		ProgramName = programName;
	}

	public String getStepId() {
		return StepId;
	}

	public void setStepId(String stepId) {
		StepId = stepId;
	}

	public Date getWaferStartTime() {
		return WaferStartTime;
	}

	public void setWaferStartTime(Date waferStartTime) {
		WaferStartTime = waferStartTime;
	}

	public Boolean getISACTIVE() {
		return ISACTIVE;
	}

	public void setISACTIVE(Boolean iSACTIVE) {
		ISACTIVE = iSACTIVE;
	}

	public String get_id() {
		return _id.toHexString();
	}

	public void set_id(ObjectId _id) {
		this._id = _id;
	}

	public Integer getTestChipCount() {
		return TestChipCount;
	}

	public void setTestChipCount(Integer testChipCount) {
		TestChipCount = testChipCount;
	}

	public List<?> getBinCount() {
		return BinCount;
	}

	public void setBinCount(List<?> binCount) {
		BinCount = binCount;
	}

	public String getTester() {
		return Tester;
	}

	public void setTester(String tester) {
		Tester = tester;
	}

	public String getProber() {
		return Prober;
	}

	public void setProber(String prober) {
		Prober = prober;
	}

	public Integer getChipArea() {
		return ChipArea;
	}

	public void setChipArea(Integer chipArea) {
		ChipArea = chipArea;
	}

	public String getProcessCode() {
		return ProcessCode;
	}

	public void setProcessCode(String processCode) {
		ProcessCode = processCode;
	}

	public String getLoadBoard() {
		return LoadBoard;
	}

	public void setLoadBoard(String loadBoard) {
		LoadBoard = loadBoard;
	}

	public String getEquipment() {
		return Equipment;
	}

	public void setEquipment(String equipment) {
		Equipment = equipment;
	}
	
}
