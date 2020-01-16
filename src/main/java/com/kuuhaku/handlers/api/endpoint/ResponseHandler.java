package com.kuuhaku.handlers.api.endpoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ResponseHandler {

	public static ResponseEntity<Object> generateResponse(HttpStatus status, boolean error, String message, Object responseObj) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			map.put("timestamp", new Date());
			map.put("status", status.value());
			map.put("error", error);
			map.put("message", message);
			map.put("data", responseObj);

			return new ResponseEntity<Object>(map, status);
		} catch (Exception e) {
			map.clear();
			map.put("timestamp", new Date());
			map.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
			map.put("message", e.getMessage());
			return new ResponseEntity<Object>(map, status);
		}
	}
}