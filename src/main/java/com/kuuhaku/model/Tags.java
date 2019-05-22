/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model;

import org.json.JSONObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Map;

@Entity
public class Tags {
    @Id
    private String id;
    private boolean Staff;
    private boolean Partner;
    private boolean Helper;
    private boolean Toxic;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isStaff() {
        return Staff;
    }

    public void setStaff() {
        Staff = true;
    }

    public boolean isPartner() {
        return Partner;
    }

    public void setPartner() {
        Partner = true;
    }

    public boolean isHelper() {
        return Helper;
    }

    public void setHelper() {
        Helper = true;
    }

    public boolean isToxic() {
        return Toxic;
    }

    public void setToxic() {
        Toxic = true;
    }
}
