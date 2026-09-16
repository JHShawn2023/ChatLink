package chatapp;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField ipField;
    private JLabel confirmPasswordLabel;
    private JButton authButton;
    private JButton switchButton;

    private boolean signupMode = false;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private static final int SERVER_PORT = 5000;

    public LoginFrame() {
        setTitle("ChatLink - Login");
        setSize(450, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 35, 60));

        JLabel titleLabel = new JLabel("CHATLINK");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(new Color(40, 40, 40));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Welcome back!");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        // Server IP
        JLabel ipLabel = new JLabel("Server IP Address");
        styleLabel(ipLabel);
        mainPanel.add(ipLabel);
        mainPanel.add(Box.createVerticalStrut(7));

        ipField = new JTextField("localhost");
        styleTextField(ipField);
        mainPanel.add(ipField);
        mainPanel.add(Box.createVerticalStrut(6));

        JLabel tipLabel = new JLabel("<html><center>Use <b>localhost</b> for same PC<br>or type the server IP (e.g. 192.168.0.105)</center></html>");
        tipLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        tipLabel.setForeground(new Color(120, 120, 120));
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(tipLabel);
        mainPanel.add(Box.createVerticalStrut(18));

        // Username
        JLabel usernameLabel = new JLabel("Username");
        styleLabel(usernameLabel);
        mainPanel.add(usernameLabel);
        mainPanel.add(Box.createVerticalStrut(7));

        usernameField = new JTextField();
        styleTextField(usernameField);
        mainPanel.add(usernameField);
        mainPanel.add(Box.createVerticalStrut(18));

        // Password
        JLabel passwordLabel = new JLabel("Password");
        styleLabel(passwordLabel);
        mainPanel.add(passwordLabel);
        mainPanel.add(Box.createVerticalStrut(7));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        mainPanel.add(passwordField);

        // Confirm Password
        confirmPasswordLabel = new JLabel("Confirm Password");
        styleLabel(confirmPasswordLabel);
        mainPanel.add(confirmPasswordLabel);
        mainPanel.add(Box.createVerticalStrut(7));

        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        mainPanel.add(confirmPasswordField);

        confirmPasswordLabel.setVisible(false);
        confirmPasswordField.setVisible(false);

        mainPanel.add(Box.createVerticalStrut(28));

        // Auth button
        authButton = new JButton("LOGIN");
        authButton.setFont(new Font("Arial", Font.BOLD, 14));
        authButton.setForeground(Color.WHITE);
        authButton.setBackground(new Color(55, 105, 200));
        authButton.setFocusPainted(false);
        authButton.setBorderPainted(false);
        authButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        authButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(authButton);

        mainPanel.add(Box.createVerticalStrut(22));

        JLabel switchText = new JLabel("Don't have an account?");
        switchText.setFont(new Font("Arial", Font.PLAIN, 13));
        switchText.setForeground(new Color(100, 100, 100));
        switchText.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(switchText);
        mainPanel.add(Box.createVerticalStrut(3));

        switchButton = new JButton("Create Account");
        switchButton.setFont(new Font("Arial", Font.BOLD, 13));
        switchButton.setForeground(new Color(45, 100, 200));
        switchButton.setBackground(new Color(245, 247, 250));
        switchButton.setBorderPainted(false);
        switchButton.setFocusPainted(false);
        switchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(switchButton);

        authButton.addActionListener(e -> authenticate());
        switchButton.addActionListener(e -> switchMode());

        add(mainPanel);
    }

    private void styleLabel(JLabel label) {
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(new Color(55, 55, 55));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 15));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private void switchMode() {
        signupMode = !signupMode;

        if (signupMode) {
            authButton.setText("SIGN UP");
            confirmPasswordLabel.setVisible(true);
            confirmPasswordField.setVisible(true);
            subtitleLabelChange("Create your account");
            switchTextChange("Already have an account?");
            switchButton.setText("Sign In");
        } else {
            authButton.setText("LOGIN");
            confirmPasswordLabel.setVisible(false);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setText("");
            subtitleLabelChange("Welcome back!");
            switchTextChange("Don't have an account?");
            switchButton.setText("Create Account");
        }
        revalidate();
        repaint();
    }

    private void subtitleLabelChange(String text) {
        Component[] components = ((JPanel) getContentPane().getComponent(0)).getComponents();
        for (Component c : components) {
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                if (label.getText().equals("Welcome back!") || label.getText().equals("Create your account")) {
                    label.setText(text);
                    break;
                }
            }
        }
    }

    private void switchTextChange(String text) {
        Component[] components = ((JPanel) getContentPane().getComponent(0)).getComponents();
        for (Component c : components) {
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                if (label.getText().equals("Don't have an account?") || label.getText().equals("Already have an account?")) {
                    label.setText(text);
                    break;
                }
            }
        }
    }

    private void authenticate() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmPasswordField.getPassword());
        String serverIP = ipField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (serverIP.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Server IP Address.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (signupMode) {
            if (confirm.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please confirm your password.", "Signup Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!password.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.", "Signup Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        authButton.setEnabled(false);

        new Thread(() -> {
            try {
                socket = new Socket(serverIP, SERVER_PORT);
                input  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                input.readLine(); // welcome message

                if (signupMode) {
                    output.println("SIGNUP");
                    output.println(username);
                    output.println(password);

                    String response = input.readLine();

                    SwingUtilities.invokeLater(() -> {
                        authButton.setEnabled(true);
                        if ("SIGNUP_SUCCESS".equals(response)) {
                            JOptionPane.showMessageDialog(this,
                                    "Account created successfully!\nYou can now log in.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            if (signupMode) switchMode();
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Username already exists.",
                                    "Signup Failed", JOptionPane.ERROR_MESSAGE);
                        }
                        try { socket.close(); } catch (IOException ignored) {}
                    });

                } else {
                    output.println("LOGIN");
                    output.println(username);
                    output.println(password);

                    String response = input.readLine();

                    SwingUtilities.invokeLater(() -> {
                        authButton.setEnabled(true);
                        if ("LOGIN_SUCCESS".equals(response)) {
                            openChat(username);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Invalid username or password.",
                                    "Login Failed", JOptionPane.ERROR_MESSAGE);
                            try { socket.close(); } catch (IOException ignored) {}
                        }
                    });
                }

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    authButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this,
                            "Cannot connect to server.\n\n" +
                            "• Make sure the Server is running\n" +
                            "• Check the IP address\n" +
                            "• Same Wi-Fi / network required",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void openChat(String username) {
        ChatFrame chat = new ChatFrame(username, socket, input, output);
        chat.setVisible(true);
        this.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
