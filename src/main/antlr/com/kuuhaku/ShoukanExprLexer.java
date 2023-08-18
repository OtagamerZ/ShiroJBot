// Generated from C:/Users/dev204/Desktop/ShiroJBot/src/main/antlr\ShoukanExpr.g4 by ANTLR 4.12.0
package com.kuuhaku;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class ShoukanExprLexer extends Lexer {
	static {
		RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION);
	}

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
			new PredictionContextCache();
	public static final int
			T__0 = 1, T__1 = 2, T__2 = 3, MUL = 4, DIV = 5, SUM = 6, SUB = 7, MIN = 8, MAX = 9, NUM = 10,
			NAME = 11, VAR = 12;
	public static String[] channelNames = {
			"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
			"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[]{
				"T__0", "T__1", "T__2", "MUL", "DIV", "SUM", "SUB", "MIN", "MAX", "NUM",
				"NAME", "VAR"
		};
	}

	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[]{
				null, "'('", "','", "')'", "'*'", "'/'", "'+'", "'-'", "'min'", "'max'"
		};
	}

	private static final String[] _LITERAL_NAMES = makeLiteralNames();

	private static String[] makeSymbolicNames() {
		return new String[]{
				null, null, null, null, "MUL", "DIV", "SUM", "SUB", "MIN", "MAX", "NUM",
				"NAME", "VAR"
		};
	}

	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;

	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public ShoukanExprLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
	}

	@Override
	public String getGrammarFileName() {
		return "ShoukanExpr.g4";
	}

	@Override
	public String[] getRuleNames() {
		return ruleNames;
	}

	@Override
	public String getSerializedATN() {
		return _serializedATN;
	}

	@Override
	public String[] getChannelNames() {
		return channelNames;
	}

	@Override
	public String[] getModeNames() {
		return modeNames;
	}

	@Override
	public ATN getATN() {
		return _ATN;
	}

	public static final String _serializedATN =
			"\u0004\u0000\fG\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001" +
			"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004" +
			"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007" +
			"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b" +
			"\u0007\u000b\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002" +
			"\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005" +
			"\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007" +
			"\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0004\t1\b\t\u000b" +
			"\t\f\t2\u0001\t\u0001\t\u0004\t7\b\t\u000b\t\f\t8\u0003\t;\b\t\u0001\n" +
			"\u0004\n>\b\n\u000b\n\f\n?\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b" +
			"\u0003\u000bF\b\u000b\u0000\u0000\f\u0001\u0001\u0003\u0002\u0005\u0003" +
			"\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015" +
			"\u000b\u0017\f\u0001\u0000\u0002\u0001\u000009\u0001\u0000azK\u0000\u0001" +
			"\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005" +
			"\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001" +
			"\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000" +
			"\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000" +
			"\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000" +
			"\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0001\u0019\u0001\u0000" +
			"\u0000\u0000\u0003\u001b\u0001\u0000\u0000\u0000\u0005\u001d\u0001\u0000" +
			"\u0000\u0000\u0007\u001f\u0001\u0000\u0000\u0000\t!\u0001\u0000\u0000" +
			"\u0000\u000b#\u0001\u0000\u0000\u0000\r%\u0001\u0000\u0000\u0000\u000f" +
			"\'\u0001\u0000\u0000\u0000\u0011+\u0001\u0000\u0000\u0000\u00130\u0001" +
			"\u0000\u0000\u0000\u0015=\u0001\u0000\u0000\u0000\u0017A\u0001\u0000\u0000" +
			"\u0000\u0019\u001a\u0005(\u0000\u0000\u001a\u0002\u0001\u0000\u0000\u0000" +
			"\u001b\u001c\u0005,\u0000\u0000\u001c\u0004\u0001\u0000\u0000\u0000\u001d" +
			"\u001e\u0005)\u0000\u0000\u001e\u0006\u0001\u0000\u0000\u0000\u001f \u0005" +
			"*\u0000\u0000 \b\u0001\u0000\u0000\u0000!\"\u0005/\u0000\u0000\"\n\u0001" +
			"\u0000\u0000\u0000#$\u0005+\u0000\u0000$\f\u0001\u0000\u0000\u0000%&\u0005" +
			"-\u0000\u0000&\u000e\u0001\u0000\u0000\u0000\'(\u0005m\u0000\u0000()\u0005" +
			"i\u0000\u0000)*\u0005n\u0000\u0000*\u0010\u0001\u0000\u0000\u0000+,\u0005" +
			"m\u0000\u0000,-\u0005a\u0000\u0000-.\u0005x\u0000\u0000.\u0012\u0001\u0000" +
			"\u0000\u0000/1\u0007\u0000\u0000\u00000/\u0001\u0000\u0000\u000012\u0001" +
			"\u0000\u0000\u000020\u0001\u0000\u0000\u000023\u0001\u0000\u0000\u0000" +
			"3:\u0001\u0000\u0000\u000046\u0005.\u0000\u000057\u0007\u0000\u0000\u0000" +
			"65\u0001\u0000\u0000\u000078\u0001\u0000\u0000\u000086\u0001\u0000\u0000" +
			"\u000089\u0001\u0000\u0000\u00009;\u0001\u0000\u0000\u0000:4\u0001\u0000" +
			"\u0000\u0000:;\u0001\u0000\u0000\u0000;\u0014\u0001\u0000\u0000\u0000" +
			"<>\u0007\u0001\u0000\u0000=<\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000" +
			"\u0000?=\u0001\u0000\u0000\u0000?@\u0001\u0000\u0000\u0000@\u0016\u0001" +
			"\u0000\u0000\u0000AB\u0005$\u0000\u0000BE\u0003\u0015\n\u0000CD\u0005" +
			".\u0000\u0000DF\u0003\u0015\n\u0000EC\u0001\u0000\u0000\u0000EF\u0001" +
			"\u0000\u0000\u0000F\u0018\u0001\u0000\u0000\u0000\u0006\u000028:?E\u0000";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());

	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}