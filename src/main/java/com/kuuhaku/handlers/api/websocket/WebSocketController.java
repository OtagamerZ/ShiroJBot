package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

	@MessageMapping("/chat/websocket")
	@SendTo("/chat/websocket")
	public String onReceivedMessage(String message) {
		Helper.logger(this.getClass()).info("Mensagem recebida: " + message);
		return message;
	}
}
