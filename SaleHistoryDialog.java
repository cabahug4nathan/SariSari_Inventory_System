import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class SalesHistoryDialog extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(SalesHistoryDialog.class.getName());

    public SalesHistoryDialog(JFrame owner) {
        super(owner, "Sales History", true);
        setSize(700, 400);
        setLocationRelativeTo(owner);

        String[] cols = {"Sale ID", "Product", "Qty", "Total", "Time"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadSalesHistory(model);
        setVisible(true);
    }

    private void loadSalesHistory(DefaultTableModel model) {
        model.setRowCount(0); // clear existing rows

        String sql = "SELECT sales.id AS sid, " +
                "COALESCE(products.name, sales.product_name) AS name, " +
                "sales.quantity AS qty, sales.total AS tot, sales.timestamp AS ts " +
                "FROM sales " +
                "LEFT JOIN products ON sales.product_id = products.id " +
                "ORDER BY sales.id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("sid"),
                        rs.getString("name"),
                        rs.getInt("qty"),
                        rs.getDouble("tot"),
                        rs.getString("ts")
                });
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Unable to fetch sales history", ex);
            JOptionPane.showMessageDialog(this, "Unable to fetch sales history: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

