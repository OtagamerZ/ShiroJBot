// Generated from C:/Users/dev204/Desktop/ShiroJBot/src/main/antlr\ShoukanExpr.g4 by ANTLR 4.12.0
package com.kuuhaku;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ShoukanExprParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ShoukanExprVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ShoukanExprParser#line}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLine(ShoukanExprParser.LineContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SumSub}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSumSub(ShoukanExprParser.SumSubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(ShoukanExprParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Group}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup(ShoukanExprParser.GroupContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MulDiv}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulDiv(ShoukanExprParser.MulDivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Value}
	 * labeled alternative in {@link ShoukanExprParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(ShoukanExprParser.ValueContext ctx);
}