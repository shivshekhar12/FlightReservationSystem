# Flight Reservation System — Project Context

## Project Overview
A Java Swing + MySQL flight reservation desktop application for a database course project.
Three user roles: **Customer**, **Customer Representative**, and **Admin**.

## Tech Stack
- **Language**: Java (no build tool — plain `.java` files compiled with `javac`)
- **UI**: Java Swing
- **Database**: MySQL 8.x, database name `flight_reservation`
- **Connectivity**: JDBC (`DriverManager`)
- **IDE**: IntelliJ IDEA

## File Structure
```
ProjectFrame.java     # Original login prototype (legacy, not used in final app)
LoginFrame.java       # Main entry point — login + customer self-registration
AdminFrame.java       # Admin dashboard (6 tabs)
RepFrame.java         # Customer Rep dashboard (8 tabs)
CustomerFrame.java    # Customer dashboard (4 tabs)
DBConnection.java     # Static DB connection singleton (uses Config.java)
Config.java           # DB credentials — DO NOT COMMIT (gitignored)
ConfigExample.java    # Safe template to share credentials format
schema.sql            # Full DDL + seed data
```

## Database Schema Summary
Key tables and their primary keys:
- `Airline(airline_id CHAR(2))`
- `Airport(airport_id CHAR(3))`
- `Aircraft(aircraft_id INT, airline_id)`
- `Flight(flight_no, airline_id)` — composite PK
- `Flight_Day(flight_no, airline_id, day_of_week)` — operating schedule
- `Customer(cust_id AUTO_INCREMENT, username UNIQUE)`
- `Employee(emp_id AUTO_INCREMENT, role ENUM('admin','customer_rep'), username UNIQUE)`
- `Ticket(ticket_no AUTO_INCREMENT, cust_id, handled_by, status ENUM('active','cancelled','completed'))`
- `Ticket_Flight(ticket_no, leg_order)` — composite PK, links tickets to specific flight legs
- `Waitlist(cust_id, flight_no, airline_id, dep_date)` — composite PK
- `Customer_Question(question_id AUTO_INCREMENT, cust_id, rep_id)`

## How the App Starts
`LoginFrame` is the entry point. It calls `DBConnection.connect()` then opens the correct
dashboard based on the login role:
- Employee with role `admin` → `AdminFrame(empId, name)`
- Employee with role `customer_rep` → `RepFrame(empId, name)`
- Customer → `CustomerFrame(custId, name)`

Default admin credentials (seeded in schema.sql): `admin` / `admin123`

## DB Connection Pattern
All DB access goes through the static `DBConnection.getStatement()`.
Connection is opened once at startup and reused throughout the session.
Credentials live in `Config.java` (excluded from version control).

## Known Issues / Limitations to Be Aware Of
- **SQL injection**: Queries use raw string concatenation — acceptable for this course project
- `DBConnection` uses a single shared `Statement` object; **do not open two ResultSets simultaneously** on different queries or one will close the other
- `ProjectFrame.java` is a leftover prototype — ignore it
- `Config.java` contains real credentials — treat as sensitive

## Project Checklist (what still needs work)
Reference `ProjectChecklistSp261.pdf` for full rubric. Key gaps to check:
- [ ] Flight search: "number of stops" filter is a placeholder (`1=1`)
- [ ] Economy ticket cancellation requires a fee — currently just blocked with a message
- [ ] Waitlist notification logic exists in `doCancelReservation` but only marks `notified=1`; no actual email
- [ ] Admin tab is missing: "Produce a list of all flights for a given airport" (this exists in RepFrame but not AdminFrame)
- [ ] Round-trip booking uses the same flight number for both legs (outbound and return should differ)

## Coding Conventions in This Codebase
- Each frame is a self-contained `JFrame` subclass with a `buildUI()` method
- Tab panels are built in private `buildXxxTab()` methods returning `JPanel`
- Table data uses `DefaultTableModel` with `isCellEditable` returning false
- Status messages use a `JLabel lblMsg` at the bottom of each panel
- SQL strings are built inline with string concatenation (not PreparedStatements)
- Font: `new Font("Lucida Sans", Font.PLAIN, 14)` / `Font.BOLD, 15`

## How to Run
1. Start MySQL and create the DB: `mysql -u root -p < schema.sql`
2. Fill in `Config.java` with your credentials (copy from `ConfigExample.java`)
3. Add MySQL JDBC driver JAR to the IntelliJ project (File → Project Structure → Libraries)
4. Run `LoginFrame.java` as the main class

## What to Ask Claude Code to Help With
- Fixing specific bugs (describe the tab and button)
- Adding missing checklist features (reference the feature by name above)
- Refactoring queries to use `PreparedStatement` (optional improvement)
- Adding input validation before SQL execution
- Fixing the round-trip return leg booking logic
