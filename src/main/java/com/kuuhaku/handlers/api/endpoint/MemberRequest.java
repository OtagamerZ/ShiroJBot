package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.Member;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
