
import java.util.ArrayList;
import java.util.List;


public class RegexParser {
	protected final char[] pattern;	// the regex pattern to parse
	protected int p = 0;	 		// pattern[p] is next char to match
	protected final int n;
	protected final List<String> errors = new ArrayList<>();


	public RegexParser(String pattern) {
		this.pattern = pattern.toCharArray();
		n = this.pattern.length;
	}

	public List<String> getErrors() {
		return errors;
	}

	public NFA parse() {
		NFA nfa = regex();
		if ( p<n ) {
			error("unrecognized input");
			return NFA.error();
		}
		return nfa;
	}


	// P a r s e  m e t h o d s

	/** Parse regex : sequence ('|' sequence)* ; */
	public NFA regex() {
		NFA r;
		r = sequence();

		if (r == null) {
			r = NFA.atom(NFA.EPSILON);
		}

		if (look() == '|'){ //if there is an |, we need to create a new start and end with epsilons
			NFA tmp1 = new NFA();
			tmp1.start.epsilon(r.start);
			r.stop.epsilon(tmp1.stop);
			r = tmp1;
		}

		while ( p<n && look()=='|' ) {
			match('|');
			if (look() == ')'){ //for cases where there is nothing after the or. ex: (|) or (a|)
				NFA tmp = NFA.atom(NFA.EPSILON);
				r.start.epsilon(tmp.start);
				tmp.stop.epsilon(r.stop);
				return r;
			}
			NFA x = sequence();
			r.start.epsilon(x.start); // connect the next sequence
			x.stop.epsilon(r.stop);	// sequence connects to stop node
		}

		return r;
	}

	/** Parse sequence : closure sequence | ; */
	public NFA sequence() {
		NFA r;
		if (isElem() || look() == '('){
			r = closure();
			NFA k = sequence();
			if (k != null) {
				r.stop.epsilon(k.start);
				return new NFA(r.start, k.stop);
			} else {
				return r;
			}
		}

		else {
			return null;
		}
	}


	/** Parse closure : element '*' | element ; */
	public NFA closure() {
		NFA r;
		r = element();
		while (look() == '*'){
			match('*');
			NFA tmp = new NFA();
			tmp.start.epsilon(r.start);
			r.stop.epsilon(tmp.start);
			r.stop.epsilon(tmp.stop);
			tmp.start.epsilon(tmp.stop);
			r = tmp;
			if (look() == '*'){
				error("unrecognized input");
			}
		}
		return r;
	}

	/** Parse element : letter | '(' regex ')' ; */
	public NFA element() {
		NFA r;
		char k;
		if (isLetter()){
			k = pattern[p];
			match(k);
			r = NFA.atom(k);
			return r;
		} if (look() == '(') {
			match('(');
			r = regex();
			match(')');
			return r;
		} else  {
			return NFA.error();
		}
	}

	// S u p p o r t
	public boolean isLetter(){
		return look() <= 'z' && look() >= 'a';
	}

	public boolean isElem() {
		return look() >= 97 && look() <= 122;
	}

	public void match(char c) {
		if (look() == c) consume();
		else error("expected ) at EOF");
	}

	public void consume() {
		p++;
	}


	public char look(){
		if (p >= pattern.length) return (char) -1;
		return pattern[p];
	}


	public void error(String msg) {
		StringBuilder buf = new StringBuilder();
		buf.append(msg).append(" in ").append(new String(pattern));
		buf.append("\n");
		int spaces = p+msg.length()+" in ".length();
		for (int i=0; i<=spaces; i++) buf.append(" ");
		buf.append("^");
		errors.add(buf.toString());
	}


}
