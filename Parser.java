package cop5555sp15;

import static cop5555sp15.TokenStream.Kind.*;

import java.util.LinkedList;
import java.util.List;

import cop5555sp15.TokenStream.Kind;
import cop5555sp15.TokenStream.Token;
import cop5555sp15.ast.*;

public class Parser {

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

	@SuppressWarnings("serial")
	public class ParseException extends Exception {
		
	}
	
	List<SyntaxException> exceptionList = new LinkedList<>();
	TokenStream tokens;
	Token t;
	Token consumedToken;

	Parser(TokenStream tokens) {
		this.tokens = tokens;
		t = tokens.nextToken();
		consumedToken = null;
	}

	private Kind match(Kind kind) throws SyntaxException {
		if (isKind(kind)) {
			consume();
			return kind;
		}
		SyntaxException syntaxException = new SyntaxException(t, kind);
		exceptionList.add(syntaxException);
		throw syntaxException;
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
		SyntaxException syntaxException = new SyntaxException(t, "expected one of " + sb.toString());
		exceptionList.add(syntaxException);
		throw syntaxException;
	}

	private boolean isKind(Kind kind) {
		return (t.kind == kind);
	}

	private void consume() {
		if (t.kind != EOF)
			consumedToken = t;
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


	public Program parse() {
		Program p = null;
		try {
			p = Program();
			if (p != null) {
				try {
					match(EOF);
				} catch (SyntaxException e) {
					exceptionList.add(e);
				}
			}
		} catch (ParseException e) {
			return null;
		}
		if (exceptionList.isEmpty()) {
			return p;
		} else {
			return null;
		}
	}

	private Program Program() throws ParseException {
		try {
			Token firstToken = t;
			List<QualifiedName> imports = ImportList();
			match(KW_CLASS);
			match(IDENT);
			String className = consumedToken.getText();
			Block block = Block();
			return new Program(firstToken, imports, className, block);
		} catch (SyntaxException e) {
			exceptionList.add(e);
			throw new ParseException();
		}
	}

	private List<QualifiedName> ImportList() throws SyntaxException {
		List<QualifiedName> imports = new LinkedList<>();
		while (true) {
			try {
				match(KW_IMPORT);
			} catch (SyntaxException synEx) {
				return imports;
			}
			StringBuilder importNameBuilder = new StringBuilder();
			Token firstToken = t;
			match(IDENT);
			importNameBuilder.append(consumedToken.getText());
			if (t.kind == Kind.DOT) {
				while (true) {
					match(DOT);
					importNameBuilder.append("/");
					match(IDENT);
					importNameBuilder.append(consumedToken.getText());
					if (t.kind != Kind.DOT) {
						break;
					}
				}
			}
			imports.add(new QualifiedName(firstToken, importNameBuilder.toString()));
			match(SEMICOLON);
			if (t.kind != Kind.KW_IMPORT) {
				break;
			}
		}
		return imports;
	}

	private Block Block() throws SyntaxException {
		List<BlockElem> blockElems = new LinkedList<>();
		Token firstToken = t;
		match(LCURLY);
		while (true) {
			if (t.kind == Kind.KW_DEF) {
				blockElems.add(Declaration());
				match(SEMICOLON);
			} else if (t.kind == RCURLY){
				break;
			} else {
				blockElems.add(Statement());
				match(SEMICOLON);
			}
		}
		match(RCURLY);
		return new Block(firstToken, blockElems);
	}

	private Declaration Declaration() throws SyntaxException {
		Token firstToken = t;
		match(KW_DEF);
		match(IDENT);
		Token identToken = consumedToken;
		if (t.kind == Kind.ASSIGN) {
			return ClosureDec(firstToken, identToken);
		} else {
			return VarDec(identToken);
		}
	}

	private ClosureDec ClosureDec(final Token firstToken, final Token identToken) throws SyntaxException {
		match(ASSIGN);
		Closure closure = Closure();
		return new ClosureDec(firstToken, identToken, closure);
	}
	
	private Closure Closure() throws SyntaxException {
		Token firstToken = t;
		match(LCURLY);
		List<VarDec> formalArgList = FormalArgList();
		match(ARROW);
		List<Statement> statementList = new LinkedList<>();
		while (t.kind != RCURLY) {
			statementList.add(Statement());
			match(SEMICOLON);
		}
		match(RCURLY);
		return new Closure(firstToken, formalArgList, statementList);
	}
	
	private List<VarDec> FormalArgList() throws SyntaxException {
		List<VarDec> formalArgList = new LinkedList<>();
		if (t.kind != Kind.IDENT) {
			return formalArgList;
		}
		formalArgList.add(VarDec(null));
		while (true) {
			if (t.kind == COMMA) {
				match(COMMA);
				formalArgList.add(VarDec(null));
			} else {
				break;
			}
		}
		return formalArgList;
	}
	
	private VarDec VarDec(Token identToken) throws SyntaxException {
		if (identToken == null) {
			match(IDENT);
			identToken = consumedToken;
		}
		if (t.kind != Kind.COLON) {
			return new VarDec(identToken, identToken, new UndeclaredType(identToken));
		}
		match(COLON);
		Type type = Type();
		return new VarDec(identToken, identToken, type);
	}

	private Type Type() throws SyntaxException {
		Token firstToken = t;
		if (isKind(SimpleTypes)) {
			return SimpleType(firstToken);
		} 
		if (t.kind == Kind.AT) {
			match(AT);
			if (t.kind == Kind.AT) {
				return KeyValueType(firstToken);
			} 
			if (t.kind == Kind.LSQUARE) {
				return ListType(firstToken);
			}
		}
		throw new SyntaxException(t, "Unable to match token to expected TYPE token.");
	}
	
	private SimpleType SimpleType(final Token firstToken) throws SyntaxException {
		match(SimpleTypes);
		return new SimpleType(firstToken, consumedToken);
	}
	
	private KeyValueType KeyValueType(final Token firstToken) throws SyntaxException {
		match(AT);
		match(LSQUARE);
		SimpleType simpleType = SimpleType(t);
		match(COLON);
		Type type = Type();
		match(RSQUARE);
		return new KeyValueType(firstToken, simpleType, type);
	}
	
	private ListType ListType(final Token firstToken) throws SyntaxException {
		match(LSQUARE);
		Type type = Type();
		match(RSQUARE);
		return new ListType(firstToken, type);
	}
	
	private Statement Statement() throws SyntaxException {
		Token firstToken = t;
		if (t.kind == IDENT) {
			LValue lValue = LValue();
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
	
	private LValue LValue() throws SyntaxException {
		Token firstToken = t;
		match(IDENT);
		Token identToken = consumedToken;
		if (t.kind != LSQUARE) {
			return new IdentLValue(firstToken, identToken);
		}
		match(LSQUARE);
		Expression expression = Expression(false, false);
		match(RSQUARE);
		return new ExpressionLValue(firstToken, identToken, expression);
	}
	
	private Expression Expression(final boolean transferFromRangeExpr, final boolean allowTransferToRangeExpr) throws SyntaxException {
		Expression leftTerm = Term();
		Token relOp = null;
		Expression rightTerm = null;
		while (true) {
			if (isKind(REL_OPS)) {
				relOp = RelOp();
				rightTerm = Term();
			} else {
				break;
			}
		}
		if (!transferFromRangeExpr && allowTransferToRangeExpr && t.kind == RANGE) {
			RangeExpression(true);
		}
		if (relOp == null && rightTerm == null) {
			return leftTerm;
		}
		return new BinaryExpression(leftTerm.firstToken, leftTerm, relOp, rightTerm);
	}
	
	private void RangeExpression(final boolean transferFromExpression) throws SyntaxException {
		if (!transferFromExpression) {
			Expression(true, false);
		}
		match(RANGE);
		Expression(true, false);
		return;
	}
	
	private Token RelOp() throws SyntaxException {
		match(REL_OPS);
		return consumedToken;
	}
	
	private Expression Term() throws SyntaxException {
		Expression leftElem = Elem();
		Token weakOp = null;
		Expression rightElem = null;
		while (true) {
			if (isKind(WEAK_OPS)) {
				weakOp = WeakOp();
				rightElem = Elem();
			} else {
				break;
			}
		}
		if (weakOp == null && rightElem == null) {
			return leftElem;
		}
		return new BinaryExpression(leftElem.firstToken, leftElem, weakOp, rightElem);
	}
	
	private Token WeakOp() throws SyntaxException {
		match(WEAK_OPS);
		return consumedToken;
	}
	
	private Expression Elem() throws SyntaxException {
		Expression thing = Thing();
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
	
	private Expression Thing() throws SyntaxException {
		Expression factor = Factor();
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
	
	private Expression Factor() throws SyntaxException {
		Token firstToken = t;
		if (t.kind == IDENT) {
			match(IDENT);
			Token identToken = consumedToken;
			if (t.kind == LSQUARE) {
				match(LSQUARE);
				Expression expression = Expression(false, false);
				match(RSQUARE);
				return new ListOrMapElemExpression(firstToken, identToken, expression);
			}
			if (t.kind == LPAREN) {
				return ClosureEvalExpression(firstToken, identToken);
			}
			return new IdentExpression(firstToken, identToken);
		}
		if (t.kind == INT_LIT) {
			match(INT_LIT);
			return new IntLitExpression(firstToken, consumedToken.getIntVal());
		}
		if (t.kind == BL_TRUE) {
			match(BL_TRUE);
			return new BooleanLitExpression(firstToken, consumedToken.getBooleanVal());
		}
		if (t.kind == BL_FALSE) {
			match(BL_FALSE);
			return new BooleanLitExpression(firstToken, consumedToken.getBooleanVal());
		}
		if (t.kind == STRING_LIT) {
			match(STRING_LIT);
			return new StringLitExpression(firstToken, consumedToken.getText());
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
	
	private ClosureEvalExpression ClosureEvalExpression(Token firstToken, Token identToken) throws SyntaxException {
		if (identToken == null) {
			match(IDENT);
			firstToken = consumedToken;
			identToken = consumedToken;
		}
		match(LPAREN);
		List<Expression> listExpression = ExpressionList();
		match(RPAREN);
		return new ClosureEvalExpression(firstToken, identToken, listExpression);
	}
	
	private List<Expression> ExpressionList() throws SyntaxException {
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