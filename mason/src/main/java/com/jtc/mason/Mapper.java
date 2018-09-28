package com.jtc.mason;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONObject;

public class Mapper {
	public void mapFile() {

		ClassLoader classLoader = getClass().getClassLoader();
		
		try {
			File file = new File(classLoader.getResource("data/main2.json").getFile());
			String contents = new String(Files.readAllBytes(file.toPath()));
			
			File mappedFile = new File(classLoader.getResource("data/mapped2.json").getFile());
			String mappedContents = new String(Files.readAllBytes(mappedFile.toPath()));
			
			JSONObject from = new JSONObject(contents);
			JSONObject template = new JSONObject(mappedContents);
			String path = this.getClass().getResource("/").getFile();

			Object to = readObj(from, template);

			if(to != null) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(path+"data/outbound.json"));
				writer.write(to.toString());
				writer.close();

				System.out.println("Successfully dropped file");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Object readObj(JSONObject from, JSONObject template) {
		Object to = null;
			
		if(template.has("path")) {
			String path = template.getString("path");
			if(path.toLowerCase().equals("$$empty")){
				to = new JSONObject();
				if(template.has("nested")){
					JSONObject nestedObject = (JSONObject)template.get("nested");
					for(String key : nestedObject.keySet()){
						Object nestedValue = readObj(from, (JSONObject)nestedObject.get(key));
						((JSONObject)to).put(key, nestedValue);
					}
				}
			} else {
				Object newObject = getValue(from, path);

				if(newObject instanceof JSONObject){
					JSONObject newValue = (JSONObject)newObject;
					if(template.has("nested")){
						JSONObject nestedObject = (JSONObject)template.get("nested");
						for(String key : nestedObject.keySet()){
							Object nestedValue = readObj(newValue, (JSONObject)nestedObject.get(key));
							newValue.put(key, nestedValue);
						}
					}
					to = newValue;
				} else if(newObject instanceof JSONArray){
					if(template.has("nested")){
						((JSONArray)newObject).forEach(item -> {
							JSONObject obj = (JSONObject) item;
							JSONObject nestedObject = (JSONObject)template.get("nested");
							
							for(String key : nestedObject.keySet()){
								Object nestedValue = readObj(obj, (JSONObject)nestedObject.get(key));
								obj.put(key, nestedValue);
							}
						});
						to = newObject;
					} else {
						to = newObject;
					}
				} else {
					to = newObject;
				}
			}
		} else {
			for(String key : template.keySet()) {
				if(template.get(key) instanceof JSONObject) {
					Object newValue = readObj(from, (JSONObject)template.get(key));
					if(newValue instanceof JSONObject)
					{
						if(to == null){ to = new JSONObject();}
						((JSONObject)to).put(key, newValue);
					}
				}
			}
		}

		return to;
	}
	
	private Object getValue(JSONObject from, String path) {
		String [] keys = path.split("\\.");
		Object value = from;
		for(String key : keys) {
			if(value instanceof JSONObject && ((JSONObject)value).has(key)){
				if(((JSONObject)value).get(key) instanceof JSONObject){
					value = ((JSONObject)value).getJSONObject(key);
				} else if(((JSONObject)value).get(key) instanceof JSONArray){
					value = ((JSONObject)value).getJSONArray(key);
				} else {
					value = ((JSONObject)value).get(key);
				}
			} else if(value instanceof JSONArray) {
				value = from.getJSONArray(key);
			} else {
				value = value;
			}
		}

		// remove last key
		if(value != null){
			String lastKey = keys[keys.length-1];
			from.remove(lastKey);
		}
		
		return value;
	}
}

