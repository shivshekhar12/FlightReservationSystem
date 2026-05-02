import java.awt.*;
import javax.swing.*;

public class AdminFrame extends JFrame {

    private int empId;
    private String empName;

    public AdminFrame(int empId, String empName) {
        this.empId = empId;
        this.empName = empName;
        buildUI();
    }

    private void buildUI() {
        JTabbedPane adminTabs = new JTabbedPane();
        adminTabs.addTab("Manage Users", ManageUsersTab.build(this));
        adminTabs.addTab("Sales Report", SalesReportTab.build());
        adminTabs.addTab("Reservations List", ReservationsListTab.build());
        adminTabs.addTab("Revenue Summary", RevenueSummaryTab.build());
        adminTabs.addTab("Top Customers", TopCustomersTab.build());
        adminTabs.addTab("Active Flights", ActiveFlightsTab.build());
        adminTabs.addTab("Flights by Airport", FlightsByAirportTab.build());
        this.add(adminTabs);
        this.setTitle("Admin Dashboard   " + empName);
        this.setSize(950, 620);
        this.setMinimumSize(new Dimension(750, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}
