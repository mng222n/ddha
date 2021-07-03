package function;

import java.io.StreamTokenizer;

public class SyntaxErrorException extends Exception {

	private static final long serialVersionUID = 1035562010426719163L;

	StreamTokenizer st = null;

	String msg;

	public SyntaxErrorException(StreamTokenizer st) {
		this.st = st;
	}

	public SyntaxErrorException(StreamTokenizer st, String msg) {
		this.st = st;
		this.msg = msg;
	}

	public StreamTokenizer tokenizer() {
		return st;
	}

	public String getMessage() {
		return "Syntax Error " + st + ((msg == null) ? "" : " " + msg);
	}
}
