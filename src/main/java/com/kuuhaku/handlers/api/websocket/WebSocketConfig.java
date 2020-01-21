package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final Logger LOGGER = Helper.logger(WebSocketConfig.class);

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/chat");
		registry.addEndpoint("/chat").withSockJS();
	}

	@MessageMapping("/chat")
	@SendTo("/topic/messages")
	public OutputMessage send(Message message) throws Exception {
		String time = new SimpleDateFormat("HH:mm").format(new Date());
		return new OutputMessage(message.getFrom(), message.getText(), time);
	}
}