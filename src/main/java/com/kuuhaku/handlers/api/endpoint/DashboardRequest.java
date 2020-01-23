package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.controller.mysql.DashboardDAO;
import com.kuuhaku.controller.mysql.GlobalMessageDAO;
import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.AppUser;
import com.kuuhaku.model.GlobalMessage;
import com.kuuhaku.model.Member;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DashboardRequest {

	@RequestMapping(value = "/app/messages", method = RequestMethod.POST)
	public List<GlobalMessage> retrieveMessageCache(@RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return GlobalMessageDAO.getMessages();
	}

	@RequestMapping(value = "/app/auth", method = RequestMethod.POST)
	public String validateAccount(@RequestHeader(value = "login") String login, @RequestHeader(value = "password") String pass, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return MemberDAO.authMember(login, pass);
	}

	@RequestMapping(value = "/app/data", method = RequestMethod.POST)
	public AppUser retrieveData(@RequestHeader(value = "uid") String uid, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return DashboardDAO.getData(uid);
	}

	@RequestMapping(value = "/app/profile", method = RequestMethod.POST)
	public List<Member> retrieveProfiles(@RequestHeader(value = "id") String id, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return MemberDAO.getMemberByMid(id);
	}
}
