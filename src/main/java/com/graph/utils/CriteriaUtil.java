package com.graph.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import com.graph.model.wt.DataQueries;
import com.graph.model.wt.FilterParam;
import com.graph.model.wt.LockParam;
import com.graph.model.wt.Message;
import com.graph.model.wt.QueryList;
import com.graph.model.wt.SortParam;
import com.graph.service.wt.AdvanceGraphService;

public class CriteriaUtil {

public final static Logger logger = LoggerFactory.getLogger(AdvanceGraphService.class);
	
	
		public  static List<Criteria> generateCriteriaForFileds(List<FilterParam> filterParams, String CollectionName) {

		   List<Criteria> criteriaList = new ArrayList<Criteria>();
		   String fieldName;
		   String operator;
		   String fromDate;
		   String toDate;
		   String includeDateValue;

		   for (FilterParam searchFilter : filterParams) {
			  List<?> excludeValues = searchFilter.getExcludeValues();
			  Criteria criteria = null;
			  fieldName = searchFilter.getField().trim();
			  String fieldNameCheck = null;
			  if (fieldName.endsWith("EndTime") || fieldName.endsWith("StartTime")) {
				 fieldNameCheck = "WaferTime";
			  }
			  else {
				 fieldNameCheck = fieldName;
			  }
			  switch (fieldNameCheck) {
				 case "WaferTime":
					fromDate = searchFilter.getValueFirst();
					toDate = searchFilter.getValueSecond();
					operator = searchFilter.getOperator();
					includeDateValue = searchFilter.getIncludeValue();
					criteria = generateCriteriaForDateField(generateFieldNameForCriteria(fieldName), operator, fromDate,
						   toDate, excludeValues, includeDateValue, CollectionName);
					if (criteria != null) {
					    criteriaList.add(criteria);
					}
					break;
				 case FilterConstants.CREATED_AT:
					fromDate = searchFilter.getValueFirst();
					toDate = searchFilter.getValueSecond();
					operator = searchFilter.getOperator();
					includeDateValue = searchFilter.getIncludeValue();
					criteria = generateCriteriaForDateGrid(generateFieldNameForCriteria(fieldName), operator, fromDate,
						   toDate, excludeValues, includeDateValue);
					if (criteria != null) {
					    criteriaList.add(criteria);
					}
					break;
				 case FilterConstants.LAST_UPDATED_DATE:
					fromDate = searchFilter.getValueFirst();
					toDate = searchFilter.getValueSecond();
					operator = searchFilter.getOperator();
					includeDateValue = searchFilter.getIncludeValue();
					criteria = generateCriteriaForDateGrid(generateFieldNameForCriteria(fieldName), operator, fromDate,
						   toDate, excludeValues, includeDateValue);
					if (criteria != null) {
					    criteriaList.add(criteria);
					}
					break;
				 case FilterConstants.ACCESS_TIME:
					fromDate = searchFilter.getValueFirst();
					toDate = searchFilter.getValueSecond();
					operator = searchFilter.getOperator();
					includeDateValue = searchFilter.getIncludeValue();
					criteria = generateCriteriaForDateGrid(generateFieldNameForCriteria(fieldName), operator, fromDate,
						   toDate, excludeValues, includeDateValue);
					if (criteria != null) {
					    criteriaList.add(criteria);
					}
					break;
				 case "savedTime":
					fromDate = searchFilter.getValueFirst();
					toDate = searchFilter.getValueSecond();
					operator = searchFilter.getOperator();
					includeDateValue = searchFilter.getIncludeValue();
					criteria = generateCriteriaForDateGrid(generateFieldNameForCriteria(fieldName), operator, fromDate,
						   toDate, excludeValues, includeDateValue);
					if (criteria != null) {
					    criteriaList.add(criteria);
					}
					break;
				 default:
					criteria = FilterUtil.generateCriteriaForList(generateFieldNameForCriteria(fieldName),
						   searchFilter, CollectionName);
					if (criteria != null) {
					    criteriaList.add(criteria);
					}
					break;
			  }
		   }
		   return criteriaList;
	    }
	/*
	 * 
	 */
	    public static Criteria generateCriteriaForDateGrid(String fieldName, String operator, String fromDate, String toDate,
	  		  List<?> excludeValues, String includeValue) {
	  	   Date from = null;
	  	   Date to = null;
	  	   Criteria criteria = null;

	  	   try {
	  		  logger.info("Generating Criteria for Date Fields");

	  		  if (operator.equals("Not Between")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate);
	  			 criteria = new Criteria().norOperator(Criteria.where(fieldName).gt(from).lt(to));
	  		  }
	  		  else if (operator.equals("between")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate);
	  			 criteria = new Criteria().andOperator(Criteria.where(fieldName).gt(from).lt(to));
	  		  }

	  		  else if (operator.equals(">")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 criteria = Criteria.where(fieldName).gt(from);
	  		  }
	  		  else if (operator.equals(">=")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 criteria = Criteria.where(fieldName).gte(from);
	  		  }
	  		  else if (operator.equals("<")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 criteria = Criteria.where(fieldName).lt(from);
	  		  }
	  		  else if (operator.equals("<=")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate.substring(0, 19));
	  			 criteria = Criteria.where(fieldName).lte(from);
	  		  }
	  		  else if (operator.equals("=")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 criteria = Criteria.where(fieldName).gte(from).lte(to);
	  		  }
	  		  else if (operator.equals("A<X<B")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate);
	  			 criteria = Criteria.where(fieldName).gt(from).lt(to);
	  		  }
	  		  else if (operator.equals("A<=X<B")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate);
	  			 criteria = Criteria.where(fieldName).gte(from).lt(to);
	  		  }
	  		  else if (operator.equals("A<X<=B")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate.substring(0, 19) + ".999Z");
	  			 criteria = Criteria.where(fieldName).gt(from).lte(to);
	  		  }
	  		  else if (operator.equals("A<=X<=B")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate.substring(0, 19) + ".999Z");
	  			 criteria = Criteria.where(fieldName).gte(from).lte(to);
	  		  }
	  		  else if (operator.equals("A>X>B")) {
	  			 from = DateUtil.convertStringToDateTimeStamp(fromDate);
	  			 to = DateUtil.convertStringToDateTimeStamp(toDate);
	  			 criteria = Criteria.where(fieldName).lt(from).gt(to);
	  		  }
	  		  else {
	  			 from = DateUtil.getCurrentDateInUTC();
	  			 criteria = Criteria.where(fieldName).is(from);
	  		  }
	  		  return criteria;

	  	   }
	  	   catch (Exception e) {
	  		  logger.error("Exception occured:" + e.getMessage());
	  	   }
	  	   return criteria;
	      }

		/*
		 * generateFieldNameForCriteria
		 */
		  public static String generateFieldNameForCriteria(String fieldName) {
			   switch (fieldName) {
				  case "LotId":
					 return "mesMap.LotId";
				  case "ArticleName":
				  case "ProductName":
					 return "mesMap.ProductName";
				  case "CareerID":
					 return "mesMap.CareerID";
				  case "Device":
					 return "ProductInfo.deviceName";
				  case "Tester":
					 return FilterConstants.MES_MAP_DEVICE_ID;
				  case "Prober":
					 return FilterConstants.MES_MAP_DEVICE_ID;
				  case "Equipment":
					 return FilterConstants.MES_MAP_DEVICE_ID;
				  case "OriginalLotID":
					 return "mesMap.OriginalLotID";
				  case "LotNumber":
					 return "mesMap.LotNumber";
				  case "Program":
					 return "mesMap.ProgramName";
				  case "LotStartTime":
					 return "mesMap.LotStartTime";
				  case "Process":
					 return "mesMap.ProcessCode";
				  case "StepID":
					 return "mesMap.ProcessCode";
				  case "LotEndTime":
					 return "mesMap.LotEndTime";
				  case "ProbeCard":
					 return "mesMap.ProbeCardID";
				  case "TestBoardID":
				  case "LoadBoard":
					 return "mesMap.TestBoardID";
				  case "Status":
					 return "waferStatus.status";
				  case "chipArea":
					 return "ProductInfo.chipArea";
				  case "deviceGroup":
					 return "ProductInfo.deviceGroup";
				  case "deviceDesignGroup":
					 return "ProductInfo.deviceDesignGroup";
				  case "WaferId":
					 return "mesWafer._id";
				  case "WaferNumber":
					 return "mesWafer.WaferNumber";
				  case "SlotNumber":
					 return "mesWafer.SlotNumber";
				  case "WaferStartTime":
					 return "mesWafer.WaferStartTime";
				  case "WaferEndTime":
					 return "mesWafer.WaferEndTime";
				  case "TestTime":
					 return "mesWafer.TestTime";
				  case "GoodChipCount":
					 return "mesWafer.GoodChipCount";
				  case "LastYield":
					 return "mesWafer.LastYield";
				  case "Yield":
					 return "mesWafer.Yield";
				  case "NumberOfFailedChips":
					 return "mesWafer.TestFailCount";
				  case "NumberOfPassedChips":
					 return "mesWafer.TestPassCount";
				  case "TestChipCount":
					 return "mesWafer.TestChipCount";
				  case "TestChipRatio":
					 return "mesWafer.TestChipRatio";
				  case "FailRatio":
					 return "mesWafer.FailRatio";
				  case "NumberOfChipsPerBin":
					 return "NumberOfChipsPerBin";
				  case "BinRatio":
					 return "BinRatio";
				  case "DUT":
					 return "DUTNumber";
				  case "DUTWaferStartTime":
					 return "TestHistory.mesWafer.WaferStartTime";
				  case "DUTWaferEndTime":
					 return "TestHistory.mesWafer.WaferEndTime";
				  case "DUTLotStartTime":
					 return "TestHistory.mesMap.LotStartTime";
				  case "DUTLotEndTime":
					 return "TestHistory.mesMap.LotEndTime";
				  case "empty":
					 return "mesWafer._id";
				  case "Bin":
					 return "$mesWafer.BinSummary.BinCount.BINNumber";
				  case "TestPassChips":
					 return "";
				  case FilterConstants.CREATED_AT:
					 return  FilterConstants.CREATED_AT;
				  case FilterConstants.LAST_UPDATED_DATE:
					 return FilterConstants.LAST_UPDATED_DATE;
				  case FilterConstants.ACCESS_TIME:
					 return FilterConstants.ACCESS_TIME;
				  case "TestHistory.ProductInfo.deviceName":
					  return "TestHistory.ProductInfo.deviceName";
				  default:
					 return null;
			   }
		    }
		  
		  /*
		   * 
		   */
		  public static Criteria generateCriteriaForDateField(String fieldName, String operator, String fromDate, String toDate,
				  List<?> excludeValues, String includeValue, String CollectionName) {
			   Date from = null;
			   Date to = null;
			   Criteria criteria = null;

			   if(CollectionName!=null) {
				   fieldName = CollectionName+"."+fieldName;
			   }
			   try {
				  logger.info("Generating Criteria for Date Fields");

				  if (operator.equals("Not Between")) {
					 from = DateUtil.convertStringToDate(fromDate);
					 to = DateUtil.convertStringToDate(toDate);
					 criteria = new Criteria().norOperator(Criteria.where(fieldName).gt(from).lt(to));
				  }
				  else if (operator.equals(">")) {
					 from = DateUtil.convertStringToDate(fromDate);
					 criteria = Criteria.where(fieldName).gt(from);
				  }
				  else if (operator.equals(">=")) {
					 from = DateUtil.convertStringToDate(fromDate);
					 criteria = Criteria.where(fieldName).gte(from);
				  }
				  else if (operator.equals("<")) {
					 from = DateUtil.convertStringToDate(fromDate);
					 criteria = Criteria.where(fieldName).lt(from);
				  }
				  else if (operator.equals("<=")) {
					 from = DateUtil.convertStringToDate(fromDate.substring(0, 19) + ".999Z");
					 criteria = Criteria.where(fieldName).lte(from);
				  }
				  else if (operator.equals("=")) {
					 Date fromValueFirst = DateUtil.convertStringToDate(fromDate.substring(0, 19) + ".000Z");
					 Date fromValueLast = DateUtil.convertStringToDate(fromDate.substring(0, 19) + ".999Z");
					 criteria = Criteria.where(fieldName).gte(fromValueFirst).lte(fromValueLast);
				  }
				  else {
					 from = DateUtil.getCurrentDateInUTC();
					 criteria = Criteria.where(fieldName).is(from);
				  }
				  return criteria;

			   }
			   catch (Exception e) {
				  logger.error("Exception occured:" + e.getMessage());
			   }
			   return criteria;
		    }
		  /*
		     * Method for sorting axis parameters.
		     */
		    public static List<Document> sortAxisParameters(List<Document> results, List<SortParam> sortParams) {

			   String fieldName = null;
			   for (SortParam sortParam : sortParams) {
				  fieldName = sortParam.getField();
				  switch (fieldName) {
					 case "xAxis.axisParameter":
						results = ValidationUtil.sortAxisParameter(results, "xAxis", sortParam.isAsc());
						break;
					 case "yAxis.axisParameter":
						results = ValidationUtil.sortAxisParameter(results, "yAxis", sortParam.isAsc());
						break;
					 case "zAxis.axisParameter":
						results = ValidationUtil.sortAxisParameter(results, "zAxis", sortParam.isAsc());
						break;
					 case "grouping.axisParameter":
						results = ValidationUtil.sortAxisParameter(results, "grouping", sortParam.isAsc());
						break;
					 default:
						break;
				  }
			   }
			   return results;
		    }
		    /*
		     * getDataFromQueryList
		     */
		    public static List<FilterParam> getDataFromQueryList(Document results) {
			   List<FilterParam> filterParams = new ArrayList<FilterParam>();
			   if (results != null) {
				  LocalDateTime now;
				  LocalDateTime earlier;
				  String day;
				  String month;
				  int hour;
				  int minute;
				  int sec;
				  int year;
				  String date = "";
				  List<Document> queryList = (List<Document>) results.get("queryList");
				  for (Document document : queryList) {
					 FilterParam filterParam = new FilterParam();
					 filterParam.setField((String) document.get("field"));
					 filterParam.setOperator((String) document.get("operator"));
					 filterParam.setValueFirst((String) document.get("value"));
					 String value = (String) document.get("value");
					 if (filterParam.getField().endsWith("Time")) {
						getDateByQueryList(filterParams, filterParam, value);
					 }
					 else {
						filterParams.add(filterParam);
					 }
				  }
			   }

			   return filterParams;
		    }
		    /*
		     * getDateByQueryList
		     */
		    public static void getDateByQueryList(List<FilterParam> filterParams, FilterParam filterParam, String value) {
			   LocalDateTime now;
			   LocalDateTime earlier;
			   String date;
			   String[] arrOfStr = value.split("[(,)]+");// splitting @Currentdate and (3M)
			   if (arrOfStr.length > 1 && value.startsWith("@CurrentDate")) {
				  String parameter = arrOfStr[1];
				  Pattern pattern = Pattern.compile("\\d+");
				  Matcher matcher = pattern.matcher(parameter);
				  int count = 0;
				  while (matcher.find()) {
					 count = count + Integer.parseInt(matcher.group());
				  }
				  Pattern p = Pattern.compile("[A-Z]+");
				  Matcher m = p.matcher(parameter);
				  String checkingCondition = ""; // taking M
				  while (m.find()) {
					 checkingCondition = checkingCondition + m.group();
				  }
				  checkDateExists(filterParam, count, checkingCondition);
				  filterParams.add(filterParam);
			   }
			   else if (value.startsWith("@CurrentDate")) {
				  now = LocalDateTime.now();// 2015-11-24
				  date = getDatesforDataQuery(now);
				  filterParam.setValueFirst(date);
				  filterParams.add(filterParam);
			   }
			   else {
				  filterParams.add(filterParam);
			   }
		    }
		    /*
		     * getDatesforDataQuery
		     */
		    public static String getDatesforDataQuery(LocalDateTime earlier) {
			   String day;
			   String month;
			   String hour;
			   String minute;
			   String sec;
			   int year;
			   String date;
			   day = String.format("%02d", earlier.getDayOfMonth()); // java.time.Month = OCTOBER
			   month = String.format("%02d", earlier.getMonthValue()); // 10
			   year = earlier.getYear();
			   hour = String.format("%02d", earlier.getHour());
			   minute = String.format("%02d", earlier.getMinute());
			   sec = String.format("%02d", earlier.getSecond());
			   date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + sec;
			   return date;
		    }
		    
		    
		    /*
		     * checkDateExists
		     */
		    public static void checkDateExists(FilterParam filterParam, int count, String checkingCondition) {
			   LocalDateTime now;
			   LocalDateTime earlier;
			   String date;
			   switch (checkingCondition) {
				  case "M":
					 now = LocalDateTime.now();// 2015-11-24
					 earlier = now.minus(count, ChronoUnit.MONTHS);
					 date = getDatesforDataQuery(earlier);
					 filterParam.setValueFirst(date);
					 break;
				  case "Y":
					 now = LocalDateTime.now();// 2015-11-24
					 earlier = now.minus(count, ChronoUnit.YEARS);
					 date = getDatesforDataQuery(earlier);
					 filterParam.setValueFirst(date);
					 break;
				  case "W":
					 now = LocalDateTime.now();// 2015-11-24
					 earlier = now.minus(count, ChronoUnit.WEEKS);
					 earlier = now.minusWeeks(count);
					 date = getDatesforDataQuery(earlier);
					 filterParam.setValueFirst(date);
					 break;
				  case "D":
					 now = LocalDateTime.now();// 2015-11-24
					 earlier = now.minus(count, ChronoUnit.DAYS);
					 date = getDatesforDataQuery(earlier);
					 filterParam.setValueFirst(date);
					 break;
			   }
		    }
}
