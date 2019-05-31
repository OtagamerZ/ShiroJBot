package com.kuuhaku.model;

import java.util.List;

public class DataDump {
    private List<CustomAnswers> caDump;
    private List<Member> mDump;
    private List<guildConfig> gcDump;

    public DataDump(List<CustomAnswers> caDump, List<Member> mDump, List<guildConfig> gcDump) {
        this.caDump = caDump;
        this.mDump = mDump;
        this.gcDump = gcDump;
    }

    public List<CustomAnswers> getCaDump() {
        return caDump;
    }

    public List<Member> getmDump() {
        return mDump;
    }

    public List<guildConfig> getGcDump() {
        return gcDump;
    }
}
