import java.awt.*;
import javax.swing.*;

public class RepFrame extends JFrame {

    private final int empId;
    private final String empName;

    public RepFrame(int empId, String empName) {
        this.empId   = empId;
        this.empName = empName;
        buildUI();
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Book for Customer",  BookForCustomerTab.build(this, empId));
        tabs.addTab("Edit Reservations",  EditReservationsTab.build(this));
        tabs.addTab("Manage Flights",     ManageFlightsTab.build(this));
        tabs.addTab("Manage Airports",    ManageAirportsTab.build(this));
        tabs.addTab("Manage Aircraft",    ManageAircraftTab.build(this));
        tabs.addTab("Waitlist",           WaitlistTab.build());
        tabs.addTab("Flights by Airport", FlightsByAirportTab.build());
        tabs.addTab("Answer Questions",   AnswerQuestionsTab.build(this, empId));

        this.add(tabs);
        this.setTitle("Representative Dashboard  —  " + empName);
        this.setSize(950, 620);
        this.setMinimumSize(new Dimension(750, 500));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}
