import java.sql.*;
import java.util.Scanner;

public class JDBCTrainReservation {

    // Database connection details
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";  // Oracle DB URL
    private static final String USER = "SYSTEM";  // Replace with your DB username
    private static final String PASSWORD = "1505"; // Replace with your DB password

    public static void main(String[] args) {
        try {
            // Load JDBC Driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            
            // Check if connection is successful
            if (conn != null) {
                System.out.println("✅ Connected to the database successfully!");
            } else {
                System.out.println("❌ Failed to connect to the database.");
                return;
            }

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\n--- Train Reservation System using JDBC ---");
                System.out.println("1. View All Trains");
                System.out.println("2. Book Ticket");
                System.out.println("3. Cancel Ticket");
                System.out.println("4. View My Tickets");
                System.out.println("5. Exit");
                System.out.print("Enter choice: ");
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        viewTrains(conn);
                        break;
                    case 2:
                        bookTicket(conn, scanner);
                        break;
                    case 3:
                        cancelTicket(conn, scanner);
                        break;
                    case 4:
                        viewTickets(conn, scanner);
                        break;
                    case 5:
                        conn.close();
                        System.out.println("Thank you for using the Train Reservation System!");
                        return;
                    default:
                        System.out.println("Invalid choice! Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to display all available trains
    private static void viewTrains(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM trains")) {

            boolean found = false;
            System.out.println("\nAvailable Trains:");
            while (rs.next()) {
                int trainNo = rs.getInt("TRAINNO");
                String trainName = rs.getString("TRAINNAME");
                String source = rs.getString("SOURCE");
                String destination = rs.getString("DESTINATION");
                int availableSeats = rs.getInt("AVAILABLESEATS");
                double price = rs.getDouble("PRICE");

                System.out.println(trainNo + " - " + trainName + " | From: " + source + " To: " + destination +
                        " | Seats: " + availableSeats + " | Price: ₹" + price);
                found = true;
            }

            if (!found) {
                System.out.println("No trains available in the database.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to book a ticket
    private static void bookTicket(Connection conn, Scanner scanner) {
        try {
            System.out.print("\nEnter Your Name: ");
            scanner.nextLine(); // Consume newline
            String passengerName = scanner.nextLine();

            viewTrains(conn);
            System.out.print("Enter Train Number to Book: ");
            int trainNo = scanner.nextInt();

            String query = "SELECT AVAILABLESEATS, PRICE FROM trains WHERE TRAINNO = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, trainNo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int availableSeats = rs.getInt("AVAILABLESEATS");
                double price = rs.getDouble("PRICE");

                if (availableSeats > 0) {
                    int seatNumber = 100 - availableSeats + 1;

                    // Update seat availability
                    String updateQuery = "UPDATE trains SET AVAILABLESEATS = AVAILABLESEATS - 1 WHERE TRAINNO = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setInt(1, trainNo);
                    updateStmt.executeUpdate();

                    // Insert ticket booking (No TICKETID since Oracle auto-generates it)
                    String insertQuery = "INSERT INTO tickets (PASSENGERNAME, TRAINNO, SEATNUMBER, AMOUNTPAID) VALUES (?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                    insertStmt.setString(1, passengerName);
                    insertStmt.setInt(2, trainNo);
                    insertStmt.setInt(3, seatNumber);
                    insertStmt.setDouble(4, price);
                    insertStmt.executeUpdate();

                    System.out.println("🎟 Booking Successful! Seat Number: " + seatNumber + " | Price: ₹" + price);
                    
                    // Commit the transaction
                    conn.commit();
                } else {
                    System.out.println("🚫 No available seats on this train.");
                }
            } else {
                System.out.println("🚆 Train not found!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to cancel a ticket
    private static void cancelTicket(Connection conn, Scanner scanner) {
        try {
            System.out.print("\nEnter Your Name: ");
            scanner.nextLine();
            String passengerName = scanner.nextLine();

            String query = "DELETE FROM tickets WHERE PASSENGERNAME = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, passengerName);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("❌ Ticket canceled successfully!");
            } else {
                System.out.println("⚠ No ticket found under this name.");
            }

            // Commit the transaction
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to view booked tickets
    private static void viewTickets(Connection conn, Scanner scanner) {
        try {
            System.out.print("\nEnter Your Name: ");
            scanner.nextLine();
            String passengerName = scanner.nextLine();

            String query = "SELECT * FROM tickets WHERE PASSENGERNAME = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, passengerName);
            ResultSet rs = stmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                int trainNo = rs.getInt("TRAINNO");
                int seatNumber = rs.getInt("SEATNUMBER");
                double amountPaid = rs.getDouble("AMOUNTPAID");

                System.out.println("🎟 Train No: " + trainNo + " | Seat No: " + seatNumber + " | Amount Paid: ₹" + amountPaid);
                found = true;
            }

            if (!found) {
                System.out.println("⚠ No tickets found under this name.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
