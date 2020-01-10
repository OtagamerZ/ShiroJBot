package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.handlers.api.exception.Exception;
import com.kuuhaku.utils.Helper;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
@ControllerAdvice
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	public Exception error(@RequestParam(value = "code") String code, @RequestParam(value = "cause") String cause) {
		return new Exception(Integer.parseInt(code), cause);
	}

	@ExceptionHandler(java.lang.Exception.class)
	public String collectData(Exception e) {
		try {
			return "forward:/error?code=" + e.getCode() + "&cause=" + URLEncoder.encode(e.getCause(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Helper.logger(this.getClass()).error(ex + " | " + ex.getStackTrace()[0]);
			return null;
		}
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}
