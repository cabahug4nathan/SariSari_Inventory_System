import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SariSariInventorySystem {

    static final Logger LOGGER =
            Logger.getLogger(SariSariInventorySystem.class.getName());

    public static void main(String[] args) {
        // ensure DB exists and tables created
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}

/* ------------------ Login / Signup ------------------ */
class LoginFrame extends JFrame {
    public LoginFrame() {
        setTitle("Sari-Sari Store Inventory System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 220);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));

        JLabel title = new JLabel("SARI-SARI STORE INVENTORY SYSTEM", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3,2,8,8));
        center.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        center.add(new JLabel("Username: "));
        center.add(userField);
        center.add(new JLabel("Password: "));
        center.add(passField);

        JButton loginBtn = new JButton("LOG IN");
        JButton signupBtn = new JButton("SIGN UP");

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        bottom.add(loginBtn);
        bottom.add(signupBtn);

        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username and password.");
                return;
            }
            if (authenticate(username, password)) {
                dispose();
                new DashboardFrame(username);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.");
            }
        });

        signupBtn.addActionListener(e -> {
            dispose();
            new SignupFrame();
        });

        setVisible(true);
    }

    private boolean authenticate(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, password);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException ex) {
            SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Login failed", ex);
            JOptionPane.showMessageDialog(this, "Database error occurred.");
            return false;
        }
    }
}

class SignupFrame extends JFrame {
    public SignupFrame() {
        setTitle("Create Account");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 260);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel center = new JPanel(new GridLayout(4,2,8,8));
        center.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));

        JTextField user = new JTextField();
        JTextField email = new JTextField();
        JPasswordField pass = new JPasswordField();

        center.add(new JLabel("Username: "));
        center.add(user);
        center.add(new JLabel("Email: "));
        center.add(email);
        center.add(new JLabel("Password: "));
        center.add(pass);

        JButton create = getJButton(user, email, pass);

        add(center, BorderLayout.CENTER);
        JPanel b = new JPanel();
        b.add(create);
        add(b, BorderLayout.SOUTH);
        setVisible(true);
    }

    private JButton getJButton(JTextField user, JTextField email, JPasswordField pass) {
        JButton create = new JButton("CREATE");
        create.addActionListener(e -> {
            String u = user.getText().trim();
            String em = email.getText().trim();
            String pw = new String(pass.getPassword()).trim();

            if (u.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password required.");
                return;
            }
            if (createUser(u, em, pw)) {
                JOptionPane.showMessageDialog(this, "Account created. Log in.");
                dispose();
                new LoginFrame();
            }
        });
        return create;
    }

    private boolean createUser(String username, String email, String password) {
        String sql = "INSERT INTO users(username, email, password) VALUES(?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, email);
            p.setString(3, password);
            p.executeUpdate();
            return true;
        } catch (SQLException ex) {
            if (ex.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
            } else {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
            return false;
        }
    }
}

/* ------------------ Main Dashboard ------------------ */
class DashboardFrame extends JFrame {
    private final String currentUser;
    private final DefaultTableModel tableModel;
    private final JTable productTable;

    public DashboardFrame(String username) {
        this.currentUser = username;
        setTitle("Inventory Dashboard - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));

        // Left menu
        JPanel menu = new JPanel(new GridLayout(8,1,6,6));
        menu.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JButton profileBtn = new JButton("PROFILE");
        JButton addBtn = new JButton("ADD");
        JButton updateBtn = new JButton("UPDATE");
        JButton restockBtn = new JButton("RESTOCK");
        JButton sellBtn = new JButton("SALE");
        JButton refreshBtn = new JButton("REFRESH");
        JButton historyBtn = new JButton("SALES HISTORY");
        JButton logoutBtn = new JButton("LOGOUT");

        menu.add(profileBtn);
        menu.add(addBtn);
        menu.add(updateBtn);
        menu.add(restockBtn);
        menu.add(sellBtn);
        menu.add(refreshBtn);
        menu.add(historyBtn);
        menu.add(logoutBtn);

        add(menu, BorderLayout.WEST);

        // Table
        String[] cols = {"ID", "Product Name", "Quantity", "Price"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane jsp = new JScrollPane(productTable);
        add(jsp, BorderLayout.CENTER);

        // Footer showing current user
        JLabel info = new JLabel("Logged in as: " + username);
        info.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        add(info, BorderLayout.SOUTH);

        // Actions
        refreshBtn.addActionListener(e -> loadProducts());
        addBtn.addActionListener(e -> new AddProductDialog(this));
        updateBtn.addActionListener(e -> openUpdateDialog());
        restockBtn.addActionListener(e -> openRestockDialog());
        sellBtn.addActionListener(e -> openSellDialog());
        profileBtn.addActionListener(e -> showProfile());
        historyBtn.addActionListener(e -> new SalesHistoryDialog(this));
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        // initial load
        loadProducts();
        setVisible(true);
    }

    void loadProducts() {
        tableModel.setRowCount(0);
        String sql = "SELECT id, name, quantity, price FROM products ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                Object[] row = new Object[] {
                        r.getInt("id"),
                        r.getString("name"),
                        r.getInt("quantity"),
                        r.getDouble("price")
                };
                tableModel.addRow(row);
            }
        } catch (SQLException ex) {
            SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Load products failed", ex);
            JOptionPane.showMessageDialog(this, "Could not load products.");
        }
    }

    private void openUpdateDialog() {
        int sel = productTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to update.");
            return;
        }
        int id = (int) tableModel.getValueAt(sel, 0);
        new UpdateProductDialog(this, id);
    }

    private void openRestockDialog() {
        int sel = productTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to restock.");
            return;
        }
        int id = (int) tableModel.getValueAt(sel, 0);
        new RestockDialog(this, id);
    }

    private void openSellDialog() {
        int sel = productTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to sell.");
            return;
        }
        int id = (int) tableModel.getValueAt(sel, 0);
        new SellDialog(this, id);
    }

    private void showProfile() {

        JOptionPane.showMessageDialog(this, "Profile:\nUsername: " + currentUser);
    }
}

/* ------------------ Add Product ------------------ */
class AddProductDialog extends JDialog {
    public AddProductDialog(DashboardFrame owner) {
        super(owner, "Add Item", true);
        setSize(380, 240);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(4,2,8,8));
        setResizable(false);

        JTextField name = new JTextField();
        JTextField qty = new JTextField();
        JTextField price = new JTextField();

        add(new JLabel("Product name: "));
        add(name);
        add(new JLabel("Quantity: "));
        add(qty);
        add(new JLabel("Price: "));
        add(price);

        JButton addBtn = new JButton("ADD");
        addBtn.addActionListener(e -> {
            try {
                String n = name.getText().trim();
                int q = Integer.parseInt(qty.getText().trim());
                double p = Double.parseDouble(price.getText().trim());
                if (n.isEmpty()) throw new IllegalArgumentException("Name empty");
                String sql = "INSERT INTO products (name, quantity, price) VALUES (?,?,?)";
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, n);
                    ps.setInt(2, q);
                    ps.setDouble(3, p);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Added.");
                    dispose();
                    owner.loadProducts();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Quantity and price must be numbers.");
            } catch (Exception ex) {
                SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Add product failed", ex);
                JOptionPane.showMessageDialog(this, "Error adding product.");
            }
        });

        add(new JPanel()); // filler
        JPanel p = new JPanel();
        p.add(addBtn);
        add(p);
        setVisible(true);
    }
}

/* ------------------ Update Product ------------------ */
class UpdateProductDialog extends JDialog {
    private final int productId;

    public UpdateProductDialog(DashboardFrame owner, int pid) {
        super(owner, "Update Product", true);
        this.productId = pid;
        setSize(420, 220);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(4,2,8,8));
        setResizable(false);

        // labels (read-only show)
        JLabel nameLabel = new JLabel();        // shows product name
        JLabel qtyLabel  = new JLabel();        // shows current quantity

        // editable field for price only
        JTextField priceField = new JTextField();

        // Load product values from DB
        String sql = "SELECT name, quantity, price FROM products WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, pid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nameLabel.setText(rs.getString("name"));
                    qtyLabel.setText(String.valueOf(rs.getInt("quantity")));
                    priceField.setText(String.valueOf(rs.getDouble("price")));
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found.");
                    dispose();
                    return;
                }
            }
        } catch (SQLException ex) {
            SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Update product failed", ex);
            JOptionPane.showMessageDialog(this, "Database error.");
            return;
        }

        add(new JLabel("Product name: "));
        add(nameLabel);
        add(new JLabel("Quantity: "));
        add(qtyLabel);
        add(new JLabel("Price: "));
        add(priceField);

        JPanel buttons = getJPanel(owner, priceField);
        add(new JPanel()); // filler
        add(buttons);

        setVisible(true);
    }

    private JPanel getJPanel(DashboardFrame owner, JTextField priceField) {
        JButton saveBtn = new JButton("SAVE");
        JButton deleteBtn = new JButton("DELETE");

        // SAVE: update only price (you can expand to update name/qty too if you wish)
        saveBtn.addActionListener(e -> {
            try {
                double newPrice = Double.parseDouble(priceField.getText().trim());
                try (Connection c = DBConnection.getConnection();
                     PreparedStatement ps = c.prepareStatement("UPDATE products SET price = ? WHERE id = ?")) {
                    ps.setDouble(1, newPrice);
                    ps.setInt(2, productId);
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        JOptionPane.showMessageDialog(this, "Price updated.");
                        dispose();
                        owner.loadProducts();
                    } else {
                        JOptionPane.showMessageDialog(this, "Update failed - product may not exist.");
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Price must be a number.");
            } catch (SQLException ex) {
                SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Update product failed", ex);
                JOptionPane.showMessageDialog(this, "Database error.");
            }
        });

        // DELETE: proper try-with-resources usage
        deleteBtn.addActionListener(e -> {
            int conf = JOptionPane.showConfirmDialog(this, "Delete this product?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) return;

            String delSql = "DELETE FROM products WHERE id = ?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(delSql)) {
                ps.setInt(1, productId);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(this, "Deleted.");
                    dispose();
                    owner.loadProducts();
                } else {
                    JOptionPane.showMessageDialog(this, "Delete failed - product may not exist.");
                }
            } catch (SQLException ex) {
                SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Update product failed", ex);
                JOptionPane.showMessageDialog(this, "Database error.");
            }
        });

        JPanel buttons = new JPanel();
        buttons.add(saveBtn);
        buttons.add(deleteBtn);
        return buttons;
    }
}


/* ------------------ Restock ------------------ */
class RestockDialog extends JDialog {
    public RestockDialog(DashboardFrame owner, int productId) {
        super(owner, "Restock", true);
        setSize(380, 200);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(3,2,8,8));
        setResizable(false);

        JTextField qty = new JTextField();
        add(new JLabel("Add quantity: "));
        add(qty);

        JButton confirm = new JButton("CONFIRM");
        confirm.addActionListener(e -> {
            try {
                int add = Integer.parseInt(qty.getText().trim());
                if (add <= 0) throw new IllegalArgumentException("Must be > 0");
                try (Connection c = DBConnection.getConnection()) {
                    c.setAutoCommit(false);
                    try (PreparedStatement select = c.prepareStatement("SELECT quantity FROM products WHERE id=?");
                         PreparedStatement update = c.prepareStatement("UPDATE products SET quantity=? WHERE id=?")) {
                        select.setInt(1, productId);
                        try (ResultSet r = select.executeQuery()) {
                            if (!r.next()) throw new SQLException("Product missing");
                            int cur = r.getInt(1);
                            int updated = cur + add;
                            update.setInt(1, updated);
                            update.setInt(2, productId);
                            update.executeUpdate();
                        }
                    }
                    c.commit();
                    JOptionPane.showMessageDialog(this, "Restocked.");
                    dispose();
                    owner.loadProducts();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid number.");
            } catch (Exception ex) {
                SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Restock failed", ex);
                JOptionPane.showMessageDialog(this, "Error during restock.");
            }
        });

        add(new JPanel());
        JPanel p = new JPanel();
        p.add(confirm);
        add(p);
        setVisible(true);
    }
}

/* ------------------ Sell dialog ------------------ */
class SellDialog extends JDialog {
    public SellDialog(DashboardFrame owner, int productId) {
        super(owner, "Sell", true);
        setSize(420, 240);
        setLocationRelativeTo(owner);
        setLayout(new GridLayout(4,2,8,8));
        setResizable(false);

        JTextField qty = new JTextField();

        // load product name and price
        String tempName;
        double price;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT name, price, quantity FROM products WHERE id=?")) {
            p.setInt(1, productId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    tempName = r.getString("name");
                    price = r.getDouble("price");
                } else {
                    JOptionPane.showMessageDialog(this, "Product doesn't exist.");
                    dispose(); 
                    return;
                }
            }
        } catch (Exception ex) {
            SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Sell failed", ex);
            JOptionPane.showMessageDialog(this, "Sale failed.");
            return;
        }
        final String nameFinal = tempName;

        add(new JLabel("Product name: "));
        add(new JLabel(tempName));
        add(new JLabel("Quantity to sell: "));
        add(qty);
        add(new JLabel("Price per item: "));
        add(new JLabel(String.valueOf(price)));

        JButton sell = new JButton("SELL");

        sell.addActionListener(e -> {
            try {
                int count = Integer.parseInt(qty.getText().trim());
                if (count <= 0) throw new IllegalArgumentException("Quantity must be > 0");

                try (Connection c = DBConnection.getConnection()) {
                    c.setAutoCommit(false);
                    try (PreparedStatement psel = c.prepareStatement("SELECT quantity, price FROM products WHERE id=? FOR UPDATE")) {
                        psel.setInt(1, productId);
                        try (ResultSet r = psel.executeQuery()) {
                            if (!r.next()) throw new SQLException("Product missing");
                            int current = r.getInt("quantity");
                            double unitPrice = r.getDouble("price");
                            if (current < count) {
                                JOptionPane.showMessageDialog(this, "Not enough stock.");
                                c.rollback();
                                return;
                            }
                            int newQty = current - count;
                            try (PreparedStatement pup = c.prepareStatement("UPDATE products SET quantity=? WHERE id=?")) {
                                pup.setInt(1, newQty);
                                pup.setInt(2, productId);
                                pup.executeUpdate();
                            }
                            double total = unitPrice * count;
                            // Insert sale with product_id AND product_name
                            try (PreparedStatement insert = c.prepareStatement(
                                    "INSERT INTO sales (product_id, product_name, quantity, total, timestamp) VALUES (?,?,?,?,?)")) {

                                insert.setInt(1, productId);             // product ID (can be NULL if deleted later)
                                insert.setString(2, nameFinal);              // product name copied for history
                                insert.setInt(3, count);                 // quantity sold
                                insert.setDouble(4, total);              // total amount

                                // Format timestamp as 12-hour format
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd h:mm a");
                                String formattedTime = sdf.format(new Date());

                                insert.setString(5, formattedTime);  // Insert formatted time

                                System.out.println("Formatted Time: " + formattedTime);

                                insert.executeUpdate();
                            }

                            c.commit();
                            JOptionPane.showMessageDialog(this, "Sold " + count + " units. Total = " + total);
                            dispose();
                            owner.loadProducts();
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid integer quantity.");
            } catch (Exception ex) {
                SariSariInventorySystem.LOGGER.log(Level.SEVERE, "Sell failed", ex);
                JOptionPane.showMessageDialog(this, "Sale failed.");
            }
        });

        JPanel buton = new JPanel();
        buton.add(sell);
        add(new JPanel());
        add(buton);
        setVisible(true);
    }
}

