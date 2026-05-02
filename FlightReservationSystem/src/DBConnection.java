import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = Config.DB_URL;
    private static final String DB_USER = Config.DB_USER;
    private static final String DB_PASS = Config.DB_PASS;

    private static Connection connection = null;
    private static Statement statement = null;

    public static void connect() throws SQLException{
        connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
        statement = connection.createStatement();
    }
    
    public static Statement getStatement() {
        return statement;
    }

    public static void close() {
        try {
            if (connection != null){
                connection.close();
            }
        } 
        catch (SQLException e) {
        }
    }
}



