package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/relay")
public class RelayWebsocket {

	@OnOpen
	public void onOpen(Session session) {
		Helper.logger(this.getClass()).info("Usuário conectado com o ID " + session.getId());
	}

	@OnClose
	public void onClose(Session session) {
		Helper.logger(this.getClass()).info("Usuário com o ID " +  session.getId() + " desconectado");
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		Helper.logger(this.getClass()).info("Mensagem recebida do usuário " + session.getId() + ": " + message);

		try {
			session.getBasicRemote().sendText("Hello Client " + session.getId() + "!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnError
	public void onError(Throwable t) {
		Helper.logger(this.getClass()).error("Erro no websocket: " + t.getMessage());
	}
}
