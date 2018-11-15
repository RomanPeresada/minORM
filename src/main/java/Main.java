import config.TablesInORM;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
        //ConnectionWithDb.getConnection().close();
        TablesInORM.autoCreatingTablesAfterStartOfProgram();
    }
}
