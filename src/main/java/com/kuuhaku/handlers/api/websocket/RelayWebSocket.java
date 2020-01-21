package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;
import org.apache.logging.log4j.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/relay")
public class RelayWebSocket {

	private static final Logger LOGGER = Helper.logger(RelayWebSocket.class);

	@OnOpen
	public void onOpen(Session session) {
		LOGGER.debug(String.format("WebSocket opened: %s", session.getId()));
	}

	@OnMessage
	public void onMessage(String txt, Session session) throws IOException {
		LOGGER.debug(String.format("Message received: %s", txt));
		session.getBasicRemote().sendText(txt.toUpperCase());
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) {
		LOGGER.debug(String.format("Closing a WebSocket (%s) due to %s", session.getId(), reason.getReasonPhrase()));
	}

	@OnError
	public void onError(Session session, Throwable t) {
		LOGGER.error(String.format("Error in WebSocket session %s%n", session == null ? "null" : session.getId()), t);
	}
}