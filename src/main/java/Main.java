import config.TablesInORM;
import domain.Person;
import service.EntityManager;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        //ConnectionWithDb.getConnection().close();
        TablesInORM.autoCreatingTablesAfterStartOfProgram();
        Person person = new Person(1L, "login", "name", "pass", null);
        EntityManager entityManager = new EntityManager();
        entityManager.createObject(person);
    }
}
