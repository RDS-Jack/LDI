package uk.ac.derby.ldi.calc.calculator;

public class ValueDouble extends ValueAbstract {
	
	private double internalValue;
	
	public ValueDouble(double b) {
		internalValue = b;
	}
	
	/** Convert this to a primitive boolean. */
	public boolean booleanValue() {
		throw new ExceptionSemantic("Cannot convert double to boolean.");
	}
	
	/** Convert this to a primitive integer. */
	public int intValue() {
		return (int)internalValue;
	}
	
	/** Convert this to a primitive double. */
	public double doubleValue() {
		return internalValue;
	}
	
	public Value or(Value v) {
		throw new ExceptionSemantic("Cannot perform logical OR on double.");
	}

	public Value and(Value v) {
		throw new ExceptionSemantic("Cannot perform logical AND on double.");
	}

	public Value not() {
		throw new ExceptionSemantic("Cannot perform logical NOT on double.");
	}

	public int compare(Value v) {
		if (internalValue == v.doubleValue())
			return 0;
		else if (internalValue > v.doubleValue())
			return 1;
		else
			return -1;
	}
	
	public Value add(Value v) {
		return new ValueDouble(internalValue + v.doubleValue());
	}

	public Value subtract(Value v) {
		return new ValueDouble(internalValue - v.doubleValue());
	}

	public Value mult(Value v) {
		return new ValueDouble(internalValue * v.doubleValue());
	}

	public Value div(Value v) {
		return new ValueDouble(internalValue / v.doubleValue());
	}

	public Value unary_plus() {
		return new ValueDouble(internalValue);
	}

	public Value unary_minus() {
		return new ValueDouble(-internalValue);
	}
	
	public String toString() {
		return "" + internalValue;
	}

	public String stringValue() {
		return toString();
	}
}
