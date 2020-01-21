package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class RelayWebSocket {

	@MessageMapping("/relay")
	@SendTo("/topic/relay")
	public Message onMessage(Sender sender) {
		Helper.logger(this.getClass()).info("sender");

		return new Message("Sua mensagem foi recebida, " + sender.getName());
	}
}
