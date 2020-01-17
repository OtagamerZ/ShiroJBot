package com.kuuhaku.handlers.api.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import com.kuuhaku.model.Member;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class MemberRequest {

	@RequestMapping(value = "/member/get/byid", method = RequestMethod.GET)
	public Member requestProfileById(@RequestParam(value = "id") String id) {
		return MemberDAO.getMemberById(id);
	}

	@RequestMapping(value = "/member/get/bymid", method = RequestMethod.GET)
	public Member[] requestProfileByMid(@RequestParam(value = "id") String mid) {
		return MemberDAO.getMemberByMid(mid).toArray(new Member[0]);
	}

	@RequestMapping(value = "/member/get/bysid", method = RequestMethod.GET)
	public Member[] requestProfileBySid(@RequestParam(value = "id") String sid) {
		return MemberDAO.getMemberBySid(sid).toArray(new Member[0]);
	}

	@RequestMapping(value = "/member/auth", method = RequestMethod.POST)
	public Object authProfile(@RequestHeader(value = "login") String login, @RequestHeader(value = "password") String pass) {
		try {
			List<Member> profileList = MemberDAO.authMember(login, pass);
			List<String> guildIds = profileList.stream().map(Member::getSid).collect(Collectors.toList());
			List<Guild> gs = Main.getInfo().getAPI().getGuilds().stream().filter(g -> guildIds.contains(g.getId())).collect(Collectors.toList());

			List<com.kuuhaku.model.jda.Guild> svs = new ArrayList<>();
			gs.forEach(s -> svs.add(new com.kuuhaku.model.jda.Guild(s.getId(), s.getName(), s.getIconUrl())));

			List<com.kuuhaku.model.jda.Member> mbs = new ArrayList<>();
			gs.forEach(s -> mbs.addAll(s.getMembers().stream().map(m -> new com.kuuhaku.model.jda.Member(m.getId(), s.getId(), m.getEffectiveName(), m.getNickname(), m.getUser().getAvatarUrl())).collect(Collectors.toList())));

			return new Object(){
				public List<Member> profiles = profileList;
				public List<com.kuuhaku.model.jda.Guild> guilds = svs;
				public List<com.kuuhaku.model.jda.Member> members = mbs;
			};
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@RequestMapping(value = "/member/update", method = RequestMethod.POST)
	public void updateProfile(@RequestHeader String profile, @RequestHeader String token) {
		try {
			ObjectMapper mapper = new ObjectMapper();

			Member m = mapper.readValue(profile, Member.class);

			if (TokenDAO.validateToken(token)) {
				MemberDAO.updateMemberConfigs(m);
			} else {
				throw new InvalidTokenException();
			}
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
