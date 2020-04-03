/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.controller.postgresql.GlobalMessageDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.controller.sqlite.DashboardDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.persistent.GlobalMessage;
import com.kuuhaku.model.persistent.Member;
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
		return DashboardDAO.auth(login, pass);
	}

	@RequestMapping(value = "/app/data", method = RequestMethod.POST)
	public String retrieveData(@RequestHeader(value = "uid") String uid, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return DashboardDAO.getData(uid).toString();
	}

	@RequestMapping(value = "/app/profile", method = RequestMethod.POST)
	public Member retrieveProfiles(@RequestHeader(value = "id") String id, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return MemberDAO.getMemberById(id);
	}
}
