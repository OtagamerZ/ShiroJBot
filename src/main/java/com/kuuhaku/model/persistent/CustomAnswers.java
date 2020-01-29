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

package com.kuuhaku.model.persistent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CustomAnswers {
    @Id
    @Column(columnDefinition = "BIGINT")
    private final long id = System.currentTimeMillis();

    @Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
    private String guildID = "";

    @Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
    private String gatilho = "";

    @Column(columnDefinition = "TEXT DEFAULT ''")
    private String answer = "";

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean markForDelete = false;

    public CustomAnswers() {

    }

    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    public String getGatilho() {
        return gatilho;
    }

    public void setGatilho(String gatilho) {
        this.gatilho = gatilho;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getId() {
        return id;
    }

    public String getGuildID() {
        return guildID;
    }

    public boolean isMarkForDelete() {
        return markForDelete;
    }

    public void setMarkForDelete(boolean markForDelete) {
        this.markForDelete = markForDelete;
    }
}
