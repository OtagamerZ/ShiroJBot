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
import java.util.Map;

@Entity
public class Tags {
    private final String Owner = "350836145921327115";
    private String Partners = "{}";
    private String Toxic = "{}";
    private String Helpers = "{}";

    public Map<String, Object> getPartners() {
        return new JSONObject(Partners).toMap();
    }

    public void setPartners(Map<String, String> partners) {
        Partners = partners.toString();
    }

    public Map<String, Object> getToxic() {
        return new JSONObject(Toxic).toMap();
    }

    public void setToxic(Map<String, String> toxic) {
        Toxic = toxic.toString();
    }

    public Map<String, Object> getHelpers() {
        return new JSONObject(Helpers).toMap();
    }

    public void setHelpers(Map<String, String> helpers) {
        Helpers = helpers.toString();
    }
}
