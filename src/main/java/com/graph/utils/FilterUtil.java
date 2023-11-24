package com.graph.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import com.graph.model.vdi.FilterParamVdi;
import com.graph.model.vdi.LockParamVdi;
import com.graph.model.vdi.SortParamVdi;
import com.graph.model.wt.FilterParam;
import com.graph.model.wt.LockParam;
import com.graph.model.wt.SortParam;
import com.graph.service.wt.AdvanceGraphService;

public class FilterUtil {
	
	public  static final Logger logger = LoggerFactory.getLogger(FilterUtil.class);
	
	public  static Sort generateSortQueryDynamically(List<SortParam> sortParams, LockParam lock) throws IOException {
	
		Sort sortCombinedQuery = null;

		List<Sort> listSort = new ArrayList<Sort>();
		Sort sort = null;
		List<SortParam> sortParmsList = Collections.synchronizedList(sortParams);

		try {
			if (sortParams != null && sortParams.size() >= 1) {
				if (lock != null && lock.isLock()) {

					SortParam tempObj = new SortParam();
					tempObj.setField(lock.getField());
					int itemPos = sortParmsList.indexOf(tempObj);
					SortParam sortParamObj = sortParmsList.get(itemPos);
					sortParmsList.remove(sortParamObj);
					sortParmsList.add(0, sortParamObj);

				}
				for (SortParam sortObj : sortParmsList) {
					sort = Sort.by(sortObj.getField());
					if (sortObj.isAsc()) {
						sort = sort.ascending();
					} else {
						sort = sort.descending();
					}
					listSort.add(sort);
					sort = null;
				}

				sortCombinedQuery = null;
				sortCombinedQuery = listSort.get(0);

				for (int i = 1; i < listSort.size(); i++) {
					sortCombinedQuery = sortCombinedQuery.and(listSort.get(i));
				}
			}

		} catch (Exception e) {
			 logger.error("Generating Sorting Query Error");
		}

		return sortCombinedQuery;
	}

	public static Criteria generateCriteriaForList(String fieldName, FilterParam searchFields, String CollectionName) {
	    String operator;
	    String valueFirst;
	    boolean exclude = searchFields.isExclude();
	    Criteria criteria = null;
	    List<String> excludeValues = new ArrayList<String>();
	    String includeValue=null;
	    
	    if (CollectionName != null && !fieldName.contains("ProductInfo.deviceName")) {
	    	
			fieldName = CollectionName + "." + fieldName;
		}
	 
	    try {
		   logger.info("Generating Criteria for String fileds");
		   if (fieldName == null) {
			  fieldName = searchFields.getField() != "" ? searchFields.getField() : "";
		   }
		   
		   if(fieldName.equals(FilterConstants.ACCESS_TYPE)) {
			    
			   List<String> newExcludeValues = (List<String>) searchFields.getExcludeValues();
				if(searchFields.getIncludeValue()!=null) {
			   includeValue = searchFields.getIncludeValue().toLowerCase();
				}
				
				if(newExcludeValues.size() > 0 && newExcludeValues!=null){
					for(String excludeValue : newExcludeValues){
					excludeValue = excludeValue.toLowerCase();
					excludeValues.add(excludeValue);
					}
					}
			   }
			   
			   else {
				   excludeValues = (List<String>) searchFields.getExcludeValues();
				   includeValue = searchFields.getIncludeValue();
			   }
		   operator = searchFields.getOperator() != "" ? searchFields.getOperator() : "";
		   valueFirst = searchFields.getValueFirst() != "" ? searchFields.getValueFirst() : "";

		   if ((excludeValues != null && excludeValues.size() > 0)
					|| (includeValue != null && !includeValue.equals("") && includeValue.length() > 0)) {

				if (includeValue != null && !includeValue.equals("") && includeValue.length() > 0) {
					if (excludeValues == null) {
						excludeValues = new ArrayList<String>();
					}
					criteria = Criteria.where(fieldName).is(includeValue)
							.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					return criteria;
				} else if(excludeValues != null && excludeValues.size() > 0){
					criteria = Criteria.where(fieldName).not().in(excludeValues);
					return criteria;
				}
		   }else {  
		   if (operator.equalsIgnoreCase(FilterConstants.ENDSWITH)) {
			  final String field = fieldName;
			  String[] arrOfStr = valueFirst.split(",");
			  List<String> values = Arrays.asList(arrOfStr);
			  Criteria[] criterias = values.stream().map(s -> Criteria.where(field).regex(s + "$", "i"))
					.toArray(Criteria[]::new);
			  criteria = new Criteria().orOperator(criterias);
		   }
		   else if (operator.equalsIgnoreCase(FilterConstants.STARTSWITH)) {
			  final String field = fieldName;
			  String[] arrOfStr = valueFirst.split(",");
			  List<String> values = Arrays.asList(arrOfStr);
			  Criteria[] criterias = values.stream().map(s -> Criteria.where(field).regex("^" + s, "i"))
					.toArray(Criteria[]::new);
			  criteria = new Criteria().orOperator(criterias);
		   }

		   else if (operator.equalsIgnoreCase(FilterConstants.CONTAINS)) {
			  final String field = fieldName;
			  String[] arrOfStr = valueFirst.split(",");
			  List<String> values = Arrays.asList(arrOfStr);
			  Criteria[] criterias = values.stream().map(s -> Criteria.where(field).regex(s, "i"))
					.toArray(Criteria[]::new);
			  criteria = new Criteria().orOperator(criterias);
		   }
		   else if (operator.equalsIgnoreCase(FilterConstants.EQUAL)) {
			  String[] arrOfStr = valueFirst.split(",");
			  criteria = Criteria.where(fieldName).in(arrOfStr);
		   }
		   else {
			  criteria = Criteria.where(fieldName).regex("^");
		   }
		   logger.info("Generated Criteria for the Field:" + fieldName);
	    }
		   
	    }

	    catch (Exception e) {
		   logger.error("Exception occurred in generating criteria:" + e.getMessage());
	    }

	    return criteria;
	}
	
	public static Criteria generateCriteriaForString(String fieldName,FilterParam searchFields) {
	    
	    String operator;
	    String valueFirst;

	    Criteria criteria = null;

	    @SuppressWarnings("unchecked")
	    List<String> excludeValues = (List<String>) searchFields.getExcludeValues();
	    String includeValue = searchFields.getIncludeValue();

	    try {
		   logger.info("Generating Criteria for String fileds");
		   if (fieldName == null) {
			  fieldName = searchFields.getField() != "" ? searchFields.getField() : "";
		   }
			if (fieldName.equals(FilterConstants.ACCESS_TYPE) && includeValue != null) {
				includeValue = includeValue.toLowerCase();
			}
			if (fieldName.equals(FilterConstants.ACCESS_TYPE) && (excludeValues != null && excludeValues.size() > 0)) {
				ListIterator<String> listIterator = excludeValues.listIterator();
				while (listIterator.hasNext()) {
					listIterator.set(listIterator.next().toLowerCase());
				}
			}
		   operator = searchFields.getOperator() != "" ? searchFields.getOperator() : "";
		   valueFirst = searchFields.getValueFirst() != "" ? searchFields.getValueFirst() : "";

		   if (valueFirst.length() > 0 && !valueFirst.equals("")) {

			  if (valueFirst.contains("*")) {
				 valueFirst = valueFirst.replace("*", ".*");
			  }
			  if (valueFirst.contains("?")) {
				 valueFirst = valueFirst.replace("?", ".?");
			  }
			  if (valueFirst.contains("+")) {
				 valueFirst = valueFirst.replace("+", ".+");
			  }
		   }
		   if ((excludeValues != null && excludeValues.size() > 0)
				 || (includeValue != null && !includeValue.equals("") && includeValue.length() > 0)) {

			  if (includeValue != null && !includeValue.equals("") && includeValue.length() > 0) {
				 if (excludeValues == null) {
					excludeValues = new ArrayList<String>();
				 }
				 criteria = Criteria.where(fieldName).is(includeValue)
					    .andOperator(Criteria.where(fieldName).not().in(excludeValues));
				 return criteria;
			  }
			  else {

				 if (operator.equalsIgnoreCase(FilterConstants.ENDSWITH)) {
					criteria = Criteria.where(fieldName).regex(valueFirst + "$", "i")
						   .andOperator(Criteria.where(fieldName).not().in(excludeValues));
				 }
				 else if (operator.equalsIgnoreCase(FilterConstants.STARTSWITH)) {
					criteria = Criteria.where(fieldName).regex("^" + valueFirst, "i")
						   .andOperator(Criteria.where(fieldName).not().in(excludeValues));
				 }
				 else if (operator.equalsIgnoreCase(FilterConstants.CONTAINS)) {
					criteria = Criteria.where(fieldName).regex(valueFirst, "i")
						   .andOperator(Criteria.where(fieldName).not().in(excludeValues));
				 }
				 else if (operator.equalsIgnoreCase(FilterConstants.EQUAL)) {
					criteria = Criteria.where(fieldName).is(valueFirst)
						   .orOperator(Criteria.where(fieldName).regex("^" + valueFirst + "$"))
						   .andOperator(Criteria.where(fieldName).not().in(excludeValues));
				 }
				 else {
					criteria = Criteria.where(fieldName).not().in(excludeValues);
				 }
			  }
		   }
			else {
				if (operator.equalsIgnoreCase(FilterConstants.ENDSWITH)) {
					criteria = Criteria.where(fieldName).regex(valueFirst + "$", "i");
				} else if (operator.equalsIgnoreCase(FilterConstants.STARTSWITH)) {
					criteria = Criteria.where(fieldName).regex("^" + valueFirst, "i");
				} else if (operator.equalsIgnoreCase(FilterConstants.CONTAINS)) {
					criteria = Criteria.where(fieldName).regex(valueFirst, "i");
				} else if (operator.equalsIgnoreCase(FilterConstants.EQUAL)) {
					if (valueFirst.equalsIgnoreCase("-")) {
						criteria = Criteria.where(fieldName).exists(false);
					} else if (valueFirst.equalsIgnoreCase("private,common")) {
						criteria = Criteria.where(fieldName).exists(true);
					} else if (valueFirst.equalsIgnoreCase("common,-")) {
						criteria = Criteria.where(fieldName).nin("private");
					} else if (valueFirst.equalsIgnoreCase("private,-")) {
						criteria = Criteria.where(fieldName).nin("common");
					} else {
						criteria = Criteria.where(fieldName).is(valueFirst)
								.orOperator(Criteria.where(fieldName).regex("^" + valueFirst + "$"));
					}
				} else if (fieldName.equalsIgnoreCase("accessType") && operator.equalsIgnoreCase("true")) {
					if (valueFirst.equalsIgnoreCase("true")) {
						criteria = Criteria.where(fieldName).nin("true");
					}
				} else {
					criteria = Criteria.where(fieldName).regex("^");
				}
				logger.info("Generated Criteria for the Field:" + fieldName);
			}
	    }
	    catch (Exception e) {
		   logger.error("Exception occurred in generating criteria:" + e.getMessage());
	    }

	    return criteria;
	}

	public static Sort generateSortQueryDynamically(List<SortParamVdi> sortParams, LockParamVdi lock,
			String fileName) throws IOException {
		Sort sortCombinedQuery = null;
		List<Sort> listSort = new ArrayList<Sort>();
		Sort sort = null;

		if (sortParams != null) {
			List<SortParamVdi> sortParmsList = Collections.synchronizedList(sortParams);
			if (sortParams != null && sortParams.size() >= 1) {
				if (lock != null && lock.isLock() && !lock.getField().equals("")) {
					SortParam tempObj = new SortParam();
					tempObj.setField(lock.getField());
					int itemPos = sortParmsList.indexOf(tempObj);

					if (itemPos >= 0) {
						SortParamVdi sortParamObj = sortParmsList.get(itemPos);
						sortParmsList.remove(sortParamObj);
						sortParmsList.add(0, sortParamObj);
					}
				}

				for (SortParamVdi sortObj : sortParmsList) {
					String fieldName = sortObj.getField().trim();
					Map<String, String> jsonObject = getMasterColumnSchema(fileName);
					String fieldNameInCollection = jsonObject.get(fieldName);
					if (fieldNameInCollection != null) {
						fieldName = fieldNameInCollection;
					}
					sort = Sort.by(fieldName);
					if (sortObj.isAsc()) {
						sort = sort.ascending();
					} else {
						sort = sort.descending();
					}
					listSort.add(sort);
					sort = null;
				}
				sortCombinedQuery = null;
				sortCombinedQuery = listSort.get(0);

				for (int i = 1; i < listSort.size(); i++) {
					sortCombinedQuery = sortCombinedQuery.and(listSort.get(i));
				}
				logger.info("Combined Sort Query :" + sortCombinedQuery);
			}
		}
		return sortCombinedQuery;
	}

		public static Map<String, String> getMasterColumnSchema(String file) throws IOException {

		Map<String, String> columnHeaderMap = new LinkedHashMap<>();
		Path path = Paths.get(file);
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String str;
			while ((str = reader.readLine()) != null) {
				String[] colonSplit = str.split("=");
				columnHeaderMap.put(colonSplit[0].trim(), colonSplit[1].trim());
			}
		}
		return columnHeaderMap;
	}

		public static Criteria generateCriteriaForNumberArrayFileds(FilterParamVdi searchFilter, String fieldName,
				Map<String, String> jsonObject) throws IOException {

			List<?> excludeValues = searchFilter.getExcludeValues();
			String operator = searchFilter.getOperator() != "" ? searchFilter.getOperator() : "";
			List<Double> firstValues = new ArrayList<>();
			if (searchFilter.getFirstValues() != null && searchFilter.getFirstValues().size() > 0) {
				firstValues = searchFilter.getFirstValues().stream()
						.map(val -> Double.valueOf(val.toString()))
						.collect(Collectors.toList());
			}
			Criteria criteria = null;

			String fieldNameInCollection = jsonObject.get(fieldName);
			if (fieldNameInCollection != null) {
				fieldName = fieldNameInCollection;
			}

			String inclVal = searchFilter.getIncludeValue();
			Double includeValue = null;
			if (inclVal != null && !inclVal.equals("")) {
				includeValue = Double.parseDouble(inclVal);
			}

			if ((excludeValues != null && excludeValues.size() > 0) || (includeValue != null)) {
				@SuppressWarnings("unchecked")
				List<Number> excludeValuesNum = (List<Number>) excludeValues;
				if (includeValue != null) {
					criteria = Criteria.where(fieldName).is(includeValue)
							.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
				} else {
					if (operator.equals(FilterConstants.IN)) {
						criteria = Criteria.where(fieldName).in(firstValues)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else {
						criteria = Criteria.where(fieldName).not().in(excludeValuesNum);
					}
				}
			} else {
				if (operator.equals(FilterConstants.IN)) {
					criteria = Criteria.where(fieldName).in(firstValues);
				} else {
					criteria = Criteria.where(fieldName);
				}
			}

			logger.info("Generated filter criteria for the number field:" + fieldName);
			return criteria;
		}

		public static Criteria generateCriteriaForNumberFileds(FilterParamVdi searchFilter, String fieldName,
				Map<String, String> jsonObject) throws IOException {

			List<?> excludeValues = searchFilter.getExcludeValues();
			String operator = searchFilter.getOperator() != "" ? searchFilter.getOperator() : "";
			double firstValue = Double.parseDouble(searchFilter.getValueFirst() != "" ? searchFilter.getValueFirst() : "0");
			double secondValue = Double
					.parseDouble(searchFilter.getValueSecond() != "" ? searchFilter.getValueSecond() : "0");
			Criteria criteria = null;

			String fieldNameInCollection = jsonObject.get(fieldName);
			if (fieldNameInCollection != null) {
				fieldName = fieldNameInCollection;
			}

			String inclVal = searchFilter.getIncludeValue();
			Double includeValue = null;
			if (inclVal != null && !inclVal.equals("")) {
				includeValue = Double.parseDouble(inclVal);
			}

			if ((excludeValues != null && excludeValues.size() > 0) || (includeValue != null)) {
				@SuppressWarnings("unchecked")
				List<Number> excludeValuesNum = (List<Number>) excludeValues;
				if (includeValue != null) {
					criteria = Criteria.where(fieldName).is(includeValue)
							.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
				} else {
					if (operator.equals(FilterConstants.BETWEEN)) {
						criteria = Criteria.where("")
								.orOperator(Criteria.where(fieldName).gte(firstValue),
										Criteria.where(fieldName).lte(secondValue))
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else if (operator.equals(FilterConstants.GREATER_THAN)) {
						criteria = Criteria.where(fieldName).gt(firstValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else if (operator.equals(FilterConstants.GREATER_THAN_OR_EQUAL_TO)) {
						criteria = Criteria.where(fieldName).gte(firstValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else if (operator.equals(FilterConstants.LESS_THAN)) {
						criteria = Criteria.where(fieldName).lt(firstValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else if (operator.equals(FilterConstants.LESS_THAN_OR_EQUAL_TO)) {
						criteria = Criteria.where(fieldName).lte(firstValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else if (operator.equals(FilterConstants.EQUAL_TO)) {
						criteria = Criteria.where(fieldName).is(firstValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else if (operator.equals(FilterConstants.NOT_EQUAL_TO)) {
						criteria = Criteria.where(fieldName).ne(firstValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else {
						criteria = Criteria.where(fieldName).not().in(excludeValuesNum);
					}
				}
			} else {
				if (operator.equals(FilterConstants.BETWEEN)) {
					criteria = Criteria.where(fieldName).gte(firstValue)
							.andOperator(Criteria.where(fieldName).lte(secondValue));
				} else if (operator.equals(FilterConstants.GREATER_THAN)) {
					criteria = Criteria.where(fieldName).gt(firstValue);
				} else if (operator.equals(FilterConstants.GREATER_THAN_OR_EQUAL_TO)) {
					criteria = Criteria.where(fieldName).gte(firstValue);
				} else if (operator.equals(FilterConstants.LESS_THAN)) {
					criteria = Criteria.where(fieldName).lt(firstValue);
				} else if (operator.equals(FilterConstants.LESS_THAN_OR_EQUAL_TO)) {
					criteria = Criteria.where(fieldName).lte(firstValue);
				} else if (operator.equals(FilterConstants.EQUAL_TO)) {
					criteria = Criteria.where(fieldName).is(firstValue);
				} else if (operator.equals(FilterConstants.NOT_EQUAL_TO)) {
					criteria = Criteria.where(fieldName).ne(firstValue);
				} else {
					criteria = Criteria.where(fieldName).is(firstValue)
							.orOperator(Criteria.where(fieldName).is(secondValue));
				}
			}

			logger.info("Generated filter criteria for the number field:" + fieldName);
			return criteria;
		}

		public static Criteria generateCriteriaForStringArrayFields(FilterParamVdi searchFilter, String fieldName,
				Map<String, String> jsonObject) throws IOException {

			List<?> excludeValues = searchFilter.getExcludeValues();
			String includeValue = searchFilter.getIncludeValue();
			String operator = searchFilter.getOperator() != "" ? searchFilter.getOperator() : "";
			Criteria criteria = null;
			List<String> firstValues = new ArrayList<>();
			
			if (searchFilter.getFirstValues() != null && searchFilter.getFirstValues().size() > 0) {
					firstValues = searchFilter.getFirstValues().stream().map(val ->val.toString()).collect(Collectors.toList());
			}		

			String fieldNameInCollection = jsonObject.get(fieldName);
			if (fieldNameInCollection != null) {
				fieldName = fieldNameInCollection;
			}

			if ((excludeValues != null && excludeValues.size() > 0) || (includeValue != null)) {
				@SuppressWarnings("unchecked")
				List<Number> excludeValuesNum = (List<Number>) excludeValues;
				if (includeValue != null) {
					criteria = Criteria.where(fieldName).is(includeValue)
							.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
				} else {
					if (operator.equals(FilterConstants.IN)) {
						criteria = Criteria.where(fieldName).in(firstValues)
								.andOperator(Criteria.where(fieldName).not().in(excludeValuesNum));
					} else {
						criteria = Criteria.where(fieldName).not().in(excludeValuesNum);
					}
				}
			} else {
				if (operator.equals(FilterConstants.IN)) {
					criteria = Criteria.where(fieldName).in(firstValues);
				} else {
					criteria = Criteria.where(fieldName);
				}
			}

			logger.info("Generated filter criteria for the number field:" + fieldName);
			return criteria;
		}

		public static Criteria generateCriteriaForString(FilterParamVdi searchFilter, String fieldName,
				Map<String, String> jsonObject) throws IOException {
			String operator;
			String valueFirst;
			List<?> excludeValues = searchFilter.getExcludeValues();
			String includeValue = searchFilter.getIncludeValue();
			Criteria criteria = null;

			String fieldNameInCollection = jsonObject.get(fieldName);
			if (fieldNameInCollection != null) {
				fieldName = fieldNameInCollection;
			}

			operator = searchFilter.getOperator() != "" ? searchFilter.getOperator() : "";
			valueFirst = searchFilter.getValueFirst() != "" ? searchFilter.getValueFirst() : "";

			if (valueFirst.length() > 0 && !valueFirst.equals("")) {
				if (valueFirst.contains(FilterConstants.ASTERISK)) {
					valueFirst = valueFirst.replace(FilterConstants.ASTERISK, FilterConstants.ZERO_OR_MORE);
				}
				if (valueFirst.contains(FilterConstants.QUESTION_MARK)) {
					valueFirst = valueFirst.replace(FilterConstants.QUESTION_MARK, FilterConstants.ZERO_OR_ONE);
				}
				if (valueFirst.contains(FilterConstants.PLUS)) {
					valueFirst = valueFirst.replace(FilterConstants.PLUS, FilterConstants.ONE_OR_MORE);
				}
				if (valueFirst.contains(".") && !operator.equals(FilterConstants.EQUAL)) {
					valueFirst = valueFirst.replaceAll("\\.", "\\\\.");
				}
			}

			if ((excludeValues != null && excludeValues.size() >= 0)
					|| (includeValue != null && !includeValue.equals("") && includeValue.length() >= 0)) {

				if (includeValue != null && includeValue.length() >= 0) {
					if (excludeValues == null) {
						excludeValues = new ArrayList<String>();
					}
					criteria = Criteria.where(fieldName).is(includeValue)
							.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					return criteria;
				} else {
					if (operator.equals(FilterConstants.ENDSWITH)) {
						criteria = Criteria.where(fieldName)
								.regex(valueFirst + FilterConstants.END_OF_LINE, FilterConstants.IGNORE_CASE)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else if (operator.equals(FilterConstants.STARTSWITH)) {
						criteria = Criteria.where(fieldName)
								.regex(FilterConstants.START_OF_LINE + valueFirst, FilterConstants.IGNORE_CASE)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else if (operator.equals(FilterConstants.CONTAINS)) {
						criteria = Criteria.where(fieldName).regex(valueFirst, FilterConstants.IGNORE_CASE)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else if (operator.equals(FilterConstants.EQUAL)||operator.equals(FilterConstants.EQUAL_TO)) {
						criteria = Criteria.where(fieldName).is(valueFirst)
								.orOperator(Criteria.where(fieldName)
										.regex(FilterConstants.START_OF_LINE + valueFirst + FilterConstants.END_OF_LINE))
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else if (operator.equals(FilterConstants.NOTEQUALS)) {
						criteria = Criteria.where(fieldName).ne(valueFirst)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else {
						criteria = Criteria.where(fieldName).not().in(excludeValues);
					}
				}
				logger.info("Generated filter criteria for the string field:" + fieldName);
			} else {
				if (operator.equals(FilterConstants.ENDSWITH)) {
					criteria = Criteria.where(fieldName).regex(valueFirst + FilterConstants.END_OF_LINE,
							FilterConstants.IGNORE_CASE);
				} else if (operator.equals(FilterConstants.STARTSWITH)) {
					criteria = Criteria.where(fieldName).regex(FilterConstants.START_OF_LINE + valueFirst,
							FilterConstants.IGNORE_CASE);
				} else if (operator.equals(FilterConstants.CONTAINS)) {
					criteria = Criteria.where(fieldName).regex(valueFirst, FilterConstants.IGNORE_CASE);
				} else if (operator.equals(FilterConstants.EQUAL)||operator.equals(FilterConstants.EQUAL_TO)) {
					criteria = Criteria.where(fieldName).is(valueFirst).orOperator(Criteria.where(fieldName)
							.regex(FilterConstants.START_OF_LINE + valueFirst + FilterConstants.END_OF_LINE));
				} else if (operator.equals(FilterConstants.NOTEQUALS)) {
					criteria = Criteria.where(fieldName).ne(valueFirst);
				} else {
					criteria = Criteria.where(fieldName).regex(FilterConstants.START_OF_LINE);
				}
				logger.info("Generated filter criteria for the string field:" + fieldName);
			}
			return criteria;
		}

		public static Criteria generateCriteriaForBooleanAndNull(FilterParamVdi searchFilter, String fieldName,
				Map<String, String> jsonObject) throws IOException {
			String operator;
			Boolean valueFirst;
			List<?> excludeValues = searchFilter.getExcludeValues();
			String inclVal = searchFilter.getIncludeValue();
			Criteria criteria = null;

			String fieldNameInCollection = jsonObject.get(fieldName);
			if (fieldNameInCollection != null) {
				fieldName = fieldNameInCollection;
			}
			Boolean includeValue = null;
			if (inclVal != null && !inclVal.equals("")) {
				includeValue = Boolean.parseBoolean(inclVal);
			}
			operator = searchFilter.getOperator() != "" ? searchFilter.getOperator() : "";
			valueFirst = searchFilter.getValueFirst() != "" ? Boolean.parseBoolean(searchFilter.getValueFirst()) : false;

			if (valueFirst == false) {
				valueFirst = null;
			}

			if ((excludeValues != null && excludeValues.size() > 0) || (includeValue != null)) {
				if (includeValue != null) {
					if (excludeValues == null) {
						excludeValues = new ArrayList<String>();
					}
					if (includeValue != false) {
						criteria = Criteria.where(fieldName).is(includeValue)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else {
						criteria = Criteria.where(fieldName).is(null)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					}
					return criteria;
				} else {

					if (!excludeValues.contains(true)) {
						excludeValues = new ArrayList<>();
						excludeValues.add(null);
					}

					if (excludeValues.contains(false) && excludeValues.contains(true)) {
						excludeValues.clear();
						List<Boolean> tempExcludeValues = new ArrayList<>();
						tempExcludeValues.add(null);
						tempExcludeValues.add(true);
						excludeValues = tempExcludeValues;
					}

					if (operator.equals(FilterConstants.EQUAL_TO)) {
						criteria = Criteria.where(fieldName).is(valueFirst)
								.andOperator(Criteria.where(fieldName).not().in(excludeValues));
					} else {
						criteria = Criteria.where(fieldName).not().in(excludeValues);
					}
				}
				logger.info("Generated filter criteria for the boolean field:" + fieldName);
			} else {
				if (operator.equals(FilterConstants.EQUAL_TO)) {
					criteria = Criteria.where(fieldName).is(valueFirst);
				}
				logger.info("Generated filter criteria for the boolean field:" + fieldName);
			}
			return criteria;
		}
}