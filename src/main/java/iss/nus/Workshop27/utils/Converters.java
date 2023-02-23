package iss.nus.Workshop27.utils;

import java.io.Reader;
import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class Converters {
    
    public static JsonObject toJson(String str) {
		Reader reader = new StringReader(str);
		JsonReader jsonReader = Json.createReader(reader);
		return jsonReader.readObject();
	}

}
