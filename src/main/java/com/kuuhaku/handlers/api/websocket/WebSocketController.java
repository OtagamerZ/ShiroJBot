package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class WebSocketController {

	private SimpMessagingTemplate template;

	@Autowired
	WebSocketController(SimpMessagingTemplate template) {
		this.template = template;
	}

	@RequestMapping(path = "/chat", method = POST)
	public void onReceivedMessage(String message) {
		Helper.logger(this.getClass()).info("Mensagem recebida: " + message);
		this.template.convertAndSend("/topic/message", message);
	}
}
