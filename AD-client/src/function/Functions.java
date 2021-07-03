package function;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class Functions {
	public static final Function SIN = new Function() {
		public double value(double x) {
			return Math.sin(x);
		}

	};

	public static final Function COS = new Function() {
		public double value(double x) {
			return Math.cos(x);
		}
	};

	public static final Function X = new Function() {
		public double value(double x) {
			return x;
		}
	};

	public static final Function compose(final Function f1, final Function f2) {
		return new Function() {
			public double value(double x) {
				return f1.value(f2.value(x));
			}
		};
	}

	public static final Function plus(final Function f1, final Function f2) {
		return new Function() {
			public double value(double x) {
				return f1.value(x) + f2.value(x);
			}
		};
	}

	public static final Function minus(final Function f1, final Function f2) {
		return new Function() {
			public double value(double x) {
				return f1.value(x) - f2.value(x);
			}
		};
	}

	public static final Function times(final Function f1, final Function f2) {
		return new Function() {
			public double value(double x) {
				return f1.value(x) * f2.value(x);
			}
		};
	}

	public static final Function over(final Function f1, final Function f2) {
		return new Function() {
			public double value(double x) {
				return f1.value(x) / f2.value(x);
			}
		};
	}

	public static final Function constant(final double c) {
		return new Function() {
			public double value(double x) {
				return c;
			}
		};
	}

	public static final Map<String, Operator> operators = new HashMap<String, Operator>();

	private static abstract class Operator {
		final String name;

		final int arity;

		public Operator(String name, int arity) {
			this.name = name;
			this.arity = arity;
		}

		public abstract Function eval(Function... args);
	}

	private static Operator[] entries = { new Operator("x", 0) {
		public Function eval(Function... functions) {
			return X;
		}
	}, new Operator("sin", 1) {
		public Function eval(Function... args) {
			return compose(SIN, args[0]);
		}
	}, new Operator("cos", 1) {
		public Function eval(Function... args) {
			return compose(COS, args[0]);
		}
	}, new Operator("+", 2) {
		public Function eval(Function... args) {
			return plus(args[0], args[1]);
		}
	}, new Operator("-", 2) {
		public Function eval(Function... args) {
			return minus(args[0], args[1]);
		}
	}, new Operator("*", 2) {
		public Function eval(Function... args) {
			return times(args[0], args[1]);
		}
	}, new Operator("/", 2) {
		public Function eval(Function... args) {
			return over(args[0], args[1]);
		}
	} };

	static {
		for (Operator e : entries) {
			operators.put(e.name, e);
		}
	}

	private static final Function parseFunction(StreamTokenizer st)
			throws SyntaxErrorException, IOException {
		switch (st.ttype) {
		case StreamTokenizer.TT_NUMBER:
			return constant(st.nval);
		case StreamTokenizer.TT_WORD:
			Operator op = operators.get(st.sval);
			if (op == null) {
				throw new SyntaxErrorException(st, "Unknown operator");
			}
			Function[] args = new Function[op.arity];
			for (int i = 0; i < op.arity; ++i) {
				st.nextToken();
				args[i] = parseFunction(st);
			}
			return op.eval(args);
		default:
			throw new SyntaxErrorException(st);
		}
	}

	public static final Function parse(String s) throws SyntaxErrorException,
			IOException {

		StreamTokenizer st = new StreamTokenizer(new StringReader(s));
		st.wordChars('!', '~');
		st.eolIsSignificant(false);
		st.nextToken();
		Function f = parseFunction(st);
		st.nextToken();
		if (st.ttype != StreamTokenizer.TT_EOF) {
			throw new SyntaxErrorException(st);
		}
		return f;
	}

}
