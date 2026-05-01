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
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Users", ManageUsersTab.build(this));
        tabs.addTab("Sales Report", SalesReportTab.build());
        tabs.addTab("Reservations List", ReservationsListTab.build());
        tabs.addTab("Revenue Summary", RevenueSummaryTab.build());
        tabs.addTab("Top Customers", TopCustomersTab.build());
        tabs.addTab("Active Flights", ActiveFlightsTab.build());
        tabs.addTab("Flights by Airport", FlightsByAirportTab.build());
        this.add(tabs);
        this.setTitle("Admin Dashboard  —  " + empName);
        this.setSize(950, 620);
        this.setMinimumSize(new Dimension(750, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}
