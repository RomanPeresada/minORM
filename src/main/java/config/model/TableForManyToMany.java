package config.model;

public class TableForManyToMany {
    private String name;
    private String column;
    private String tableNameOfColumn;
    private String columnType;
    private String inverseColumn;
    private String tableNameOfInverseColumn;
    private String inverseColumnType;

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getInverseColumnType() {
        return inverseColumnType;
    }

    public void setInverseColumnType(String inverseColumnType) {
        this.inverseColumnType = inverseColumnType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTableNameOfInverseColumn() {
        return tableNameOfInverseColumn;
    }

    public void setTableNameOfInverseColumn(String tableNameOfInverseColumn) {
        this.tableNameOfInverseColumn = tableNameOfInverseColumn;
    }

    public String getTableNameOfColumn() {
        return tableNameOfColumn;
    }

    public void setTableNameOfColumn(String tableNameOfColumn) {
        this.tableNameOfColumn = tableNameOfColumn;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getInverseColumn() {
        return inverseColumn;
    }

    public void setInverseColumn(String inverseColumn) {
        this.inverseColumn = inverseColumn;
    }

}
