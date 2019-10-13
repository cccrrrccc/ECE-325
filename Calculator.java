import java.util.regex.*;
import java.util.*;
/**
 * Assignment 3: Exception handling <br />
 * Calculator using BNF
 */

/**
 * This assignment is to make a calculator and handle the exception
 * To be convenient, I assume this calculator only support one-digit
 * And I also assume that the blank space is placed properly
 * If not, the following part need to be modified:
 * 		(1) The way to get number/variable in method calculation()
 * @author Vincent
 *
 */

class SyntaxError extends Exception {
	public SyntaxError (String message) {
		super(message);
	}
}

class RuntimeError extends Exception {
	public RuntimeError (String message) {
		super(message);
	}
}

public class Calculator {
	
	String exp;
	
	public String calculation(String str) throws SyntaxError, RuntimeError {
		/**
		 * This method is gonna calculate an expression such as 
		 * 2 + 3 * 2 - 5
		 * in math order ^  /  *  /  ( +  - )
		 */
		String v = str;
		int overflow = 0;
		Pattern p = Pattern.compile("\\^");
		Matcher m = p.matcher(v);
		while (m.find()) {
			int i = m.start();
			int a = Integer.valueOf(v.charAt(i - 2) - '0');
			int b = Integer.valueOf(v.charAt(i + 2) - '0');
			String c;
			if ((Math.pow(a, b)) >= 10) {
				overflow += (int) (Math.pow(a, b)) / 10;
				int k =(int) (Math.pow(a, b)) % 10;
				c = Integer.toString(k);
			}
			else {
				c = Integer.toString((int)Math.pow(a, b));
			}
			v = v.replace(v.substring(i - 2, i + 3), c);
			m = p.matcher(v);
		}
		p = Pattern.compile("\\*");
		m = p.matcher(v);
		while (m.find()) {
			int i = m.start();
			int a = Integer.valueOf(v.charAt(i - 2) - '0');
			int b = Integer.valueOf(v.charAt(i + 2) - '0');
			String c;
			if ((a * b) >= 10) {
				overflow += (a * b) / 10;
				int k = (a * b) % 10;
				c = Integer.toString(k);
			}
			else {
				c = Integer.toString(a * b);
			}
			v = v.replace(v.substring(i - 2, i + 3), c);
			m = p.matcher(v);
		}
		p = Pattern.compile("\\+");
		m = p.matcher(v);
		while (m.find()) {
			int i = m.start();
			int a = Integer.valueOf(v.charAt(i - 2) - '0');
			int b = Integer.valueOf(v.charAt(i + 2) - '0');
			String c;
			// Handle overflow
			if ((a + b) >= 10) {
				overflow += (a + b) / 10;
				int k = (a + b) % 10;
				c = Integer.toString(k);
			}
			else {
				c = Integer.toString(a + b);
			}
			v = v.replace(v.substring(i - 2, i + 3), c);
			m = p.matcher(v);
		}
		p = Pattern.compile("\\-");
		m = p.matcher(v);
		while (m.find()) {
			int i = m.start();
			int a = Integer.valueOf(v.charAt(i - 2) - '0');
			int b = Integer.valueOf(v.charAt(i + 2) - '0');
			String c;
			if ((a - b) < 0) {
				overflow -= 1;
				int k = (a - b) + 10;
				c = Integer.toString(k);
			}
			else {
				c = Integer.toString(a - b);
			}
			v = v.replace(v.substring(i - 2, i + 3), c);
			m = p.matcher(v);
		}
		int fin = Integer.parseInt(v) + overflow * 10;
		return Integer.toString(fin);
	}
	
	public String let(String v, Dictionary variable) throws SyntaxError, RuntimeError {
		// v in form: "let \w = \w"
		String a = v.substring(0, 3);
		if (!a.equals((String) "let")) {
			throw new RuntimeError("Runtime error: undefined function");
		}
		String key = v.substring(4, 5);
		String value = v.substring(8);
		Pattern p = Pattern.compile("[a-zA-Z]");
		Matcher m = p.matcher(value);
		if (m.matches()) {
			value = (String) variable.get(value);
		}
		variable.put(key, value);
		return value;
	}
	
	/**
	 * This method is to turn <value> into a number (String)
	 */
	public String getValue(String v, Dictionary variable) throws SyntaxError, RuntimeError {
		Pattern p = Pattern.compile("[0-9]");
		Matcher m = p.matcher(v);
		if (m.matches()) {
			// Pure number
			return v;
		}
		p = Pattern.compile("[a-zA-Z]");
		m = p.matcher(v);
		if (m.matches()) {
			// variable
			// use Dictionary.get() here
			// if return is null, then undefined exception
			String s = (String) variable.get(v);
			if (s == null) {
				throw new RuntimeError(String.format("runtime error: '%s' undefined", v));
			}
			return s;
		}
		p = Pattern.compile("[a-zA-Z]{3} \\w = \\w");
		m = p.matcher(v);
		if (m.matches()) {
			return let(v, variable);
		}
		/**
		 * Here comes the blankets, use a recursion
		 */
		return blanketRecursion(v, variable);
	}
	
	public String blanketRecursion(String v, Dictionary variable) throws SyntaxError, RuntimeError {
		/**
		 * For a single pair of blankets, 2 cases:
		 * One is calculation, the other is let x = ?
		 * For several pairs of blankets:
		 * Use recursion to reach the inner blankets first, than the outer
		 */
		// No matter what, v = "(content)", get the content
		// In order to handle the exception better, use a new method to get contents from blankets
		String str = getBlanket(v);
		// use while-loop to recurse
		while (str.indexOf('(') != -1 || str.indexOf(')') != -1) {
			/**
			 * possible cases: ( )... ; (( )..)..; (( )..( )).. 
			 */
			
			int j = str.indexOf(')'); // first index of ')'
			int i = str.lastIndexOf('(', j);
			// substitute the content inside blankets with its return from blanketRecursion()
			str = str.replace(str.substring(i, j + 1), blanketRecursion(str.substring(i, j + 1), variable));
		}
		
		// now the only left is content
		// content case: (1) pure calculation expression: need to substitute the variable with value
		// (2) include let function
		// syntax error: '=' expected
		Pattern p = Pattern.compile("[a-zA-Z]{3} \\w \\w");
		Matcher m = p.matcher(str);
		if (m.find()) {
			throw new SyntaxError("syntax error: '=' expected");
		}
		
		// Do (1), \w( [+\-\*\^] \w)*
		p = Pattern.compile("\\w( [+\\-\\*\\^] \\w)*");
		m = p.matcher(str);
		if (m.matches()) {
			p = Pattern.compile("[a-zA-Z]");
			m = p.matcher(str);
			while (m.find()) {
				int i = m.start();
				String ch = str.substring(i, i + 1);
				String s = (String) variable.get(ch);
				if (s == null) {
					throw new RuntimeError(String.format("runtime error: '%s' undefined", ch));
				}
				str = str.replaceAll(ch, s);
			}
			return calculation(str);
		}
		// Do (2)
		p = Pattern.compile("[a-zA-Z]{3} \\w = \\w");
		m = p.matcher(str);
		while (m.find()) {
			// not to overflow in String.substring
			if (str.length() == 9) {
				return let(str, variable);
			}
			else {
				int i = m.start();
				String num = str.substring(i + 8);
				p = Pattern.compile("[a-zA-Z]");
				m = p.matcher(num);
				String s = num;
				while (m.find()) {
					// variable
					// use Dictionary.get() here
					// if return is null, then undefined exception
					int j = m.start();
					String g = num.substring(j, j + 1);
					if (variable.get(g) == null) {
						throw new RuntimeError(String.format("runtime error: '%s' undefined", g));
					}
					s = s.replace(g, (String) variable.get(g));
				}
				str = str.replace(num, calculation(s));
				String l = str.substring(i, i + 9);
				str = str.replace(l, let(l, variable));
				return calculation(str);
			}
		}
		return "Exception";
	}
	
	public String getBlanket(String v) {
		// Get rid of the outer blankets, if there is exception handle it
		int i = v.indexOf('(');
		int j = v.lastIndexOf(')');
		if (i < j && i != -1 && j != -1) {
			// No exception
			return v.substring(i + 1, j);
		}
		return "Exception";
	}

	public String operation(String e) throws SyntaxError, RuntimeError {
		/**
		 * Method value() will cut the <value> out, remain (SP <op> SP <value>)*
		 * So method operation should cut SP <op> SP out
		 * In operation() state, the start of exp should be " [+-\*\^] "
		 * Take it out, and add return it
		 */
		String str = e.substring(0, 3);
		Pattern p = Pattern.compile(" [+\\-\\*\\^] ");
		Matcher m = p.matcher(str);
		if (m.matches()) {
			exp = e.substring(3);
			return str;
		}
		else {
			throw new SyntaxError("syntax error");
		}
	}
	
	public String value(String e, Dictionary variable) throws SyntaxError, RuntimeError{
		/**
		 * <value> (SP <operation> SP <value>)*
		 * Find the index first match SP <op> SP
		 * String before it is <value>
		 * However, blanket is inside <value>
		 * We have to make sure the operation mark inside blanket are not include
		 * Can't use regular expression directly here.
		 */
		int count = 0; // count++ when meet "(", count-- when meet ")"
		int i = 0;
		String v;
		while (i < e.length()) {
			if (e.charAt(i) == '(') {
				count++;
				i++;
				continue;
			}
			if (e.charAt(i) == ')') {
				count--;
				i++;
				continue;
			}
			// not inside any blanket
			// search the first SP <op> SP (length == 3)
			// <value> is all that before it
			if (count == 0 && (i+3) < e.length()) {
				String str = e.substring(i, i+3);
				Pattern p = Pattern.compile(" [+\\-*\\^] ");
				Matcher m = p.matcher(str);
				if (m.matches()) {
					v = e.substring(0, i);
					exp = e.substring(i);
					return getValue(v, variable);
				}
				i++;
			}
			else i++;
		}
		if (count > 0) {
			throw new SyntaxError("syntax error: ')' expected");
		}
		else if (count < 0) {
			throw new SyntaxError("syntax error: '(' expected");
		}
		// Now there is no SP <op> SP

		exp = "";
		return getValue(e, variable);
		
		/**
		 * If the method goes down here
		 * there is a exception
		 */
	}
	
    /**
     * Execute the expression, and return the correct value
     * @param exp           {@code String} The expression string
     * @return              {@code int}    The value of the expression
     */
    public String execExpression(String str) throws SyntaxError, RuntimeError{
        Dictionary variable = new Hashtable(); // Use hashtable to store variable
        exp = str.substring(0, str.length() - 1);
        String result = "";
        // TODO: Assignment 3 Part 1 -- parse, calculate the expression, and return the correct value
        /**
         * The final expression should be:
         * <value> (SP <operation> SP <value>)*
         * Therefore, I use two function to read the value and expression from the String exp
         * pseudocode is:
         * value();
         * while(exp not reach the end) {
         * 		operation();
         * 		value();
         * }
         */
        result += value(exp, variable);
        while (exp.length() > 1) {
        	result += operation(exp);
        	result += value(exp, variable);
        }
        return calculation(result);

        // TODO: Assignment 3 Part 2-1 -- when come to illegal expressions, raise proper exceptions


        //return returnValue;
    }

    /**
     * Main entry
     * @param args          {@code String[]} Command line arguments
     */
    public static void main(String[] args) throws SyntaxError, RuntimeError {
        Calculator calc = new Calculator();
        // Part 1
        String[] inputs = {
           "let x = 1;",                                                                           // 1, returns 1
           "(let x = 1) + x;",                                                                     // 2, returns 2
           "(let a = 2) + 3 * a - 5;",                                                             // 3, returns 3
           "(let x = (let y = (let z = 1))) + x + y + z;",                                         // 4, returns 4
           "1 + (let x = 1) + (let y = 2) + (1 + x) * (1 + y) - (let x = y) - (let y = 1) - x;",   // 5, returns 5
           "1 + (let a = (let b = 1) + b) + a + 1;",                                               // 6, returns 6
           "(let a = (let a = (let a = (let a = 2) + a) + a) + a) - 9;",                           // 7, returns 7
           "(let x = 2) ^ (let y = 3);",                                                           // 8, returns 8
           "(let y = 3) ^ (let x = 2);"                                                            // 9, returns 9
        };
        for (int i = 0; i < inputs.length; i++)
        	System.out.println(calc.execExpression(inputs[i]));
        //for (int i = 0; i < inputs.length; i++)
        //    System.out.println(String.format("%d -- %-90s %d", i+1, inputs[i], calc.execExpression(inputs[i])));
        
        // Part 2
        inputs = new String[] {
              //  "1 + (2 * 3;",                  // 1, syntax error: ')' expected
              //  "(let x 5) + x;",               // 2, syntax error: '=' expected
              //  "(let x = 5) (let y = 6);",     // 3, syntax error: operator expected
              //  "(let x = 5 let y = 6);",       // 4, syntax error: ')' expected
              //  "(ler x = 5) ^ (let y = 6);",   // 5, runtime error: 'ler' undefined
                "(let x = 5) + y;"              // 6, runtime error: 'y' undefined
        };
        // TODO: Assignment 3 Part 2-2 -- catch and deal with your exceptions here
        for (int i = 0; i < inputs.length; i++)
            System.out.println(String.format("%d -- %-30s %d", i+1, inputs[i], calc.execExpression(inputs[i])));
        
    }

}
