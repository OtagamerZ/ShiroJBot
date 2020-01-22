package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.GlobalMessageDAO;
import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.GlobalMessage;
import com.kuuhaku.model.GuildConfig;
import com.kuuhaku.model.Member;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class DashboardRequest {

	@RequestMapping(value = "/app/messages", method = RequestMethod.POST)
	public List<GlobalMessage> retrieveMessageCache(@RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return GlobalMessageDAO.getMessages();
	}

	@RequestMapping(value = "/app/auth", method = RequestMethod.POST)
	public List<Member> validateAccount(@RequestHeader(value = "login") String login, @RequestHeader(value = "password") String pass, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return MemberDAO.authMember(login, pass);
	}

	@RequestMapping(value = "/app/profiles", method = RequestMethod.POST)
	public List<Member> retrieveProfiles(@RequestHeader(value = "id") String id, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		return MemberDAO.getMemberByMid(id);
	}

	@RequestMapping(value = "/app/guilds", method = RequestMethod.POST)
	public List<GuildConfig> retrieveGuilds(@RequestHeader(value = "id") String id, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		String[] ids = Main.getInfo().getAPI().getGuilds().stream().filter(g -> g.getMemberById(id) != null && Objects.requireNonNull(g.getMemberById(id)).hasPermission(Permission.MANAGE_SERVER)).map(Guild::getId).toArray(String[]::new);
		return GuildDAO.getAllGuilds().stream().filter(gc -> Helper.containsAny(gc.getGuildID(), ids)).collect(Collectors.toList());
	}

	@RequestMapping(value = "/app/jda/member", method = RequestMethod.POST)
	public com.kuuhaku.model.jda.Member retrieveJDAMember(@RequestHeader(value = "id") String id, @RequestHeader(value = "guild") String guild, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		net.dv8tion.jda.api.entities.Member m = Main.getInfo().getGuildByID(guild).getMemberById(id);
		assert m != null;
		return new com.kuuhaku.model.jda.Member(id, guild, m.getUser().getName(), m.getNickname(), m.getUser().getAvatarUrl());
	}

	@RequestMapping(value = "/app/jda/guild", method = RequestMethod.POST)
	public com.kuuhaku.model.jda.Guild retrieveJDAGuild(@RequestHeader(value = "id") String id, @RequestHeader(value = "token") String token) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		net.dv8tion.jda.api.entities.Guild g = Main.getInfo().getGuildByID(id);
		assert g != null;
		return new com.kuuhaku.model.jda.Guild(id, g.getName(), g.getIconUrl(), g.getOwnerId());
	}
}
