package com.kuuhaku.handlers.api.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class ChatHandler {

	@MessageMapping("/hello")
	@SendTo("/topic/greetings")
	public Message greeting(Message message) throws Exception {
		return new Message("Test", "Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
	}
}
