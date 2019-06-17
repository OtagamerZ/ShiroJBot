package com.kuuhaku.model;

import java.util.ArrayList;
import java.util.List;

public class DataDump {
    private List<CustomAnswers> caDump;
    private List<Member> mDump;
    private List<guildConfig> gcDump;

    public DataDump(List<CustomAnswers> caDump, List<Member> mDump, List<guildConfig> gcDump) {
        this.caDump = caDump;
        this.gcDump = gcDump;
        this.mDump = mDump;
    }

    public DataDump(List<CustomAnswers> caDump, List<guildConfig> gcDump) {
        this.caDump = caDump;
        this.gcDump = gcDump;
        this.mDump = new ArrayList<>();
    }

    public DataDump(List<Member> mDump) {
        this.caDump = new ArrayList<>();
        this.gcDump = new ArrayList<>();
        this.mDump = mDump;
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
