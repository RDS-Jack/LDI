package uk.ac.derby.ldi.sili2.values;

public class ValueDouble extends ValueAbstract {

	private double internalValue;
	
	public ValueDouble(double b) {
		internalValue = b;
	}
	
	public String getName() {
		return "double";
	}
	
	/** Convert this to a primitive long. */
	public long longValue() {
		return (long)internalValue;
	}
	
	/** Convert this to a primitive double. */
	public double doubleValue() {
		return internalValue;
	}
	
	/** Convert this to a primitive String. */
	public String stringValue() {
		return "" + internalValue;
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
}
