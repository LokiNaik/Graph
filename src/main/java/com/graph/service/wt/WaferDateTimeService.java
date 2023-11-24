package com.graph.service.wt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.graph.model.wt.FilterParam;
import com.graph.model.wt.WaferDateTime;
import com.graph.utils.CriteriaUtil;
import com.graph.utils.DateUtil;

@Service
public class WaferDateTimeService {

	@Autowired
	@Qualifier(value = "primaryMongoTemplate")
	private MongoTemplate mongoTemplate;

	private static final String WAFER_START_TIME = "WaferStartTime";
    private static final String WAFER_END_TIME = "WaferEndTime";
    private static final String LOT_START_TIME = "LotStartTime";
    private static final String LOT_END_TIME = "LotEndTime";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	
	/*
     * handleWaferDates
     */
     WaferDateTime handleWaferDates(List<FilterParam> filterParams) {
	   List<LocalDate> startDateTotal = new ArrayList<LocalDate>();
	   List<LocalDate> endDateTotal = new ArrayList<LocalDate>();
	   int ws = 0;
	   int we = 0;
	   int ls = 0;
	   int le = 0;
	   String fromDate = null;
	   String toDate = null;
	   LocalDate toLocal = null;
	   LocalDateTime now;
	   LocalDate maxDate = null;
	   LocalDate minDate = null;
	   String from = null;
	   String day;
	   String month;
	   String hour;
	   String minute;
	   String sec;
	   String year;
	   String wsValueFirst = "";
	   String weValueFirst = "";
	   String lsValueFirst = "";
	   String leValueFirst = "";
	   LocalDate fromLocal = null;
	   now = LocalDateTime.now();
	   WaferDateTime waferDateTime = new WaferDateTime();
	   for (FilterParam filter : filterParams) {
		  String field = filter.getField();

		  if (!field.equalsIgnoreCase(WAFER_START_TIME) && !field.equalsIgnoreCase(WAFER_END_TIME)
				&& !field.equalsIgnoreCase(LOT_START_TIME) && !field.equalsIgnoreCase(LOT_END_TIME)) {
			 return waferDateTime;
		  }

		  if ((filterParams.size() == 1)
				&& (field.equalsIgnoreCase(WAFER_START_TIME) || field.equalsIgnoreCase(LOT_START_TIME)
					   || field.equalsIgnoreCase(LOT_END_TIME) || field.equalsIgnoreCase(WAFER_END_TIME))) {
			 getWLOneDataQuery(startDateTotal, endDateTotal, now, waferDateTime, filter);
		  }
		  else if (filterParams.size() >= 2) {
			 if (field.equalsIgnoreCase(WAFER_START_TIME)) {
				ws = ws + 1;
				wsValueFirst = filter.getValueFirst();
			 }
			 else if (field.equalsIgnoreCase(WAFER_END_TIME)) {
				we = we + 1;
				weValueFirst = filter.getValueFirst();
			 }
			 else if (field.equalsIgnoreCase(LOT_START_TIME)) {
				ls = ls + 1;
				lsValueFirst = filter.getValueFirst();
			 }
			 else if (field.equalsIgnoreCase(LOT_END_TIME)) {
				le = le + 1;
				leValueFirst = filter.getValueFirst();
			 }
		  }
	   }

	   for (FilterParam filter : filterParams) {
		  if (ws == 1 && we == 1) {
			 if (wsValueFirst.equals(weValueFirst)) {
				if (filter.getOperator().equals(">") || filter.getOperator().equals(">=")) {
				    if (filter.getField().equalsIgnoreCase(WAFER_START_TIME)) {
					   getGOperatorWSLS(startDateTotal, waferDateTime, filter);

				    }
				    else if (filter.getField().equalsIgnoreCase(WAFER_END_TIME)) {
					   getGOperatorWELE(endDateTotal, now, waferDateTime);
				    }
				}
				else if (filter.getOperator().equals("<") || filter.getOperator().equals("<=")) {
				    return waferDateTime;
				}
			 }
			 else if (!wsValueFirst.equals(weValueFirst)) {
				if (filter.getField().equalsIgnoreCase(WAFER_START_TIME)) {
				    getWSandLSDates(startDateTotal, waferDateTime, filter);

				}
				else if (filter.getField().equalsIgnoreCase(WAFER_END_TIME)) {
				    getWEAndLEDates(endDateTotal, waferDateTime, filter);

				}
			 }
		  }
		  else if (ls == 1 && le == 1) {
			 if (lsValueFirst.equals(leValueFirst)) {
				if (filter.getOperator().equals(">")) {
				    if (filter.getField().equalsIgnoreCase(LOT_START_TIME)) {
					   getGOperatorWSLS(startDateTotal, waferDateTime, filter);

				    }
				    else if (filter.getField().equalsIgnoreCase(LOT_END_TIME)) {
					   getGOperatorWELE(endDateTotal, now, waferDateTime);
				    }
				}
				else if (filter.getOperator() == "<") {
				    return waferDateTime;
				}
			 }
			 else if (!lsValueFirst.equals(leValueFirst)) {
				if (filter.getField().equalsIgnoreCase(LOT_START_TIME)) {
				    getWSandLSDates(startDateTotal, waferDateTime, filter);

				}
				else if (filter.getField().equalsIgnoreCase(LOT_END_TIME)) {
				    getWEAndLEDates(endDateTotal, waferDateTime, filter);
				}
			 }
		  }
		  else if ((ws >= 2 || ls >= 2 || (ws >= 2 && we >= 2)) || (we >= 2 || le >= 2 || (ls >= 2 && le >= 2))) {
			 getWLMultipleQueries(startDateTotal, waferDateTime, filter);
		  }
	   }
	   return waferDateTime;
    }

    /*
     * getWLMultipleQueries
     */
    private void getWLMultipleQueries(List<LocalDate> startDateTotal, WaferDateTime waferDateTime, FilterParam filter) {
	   String fromDate;
	   LocalDate maxDate;
	   LocalDate minDate;
	   String from;
	   from = formatDates(filter);
	   fromDate = CriteriaUtil.getDatesforDataQuery(LocalDateTime.parse(from, DateUtil.FORMATTER_YYYY_MM_DD));
	   LocalDate localFrom = LocalDate.parse(from, DateUtil.FORMATTER_YYYY_MM_DD);
	   startDateTotal.add(localFrom);
	   minDate = startDateTotal.stream().min(Comparator.comparing(LocalDate::toEpochDay)).get();
	   maxDate = startDateTotal.stream().max(Comparator.comparing(LocalDate::toEpochDay)).get();
	   waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(minDate));
	   waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(maxDate));
    }

    /*
     * getWLOneDataQuery
     */
    private void getWLOneDataQuery(List<LocalDate> startDateTotal, List<LocalDate> endDateTotal, LocalDateTime now,
		  WaferDateTime waferDateTime, FilterParam filter) {
	   String fromDate;
	   String toDate;
	   LocalDate maxDate;
	   LocalDate minDate;
	   String from;
	   from = formatDates(filter);
	   fromDate = CriteriaUtil.getDatesforDataQuery(LocalDateTime.parse(from, DateUtil.FORMATTER_YYYY_MM_DD));
	   LocalDate localFrom = LocalDate.parse(from, DateUtil.FORMATTER_YYYY_MM_DD);
	   startDateTotal.add(localFrom);
	   toDate = CriteriaUtil.getDatesforDataQuery(now);
	   LocalDate localTo = LocalDate.parse(toDate, DateUtil.FORMATTER_YYYY_MM_DD);
	   endDateTotal.add(localTo);
	   maxDate = endDateTotal.stream().max(Comparator.comparing(LocalDate::toEpochDay)).get();
	   minDate = startDateTotal.stream().min(Comparator.comparing(LocalDate::toEpochDay)).get();
	   waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(minDate));
	   waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(maxDate));
    }

    /*
     * getGOperatorWELE
     */
    private void getGOperatorWELE(List<LocalDate> endDateTotal, LocalDateTime now, WaferDateTime waferDateTime) {
	   String toDate;
	   LocalDate maxDate;
	   toDate = CriteriaUtil.getDatesforDataQuery(now);
	   LocalDate localTo = LocalDate.parse(toDate, DateUtil.FORMATTER_YYYY_MM_DD);
	   endDateTotal.add(localTo);
	   maxDate = endDateTotal.stream().max(Comparator.comparing(LocalDate::toEpochDay)).get();
	   waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(maxDate));
    }

    /*
     * getGOperatorWSLS
     */
    private void getGOperatorWSLS(List<LocalDate> startDateTotal, WaferDateTime waferDateTime, FilterParam filter) {
	   LocalDate minDate;
	   LocalDate fromLocal;
	   fromLocal = LocalDate.parse(filter.getValueFirst(), DateUtil.FORMATTER_YYYY_MM_DD);
	   startDateTotal.add(fromLocal);
	   minDate = startDateTotal.stream().min(Comparator.comparing(LocalDate::toEpochDay)).get();
	   waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(minDate));
    }

    /*
     * getWEAndLEDates
     */
    private void getWEAndLEDates(List<LocalDate> endDateTotal, WaferDateTime waferDateTime, FilterParam filter) {
	   LocalDate toLocal;
	   LocalDate maxDate;
	   toLocal = LocalDate.parse(filter.getValueFirst(), DateUtil.FORMATTER_YYYY_MM_DD);
	   endDateTotal.add(toLocal);
	   maxDate = endDateTotal.stream().max(Comparator.comparing(LocalDate::toEpochDay)).get();
	   waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(maxDate));
    }

    /*
     * getWSandLSDates
     */
    private void getWSandLSDates(List<LocalDate> startDateTotal, WaferDateTime waferDateTime, FilterParam filter) {
	   String fromDate;
	   LocalDate minDate;
	   String from;
	   from = formatDates(filter);
	   fromDate = CriteriaUtil.getDatesforDataQuery(LocalDateTime.parse(from, DateUtil.FORMATTER_YYYY_MM_DD));
	   LocalDate localFrom = LocalDate.parse(from, DateUtil.FORMATTER_YYYY_MM_DD);
	   startDateTotal.add(localFrom);
	   minDate = startDateTotal.stream().min(Comparator.comparing(LocalDate::toEpochDay)).get();
	   waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(minDate));
    }

    /*
     * formatDates
     */
    private String formatDates(FilterParam filter) {
	   String from;
	   String day;
	   String month;
	   String hour;
	   String minute;
	   String sec;
	   String year;
	   String[] arrOfStr = filter.getValueFirst().split("\\s+");
	   String[] arrOfStrDate = arrOfStr[0].split("-");
	   String[] arrOfStrDateTime = arrOfStr[1].split(":");
	   day = String.format("%02d", Integer.parseInt(arrOfStrDate[2]));
	   month = String.format("%02d", Integer.parseInt(arrOfStrDate[1]));
	   year = arrOfStrDate[0];
	   hour = String.format("%02d", Integer.parseInt(arrOfStrDateTime[0]));
	   minute = String.format("%02d", Integer.parseInt(arrOfStrDate[1]));
	   sec = String.format("%02d", Integer.parseInt(arrOfStrDate[2]));
	   from = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + sec;
	   return from;
    }

    /*
     * getQueryList
     */
    public List<Document> getQueryList(String dataQuery_Id) {
	   ObjectId objId = new ObjectId(dataQuery_Id);
	   MatchOperation matchQueryName = Aggregation.match(Criteria.where("_id").in(objId));
	   ProjectionOperation projectQueryNames = Aggregation.project().and("$queryList").as("queryList")
			 .andExclude("$_id");
	   Aggregation aggregation = Aggregation.newAggregation(matchQueryName, projectQueryNames);

	   List<Document> results = mongoTemplate.aggregate(aggregation, "DataQueries", Document.class).getMappedResults();
	   return results;
    }
    
    /*
     * getWaferDateResponse
     */
    WaferDateTime getWaferDateResponse(List<Document> testResults) {
	   String from;
	   List<Date> startDate = new ArrayList<Date>();
	   List<Date> endDate = new ArrayList<Date>();
	   List<LocalDate> startDateTotal = new ArrayList<LocalDate>();
	   List<LocalDate> endDateTotal = new ArrayList<LocalDate>();
	   List<Document> time = new ArrayList<Document>();
	   for (Document results : testResults) {
		  startDate = (List<Date>) results.get(WAFER_START_TIME);
		  endDate = (List<Date>) results.get(WAFER_END_TIME);
		  for (Date listDate : startDate) {
			 LocalDateTime ldt = LocalDateTime.ofInstant(listDate.toInstant(), ZoneId.systemDefault());
			 from = CriteriaUtil.getDatesforDataQuery(ldt);
			 LocalDate localDate = LocalDate.parse(from, DateUtil.FORMATTER_YYYY_MM_DD);
			 startDateTotal.add(localDate);
			 results.remove(WAFER_START_TIME);

		  }
		  for (Date listDate : endDate) {
			 LocalDateTime ldt = LocalDateTime.ofInstant(listDate.toInstant(), ZoneId.systemDefault());
			 from = CriteriaUtil.getDatesforDataQuery(ldt);
			 LocalDate localDate = LocalDate.parse(from, DateUtil.FORMATTER_YYYY_MM_DD);
			 endDateTotal.add(localDate);
			 results.remove(WAFER_END_TIME);
		  }
	   }

	   LocalDate maxDate = endDateTotal.stream().max(Comparator.comparing(LocalDate::toEpochDay)).get();
	   LocalDate minDate = startDateTotal.stream().min(Comparator.comparing(LocalDate::toEpochDay)).get();
	   WaferDateTime waferDateTime = new WaferDateTime();
	   waferDateTime.setWaferStartTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(minDate));
	   waferDateTime.setWaferEndTime(DateUtil.FORMATTER_YYYY_MM_DD_SLASH.format(maxDate));
	   return waferDateTime;
    }
    
}
