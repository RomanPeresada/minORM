package config;

public class FieldPropertiesInDatabase {
    private String name;
    private String type;
    private boolean unique;
    private boolean nullable;
    private int length;
    private boolean isPrimary;


    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public String getName() {
        return name;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

}
