package com.kuuhaku.handlers.api;

import com.kuuhaku.utils.Helper;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	public Application() {
		Thread.currentThread().setName("api");
		Helper.logger(this.getClass()).info("API inicializada.");
	}
}
