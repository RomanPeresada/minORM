package config;


import annotation.*;
import config.enumiration.RelationshipType;
import config.model.FieldPropertiesInDatabase;
import config.model.TableForManyToMany;
//import org.apache.log4j.Logger;

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
import static util.ORMUtil.getTableNameByClass;

public class TablesInORM {
    private static String pathToEntities = null;
    private static List<String> foreignkeys = new ArrayList<>();
    private static List<TableForManyToMany> tablesForManyToMany = new ArrayList<>();
    // private final static Logger log = Logger.getLogger(TablesInORM.class);


    public static void autoCreatingTablesAfterStartOfProgram() throws ClassNotFoundException, SQLException, IOException, NoSuchFieldException {
        getInfoFromProperties();
        String[] classesName = new File(pathToEntities).list();
        pathToEntities = replaceSymbolsInPathForReflect(pathToEntities);
        if (classesName != null) {
            //   log.debug("Classes were found");
            for (String name : classesName) {
                Class foundClass = Class.forName(pathToEntities + name.replaceAll(".java", ""));
                String query = getQueryForCreateTable(foundClass);
                try (Statement statement = getConnection().createStatement()) {
                    statement.executeUpdate(query);
                    // log.debug("query was completed : " + query);
                }
            }
            for (TableForManyToMany table : tablesForManyToMany) {
                try (Statement statement = getConnection().createStatement()) {
                    String queue = turnTableForManyToManyInString(table);
                    System.out.println(queue);
                    statement.executeUpdate(queue);
                }
                for (String foreignkey : foreignkeys) {
                    try (Statement statement = getConnection().createStatement()) {
                        System.out.println(foreignkey);
                        statement.executeUpdate(foreignkey);
                        //   log.debug("query was completed : " + foreignkey);
                    }
                }
            }
        }
    }

    private static String turnTableForManyToManyInString(TableForManyToMany table) {
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(table.getName())
                .append("(").append(table.getColumn()).append(" ").append(table.getColumnType()).append(",")
                .append(table.getInverseColumn()).append(" ").append(table.getInverseColumnType()).append(", PRIMARY KEY(")
                .append(table.getColumn()).append(",").append(table.getInverseColumn()).append("), ")
                .append("FOREIGN KEY(").append(table.getColumn()).append(") REFERENCES ").append(table.getTableNameOfColumn()).append("(id),")
                .append("FOREIGN KEY(").append(table.getInverseColumn()).append(") REFERENCES ").append(table.getTableNameOfInverseColumn()).append("(id));");
        return builder.toString();
    }

    private static String getQueryForCreateTable(Class foundClass) throws NoSuchFieldException {
        String tableName = getTableNameByClass(foundClass);
        StringBuilder primaryKey = new StringBuilder(" PRIMARY KEY(");

        boolean doesExistPrimaryKey = false;
        List<FieldPropertiesInDatabase> fieldsForDatabase = getFieldsPropertiesNecessaryForTableInDb(foundClass);
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append("(");
        for (FieldPropertiesInDatabase field : fieldsForDatabase) {
            StringBuilder foreignKey = new StringBuilder("FOREIGN KEY(");
            if (field.isPrimaryKey()) {
                primaryKey.append(field.getName()).append(")");
                builder.append(getRowForColumnPrimaryKeyOrId(field));
                doesExistPrimaryKey = true;
                continue;
            }
            switch (field.getRelationshipType()) {
                case ONE_TO_ONE:
                case MANY_TO_ONE:
                    builder.append(getRowForUsualField(field));
                    foreignKey.append(getForeignKeyOneToOneOrManyToOne(field));
                    foreignkeys.add("ALTER TABLE " + tableName + " ADD " + foreignKey + ";");
                    break;
                case ONE_TO_ONE_MAPPED:
                case ONE_TO_MANY:
                   // foreignKey.append(getForeignKeyOneToOneMapped(field));
                   // foreignkeys.add("ALTER TABLE " + tableName + " ADD " + foreignKey + ";");
                    break;
                case MANY_TO_MANY:
                   // foreignKey.append(getForeignKeyManyToMany(field));
                   // foreignkeys.add("ALTER TABLE " + tableName + " ADD " + foreignKey + ";");
                    break;
                default:
                    builder.append(getRowForUsualField(field));
                    break;
            }
        }
        primaryKey = replaceBracketsOfComma(primaryKey);
        builder = doesExistPrimaryKey ? builder.append(primaryKey) : builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
        System.out.println(builder.toString());
        return builder.toString();
    }


    private static FieldPropertiesInDatabase getFieldDependsOfAnnotation(Field field, Annotation currentAnnotation) throws NoSuchFieldException {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        if (currentAnnotation instanceof Id) {
            fieldInDatabase = getFieldPropertiesForAnnotationId(currentAnnotation, field);
        } else if (currentAnnotation instanceof PrimaryKey) {
            fieldInDatabase = getFieldPropertiesForAnnotationPrimaryKey(currentAnnotation, field);
        } else if (currentAnnotation instanceof Column) {
            fieldInDatabase = getFieldPropertiesForAnnotationColumn(currentAnnotation, field);
        }
        if (currentAnnotation instanceof OneToOne) {
            fieldInDatabase = getFieldPropertiesForAnnotationOneToOne(currentAnnotation, field);
        } else if (currentAnnotation instanceof ManyToOne) {
            fieldInDatabase = getFieldPropertiesForAnnotationManyToOne(currentAnnotation, field);
        } else if (currentAnnotation instanceof OneToMany) {
            fieldInDatabase = getFieldPropertiesForAnnotationOneToMany(currentAnnotation, field);
        } else if (currentAnnotation instanceof ManyToMany) {
            fieldInDatabase = getFieldPropertiesForAnnotationManyToMany(currentAnnotation, field);
        }
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationId(Annotation currentAnnotation, Field field) {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        fieldInDatabase.setName(((Id) currentAnnotation).name());
        fieldInDatabase.setPrimaryKey(true);
        fieldInDatabase.setId(true);
        fieldInDatabase.setType(field.getType().toString());
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationPrimaryKey(Annotation currentAnnotation, Field field) {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        fieldInDatabase.setName(((PrimaryKey) currentAnnotation).name());
        fieldInDatabase.setLength(((PrimaryKey) currentAnnotation).length());
        fieldInDatabase.setType(getTypeForField(field.getType().toString()));
        fieldInDatabase.setPrimaryKey(true);
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationColumn(Annotation currentAnnotation, Field field) {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        fieldInDatabase.setName(((Column) currentAnnotation).name());
        fieldInDatabase.setType(getTypeForField(field.getType().toString()));
        fieldInDatabase.setUnique(((Column) currentAnnotation).unique());
        fieldInDatabase.setNullable(((Column) currentAnnotation).nullable());
        fieldInDatabase.setLength(((Column) currentAnnotation).length());
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationOneToOne(Annotation currentAnnotation, Field field) throws NoSuchFieldException {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        if (!((OneToOne) currentAnnotation).mappedBy().isEmpty()) {
            Field fieldOfOtherEntity = ((OneToOne) currentAnnotation).targetEntity().getDeclaredField(((OneToOne) currentAnnotation).mappedBy());
            fieldInDatabase.setMappedBy(fieldOfOtherEntity.getName() + "_id");
            fieldInDatabase.setForeignEntity(((OneToOne) currentAnnotation).targetEntity());
            fieldInDatabase.setRelationshipType(RelationshipType.ONE_TO_ONE_MAPPED);
        } else {
            fieldInDatabase.setName(field.getName() + "_id");
            fieldInDatabase.setForeignKey(true);
            fieldInDatabase.setNullable(((OneToOne) currentAnnotation).doesExistWithoutOtherEntity());
            fieldInDatabase.setUnique(true);
            fieldInDatabase.setRelationshipType(RelationshipType.ONE_TO_ONE);
            fieldInDatabase.setForeignEntity(((OneToOne) currentAnnotation).targetEntity());
            fieldInDatabase.setType(((OneToOne) currentAnnotation).targetEntity().getDeclaredField("id").getType().toString());
        }
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationManyToOne(Annotation currentAnnotation, Field field) throws NoSuchFieldException {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        fieldInDatabase.setName(field.getName() + "_id");
        fieldInDatabase.setForeignKey(true);
        fieldInDatabase.setNullable(((ManyToOne) currentAnnotation).doesExistWithoutOtherEntity());
        fieldInDatabase.setRelationshipType(RelationshipType.MANY_TO_ONE);
        fieldInDatabase.setForeignEntity(((ManyToOne) currentAnnotation).targetEntity());
        fieldInDatabase.setType(((ManyToOne) currentAnnotation).targetEntity().getDeclaredField("id").getType().toString());
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationOneToMany(Annotation currentAnnotation, Field field) throws NoSuchFieldException {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        Field fieldOfOtherEntity = ((OneToMany) currentAnnotation).targetEntity().getDeclaredField(((OneToMany) currentAnnotation).mappedBy());
        fieldInDatabase.setMappedBy(fieldOfOtherEntity.getName() + "_id");
        fieldInDatabase.setForeignEntity(((OneToMany) currentAnnotation).targetEntity());
        fieldInDatabase.setRelationshipType(RelationshipType.ONE_TO_MANY);
        return fieldInDatabase;
    }

    private static FieldPropertiesInDatabase getFieldPropertiesForAnnotationManyToMany(Annotation currentAnnotation, Field field) throws NoSuchFieldException {
        FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
        fieldInDatabase.setForeignKey(true);
        fieldInDatabase.setRelationshipType(RelationshipType.MANY_TO_MANY);
        if (!((ManyToMany) currentAnnotation).mappedBy().isEmpty()) {
            Field fieldOfOtherEntity = ((ManyToMany) currentAnnotation).targetEntity().getDeclaredField(((ManyToMany) currentAnnotation).mappedBy());
            fieldInDatabase.setColumnInManyToMany(fieldOfOtherEntity.getAnnotation(JoinTable.class).inverseJoinColumn());
            fieldInDatabase.setTableInManyToMany(fieldOfOtherEntity.getAnnotation(JoinTable.class).name());
            fieldInDatabase.setMappedBy(fieldOfOtherEntity.getName());
            fieldInDatabase.setForeignEntity(((ManyToMany) currentAnnotation).targetEntity());
        } else {
            fieldInDatabase.setColumnInManyToMany(field.getAnnotation(JoinTable.class).joinColumn());
            fieldInDatabase.setTableInManyToMany(field.getAnnotation(JoinTable.class).name());
            fieldInDatabase.setForeignEntity(((ManyToMany) currentAnnotation).targetEntity());

            TableForManyToMany tableForManyToMany = new TableForManyToMany();
            tableForManyToMany.setName(field.getAnnotation(JoinTable.class).name());
            tableForManyToMany.setColumn(field.getAnnotation(JoinTable.class).joinColumn());
            tableForManyToMany.setTableNameOfColumn(getTableNameByClass(field.getDeclaringClass()));
            tableForManyToMany.setColumnType(getTypeForField(field.getDeclaringClass().getDeclaredField("id").toString()));
            tableForManyToMany.setInverseColumn(field.getAnnotation(JoinTable.class).inverseJoinColumn());
            tableForManyToMany.setTableNameOfInverseColumn(getTableNameByClass(((ManyToMany) currentAnnotation).targetEntity()));
            tableForManyToMany.setInverseColumnType(getTypeForField(((ManyToMany) currentAnnotation).targetEntity().getDeclaredField("id").toString()));
            tablesForManyToMany.add(tableForManyToMany);
        }
        return fieldInDatabase;
    }

    private static List<FieldPropertiesInDatabase> getFieldsPropertiesNecessaryForTableInDb(Class<?> c) throws
            NoSuchFieldException {
        List<FieldPropertiesInDatabase> fieldsInDb = new ArrayList<>();
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation currentAnnotation : annotations) {
                if (!(currentAnnotation instanceof JoinTable)) {
                    FieldPropertiesInDatabase fieldInDatabase = getFieldDependsOfAnnotation(field, currentAnnotation);
                    fieldsInDb.add(fieldInDatabase);
                }
            }
        }
        return fieldsInDb;
    }

    private static StringBuilder getForeignKeyOneToOneOrManyToOne(FieldPropertiesInDatabase field) {
        StringBuilder foreignKey = new StringBuilder();
        foreignKey.append(field.getName()).append(") REFERENCES ")
                .append(getTableNameByClass(field.getForeignEntity())).append("(id)");
        return foreignKey;
    }

    private static StringBuilder getForeignKeyManyToMany(FieldPropertiesInDatabase field) {
        StringBuilder foreignKey = new StringBuilder();
        foreignKey.append("id) REFERENCES ")
                .append(field.getTableInManyToMany()).append("(").append(field.getColumnInManyToMany()).append(") ON DELETE CASCADE");
        return foreignKey;
    }

    private static StringBuilder getForeignKeyOneToOneMapped(FieldPropertiesInDatabase field) {
        StringBuilder foreignKey = new StringBuilder();
        foreignKey.append("id) REFERENCES ")
                .append(getTableNameByClass(field.getForeignEntity())).append("(").append(field.getMappedBy()).append(")");
        return foreignKey;
    }

    private static StringBuilder getRowForUsualField(FieldPropertiesInDatabase field) {
        StringBuilder builder = new StringBuilder();
        String type = getTypeForField(field.getType());
        builder.append(field.getName())
                .append(" ").append(type).append(field.getType().equals("VARCHAR") ? "(" + field.getLength() + ")" : "")
                .append(field.isId() ? " AUTO_INCREMENT " : " ")
                .append(!field.isNullable() ? "NOT NULL " : "")
                .append(field.isUnique() ? "UNIQUE," : ",");
        return builder;
    }

    private static StringBuilder getRowForColumnPrimaryKeyOrId(FieldPropertiesInDatabase field) {
        StringBuilder builder = new StringBuilder();
        String type = getTypeForField(field.getType());
        builder.append(field.getName())
                .append(" ").append(type).append(field.getType().equals("VARCHAR") ? "(" + field.getLength() + ")" : "")
                .append(field.isPrimaryKey() && field.isId() ? " AUTO_INCREMENT, " : ", ");
        return builder;
    }

    private static String replaceSymbolsInPathForReflect(String fullPathWhereAreEntity) {
        fullPathWhereAreEntity = fullPathWhereAreEntity.replaceAll("[/\\\\]", ".");
        for (int i = 0; i < 3; i++) {
            int indexFirstDot = fullPathWhereAreEntity.indexOf(".");
            fullPathWhereAreEntity = fullPathWhereAreEntity.substring(indexFirstDot + 1);
        }
        return fullPathWhereAreEntity;
    }


    private static String getTypeForField(String objectType) {
        if (objectType.endsWith("String")) {
            objectType = "VARCHAR";
        } else if (objectType.toLowerCase().contains("long")) {
            objectType = "BIGINT";
        } else if (objectType.endsWith("LocalDate")) {
            objectType = "DATE";
        }else if (objectType.endsWith("Integer")) {
            objectType = "INT";
        }
        return objectType;
    }

    private static StringBuilder replaceBracketsOfComma(StringBuilder builder) {
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < builder.toString().lastIndexOf(")"); i++) {
            resultBuilder.append(builder.charAt(i) == ')' ? "," : builder.charAt(i));
        }
        return resultBuilder.append(")");
    }

    private static void getInfoFromProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("src/main/resources/db.properties")));
        pathToEntities = properties.get("pathToEntities").toString();
    }
}
