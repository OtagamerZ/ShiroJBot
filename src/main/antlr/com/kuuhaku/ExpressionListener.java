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

import java.util.List;

import static com.kuuhaku.ShoukanExprParser.*;

public class ExpressionListener extends ShoukanExprBaseListener {
	private Scalable current;
	private Double[] minMax = new Double[2];

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
			if (e instanceof ValueContext value) {
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
				current = (Scalable) curr.set(e.getRuleIndex(), new Scalable());
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
				current = (Scalable) curr.set(e.getRuleIndex(), new Scalable());
				flatten(e);
			}
		}
	}

	private void flatten(ExprContext ctx) {
		if (!ctx.getTokens(SUB).isEmpty()) {
			current.invert();
		}

		if (ctx instanceof MulDivContext md) {
			mulDiv(md);
		} else if (ctx instanceof SumSubContext ss) {
			sumSub(ss);
		} else if (ctx instanceof FunctionContext fc) {
			if (fc.func.getType() == MAX) {
				minMax[0] = Double.parseDouble(fc.left.getText());
				flatten(fc.right);
			} else {
				minMax[1] = Double.parseDouble(fc.right.getText());
				flatten(fc.left);
			}
		} else if (ctx instanceof GroupContext g) {
			flatten(g.expr());
		}
	}

	public Scalable getOutput() {
		return output;
	}

	public Double[] getMinMax() {
		return minMax;
	}
}
