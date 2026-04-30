import java.sql.SQLException;

public class Main{
    public static void main(String[] args) {
        try {
            DBConnection.connect();
        } 
        catch (SQLException e) {
            System.out.println("Can't connect to database");
        }
        new LoginFrame();
    }
}