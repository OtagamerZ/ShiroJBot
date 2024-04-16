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

package com.kuuhaku;

import com.kuuhaku.generated.ShoukanExprBaseListener;
import com.kuuhaku.generated.ShoukanExprParser;

import java.util.List;

import static com.kuuhaku.generated.ShoukanExprParser.*;

public class ExpressionListener extends ShoukanExprBaseListener {
	private Scalable current;
	private Scalable output;

	@Override
	public void enterLine(LineContext ctx) {
		current = output = new Scalable();
		flatten(ctx.expr());
	}

	public void mulDiv(MulDivContext ctx) {
		Scalable curr = current;
		curr.setDelimiter(ctx.op.getText());

		List<ExprContext> exprs = ctx.expr();
		for (int i = 0; i < exprs.size(); i++) {
			ExprContext e = exprs.get(i);
			if (e instanceof ShoukanExprParser.ValueContext value) {
				Value v;
				if (value.element.getType() == VAR) {
					v = new VariableValue(value.getText());
				} else {
					if (ctx.op.getType() == MUL) {
						v = new PercentageValue(Double.parseDouble(value.getText()));
					} else {
						v = new PercentageValue(1 / Double.parseDouble(value.getText()));
					}
				}

				curr.set(i, v);
			} else {
				current = (Scalable) curr.set(i, new Scalable());
				flatten(e);
			}
		}
	}

	public void sumSub(SumSubContext ctx) {
		Scalable curr = current;
		curr.setDelimiter(ctx.op.getText());

		List<ExprContext> exprs = ctx.expr();
		for (int i = 0; i < exprs.size(); i++) {
			ExprContext e = exprs.get(i);
			if (e instanceof ValueContext value) {
				Value v;
				if (value.element.getType() == VAR) {
					v = new VariableValue(value.getText());
				} else {
					v = new FlatValue(Double.parseDouble(value.getText()));
				}

				curr.set(i, v);
			} else {
				current = (Scalable) curr.set(i, new Scalable());
				flatten(e);
			}
		}
	}

	private void flatten(ExprContext ctx) {
		if (!ctx.getTokens(SUB).isEmpty()) {
			current.invert();
		}

		switch (ctx) {
			case ValueContext value -> {
				Value v;
				if (value.element.getType() == VAR) {
					v = new VariableValue(value.getText());
				} else {
					v = new FlatValue(Double.parseDouble(value.getText()));
				}

				current.setLeft(v);
			}
			case MulDivContext md -> mulDiv(md);
			case SumSubContext ss -> sumSub(ss);
			case FunctionContext fc -> {
				current.setGrouped(true);
				if (fc.func.getType() == MAX) {
					flatten(fc.right);
				} else {
					flatten(fc.left);
				}
			}
			case GroupContext g -> {
				current.setGrouped(true);
				flatten(g.expr());
			}
			default -> {
			}
		}
	}

	public Scalable getOutput() {
		return output;
	}
}
