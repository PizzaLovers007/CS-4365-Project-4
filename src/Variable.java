public class Variable {

    private char name;
    private boolean isSet;
    private boolean value;

    public Variable(char name) {
        this.name = name;
    }

    public Variable(char name, boolean value) {
        this(name);
        isSet = true;
        this.value = value;
    }

    public char getName() {
        return name;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean val) {
        value = val;
        isSet = true;
    }

    public void unsetValue() {
        isSet = false;
    }

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
}
