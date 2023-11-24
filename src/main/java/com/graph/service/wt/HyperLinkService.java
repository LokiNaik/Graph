package com.graph.service.wt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.graph.model.wt.DataListModel;
import com.graph.model.wt.HyperLinkModel;
import com.graph.model.wt.Message;
import com.graph.model.wt.SettingsHyperlink;
import com.graph.utils.DateUtil;
import com.graph.model.wt.AppHyperLink;

@Service
public class HyperLinkService implements IHyperLinkService {

    @Autowired
    @Qualifier(value = "primaryMongoTemplate")
    private MongoTemplate mongoTemplate;
    @Value("${currentlocale}")
    private String currentLocale; 
    @Value("${settings.path}")
    private String filePath;
    @Value("${settings.folder}")
    private String settingsFolder;
    private final Logger logger = LoggerFactory.getLogger(getClass());

	
/*
 * GetHyperLink	
 */
public List<JSONObject> getHyperLink(String request) {
    	
        List<JSONObject> settings = new ArrayList<JSONObject>();
        JSONParser jsonParser = new JSONParser();
        FileReader file = null;
        
        String fileConfiguration = settingsFolder + filePath ;
    	File file1 = new File(fileConfiguration);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file1)))){
        
            List<JSONObject> result = (List<JSONObject>) jsonParser.parse(reader);
            for (JSONObject output : result) {
                if (output.get("createdBy").equals(request)) {
                    settings.add(output);
                }
            }
        } catch (Exception e) {
			 logger.error("Fetch Hyperlink error : " + e);

        }
        return settings;
    }

/*
 * SaveHyperlink
 */
@SuppressWarnings("unchecked")
@Override

public DataListModel saveHyperLink(HyperLinkModel request) throws FileNotFoundException {
	DataListModel datalist = new DataListModel();
	Message message=new Message();
	String fileConfiguration = settingsFolder + filePath ;
	File file = new File(fileConfiguration);
	
	try {
		file.createNewFile();
	} catch (IOException e2) {
		// TODO Auto-generated catch block
		 logger.info("Creating File error : " + e2);

	}
	   
   
   for (SettingsHyperlink settingsHyperlink : request.getSettings()) {
	  String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(DateUtil.getCurrentDateInUTC());
	  settingsHyperlink.setLastUpdateddate(date);
   }
   ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
   String json = null;

   
   try {
	  json = ow.writeValueAsString(request);

   }
   catch (JsonProcessingException e1) {
	  e1.printStackTrace();
   }
   String outputToAdd = new String("["+json+"]");

   byte[] dataBytes = outputToAdd.getBytes();
   try (FileWriter writer = new FileWriter(fileConfiguration)){

	  writer.write("[");
	  writer.write(json);
	  writer.write("]");
	  writer.close();
	  message.setCode(200);
	  message.setSuccess(true);
   }catch(Exception e) {
		 logger.info("Writing File error : " + e);

   }
   datalist.setStatus(message);
   return datalist;
}
}
