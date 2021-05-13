package uk.ac.derby.ldi.sili2.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import uk.ac.derby.ldi.sili2.parser.ast.*;
import uk.ac.derby.ldi.sili2.values.*;

class VariableData{
	public Value value;
	public String type;
}

class ArrayData{
	public List<Object> Values = new ArrayList<Object>();
	public String Type;
}

class UserClass
{
	public HashMap<String,VariableData> variables = new HashMap<String, VariableData>();
}

class UserObject
{
	public String Name;
	public UserClass Class;
}

public class Parser implements SiliVisitor {
	
	// Scope display handler
	private Display scope = new Display();
	private HashMap<String,VariableData> variables = new HashMap<String, VariableData>();
	private HashMap<String,ArrayData> arrays = new HashMap<String, ArrayData>();
	private HashMap<String,UserClass> classes = new HashMap<String, UserClass>();
	private HashMap<String,UserObject> objects = new HashMap<String, UserObject>();

	private List<Object> tempStore = new ArrayList<Object>();
	
	// Get the ith child of a given node.
	private static SimpleNode getChild(SimpleNode node, int childIndex) {
		return (SimpleNode)node.jjtGetChild(childIndex);
	}
	
	// Get the token value of the ith child of a given node.
	private static String getTokenOfChild(SimpleNode node, int childIndex) {
		return getChild(node, childIndex).tokenValue;
	}
	
	// Execute a given child of the given node
	private Object doChild(SimpleNode node, int childIndex, Object data) {
		return node.jjtGetChild(childIndex).jjtAccept(this, data);
	}
	
	// Execute a given child of a given node, and return its value as a Value.
	// This is used by the expression evaluation nodes.
	Value doChild(SimpleNode node, int childIndex) {
		return (Value)doChild(node, childIndex, null);
	}
	
	// Execute all children of the given node
	Object doChildren(SimpleNode node, Object data) {
		return node.childrenAccept(this, data);
	}
	
	// Called if one of the following methods is missing...
	public Object visit(SimpleNode node, Object data) {
		System.out.println(node + ": acceptor not implemented in subclass?");
		return data;
	}
	
	// Execute a Sili program
	public Object visit(ASTCode node, Object data) {
		return doChildren(node, data);	
	}
	
	// Execute a statement
	public Object visit(ASTStatement node, Object data) {
		return doChildren(node, data);	
	}

	// Execute a block
	public Object visit(ASTBlock node, Object data) {
		return doChildren(node, data);	
	}

	// Function definition
	public Object visit(ASTFnDef node, Object data) {
		// Already defined?
		if (node.optimised != null)
			return data;
		// Child 0 - identifier (fn name)
		String fnname = getTokenOfChild(node, 0);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		// Child 1 - function definition parameter list
		doChild(node, 1, currentFunctionDefinition);
		// Add to available functions
		scope.addFunction(currentFunctionDefinition);
		// Child 2 - function body
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		// Child 3 - optional return expression
		if (node.fnHasReturn)
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 3));
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentFunctionDefinition;
		return data;
	}
	
	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		FunctionDefinition currentDefinition = (FunctionDefinition)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			currentDefinition.defineParameter(getTokenOfChild(node, i));
		return data;
	}
	
	public Object visit(ASTArrayAssignment node, Object data)
	{
		String name = getTokenOfChild(node,1);
		String type = getTokenOfChild(node,0);
		
		ArrayData newArr = arrays.get(name);
		if(newArr == null)
		{
			newArr = new ArrayData();
			newArr.Type = type;
		}
		
		//Do last node to populate tempStore
		doChild(node,2);
		
		switch(newArr.Type)
		{
			case "Integer":
				try 
				{
					for(int i=0; i<tempStore.size();i++)
					{
						newArr.Values.add(new ValueInteger(Integer.parseInt(tempStore.get(i).toString())));
					}
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Integer");
				}
				break;
				
			case "String": 
				try 
				{
					for(int i=0; i<tempStore.size();i++)
					{
						newArr.Values.add(new ValueString(tempStore.get(i).toString()));
					}
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - String");
				}
				break;
				
			case "Boolean":
				try 
				{
					for(int i=0; i<tempStore.size();i++)
					{
						newArr.Values.add(new ValueBoolean(Boolean.parseBoolean(tempStore.get(i).toString())));
					}
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Boolean");
				}
				break;
			case "Double":
				try 
				{
					for(int i=0; i<tempStore.size();i++)
					{
						newArr.Values.add(new ValueDouble(Double.parseDouble(tempStore.get(i).toString())));
					}
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Double");
				}
				break;
		}
		
		arrays.remove(name);
		arrays.put(name, newArr);
		
		//Cleanup tempStore
		tempStore.removeAll(tempStore);
		
		//Code for testing stored contents of arrays
		/*ArrayData test = arrays.get(name);
		
		for(int i = 0; i<test.Values.size(); i++)
		{
			System.out.print(test.Values.get(i));			
		}*/
		
		return data;
	}
	
	public Object visit(ASTArrayArgList node, Object data) {
		for(int i=0; i<node.jjtGetNumChildren(); i++)
		{
			tempStore.add(doChild(node,i).toString());
		}
		return data;
	}
	
	// Function body
	public Object visit(ASTFnBody node, Object data) {
		return doChildren(node, data);
	}
	
	// Function return expression
	public Object visit(ASTReturnExpression node, Object data) {
		return doChildren(node, data);
	}
	
	// Function call
	public Object visit(ASTCall node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		scope.execute(newInvocation, this);
		return data;
	}
	
	// Function invocation in an expression
	public Object visit(ASTFnInvoke node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			if (!fndef.hasReturn())
				throw new ExceptionSemantic("Function " + fnname + " is being invoked in an expression but does not have a return value.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		return scope.execute(newInvocation, this);
	}
	
	// Function invocation argument list.
	public Object visit(ASTArgList node, Object data) {
		FunctionInvocation newInvocation = (FunctionInvocation)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			newInvocation.setArgument(doChild(node, i));
		newInvocation.checkArgumentCount();
		return data;
	}
	
	// Execute an IF 
	public Object visit(ASTIfStatement node, Object data) {
		// evaluate boolean expression
		Value hopefullyValueBoolean = doChild(node, 0);
		if (!(hopefullyValueBoolean instanceof ValueBoolean))
			throw new ExceptionSemantic("The test expression of an if statement must be boolean.");
		if (((ValueBoolean)hopefullyValueBoolean).booleanValue())
			doChild(node, 1);							// if(true), therefore do 'if' statement
		else if (node.ifHasElse)						// does it have an else statement?
			doChild(node, 2);							// if(false), therefore do 'else' statement
		return data;
	}
	
	// Execute a FOR loop
	public Object visit(ASTForLoop node, Object data) {
		// loop initialisation
		doChild(node, 0);
		while (true) {
			// evaluate loop test
			Value hopefullyValueBoolean = doChild(node, 1);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a for loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			// do loop statement
			doChild(node, 3);
			// assign loop increment
			doChild(node, 2);
		}
		return data;
	}
	
	public Object visit(ASTWhileLoop node, Object data) {
		while (true) {
			Value hopefullyValueBoolean = doChild(node, 0);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a while loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			
			// do loop statement
			doChild(node, 1);
		}
		return data;
	}
	
	// Process an identifier
	// This doesn't do anything, but needs to be here because we need an ASTIdentifier node.
	public Object visit(ASTIdentifier node, Object data) {
		return data;
	}
	
	// Execute the WRITE statement
	public Object visit(ASTWrite node, Object data) {
		System.out.println(doChild(node, 0));
		return data;
	}
	
	// Dereference a variable or parameter, and return its value.
	/*public Object visit(ASTDereference node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = node.tokenValue;
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		return reference.getValue();
	}*/
	public Object visit(ASTDereference node, Object data) {
		VariableData variable = variables.get(node.tokenValue);
		if(variable == null)
		{
			Display.Reference reference;
			if (node.optimised == null) {
				String name = node.tokenValue;
				reference = scope.findReference(name);
				if (reference == null)
					throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			return reference.getValue();
		}
		return variable.value;
	}
	
	public Object visit(ASTArrayDereference node, Object data)
	{
		String name = getTokenOfChild(node,0);
		Integer index = Integer.parseInt(doChild(node,1).toString());
		
		ArrayData array = arrays.get(name);
		if(array == null)
		{
			throw new ExceptionSemantic("Referenced array does not exist.");
		}
		
		var arrData = array.Values.get(index);
		
		return arrData;
	}
	
	// Execute an assignment statement.
	public Object visit(ASTAssignment node, Object data) {
		VariableData variable = variables.get(getTokenOfChild(node,0));
		if(variable == null)
		{			
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			reference.setValue(doChild(node, 1));
		}
		else
		{
			String name = getTokenOfChild(node,0);
			Value newVal = doChild(node,1);
			switch(variable.type)
			{
			case "String":
				try {
					variable.value = new ValueString(newVal.stringValue());
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - String"); 
				}
				break;
			case "Integer":
				try {
					variable.value = new ValueInteger(Integer.parseInt(newVal.stringValue()));
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Integer");
				}
				break;
			case "Boolean":
				try {
					variable.value = new ValueBoolean(Boolean.parseBoolean(newVal.stringValue()));
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Boolean");
				}
				break;
			case "Double":
				try {
					variable.value = new ValueDouble(Double.parseDouble(newVal.stringValue()));
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Double");
				}
				break;
			}
			variables.remove(name);
			variables.put(name,variable);
		}
		return data;
	}
	
	public Object visit(ASTTypedAssignment node, Object data) {
		if (node.optimised == null)
		{
			String name = getTokenOfChild(node,1);
			
			VariableData newVar = variables.get(name);
			if(newVar == null)
			{
				newVar = new VariableData();
				newVar.type = getTokenOfChild(node,0);
			}			
			switch(newVar.type)
			{
				case "String":
					try {
						newVar.value = new ValueString(getTokenOfChild(node,2));
					}
					catch(Exception e)
					{
						throw new ExceptionSemantic("Assigned value does not match declared type - String"); 
					}
					break;
				case "Integer":
					try {
						newVar.value = new ValueInteger(Integer.parseInt(getTokenOfChild(node,2)));
					}
					catch(Exception e)
					{
						throw new ExceptionSemantic("Assigned value does not match declared type - Integer");
					}
					break;
				case "Boolean":
					try {
						newVar.value = new ValueBoolean(Boolean.parseBoolean(getTokenOfChild(node,2)));
					}
					catch(Exception e)
					{
						throw new ExceptionSemantic("Assigned value does not match declared type - Boolean");
					}
					break;
				case "Double":
					try {
						newVar.value = new ValueDouble(Double.parseDouble(getTokenOfChild(node,2)));
					}
					catch(Exception e)
					{
						throw new ExceptionSemantic("Assigned value does not match declared type - Double");
					}
					break;
					
			}
			variables.put(name, newVar);			
		}
		return data;
	}

	// OR
	public Object visit(ASTOr node, Object data) {
		return doChild(node, 0).or(doChild(node, 1));
	}

	// AND
	public Object visit(ASTAnd node, Object data) {
		return doChild(node, 0).and(doChild(node, 1));
	}

	// ==
	public Object visit(ASTCompEqual node, Object data) {
		return doChild(node, 0).eq(doChild(node, 1));
	}

	// !=
	public Object visit(ASTCompNequal node, Object data) {
		return doChild(node, 0).neq(doChild(node, 1));
	}

	// >=
	public Object visit(ASTCompGTE node, Object data) {
		return doChild(node, 0).gte(doChild(node, 1));
	}

	// <=
	public Object visit(ASTCompLTE node, Object data) {
		return doChild(node, 0).lte(doChild(node, 1));
	}

	// >
	public Object visit(ASTCompGT node, Object data) {
		return doChild(node, 0).gt(doChild(node, 1));
	}

	// <
	public Object visit(ASTCompLT node, Object data) {
		return doChild(node, 0).lt(doChild(node, 1));
	}

	// +
	public Object visit(ASTAdd node, Object data) {
		return doChild(node, 0).add(doChild(node, 1));
	}

	// -
	public Object visit(ASTSubtract node, Object data) {
		return doChild(node, 0).subtract(doChild(node, 1));
	}

	// *
	public Object visit(ASTTimes node, Object data) {
		return doChild(node, 0).mult(doChild(node, 1));
	}
	
	public Object visit(ASTModulus node, Object data)
	{
		return doChild(node,0).modulus(doChild(node,1));
	}

	// /
	public Object visit(ASTDivide node, Object data) {
		return doChild(node, 0).div(doChild(node, 1));
	}

	// NOT
	public Object visit(ASTUnaryNot node, Object data) {
		return doChild(node, 0).not();
	}

	// + (unary)
	public Object visit(ASTUnaryPlus node, Object data) {
		return doChild(node, 0).unary_plus();
	}

	// - (unary)
	public Object visit(ASTUnaryMinus node, Object data) {
		return doChild(node, 0).unary_minus();
	}

	// Return string literal
	public Object visit(ASTCharacter node, Object data) {
		if (node.optimised == null)
			node.optimised = ValueString.stripDelimited(node.tokenValue);
		return node.optimised;
	}

	// Return integer literal
	public Object visit(ASTInteger node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueInteger(Long.parseLong(node.tokenValue));
		return node.optimised;
	}

	// Return floating point literal
	public Object visit(ASTRational node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueRational(Double.parseDouble(node.tokenValue));
		return node.optimised;
	}

	// Return true literal
	public Object visit(ASTTrue node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(true);
		return node.optimised;
	}

	// Return false literal
	public Object visit(ASTFalse node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(false);
		return node.optimised;
	}
	
	public Object visit(ASTQuit node, Object data)
	{
		System.exit(0);
		return node;
	}
	
	public Object visit(ASTIntegerType node, Object data) {
		return data;
	}
	
	public Object visit(ASTStringType node, Object data) {
		return data;
	}
	
	public Object visit(ASTBoolType node, Object data) {
		return data;
	}
	
	public Object visit(ASTDoubleType node, Object data) {
		return data;
	}
	
	public Object visit(ASTClassAssignment node, Object data)
	{
		String name = getTokenOfChild(node,0);
		
		UserClass current = classes.get(name);
		if(current != null)
		{
			throw new ExceptionSemantic("Class is already defined");
		}
		current = new UserClass();
		for(int i = 1; i<node.jjtGetNumChildren(); i++)
		{
			doChild(node,i);
			VariableData newVar = new VariableData();
			newVar.type = tempStore.get(0).toString();
			current.variables.put(tempStore.get(1).toString(), newVar);
			tempStore = new ArrayList<Object>();			
		}
		
		classes.put(name, current);
		
		return data;
	}
	
	public Object visit(ASTClassVarAssignment node, Object data)
	{
		String type = getTokenOfChild(node,0);
		String name = getTokenOfChild(node,1);
		tempStore.add(type);
		tempStore.add(name);
		return data;
	}
	
	public Object visit(ASTSetObjectVar node, Object data)
	{
		String objectName = getTokenOfChild(node,0);
		String variableName = getTokenOfChild(node,1);
		
		UserObject current = objects.get(objectName);
		VariableData currentData = current.Class.variables.get(variableName);
		
		Value newVal = doChild(node,2);
		switch(currentData.type)
		{
			case "String":
				try {
					currentData.value = new ValueString(newVal.stringValue());
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - String"); 
				}
				break;
			case "Integer":
				try {
					currentData.value = new ValueInteger(Integer.parseInt(newVal.stringValue()));
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Integer");
				}
				break;
			case "Boolean":
				try {
					currentData.value = new ValueBoolean(Boolean.parseBoolean(newVal.stringValue()));
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Boolean");
				}	
				break;
			case "Double":
				try {
					currentData.value = new ValueDouble(Double.parseDouble(newVal.stringValue()));
				}
				catch(Exception e)
				{
					throw new ExceptionSemantic("Assigned value does not match declared type - Double");
				}	
				break;
			
		}
		
		current.Class.variables.put(variableName, currentData);
		
		return data;
	}
	
	public Object visit(ASTInstantiateObject node, Object data)
	{
		String objectName = getTokenOfChild(node,0);
		String className = getTokenOfChild(node,1);
		
		UserClass refClass = classes.get(className);
		UserObject newObj = new UserObject();
		newObj.Name = objectName;
		newObj.Class = refClass;
		
		objects.put(newObj.Name, newObj);
		
		return data;
	}
	
	public Object visit(ASTClassDereference node, Object data)
	{
		String objectName = getTokenOfChild(node,0);
		String varName = getTokenOfChild(node,1);
		
		UserObject currentObj = objects.get(objectName);
		if(currentObj == null)
		{
			throw new ExceptionSemantic("The referenced object does not exist");
		}
		
		VariableData currentVar = currentObj.Class.variables.get(varName);
		
		return currentVar.value;
	}
	
	public Object visit(ASTRandomNumber node, Object data)
	{
		int lowerBound = Integer.parseInt(doChild(node,0).toString());
		int upperBound = Integer.parseInt(doChild(node,1).toString());
		
		Random r = new Random();
		ValueInteger val = new ValueInteger(r.nextInt((upperBound - lowerBound) + 1) + lowerBound);
		
		return val;
	}	
	
	public Object visit(ASTSetArrayIndex node, Object data)
	{
		String arrayName = getTokenOfChild(node,0);
		ArrayData array = arrays.get(arrayName);
		if(array == null)
		{
			throw new ExceptionSemantic("The referenced array does not exist");
		}
		Integer index = Integer.parseInt(doChild(node,1).toString());
		Value newVal = doChild(node,2);
		
		switch(array.Type)
		{
		case "String":
			try {
				array.Values.set(index, new ValueString(newVal.stringValue()));
			}
			catch(Exception e)
			{
				throw new ExceptionSemantic("Assigned value does not match declared type - String"); 
			}
			break;
		case "Integer":
			try {
				array.Values.set(index, new ValueInteger(Integer.parseInt(newVal.stringValue())));
			}
			catch(Exception e)
			{
				throw new ExceptionSemantic("Assigned value does not match declared type - Integer");
			}
			break;
		case "Boolean":
			try {
				array.Values.set(index, new ValueBoolean(Boolean.parseBoolean(newVal.stringValue())));
			}
			catch(Exception e)
			{
				throw new ExceptionSemantic("Assigned value does not match declared type - Boolean");
			}	
			break;
		case "Double":
			try {
				array.Values.set(index, new ValueDouble(Double.parseDouble(newVal.stringValue())));
			}
			catch(Exception e)
			{
				throw new ExceptionSemantic("Assigned value does not match declared type - Double");
			}	
			break;
		}
		
		arrays.put(arrayName, array);
		
		return data;
	}

}
