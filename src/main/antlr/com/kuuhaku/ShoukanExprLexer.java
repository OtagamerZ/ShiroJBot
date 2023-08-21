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
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, MUL=6, DIV=7, SUM=8, SUB=9, MIN=10, 
		MAX=11, NUM=12, NAME=13, VAR=14;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "MUL", "DIV", "SUM", "SUB", "MIN", 
			"MAX", "NUM", "NAME", "VAR"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "','", "')'", "'d'", "'f'", "'*'", "'/'", "'+'", "'-'", 
			"'min'", "'max'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, "MUL", "DIV", "SUM", "SUB", "MIN", 
			"MAX", "NUM", "NAME", "VAR"
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
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ShoukanExpr.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u000eO\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0001\u0000\u0001\u0000\u0001"+
		"\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0004\u000b9\b\u000b\u000b\u000b"+
		"\f\u000b:\u0001\u000b\u0001\u000b\u0004\u000b?\b\u000b\u000b\u000b\f\u000b"+
		"@\u0003\u000bC\b\u000b\u0001\f\u0004\fF\b\f\u000b\f\f\fG\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0003\rN\b\r\u0000\u0000\u000e\u0001\u0001\u0003\u0002"+
		"\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013"+
		"\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e\u0001\u0000\u0002\u0001\u0000"+
		"09\u0001\u0000azS\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001"+
		"\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001"+
		"\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000"+
		"\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000"+
		"\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000"+
		"\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000"+
		"\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000"+
		"\u0000\u0001\u001d\u0001\u0000\u0000\u0000\u0003\u001f\u0001\u0000\u0000"+
		"\u0000\u0005!\u0001\u0000\u0000\u0000\u0007#\u0001\u0000\u0000\u0000\t"+
		"%\u0001\u0000\u0000\u0000\u000b\'\u0001\u0000\u0000\u0000\r)\u0001\u0000"+
		"\u0000\u0000\u000f+\u0001\u0000\u0000\u0000\u0011-\u0001\u0000\u0000\u0000"+
		"\u0013/\u0001\u0000\u0000\u0000\u00153\u0001\u0000\u0000\u0000\u00178"+
		"\u0001\u0000\u0000\u0000\u0019E\u0001\u0000\u0000\u0000\u001bI\u0001\u0000"+
		"\u0000\u0000\u001d\u001e\u0005(\u0000\u0000\u001e\u0002\u0001\u0000\u0000"+
		"\u0000\u001f \u0005,\u0000\u0000 \u0004\u0001\u0000\u0000\u0000!\"\u0005"+
		")\u0000\u0000\"\u0006\u0001\u0000\u0000\u0000#$\u0005d\u0000\u0000$\b"+
		"\u0001\u0000\u0000\u0000%&\u0005f\u0000\u0000&\n\u0001\u0000\u0000\u0000"+
		"\'(\u0005*\u0000\u0000(\f\u0001\u0000\u0000\u0000)*\u0005/\u0000\u0000"+
		"*\u000e\u0001\u0000\u0000\u0000+,\u0005+\u0000\u0000,\u0010\u0001\u0000"+
		"\u0000\u0000-.\u0005-\u0000\u0000.\u0012\u0001\u0000\u0000\u0000/0\u0005"+
		"m\u0000\u000001\u0005i\u0000\u000012\u0005n\u0000\u00002\u0014\u0001\u0000"+
		"\u0000\u000034\u0005m\u0000\u000045\u0005a\u0000\u000056\u0005x\u0000"+
		"\u00006\u0016\u0001\u0000\u0000\u000079\u0007\u0000\u0000\u000087\u0001"+
		"\u0000\u0000\u00009:\u0001\u0000\u0000\u0000:8\u0001\u0000\u0000\u0000"+
		":;\u0001\u0000\u0000\u0000;B\u0001\u0000\u0000\u0000<>\u0005.\u0000\u0000"+
		"=?\u0007\u0000\u0000\u0000>=\u0001\u0000\u0000\u0000?@\u0001\u0000\u0000"+
		"\u0000@>\u0001\u0000\u0000\u0000@A\u0001\u0000\u0000\u0000AC\u0001\u0000"+
		"\u0000\u0000B<\u0001\u0000\u0000\u0000BC\u0001\u0000\u0000\u0000C\u0018"+
		"\u0001\u0000\u0000\u0000DF\u0007\u0001\u0000\u0000ED\u0001\u0000\u0000"+
		"\u0000FG\u0001\u0000\u0000\u0000GE\u0001\u0000\u0000\u0000GH\u0001\u0000"+
		"\u0000\u0000H\u001a\u0001\u0000\u0000\u0000IJ\u0005$\u0000\u0000JM\u0003"+
		"\u0019\f\u0000KL\u0005.\u0000\u0000LN\u0003\u0019\f\u0000MK\u0001\u0000"+
		"\u0000\u0000MN\u0001\u0000\u0000\u0000N\u001c\u0001\u0000\u0000\u0000"+
		"\u0006\u0000:@BGM\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}