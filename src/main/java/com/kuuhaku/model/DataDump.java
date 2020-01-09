package com.kuuhaku.model;

import com.kuuhaku.controller.mysql.MemberDAO;

import java.util.ArrayList;
import java.util.List;

public class DataDump {
	private final List<CustomAnswers> caDump;
	private final List<Member> mDump;
	private final List<GuildConfig> gcDump;

    public DataDump(List<CustomAnswers> caDump, List<Member> mDump, List<GuildConfig> gcDump) {
        this.caDump = caDump;
        this.gcDump = gcDump;
        this.mDump = mDump;
    }

    public DataDump(List<CustomAnswers> caDump, List<GuildConfig> gcDump) {
        this.caDump = caDump;
        this.gcDump = gcDump;
        this.mDump = new ArrayList<>();
    }

    public DataDump(List<Member> mDump) {
        this.caDump = new ArrayList<>();
        this.gcDump = new ArrayList<>();

        List<Member> oldMembers = MemberDAO.getMembers();
        mDump.removeAll(oldMembers);

        this.mDump = mDump;
    }

    public List<CustomAnswers> getCaDump() {
        return caDump;
    }

    public List<Member> getmDump() {
        return mDump;
    }

    public List<GuildConfig> getGcDump() {
        return gcDump;
    }
}
