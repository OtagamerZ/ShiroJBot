/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import javax.persistence.Table;

@Entity
@Table(name = "tags")
public class Tags {
    @Id
    @Column(columnDefinition = "VARCHAR(191)")
    private String id;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Partner = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Toxic = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Verified = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Reader = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean Sponsor = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isPartner() {
        return Partner;
    }

    public void setPartner(boolean isPartner) {
        Partner = isPartner;
    }

    public boolean isToxic() {
        return Toxic;
    }

    public void setToxic(boolean isToxic) {
        Toxic = isToxic;
    }

    public boolean isVerified() {
        return Verified;
    }

    public void setVerified(boolean verified) {
        Verified = verified;
    }

    public boolean isReader() {
        return Reader;
    }

    public void setReader(boolean reader) {
        Reader = reader;
    }

    public boolean isSponsor() {
        return Sponsor;
    }

    public void setSponsor(boolean sponsor) {
        Sponsor = sponsor;
    }
}
