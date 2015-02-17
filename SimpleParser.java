package cop5555sp15;

import static cop5555sp15.TokenStream.Kind.AND;
import static cop5555sp15.TokenStream.Kind.ARROW;
import static cop5555sp15.TokenStream.Kind.ASSIGN;
import static cop5555sp15.TokenStream.Kind.AT;
import static cop5555sp15.TokenStream.Kind.BAR;
import static cop5555sp15.TokenStream.Kind.BL_FALSE;
import static cop5555sp15.TokenStream.Kind.BL_TRUE;
import static cop5555sp15.TokenStream.Kind.COLON;
import static cop5555sp15.TokenStream.Kind.COMMA;
import static cop5555sp15.TokenStream.Kind.DIV;
import static cop5555sp15.TokenStream.Kind.DOT;
import static cop5555sp15.TokenStream.Kind.EOF;
import static cop5555sp15.TokenStream.Kind.EQUAL;
import static cop5555sp15.TokenStream.Kind.GE;
import static cop5555sp15.TokenStream.Kind.GT;
import static cop5555sp15.TokenStream.Kind.IDENT;
import static cop5555sp15.TokenStream.Kind.INT_LIT;
import static cop5555sp15.TokenStream.Kind.KW_BOOLEAN;
import static cop5555sp15.TokenStream.Kind.KW_CLASS;
import static cop5555sp15.TokenStream.Kind.KW_DEF;
import static cop5555sp15.TokenStream.Kind.KW_ELSE;
import static cop5555sp15.TokenStream.Kind.KW_IF;
import static cop5555sp15.TokenStream.Kind.KW_IMPORT;
import static cop5555sp15.TokenStream.Kind.KW_INT;
import static cop5555sp15.TokenStream.Kind.KW_PRINT;
import static cop5555sp15.TokenStream.Kind.KW_RETURN;
import static cop5555sp15.TokenStream.Kind.KW_STRING;
import static cop5555sp15.TokenStream.Kind.KW_WHILE;
import static cop5555sp15.TokenStream.Kind.LCURLY;
import static cop5555sp15.TokenStream.Kind.LE;
import static cop5555sp15.TokenStream.Kind.LPAREN;
import static cop5555sp15.TokenStream.Kind.LSHIFT;
import static cop5555sp15.TokenStream.Kind.LSQUARE;
import static cop5555sp15.TokenStream.Kind.LT;
import static cop5555sp15.TokenStream.Kind.MINUS;
import static cop5555sp15.TokenStream.Kind.MOD;
import static cop5555sp15.TokenStream.Kind.NOT;
import static cop5555sp15.TokenStream.Kind.NOTEQUAL;
import static cop5555sp15.TokenStream.Kind.PLUS;
import static cop5555sp15.TokenStream.Kind.RANGE;
import static cop5555sp15.TokenStream.Kind.RCURLY;
import static cop5555sp15.TokenStream.Kind.RPAREN;
import static cop5555sp15.TokenStream.Kind.RSHIFT;
import static cop5555sp15.TokenStream.Kind.RSQUARE;
import static cop5555sp15.TokenStream.Kind.SEMICOLON;
import static cop5555sp15.TokenStream.Kind.STRING_LIT;
import static cop5555sp15.TokenStream.Kind.TIMES;
import cop5555sp15.TokenStream.Kind;
import cop5555sp15.TokenStream.Token;

public class SimpleParser {

	@SuppressWarnings("serial")
	public class SyntaxException extends Exception {
		Token t;
		Kind[] expected;
		String msg;

		SyntaxException(Token t, Kind expected) {
			this.t = t;
			msg = "";
			this.expected = new Kind[1];
			this.expected[0] = expected;

		}

		public SyntaxException(Token t, String msg) {
			this.t = t;
			this.msg = msg;
		}

		public SyntaxException(Token t, Kind[] expected) {
			this.t = t;
			msg = "";
			this.expected = expected;
		}

		public String getMessage() {
			StringBuilder sb = new StringBuilder();
			sb.append(" error at token ").append(t.toString()).append(" ")
					.append(msg);
			sb.append(". Expected: ");
			for (Kind kind : expected) {
				sb.append(kind).append(" ");
			}
			return sb.toString();
		}
	}

	TokenStream tokens;
	Token t;

	SimpleParser(TokenStream tokens) {
		this.tokens = tokens;
		t = tokens.nextToken();
	}

	private Kind match(Kind kind) throws SyntaxException {
		if (isKind(kind)) {
			consume();
			return kind;
		}
		throw new SyntaxException(t, kind);
	}

	private Kind match(Kind... kinds) throws SyntaxException {
		Kind kind = t.kind;
		if (isKind(kinds)) {
			consume();
			return kind;
		}
		StringBuilder sb = new StringBuilder();
		for (Kind kind1 : kinds) {
			sb.append(kind1).append(kind1).append(" ");
		}
		throw new SyntaxException(t, "expected one of " + sb.toString());
	}

	private boolean isKind(Kind kind) {
		return (t.kind == kind);
	}

	private void consume() {
		if (t.kind != EOF)
			t = tokens.nextToken();
	}

	private boolean isKind(Kind... kinds) {
		for (Kind kind : kinds) {
			if (t.kind == kind)
				return true;
		}
		return false;
	}

	//This is a convenient way to represent fixed sets of
	//token kinds.  You can pass these to isKind.
	static final Kind[] REL_OPS = { BAR, AND, EQUAL, NOTEQUAL, LT, GT, LE, GE };
	static final Kind[] WEAK_OPS = { PLUS, MINUS };
	static final Kind[] STRONG_OPS = { TIMES, DIV };
	static final Kind[] VERY_STRONG_OPS = { LSHIFT, RSHIFT };
	static final Kind[] SimpleTypes = { KW_INT, KW_BOOLEAN, KW_STRING };
	
	static final Kind[] FactorFirstSet = { IDENT, INT_LIT, BL_TRUE, BL_FALSE, STRING_LIT, LPAREN,
										   NOT, MINUS, Kind.KW_SIZE, Kind.KW_KEY, Kind.KW_VALUE, LCURLY, AT };
	static final Kind[] StatementFirstSet = { IDENT, Kind.KW_PRINT, Kind.KW_WHILE, LPAREN, Kind.KW_IF,
											  MOD, Kind.KW_RETURN};


	public void parse() throws SyntaxException {
		Program();
		match(EOF);
	}

	private void Program() throws SyntaxException {
		ImportList();
		match(KW_CLASS);
		match(IDENT);
		Block();
	}

	private void ImportList() throws SyntaxException {
		while (true) {
			try {
				match(KW_IMPORT);
			} catch (SyntaxException synEx) {
				return;
			}
			match(IDENT);
			if (t.kind == Kind.DOT) {
				while (true) {
					match(DOT);
					match(IDENT);
					if (t.kind != Kind.DOT) {
						break;
					}
				}
			}
			if (t.kind == Kind.SEMICOLON) {
				match(SEMICOLON);
				if (t.kind != Kind.KW_IMPORT) {
					break;
				}
			}
		}
		return;
	}

	private void Block() throws SyntaxException {
		match(LCURLY);
		while (true) {
			if (t.kind == Kind.KW_DEF) {
				Declaration();
				match(SEMICOLON);
			} else if (t.kind == RCURLY){
				break;
			} else {
				Statement();
				match(SEMICOLON);
			}
		}
		match(RCURLY);
		return;
	}

	private void Declaration() throws SyntaxException {
		match(KW_DEF);
		match(IDENT);
		if (t.kind == Kind.ASSIGN) {
			ClosureDec(true);
		} else {
			VarDec(true);
		}
		return;
	}

	private void ClosureDec(final boolean identMatched) throws SyntaxException {
		if (!identMatched) {
			match(IDENT);
		}
		match(ASSIGN);
		Closure();
		return;
	}
	
	private void Closure() throws SyntaxException {
		match(LCURLY);
		FormalArgList();
		match(ARROW);
		while (t.kind != RCURLY) {
			Statement();
			match(SEMICOLON);
		}
		match(RCURLY);
	}
	
	private void FormalArgList() throws SyntaxException {
		if (t.kind != Kind.IDENT) {
			return;
		}
		VarDec(false);
		while (true) {
			if (t.kind == COMMA) {
				match(COMMA);
				VarDec(false);
			} else {
				break;
			}
		}
		return;
	}
	
	private void VarDec(final boolean identMatched) throws SyntaxException {
		if (!identMatched) {
			match(IDENT);
		}
		if (t.kind != Kind.COLON) {
			return;
		}
		match(COLON);
		Type();
		return;
	}

	private void Type() throws SyntaxException {
		if (isKind(SimpleTypes)) {
			SimpleType();
			return;
		} 
		if (t.kind == Kind.AT) {
			match(AT);
			if (t.kind == Kind.AT) {
				KeyValueType(true);
				return;
			} 
			if (t.kind == Kind.LSQUARE) {
				ListType(true);
				return;
			}
		}
		throw new SyntaxException(t, "Unable to match token to expected TYPE token.");
	}
	
	private void SimpleType() throws SyntaxException {
		match(SimpleTypes);
	}
	
	private void KeyValueType(final boolean firstAtTokenConsumed) throws SyntaxException {
		if (!firstAtTokenConsumed) {
			match(AT);
		}
		match(AT);
		match(LSQUARE);
		SimpleType();
		match(COLON);
		Type();
		match(RSQUARE);
		return;
	}
	
	private void ListType(final boolean atTokenConsumed) throws SyntaxException {
		if (!atTokenConsumed) {
			match(AT);
		}
		match(LSQUARE);
		Type();
		match(RSQUARE);
		return;
	}
	
	private void Statement() throws SyntaxException {
		if (t.kind == IDENT) {
			LValue();
			match(ASSIGN);
			Expression(false, false);
			return;
		}
		if (t.kind == KW_PRINT) {
			match(KW_PRINT);
			Expression(false, false);
			return;
		}
		if (t.kind == KW_WHILE) {
			match(Kind.KW_WHILE);
			if (t.kind == TIMES) {
				match(TIMES);
			}
			match(LPAREN);
			Expression(false, true);
			match(RPAREN);
			Block();
			return;
		}
		if (t.kind == KW_IF) {
			match(Kind.KW_IF);
			match(LPAREN);
			Expression(false, false);
			match(RPAREN);
			Block();
			if (t.kind == Kind.KW_ELSE) {
				match(Kind.KW_ELSE);
				Block();
			}
			return;
		}
		if (t.kind == MOD) {
			match(MOD);
			Expression(false, false);
			return;
		}
		if (t.kind == KW_RETURN) {
			match(Kind.KW_RETURN);
			Expression(false, false);
			return;
		}
		return;
	}
	
	private void LValue() throws SyntaxException {
		match(IDENT);
		if (t.kind != LSQUARE) {
			return;
		}
		match(LSQUARE);
		Expression(false, false);
		match(RSQUARE);
		return;
	}
	
	private void Expression(final boolean transferFromRangeExpr, final boolean allowTransferToRangeExpr) throws SyntaxException {
		Term();
		while (true) {
			if (isKind(REL_OPS)) {
				RelOp();
				Term();
			} else {
				break;
			}
		}
		if (!transferFromRangeExpr && allowTransferToRangeExpr && t.kind == RANGE) {
			RangeExpression(true);
		}
		return;
	}
	
	private void RangeExpression(final boolean transferFromExpression) throws SyntaxException {
		if (!transferFromExpression) {
			Expression(true, false);
		}
		match(RANGE);
		Expression(true, false);
		return;
	}
	
	private void RelOp() throws SyntaxException {
		match(REL_OPS);
	}
	
	private void Term() throws SyntaxException {
		Elem();
		while (true) {
			if (isKind(WEAK_OPS)) {
				WeakOp();
				Elem();
			} else {
				break;
			}
		}
		return;
	}
	
	private void WeakOp() throws SyntaxException {
		match(WEAK_OPS);
	}
	
	private void Elem() throws SyntaxException {
		Thing();
		while (true) {
			if (isKind(STRONG_OPS)) {
				StrongOp();
				Thing();
			} else {
				break;
			}
		}
		return;
 	}
	
	private void StrongOp() throws SyntaxException {
		match(STRONG_OPS);
	}
	
	private void Thing() throws SyntaxException {
		Factor();
		while (true) {
			if (isKind(VERY_STRONG_OPS)) {
				VeryStrongOp();
				Factor();
			} else {
				break;
			}
		}
		return;
	}
	
	private void VeryStrongOp() throws SyntaxException {
		match(VERY_STRONG_OPS);
	}
	
	private void Factor() throws SyntaxException {
		if (t.kind == IDENT) {
			match(IDENT);
			if (t.kind == LSQUARE) {
				match(LSQUARE);
				Expression(false, false);
				match(RSQUARE);
				return;
			}
			if (t.kind == LPAREN) {
				ClosureEvalExpression(true);
			}
			return;
		}
		if (t.kind == INT_LIT) {
			match(INT_LIT);
			return;
		}
		if (t.kind == BL_TRUE) {
			match(BL_TRUE);
			return;
		}
		if (t.kind == BL_FALSE) {
			match(BL_FALSE);
			return;
		}
		if (t.kind == STRING_LIT) {
			match(STRING_LIT);
			return;
		}
		if (t.kind == LPAREN) {
			match(LPAREN);
			Expression(false, false);
			match(RPAREN);
			return;
		}
		if (t.kind == NOT) {
			match(NOT);
			Factor();
			return;
		}
		if (t.kind == MINUS) {
			match(MINUS);
			Factor();
			return;
		}
		if (t.kind == Kind.KW_SIZE) {
			match(Kind.KW_SIZE);
			match(LPAREN);
			Expression(false, false);
			match(RPAREN);
			return;
		}
		if (t.kind == Kind.KW_KEY) {
			match(Kind.KW_KEY);
			match(LPAREN);
			Expression(false, false);
			match(RPAREN);
			return;
		}
		if (t.kind == Kind.KW_VALUE) {
			match(Kind.KW_VALUE);
			match(LPAREN);
			Expression(false, false);
			match(RPAREN);
			return;
		}
		if (t.kind == LCURLY) {
			Closure();
			return;
		}
		if (t.kind == AT) {
			match(AT);
			if (t.kind == AT) {
				MapList(true);
			} else {
				List(true);
			}
			return;
		}
		throw new SyntaxException(t, "Invalid token for Factor prefix!");
	}
	
	private void ClosureEvalExpression(final boolean identMatched) throws SyntaxException {
		if (!identMatched) {
			match(IDENT);
		}
		match(LPAREN);
		ExpressionList();
		match(RPAREN);
		return;
	}
	
	private void ExpressionList() throws SyntaxException {
		if (isKind(FactorFirstSet)) {
			Expression(false, false);
			while (true) {
				if (t.kind == COMMA) {
					match(COMMA);
					Expression(false, false);
				} else {
					break;
				}
			}
		}
		return;
	}
	
	private void MapList(final boolean firstAtMatched) throws SyntaxException {
		if (!firstAtMatched) {
			match(AT);
		}
		match(AT);
		match(LSQUARE);
		KeyValueList();
		match(RSQUARE);
		return;
	}
	
	private void KeyValueList() throws SyntaxException {
		if (isKind(FactorFirstSet)) {
			KeyValueExpression();
			while (true) {
				if (t.kind == COMMA) {
					match(COMMA);
					KeyValueExpression();
				} else {
					break;
				}
			}
		} else {
			return;
		}
	}
	
	private void KeyValueExpression() throws SyntaxException {
		Expression(false, false);
		match(COLON);
		Expression(false, false);
		return;
	}
	
	private void List(final boolean atMatched) throws SyntaxException {
		if (!atMatched) {
			match(AT);
		}
		match(LSQUARE);
		ExpressionList();
		match(RSQUARE);
		return;
	}
}