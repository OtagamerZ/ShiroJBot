/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.model.records.ItemAmount;
import com.kuuhaku.util.Utils;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.intellij.lang.annotations.Language;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Trade {
    private static final MultiMap<String, Trade> pending = new MultiMap<>(ConcurrentHashMap::new);

    private Account left;
    private int leftValue;
    private Bag<Integer> leftOffer = new HashBag<>();
    private Bag<String> leftItems = new HashBag<>();

    private Account right;
    private int rightValue;
    private Bag<Integer> rightOffer = new HashBag<>();
    private Bag<String> rightItems = new HashBag<>();

    private boolean finalizing = false;

    public Trade(String left, String right) {
        this.left = DAO.find(Account.class, left);
        this.right = DAO.find(Account.class, right);
    }

    public static MultiMap<String, Trade> getPending() {
        return pending;
    }

    public Account getLeft() {
        return left;
    }

    public int getLeftValue() {
        return leftValue;
    }

    public void addLeftValue(int value) {
        leftValue += value;
    }

    public Bag<Integer> getLeftOffer() {
        return leftOffer;
    }

    public Bag<String> getLeftItems() {
        return leftItems;
    }

    public Account getRight() {
        return right;
    }

    public int getRightValue() {
        return rightValue;
    }

    public void addRightValue(int value) {
        rightValue += value;
    }

    public Bag<Integer> getRightOffer() {
        return rightOffer;
    }

    public Bag<String> getRightItems() {
        return rightItems;
    }

    public Account getSelf(String id) {
        return left.getUid().equals(id) ? left : right;
    }

    public int getSelfValue(String id) {
        return left.getUid().equals(id) ? leftValue : rightValue;
    }

    public void addSelfValue(String id, int value) {
        if (left.getUid().equals(id)) {
            leftValue += value;
        } else {
            rightValue += value;
        }
    }

    public Bag<Integer> getSelfOffers(String id) {
        return left.getUid().equals(id) ? leftOffer : rightOffer;
    }

    public Bag<String> getSelfItems(String id) {
        return left.getUid().equals(id) ? leftItems : rightItems;
    }

    public boolean validate() {
        if (!left.hasEnough(leftValue, Currency.CR) || !right.hasEnough(rightValue, Currency.CR)) return false;

        @Language("PostgreSQL")
        String query = """
                SELECT count(1)
                FROM stashed_card sc
                WHERE sc.kawaipon_uid = ?1
                  AND sc.id IN ?2
                  AND sc.deck_id IS NULL
                  AND sc.price = 0
                  AND NOT sc.account_bound
                """;

        boolean check = DAO.queryNative(Integer.class, query, left.getUid(), leftOffer) != leftOffer.size();
        check = check && DAO.queryNative(Integer.class, query, right.getUid(), rightOffer) != rightOffer.size();

        if (!check) {
            return false;
        }

        query = """
                SELECT i.id
                     , sum(cast(i.amount AS INT)) AS amount
                FROM account a
                         CROSS JOIN jsonb_each(a.inventory) as i(id, amount)
                WHERE a.uid = ?1
                GROUP BY i.id
                """;

        List<ItemAmount> rows = Utils.map(
                DAO.queryAllUnmapped(query, left.getUid()),
                o -> new ItemAmount(String.valueOf(o[0]), ((Number) o[1]).intValue())
        );
        for (ItemAmount ia : rows) {
            if (leftItems.getCount(ia.id()) > ia.amount()) return false;
        }

        rows = Utils.map(
                DAO.queryAllUnmapped(query, right.getUid()),
                o -> new ItemAmount(String.valueOf(o[0]), ((Number) o[1]).intValue())
        );
        for (ItemAmount ia : rows) {
            if (rightItems.getCount(ia.id()) > ia.amount()) return false;
        }

        return true;
    }

    public void accept() {
        left.addCR(rightValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
        left.consumeCR(leftValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
        DAO.apply("""
                UPDATE StashedCard sc
                SET kawaipon = ?1
                WHERE sc.id IN ?2
                """, left.getKawaipon(), rightOffer);

        right.addCR(leftValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
        right.consumeCR(rightValue, "Trade (" + left.getName() + "/" + right.getName() + ") commit");
        DAO.apply("""    
                UPDATE StashedCard sc
                SET kawaipon = ?1
                WHERE sc.id IN ?2
                """, right.getKawaipon(), leftOffer);

        for (String i : rightItems) {
            int amount = rightItems.getCount(i);

            left.addItem(i, amount);
            right.consumeItem(i, amount, true);
        }

        for (String i : leftItems) {
            int amount = leftItems.getCount(i);

            right.addItem(i, amount);
            left.consumeItem(i, amount, true);
        }
    }

    public String toString(I18N locale, boolean left) {
        int value = left ? leftValue : rightValue;
        List<StashedCard> offers = (left ? leftOffer : rightOffer).stream()
                .distinct()
                .map(id -> DAO.find(StashedCard.class, id))
                .sorted(Comparator.comparing(StashedCard::getType))
                .toList();
        List<UserItem> items = (left ? leftItems : rightItems).stream()
                .distinct()
                .map(id -> DAO.find(UserItem.class, id))
                .toList();

        XStringBuilder sb = new XStringBuilder("```asciidoc");
        sb.appendNewLine("= " + Utils.separate(value) + " ₵R =");

        CardType type = null;
        for (StashedCard card : offers) {
            if (type != card.getType()) {
                type = card.getType();
                sb.appendNewLine("\n[" + locale.get("type/" + type.name()) + "]");
            }

            int count = (left ? leftOffer : rightOffer).getCount(card.getId());
            if (count > 1) {
                sb.appendNewLine("- " + count + "x " + card);
            } else {
                sb.appendNewLine("- " + card);
            }
        }

        boolean header = false;
        for (UserItem item : items) {
            if (!header) {
                sb.appendNewLine("\n[" + locale.get("type/item") + "]");
                header = true;
            }

            int count = (left ? leftItems : rightItems).getCount(item.getId());
            if (count > 1) {
                sb.appendNewLine("- " + count + "x " + item.toString(locale));
            } else {
                sb.appendNewLine("- " + item.toString(locale));
            }
        }

        sb.appendNewLine("```");

        return sb.toString();
    }

    public boolean isFinalizing() {
        return finalizing;
    }

    public void setFinalizing(boolean finalizing) {
        this.finalizing = finalizing;
    }
}
