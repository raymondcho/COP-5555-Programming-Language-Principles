package cop5555sp15;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import cop5555sp15.TokenStream.Token;
import static cop5555sp15.TokenStream.Kind.*;

public class Scanner {
	
	private enum State {
		START, 
		SPACE, 
		LINE, 
		GOT_EQUALS, 
		GOT_COMMENT, 
		GOT_FORWARDSLASH, 
		GOT_DOT, 
		GOT_EXCLAM, 
		GOT_LT, 
		GOT_GT,
		GOT_MINUS,
		GOT_STRING_LIT,
		GOT_STRING_ELE,
		GOT_ESC, 
		GOT_STRING_ELE_ESC,
		IDENT_PART, 
		GOT_ZERO, 
		GOT_INT_LIT, 
		ILLEGAL_CHAR
	}
	private State state;
	private final TokenStream inputStream;
	private final char[] inputChars;
	private final LinkedList<Character> inputCharList;
	private int index;
	private int lineNum;
	private Token lastToken;
	private final String[] keywords = {	"int", "string", "boolean", "import", "class", "def", "while",
												"if", "else", "return", "print", "size", "key", "value"};
	private final TokenStream.Kind[] keywordTokens = {KW_INT, KW_STRING, KW_BOOLEAN, KW_IMPORT, KW_CLASS,
												KW_DEF, KW_WHILE, KW_IF, KW_ELSE, KW_RETURN, KW_PRINT,
												KW_SIZE, KW_KEY, KW_VALUE};
	private final Map<String, TokenStream.Kind> keywordMap;

	public Scanner(TokenStream stream) {
		state = null;
		inputStream = stream;
		inputChars = inputStream.inputChars;
		inputCharList = new LinkedList<>();
		for (char c : inputChars) {
			inputCharList.add(c);
		}
		index = 0;
		lineNum = 1;
		lastToken = null;
		keywordMap = new HashMap<>();
		if (keywords.length != keywordTokens.length) {
			throw new IllegalArgumentException("Keywords array and KeywordTokens array length mismatch!");
		}
		for (int i = 0; i < keywords.length; i++) {
			keywordMap.put(keywords[i], keywordTokens[i]);
		}
	}
	
	public Token next() throws IOException, NumberFormatException {
		if (lastToken != null && lastToken.kind == TokenStream.Kind.EOF) {
			return null;
		}
		state = State.START;
		Token t = null;
		int begOffset = index;
		Character ch = popNextChar();
		Character nextChar = null;
		do {
			if (ch == null) {
				if (state == State.START) {
					t = inputStream.new Token(TokenStream.Kind.EOF, index, index, lineNum);
					break;
				} 
				if (state == State.GOT_COMMENT){
					t = inputStream.new Token(TokenStream.Kind.UNTERMINATED_COMMENT, begOffset, index, lineNum);
					return t;
				} 
				if (state == State.GOT_STRING_ELE) {
					t = inputStream.new Token(TokenStream.Kind.UNTERMINATED_STRING, begOffset, index, lineNum);
					return t;
				}
				if (state == State.IDENT_PART) {
					t = inputStream.new Token(TokenStream.Kind.IDENT, begOffset, index, lineNum);
					return t;
				}
			}
			switch (state) {
			case START: 
				switch(ch) {
				
				case 10:	// Line feed or \n
				case 13:	// Carriage return or \r
					state = State.LINE;
					break;
					
				// Comment
				case '/':
					state = State.GOT_FORWARDSLASH;
					break;
					
				// Separators
				case '.':
					state = State.GOT_DOT;
					break;
				case ';':
					t = inputStream.new Token(TokenStream.Kind.SEMICOLON, begOffset, index, lineNum);
					break;
				case ',':
					t = inputStream.new Token(TokenStream.Kind.COMMA, begOffset, index, lineNum);
					break;
				case '(':
					t = inputStream.new Token(TokenStream.Kind.LPAREN, begOffset, index, lineNum);
					break;
				case ')':
					t = inputStream.new Token(TokenStream.Kind.RPAREN, begOffset, index, lineNum);
					break;
				case '[':
					t = inputStream.new Token(TokenStream.Kind.LSQUARE, begOffset, index, lineNum);
					break;
				case ']':
					t = inputStream.new Token(TokenStream.Kind.RSQUARE, begOffset, index, lineNum);
					break;
				case '{':
					t = inputStream.new Token(TokenStream.Kind.LCURLY, begOffset, index, lineNum);
					break;
				case '}':
					t = inputStream.new Token(TokenStream.Kind.RCURLY, begOffset, index, lineNum);
					break;
				case ':':
					t = inputStream.new Token(TokenStream.Kind.COLON, begOffset, index, lineNum);
					break;
				case '?':
					t = inputStream.new Token(TokenStream.Kind.QUESTION, begOffset, index, lineNum);
					break;
					
				// Operators
				case '=':
					state = State.GOT_EQUALS;
					break;
				case '|':
					t = inputStream.new Token(TokenStream.Kind.BAR, begOffset, index, lineNum);
					break;
				case '&':
					t = inputStream.new Token(TokenStream.Kind.AND, begOffset, index, lineNum);
					break;
				case '!':
					state = State.GOT_EXCLAM;
					break;
				case '<':
					state = State.GOT_LT;
					break;
				case '>':
					state = State.GOT_GT;
					break;
				case '+':
					t = inputStream.new Token(TokenStream.Kind.PLUS, begOffset, index, lineNum);
					break;
				case '-':
					state = State.GOT_MINUS;
					break;
				case '*':
					t = inputStream.new Token(TokenStream.Kind.TIMES, begOffset, index, lineNum);
					break;
				case '%':
					t = inputStream.new Token(TokenStream.Kind.MOD, begOffset, index, lineNum);
					break;
				case '@':
					t = inputStream.new Token(TokenStream.Kind.AT, begOffset, index, lineNum);
					break;
				
				// String literal
				case '"':
					state = State.GOT_STRING_LIT;
					break;
					
				// Zero
				case '0':
					state = State.GOT_ZERO;
					break;
					
				// Symbol ident starts
				case '$':
				case '_':
					state = State.IDENT_PART;
					break;
					
				// Letters, integers (except 0), whitespace (except new line and return)
				default:
					if ((ch >= 65 && ch <= 90) || (ch >= 97 && ch <= 122)) {
						state = State.IDENT_PART;
					} else if (ch >= 49 && ch <= 57) {
						state = State.GOT_INT_LIT;
					} else {
						if (Character.isWhitespace(ch)) {
							state = State.SPACE;
						} else {
							state = State.ILLEGAL_CHAR;
						}
					}
				}
				break;
			case SPACE:
				begOffset = index;
				ch = popNextChar();
				state = State.START;
				break;
			case LINE:
				nextChar = peekNextChar();
				if (ch == 13 && nextChar != null && nextChar == 10) {
					popNextChar();
				}
				begOffset = index;
				ch = popNextChar();
				lineNum++;
				state = State.START;
				break;
			case GOT_EQUALS:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '=') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.EQUAL, begOffset, index, lineNum);
				} else {
					t = inputStream.new Token(TokenStream.Kind.ASSIGN, begOffset, index, lineNum);
				}
				break;
			case GOT_FORWARDSLASH:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '*') {
					popNextChar();
					ch = popNextChar();
					state = State.GOT_COMMENT;
				} else {
					t = inputStream.new Token(TokenStream.Kind.DIV, begOffset, index, lineNum);
				}
				break;
			case GOT_DOT:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '.') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.RANGE, begOffset, index, lineNum);
				} else {
					t = inputStream.new Token(TokenStream.Kind.DOT, begOffset, index, lineNum);
				}
				break;
			case GOT_EXCLAM:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '=') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.NOTEQUAL, begOffset, index, lineNum);
				} else {
					t = inputStream.new Token(TokenStream.Kind.NOT, begOffset, index, lineNum);
				}
				break;
			case GOT_LT:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '=') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.LE, begOffset, index, lineNum);
				} else if (nextChar != null && nextChar == '<') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.LSHIFT, begOffset, index, lineNum);
				} else {
					t = inputStream.new Token(TokenStream.Kind.LT, begOffset, index, lineNum);
				}
				break;
			case GOT_GT:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '=') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.GE, begOffset, index, lineNum);
				} else if (nextChar != null && nextChar == '>') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.RSHIFT, begOffset, index, lineNum);
				} else {
					t = inputStream.new Token(TokenStream.Kind.GT, begOffset, index, lineNum);
				}
				break;
			case GOT_MINUS:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '>') {
					popNextChar();
					t = inputStream.new Token(TokenStream.Kind.ARROW, begOffset, index, lineNum);
				} else {
					t = inputStream.new Token(TokenStream.Kind.MINUS, begOffset, index, lineNum);
				}
				break;
			case GOT_COMMENT:
				nextChar = peekNextChar();
				if (ch == '*' && nextChar != null && nextChar == '/') {
					popNextChar();
					begOffset = index;
					ch = popNextChar();
					state = State.START;
				} else {
					ch = popNextChar();
					state = State.GOT_COMMENT;
				}
				break;
			case GOT_STRING_LIT:
				ch = popNextChar();
				state = State.GOT_STRING_ELE;
				break;
			case GOT_STRING_ELE:
				if (ch == '\\') {
					state = State.GOT_STRING_ELE_ESC;
				} else if (ch == '"'){
					t = inputStream.new Token(TokenStream.Kind.STRING_LIT, begOffset, index, lineNum);
					break;
				} else {
					ch = popNextChar();
					state = State.GOT_STRING_ELE;
				}
				break;
			case GOT_STRING_ELE_ESC:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar == '"') {
					popNextChar();
				} 
				popNextChar();
				ch = popNextChar();
				state = State.GOT_STRING_ELE;
				break;
			case GOT_ESC:
				nextChar = peekNextChar();
				if (nextChar != null && (nextChar == 'n' || nextChar == 'r' || nextChar == '"')) {
					popNextChar();
					ch = popNextChar();
					state = State.GOT_STRING_ELE;
				} else {
					state = State.ILLEGAL_CHAR;
				}
				break;
			case IDENT_PART:
				nextChar = peekNextChar();
				if (nextChar != null && ((nextChar >= 65 && nextChar <= 90) || (nextChar >= 97 && nextChar <= 122) || (nextChar == '$') || (nextChar == '_') || (nextChar >= 48 && nextChar <= 57))) {
					ch = popNextChar();
					state = State.IDENT_PART;
				} else {
					t = checkKeyword(inputStream.new Token(TokenStream.Kind.IDENT, begOffset, index, lineNum));
				}
				break;
			case GOT_ZERO:
				t = inputStream.new Token(TokenStream.Kind.INT_LIT, begOffset, index, lineNum);
				break;
			case GOT_INT_LIT:
				nextChar = peekNextChar();
				if (nextChar != null && nextChar >= 48 && nextChar <= 57) {
					ch = popNextChar();
					state = State.GOT_INT_LIT;
				} else {
					t = inputStream.new Token(TokenStream.Kind.INT_LIT, begOffset, index, lineNum);
				}
				break;
			case ILLEGAL_CHAR:
				t = inputStream.new Token(TokenStream.Kind.ILLEGAL_CHAR, begOffset, index, lineNum);
				break;
			default:
				throw new IllegalArgumentException("Unknown operation result.");
			}
		} while (t == null);
		lastToken = t;
		return t;
	}
	
	private Token checkKeyword(final Token t) {
		if (t.kind == TokenStream.Kind.IDENT) {
			String tokenText = t.getText();
			if (tokenText.equals("null")) {
				return inputStream.new Token(TokenStream.Kind.NL_NULL, t.beg, t.end, t.lineNumber);
			}
			if (tokenText.equals("true")) {
				return inputStream.new Token(TokenStream.Kind.BL_TRUE, t.beg, t.end, t.lineNumber);
			}
			if (tokenText.equals("false")) {
				return inputStream.new Token(TokenStream.Kind.BL_FALSE, t.beg, t.end, t.lineNumber);
			}
			TokenStream.Kind tokenKind = keywordMap.get(tokenText);
			if (tokenKind != null) {
				return inputStream.new Token(tokenKind, t.beg, t.end, t.lineNumber);
			}
		}
		return t;
	}

	// Fills in the stream.tokens list with recognized tokens from the input
	public void scan() {
		if (inputStream == null) {
			throw new IllegalArgumentException("Null input TokenStream.");
		}
		Token token;
		try {
			token = next();
			while (token != null) {
				inputStream.tokens.add(token);
				token = next();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Character popNextChar() {
		Character poppedChar = null;
		if (!inputCharList.isEmpty()) {
			poppedChar = inputCharList.removeFirst();
		}
		if (poppedChar != null) {
			index++;
		}
		return poppedChar;
	}
	
	private Character peekNextChar() {
		Character peekedChar = null;
		if (!inputCharList.isEmpty()) {
			peekedChar = inputCharList.getFirst();
		}
		return peekedChar;
	}

}
