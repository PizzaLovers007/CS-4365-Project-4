/**
 * Object to represent a variable and its assignment.
 */
public class Variable implements Comparable<Variable> {

    private char name;  // name of the variable
    private boolean isSet;  // true if the variable has been set
    private boolean value;  // value of the variable

    /**
     * Create unset variable.
     * @param name variable name
     */
    public Variable(char name) {
        this.name = name;
    }

    /**
     * Create preset variable.
     * @param name variable name
     * @param value preset variable value
     */
    public Variable(char name, boolean value) {
        this(name);
        isSet = true;
        this.value = value;
    }

    /**
     * Gets the name of the variable.
     * @return variable name
     */
    public char getName() {
        return name;
    }

    /**
     * Gets the value of the variable.
     * @return variable value if it has been set, false otherwise
     */
    public boolean getValue() {
        return isSet ? value : false;
    }

    /**
     * Sets the value of the variable.
     * @param val value to set the variable to
     */
    public void setValue(boolean val) {
        value = val;
        isSet = true;
    }

    /**
     * Clears the value of the variable.
     */
    public void unsetValue() {
        isSet = false;
    }

    /**
     * Gets if the variable is set.
     * @return true if variable has been set
     */
    public boolean isSet() {
        return isSet;
    }

    @Override
    public int hashCode() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Variable) {
            Variable other = (Variable)obj;
            return name == other.name;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%c%s", name, isSet ? String.format(" = %c", value ? 't' : 'f') : "");
    }

    @Override
    // Alphabetical order
    public int compareTo(Variable other) {
        return name - other.name;
    }
}
