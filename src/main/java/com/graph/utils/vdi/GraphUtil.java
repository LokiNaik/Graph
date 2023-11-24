package com.graph.utils.vdi;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graph.service.vdi.GraphService;

public class GraphUtil {
	public final static Logger logger = LoggerFactory.getLogger(GraphService.class);

	public static Map<String, String> getLineGraphSchema(String file) throws IOException {

		Map<String, String> lineHeaderMap = new LinkedHashMap<>();
		Path path = Paths.get(file);
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			String str;
			while ((str = reader.readLine()) != null) {
				String[] colonSplit = str.split("=");
				lineHeaderMap.put(colonSplit[0].trim(), colonSplit[1].trim());
			}
		}
		return lineHeaderMap;
	}

}
