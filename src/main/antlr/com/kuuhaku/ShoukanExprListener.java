// Generated from C:/Users/dev204/Desktop/ShiroJBot/src/main/antlr\ShoukanExpr.g4 by ANTLR 4.12.0
package com.kuuhaku;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ShoukanExprParser}.
 */
public interface ShoukanExprListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ShoukanExprParser#line}.
	 * @param ctx the parse tree
	 */
	void enterLine(ShoukanExprParser.LineContext ctx);
	/**
	 * Exit a parse tree produced by {@link ShoukanExprParser#line}.
	 * @param ctx the parse tree
	 */
	void exitLine(ShoukanExprParser.LineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SumSub}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterSumSub(ShoukanExprParser.SumSubContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SumSub}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitSumSub(ShoukanExprParser.SumSubContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Function}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterFunction(ShoukanExprParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitFunction(ShoukanExprParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Group}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterGroup(ShoukanExprParser.GroupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Group}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitGroup(ShoukanExprParser.GroupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MulDiv}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMulDiv(ShoukanExprParser.MulDivContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MulDiv}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMulDiv(ShoukanExprParser.MulDivContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Value}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterValue(ShoukanExprParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Value}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitValue(ShoukanExprParser.ValueContext ctx);
}