package config;

import config.enu.RelationshipType;

public class FieldPropertiesInDatabase {
    private String name;
    private String type;
    private boolean unique;
    private boolean nullable;
    private int length;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private Class foreignEntity;
    private RelationshipType relationshipType = RelationshipType.NOTHING;

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

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

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    public void setForeignKey(boolean foreignKey) {
        isForeignKey = foreignKey;
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

    public Class getForeignEntity() {
        return foreignEntity;
    }

    public void setForeignEntity(Class foreignEntity) {
        this.foreignEntity = foreignEntity;
    }
}
