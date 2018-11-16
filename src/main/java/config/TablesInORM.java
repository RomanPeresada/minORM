package config;


import annotation.*;
import config.enu.RelationshipType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static config.ConnectionWithDb.getConnection;

public class TablesInORM {
    private static String pathToEntities = null;
    private static List<String> foreignkeys = new ArrayList<>();
    private final static Logger log = Logger.getLogger(TablesInORM.class);


    public static void autoCreatingTablesAfterStartOfProgram() throws ClassNotFoundException, SQLException, IOException, NoSuchFieldException {
        getInfoFromProperties();
        String[] classesName = new File(pathToEntities).list();
        pathToEntities = replaceSymbolsInPathForReflect(pathToEntities);
        if (classesName != null) {
            log.debug("Classes were found");
            for (String name : classesName) {
                Class foundClass = Class.forName(pathToEntities + name.replaceAll(".java", ""));
                String query = getQueryForCreateTable(foundClass);
                try (Statement statement = getConnection().createStatement()) {
                    statement.executeUpdate(query);
                    log.debug("query was completed : " + query);
                }
            }
            for (String foreignkey : foreignkeys) {
                try (Statement statement = getConnection().createStatement()) {
                    statement.executeUpdate(foreignkey);
                    log.debug("query was completed : " + foreignkey);
                }
            }
        }
    }

    private static String getQueryForCreateTable(Class foundClass) throws NoSuchFieldException {
        String tableName = getTableName(foundClass);
        StringBuilder primaryKey = new StringBuilder(" PRIMARY KEY(");

        boolean doesExistPrimaryKey = false;
        int counterOfForeignKeys = 0;
        List<FieldPropertiesInDatabase> fieldsForDatabase = getFieldsPropertiesNecessaryForTableInDb(foundClass);
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append("(");
        for (FieldPropertiesInDatabase field : fieldsForDatabase) {
            StringBuilder foreignKey = new StringBuilder("FOREIGN KEY(");
            if (field.isPrimaryKey()) {
                primaryKey.append(field.getName()).append(")");
                builder.append(getRowWithAutoIncrementForPrimaryKey(field));
                doesExistPrimaryKey = true;
                continue;
            }
            switch (field.getRelationshipType()) {
                case ONE_TO_ONE:
                case MANY_TO_ONE:
                    builder.append(getRowForUsualField(field));
                    foreignKey.append(getRowForForeignKeyOneToOne(field, counterOfForeignKeys));
                    counterOfForeignKeys++;
                    foreignkeys.add("ALTER TABLE " + tableName + " ADD " + foreignKey + ";");
                    break;
                case ONE_TO_MANY:
                    foreignKey.append(getRowForForeignKeyOneToOne(field, counterOfForeignKeys));
                    counterOfForeignKeys++;
                    foreignkeys.add("ALTER TABLE " + tableName + " ADD " + foreignKey + ";");

                    break;
                case MANY_TO_MANY:
                    foreignkeys.add("ALTER TABLE " + tableName + " ADD " + foreignKey + ";");
                    break;
                default:
                    builder.append(getRowForUsualField(field));
                    break;
            }
        }
        builder = doesExistPrimaryKey ? builder.append(primaryKey) : builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
        System.out.println(builder.toString());
        return builder.toString();
    }

    private static StringBuilder getRowForForeignKeyOneToOne(FieldPropertiesInDatabase field, int counterOfForeignKeys) {
        StringBuilder foreignKey = new StringBuilder();
        if (counterOfForeignKeys > 0) {
            foreignKey.append(", FOREIGN KEY(");
        }
        foreignKey.append(field.getName()).append(") REFERENCES ")
                .append(getTableName(field.getForeignEntity())).append("(id)");
        return foreignKey;
    }

    private static StringBuilder getRowForUsualField(FieldPropertiesInDatabase field) {
        StringBuilder builder = new StringBuilder();
        String type = getTypeForField(field);
        builder.append(field.getName())
                .append(" ").append(type).append(field.getType().equals("VARCHAR") ? "(" + field.getLength() + ")" : "")
                .append(field.isPrimaryKey() ? " AUTO_INCREMENT " : " ")
                .append(!field.isNullable() ? "NOT NULL " : "")
                .append(field.isUnique() ? "UNIQUE," : ",");
        return builder;
    }

    private static StringBuilder getRowWithAutoIncrementForPrimaryKey(FieldPropertiesInDatabase field) {
        StringBuilder builder = new StringBuilder();
        String type = getTypeForField(field);
        builder.append(field.getName())
                .append(" ").append(type).append(field.getType().equals("VARCHAR") ? "(" + field.getLength() + ")" : "")
                .append(field.isPrimaryKey() && !field.getType().equals("VARCHAR") ? " AUTO_INCREMENT, " : " ");
        return builder;
    }

    private static String getTableName(Class<?> c) {
        String tableName = "";
        Annotation[] annotations = c.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Table) {
                tableName = ((Table) annotation).name();
                break;
            }
        }
        return tableName;
    }

    private static String replaceSymbolsInPathForReflect(String fullPathWhereAreEntity) {
        fullPathWhereAreEntity = fullPathWhereAreEntity.replaceAll("[/\\\\]", ".");
        for (int i = 0; i < 3; i++) {
            int indexFirstDot = fullPathWhereAreEntity.indexOf(".");
            fullPathWhereAreEntity = fullPathWhereAreEntity.substring(indexFirstDot + 1);
        }
        return fullPathWhereAreEntity;
    }

    private static List<FieldPropertiesInDatabase> getFieldsPropertiesNecessaryForTableInDb(Class<?> c) throws NoSuchFieldException {
        List<FieldPropertiesInDatabase> fieldsInDb = new ArrayList<>();
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation currentAnnotation : annotations) {
                FieldPropertiesInDatabase fieldInDatabase = getRelevantFieldDependsInAnnotation(field, currentAnnotation);
                fieldsInDb.add(fieldInDatabase);
            }
        }
        return fieldsInDb;
    }

    private static FieldPropertiesInDatabase getRelevantFieldDependsInAnnotation(Field field, Annotation currentAnnotation) throws NoSuchFieldException {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        if (currentAnnotation instanceof Id) {
            fieldInDatabase.setName(((Id) currentAnnotation).name());
            fieldInDatabase.setPrimaryKey(true);
            fieldInDatabase.setType(field.getType().toString().endsWith("String") ? "VARCHAR" : field.getType().toString());
        }
        if (currentAnnotation instanceof Column) {
            fieldInDatabase.setName(((Column) currentAnnotation).name());
            fieldInDatabase.setType(field.getType().toString().endsWith("String") ? "VARCHAR" : field.getType().toString());
            fieldInDatabase.setUnique(((Column) currentAnnotation).unique());
            fieldInDatabase.setNullable(((Column) currentAnnotation).nullable());
            fieldInDatabase.setLength(((Column) currentAnnotation).length());
        }
        if (currentAnnotation instanceof OneToOne) {
            fieldInDatabase.setName(field.getName() + "_id");
            fieldInDatabase.setForeignKey(true);
            fieldInDatabase.setNullable(((OneToOne) currentAnnotation).doesExistWithoutOtherEntity());
            fieldInDatabase.setUnique(true);
            fieldInDatabase.setRelationshipType(RelationshipType.ONE_TO_ONE);
            fieldInDatabase.setForeignEntity(((OneToOne) currentAnnotation).targetEntity());
            fieldInDatabase.setType(((OneToOne) currentAnnotation).targetEntity().getDeclaredField("id").getType().toString());
        }
        if (currentAnnotation instanceof ManyToOne) {
            fieldInDatabase.setName(field.getName() + "_id");
            fieldInDatabase.setForeignKey(true);
            fieldInDatabase.setNullable(((ManyToOne) currentAnnotation).doesExistWithoutOtherEntity());
            fieldInDatabase.setUnique(true);
            fieldInDatabase.setRelationshipType(RelationshipType.MANY_TO_ONE);
            fieldInDatabase.setForeignEntity(((ManyToOne) currentAnnotation).targetEntity());
            String typeOtherEntity = !((ManyToOne) currentAnnotation).nameOfPrimaryKeyOtherEntity().equals("") ?
                    ((ManyToOne) currentAnnotation).targetEntity().getDeclaredField(((ManyToOne) currentAnnotation).nameOfPrimaryKeyOtherEntity()).getType().toString() :
                    ((ManyToOne) currentAnnotation).targetEntity().getDeclaredField("id").getType().toString();
            fieldInDatabase.setType(typeOtherEntity);
        }
        if (currentAnnotation instanceof OneToMany) {
            fieldInDatabase.setName("id");
            fieldInDatabase.setForeignKey(true);
            fieldInDatabase.setNullable(((OneToMany) currentAnnotation).doesExistWithoutOtherEntity());
            fieldInDatabase.setRelationshipType(RelationshipType.ONE_TO_MANY);
            fieldInDatabase.setForeignEntity(((OneToMany) currentAnnotation).targetEntity());
            String typeOtherEntity = !((OneToMany) currentAnnotation).nameOfPrimaryKeyOtherEntity().equals("") ?
                    ((OneToMany) currentAnnotation).targetEntity().getDeclaredField(((OneToMany) currentAnnotation).nameOfPrimaryKeyOtherEntity()).getType().toString() :
                    ((OneToMany) currentAnnotation).targetEntity().getDeclaredField("id").getType().toString();
            fieldInDatabase.setType(typeOtherEntity);
        }
        return fieldInDatabase;
    }

    private static String getTypeForField(FieldPropertiesInDatabase field) {
        String typeInDb = field.getType();
        if (typeInDb.endsWith("String")) {
            typeInDb = "VARCHAR";
        } else if (typeInDb.endsWith("long")) {
            typeInDb = "BIGINT";
        } else if (typeInDb.endsWith("LocalDate")) {
            typeInDb = "DATE";
        }
        return typeInDb;
    }


    private static void getInfoFromProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("src/main/resources/db.properties")));
        pathToEntities = properties.get("pathToEntities").toString();
    }
}
