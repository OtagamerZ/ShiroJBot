package com.kuuhaku.handlers.api.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import com.kuuhaku.model.Member;
import com.kuuhaku.utils.Helper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
	public JSONObject authProfile(@RequestHeader(value = "login") String login, @RequestHeader(value = "password") String pass) {
		JSONObject response = new JSONObject();

		List<Member> profileList = MemberDAO.authMember(login, pass);
		List<String> guildIds = profileList.stream().map(Member::getSid).collect(Collectors.toList());
		JSONArray guilds = new JSONArray(Main.getInfo().getAPI().getGuilds().stream().filter(g -> guildIds.contains(g.getId())).collect(Collectors.toList()));

		List<String> memberIds = profileList.stream().map(Member::getMid).collect(Collectors.toList());
		JSONArray members = new JSONArray(Main.getInfo().getAPI().getGuilds().stream().flatMap(g -> g.getMembers().stream()).filter(m -> memberIds.contains(m.getId())).collect(Collectors.toList()));

		response.put("profiles", new JSONArray(profileList));
		response.put("guilds", guilds);
		response.put("members", members);

		return response;
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
