package uk.ac.derby.ldi.calc.calculator;

public class ValueString extends ValueAbstract {

	private String internalValue;
	
	public ValueString(String b) {
		internalValue = b;
	}
	
	/** Convert this to a primitive boolean. */
	public boolean booleanValue() {
		throw new ExceptionSemantic("Cannot convert string to boolean.");
	}
	
	/** Convert this to a primitive integer. */
	public int intValue() {
		return Integer.parseInt(internalValue);
	}
	
	/** Convert this to a primitive double. */
	public double doubleValue() {
		return Double.parseDouble(internalValue);
	}
	
	public Value or(Value v) {
		throw new ExceptionSemantic("Cannot perform logical OR on string.");
	}

	public Value and(Value v) {
		throw new ExceptionSemantic("Cannot perform logical AND on string.");
	}

	public Value not() {
		throw new ExceptionSemantic("Cannot perform logical NOT on string.");
	}

	public int compare(Value v) {
		if (internalValue == v.stringValue())
			return 0;
		else
			return -1;
	}

	private Value invalid() {
		throw new ExceptionSemantic("Cannot perform arithmetic on string values.");		
	}
	
	public Value add(Value v) {
		return invalid();
	}

	public Value subtract(Value v) {
		return invalid();
	}

	public Value mult(Value v) {
		return invalid();
	}

	public Value div(Value v) {
		return invalid();
	}

	public Value unary_plus() {
		return invalid();
	}

	public Value unary_minus() {
		return invalid();
	}
	
	public String toString() {
		return internalValue;
	}

	public String stringValue() {
		return toString();
	}
}
