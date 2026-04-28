import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            DBConnection.connect();
        } catch (SQLException e) {
            System.out.println("Could not connect to the database.");
            System.exit(0);
        }
        new LoginFrame();
    }
}