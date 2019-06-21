package java_chat;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java_chat.Encryption;
import java_chat.Utils;

public class ClientGui extends Thread {

    final JTextPane jtextFilDiscu = new JTextPane();
    final JTextPane jtextListUsers = new JTextPane();
    final JTextField jtextInputChat = new JTextField();
    private String oldMsg = "";
    private Thread read;
    private String serverName;
    private int PORT;
    private String name;
    BufferedReader input;
    PrintWriter output;
    Socket server;
    public Connection conn;
    private Encryption encryption;


    public ClientGui() {
        this.serverName = "localhost";
        this.PORT = 12345;
        this.name = "nickname";

        try {
            //Establish a driver
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            //Connect to the database
            conn = DriverManager.getConnection("jdbc:mysql://seitux2.adfa.unsw.edu.au/z5157409", "z5157409", "mysqlpass");
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException ex) {
            System.err.println("Unable to load MySQL driver.");
        }
        String fontfamily = "Arial, sans-serif";
        Font font = new Font(fontfamily, Font.PLAIN, 15);

        final JFrame jfr = new JFrame("Chat");
        jfr.getContentPane().setLayout(null);
        jfr.setSize(700, 500);
        jfr.setResizable(false);
        jfr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Module du fil de discussion
        jtextFilDiscu.setBounds(25, 25, 490, 320);
        jtextFilDiscu.setFont(font);
        jtextFilDiscu.setMargin(new Insets(6, 6, 6, 6));
        jtextFilDiscu.setEditable(false);
        JScrollPane jtextFilDiscuSP = new JScrollPane(jtextFilDiscu);
        jtextFilDiscuSP.setBounds(25, 25, 490, 320);

        jtextFilDiscu.setContentType("text/html");
        jtextFilDiscu.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        // Module de la liste des utilisateurs
        jtextListUsers.setBounds(520, 25, 156, 320);
        jtextListUsers.setEditable(true);
        jtextListUsers.setFont(font);
        jtextListUsers.setMargin(new Insets(6, 6, 6, 6));
        jtextListUsers.setEditable(false);
        JScrollPane jsplistuser = new JScrollPane(jtextListUsers);
        jsplistuser.setBounds(520, 25, 156, 320);

        jtextListUsers.setContentType("text/html");
        jtextListUsers.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        // Field message user input
        jtextInputChat.setBounds(0, 350, 400, 50);
        jtextInputChat.setFont(font);
        jtextInputChat.setMargin(new Insets(6, 6, 6, 6));
        final JScrollPane jtextInputChatSP = new JScrollPane(jtextInputChat);
        jtextInputChatSP.setBounds(25, 350, 650, 50);

        // button send
        final JButton jsbtn = new JButton("Send");
        jsbtn.setFont(font);
        jsbtn.setBounds(575, 410, 100, 35);

        // button Disconnect
        final JButton jsbtndeco = new JButton("Disconnect");
        jsbtndeco.setFont(font);
        jsbtndeco.setBounds(25, 410, 130, 35);

        jtextInputChat.addKeyListener(new KeyAdapter() {
            // send message on Enter
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }

                // Get last message typed
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    String currentMessage = jtextInputChat.getText().trim();
                    jtextInputChat.setText(oldMsg);
                    oldMsg = currentMessage;
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    String currentMessage = jtextInputChat.getText().trim();
                    jtextInputChat.setText(oldMsg);
                    oldMsg = currentMessage;
                }
            }
        });

        // Click on send button
        jsbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
            }
        });

        // Connection view
        final JTextField nickname = new JTextField(this.name);
        final JTextField password = new JTextField("password");
        final JButton create = new JButton("Create Account");
        final JButton connect = new JButton("Connect");

        // check if those field are not empty
        nickname.getDocument().addDocumentListener(new TextListener(nickname, password, create, connect));
        password.getDocument().addDocumentListener(new TextListener(nickname, password, create, connect));
        //   jtfAddr.getDocument().addDocumentListener(new TextListener(jtfName, jtfport, jtfAddr, jcbtn));

        // position des Modules
        connect.setFont(font);
        create.setBounds(25, 380, 135, 40);
        password.setBounds(375, 380, 135, 40);
        nickname.setBounds(200, 380, 135, 40);
        connect.setBounds(575, 380, 100, 40);

        // couleur par defaut des Modules fil de discussion et liste des utilisateurs
        jtextFilDiscu.setBackground(Color.LIGHT_GRAY);
        jtextListUsers.setBackground(Color.LIGHT_GRAY);

        // ajout des éléments
        jfr.add(connect);
        jfr.add(jtextFilDiscuSP);
        jfr.add(jsplistuser);
        jfr.add(nickname);
        jfr.add(password);
        jfr.add(create);
        jfr.setVisible(true);

        // info sur le Chat
        appendToPane(jtextFilDiscu, "<h4>Les commandes possibles dans le chat sont:</h4>"
                + "<ul>"
                + "<li><b>@nickname</b> pour envoyer un Message privé à l'utilisateur 'nickname'</li>"
                + "<li><b>#d3961b</b> pour changer la couleur de son pseudo au code hexadécimal indiquer</li>"
                + "<li><b>;)</b> quelques smileys sont implémentés</li>"
                + "<li><b>flèche du haut</b> pour reprendre le dernier message tapé</li>"
                + "</ul><br/>");

        // On connect
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    name = nickname.getText();
                    String pword = password.getText();
                    // String port = "12345";
                    serverName = "localhost";
                    PORT = 12345;

                    boolean exists = false;
                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM ACCOUNT WHERE username = ? AND password = SHA2(CONCAT(created_at,?),512);");
                    ps.setString(1, name);
                    ps.setString(2, pword);
                    ResultSet rs = ps.executeQuery();
                    if (rs.first()) {
                        exists = true;
                    }
                    //Close the connection
                    rs.close();
                    ps.close();

                    if (exists) {
                        appendToPane(jtextFilDiscu, "<span>Login Successful</span>");
                    
                    appendToPane(jtextFilDiscu, "<span>Connecting to " + serverName + " on port " + PORT + "...</span>");
                    server = new Socket(serverName, PORT);

                    appendToPane(jtextFilDiscu, "<span>Connected to "
                            + server.getRemoteSocketAddress() + "</span>");

                    input = new BufferedReader(new InputStreamReader(server.getInputStream()));
                    output = new PrintWriter(server.getOutputStream(), true);

                    // send nickname to server
                    output.println(name);

                    // create new Read Thread
                    read = new Read();
                    read.start();
                    jfr.remove(nickname);
                    jfr.remove(password);
                    jfr.remove(create);
                    jfr.remove(connect);
                    jfr.add(jsbtn);
                    jfr.add(jtextInputChatSP);
                    jfr.add(jsbtndeco);
                    jfr.revalidate();
                    jfr.repaint();
                    jtextFilDiscu.setBackground(Color.WHITE);
                    jtextListUsers.setBackground(Color.WHITE);
                    }
                    else{
                        appendToPane(jtextFilDiscu, "<span>Incorrect Login Details</span>");
                    }
                } catch (Exception ex) {
                    appendToPane(jtextFilDiscu, "<span>Could not connect to Server</span>");
                    JOptionPane.showMessageDialog(jfr, ex.getMessage());
                }
            }

        });

        // on deco
        jsbtndeco.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                jfr.add(nickname);
                jfr.add(password);
                jfr.add(create);
                jfr.add(connect);
                jfr.remove(jsbtn);
                jfr.remove(jtextInputChatSP);
                jfr.remove(jsbtndeco);
                jfr.revalidate();
                jfr.repaint();
                read.interrupt();
                jtextListUsers.setText(null);
                jtextFilDiscu.setBackground(Color.LIGHT_GRAY);
                jtextListUsers.setBackground(Color.LIGHT_GRAY);
                appendToPane(jtextFilDiscu, "<span>Connection closed.</span>");
                output.close();
            }
        });

    
    
    create.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    name = nickname.getText();
                    String pword = password.getText();
                    // String port = "12345";
                    serverName = "localhost";
                    PORT = 12345;

                    boolean exists = false;
                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM ACCOUNT WHERE username = ?;");
                    ps.setString(1, name);
                     ResultSet rs = ps.executeQuery();
                     System.out.println("gets to here");
                    if (rs.first()) {
                        exists = true;
                    }
                    //Close the connection
                    rs.close();
                    if(!exists){
                     ps = conn.prepareStatement("INSERT INTO ACCOUNT(username, password, created_at) VALUES(?,SHA2(CONCAT(CURRENT_TIMESTAMP,?),512),CURRENT_TIMESTAMP);");
                    ps.setString(1, name);
                    ps.setString(2, pword);
                          ps.executeUpdate();
                   
                    ps.close();

                appendToPane(jtextFilDiscu, "<span>Login Successful</span>");
                    
                    appendToPane(jtextFilDiscu, "<span>Connecting to " + serverName + " on port " + PORT + "...</span>");
                    server = new Socket(serverName, PORT);

                    appendToPane(jtextFilDiscu, "<span>Connected to "
                            + server.getRemoteSocketAddress() + "</span>");

                    input = new BufferedReader(new InputStreamReader(server.getInputStream()));
                    output = new PrintWriter(server.getOutputStream(), true);

                    // send nickname to server
                    output.println(name);

                    // create new Read Thread
                    read = new Read();
                    read.start();
                    jfr.remove(nickname);
                    jfr.remove(password);
                    jfr.remove(create);
                    jfr.remove(connect);
                    jfr.add(jsbtn);
                    jfr.add(jtextInputChatSP);
                    jfr.add(jsbtndeco);
                    jfr.revalidate();
                    jfr.repaint();
                    jtextFilDiscu.setBackground(Color.WHITE);
                    jtextListUsers.setBackground(Color.WHITE);
                     }
                    else{
                        appendToPane(jtextFilDiscu, "<span>Nickname already taken</span>");
                      //  System.out.println("nickname already taken");
                    }
                } catch (IOException | SQLException ex) {
                    appendToPane(jtextFilDiscu, "<span>Could not connect to Server</span>");
                    JOptionPane.showMessageDialog(jfr, ex.getMessage());
                }
            }

        });

        // on deco
        jsbtndeco.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                jfr.add(nickname);
                jfr.add(password);
                jfr.add(create);
                jfr.add(connect);
                jfr.remove(jsbtn);
                jfr.remove(jtextInputChatSP);
                jfr.remove(jsbtndeco);
                jfr.revalidate();
                jfr.repaint();
                read.interrupt();
                jtextListUsers.setText(null);
                jtextFilDiscu.setBackground(Color.LIGHT_GRAY);
                jtextListUsers.setBackground(Color.LIGHT_GRAY);
                appendToPane(jtextFilDiscu, "<span>Connection closed.</span>");
                output.close();
            }
        });

    }

    // check if if all field are not empty
    public class TextListener implements DocumentListener {

        JTextField jtf1;
        JTextField jtf2;
        JButton create;
        JButton jcbtn;

        public TextListener(JTextField jtf1, JTextField jtf2, JButton create, JButton jcbtn) {
            this.jtf1 = jtf1;
            this.jtf2 = jtf2;
            this.create = create;
            this.jcbtn = jcbtn;
        }

        public void changedUpdate(DocumentEvent e) {
        }

        public void removeUpdate(DocumentEvent e) {
            if (jtf1.getText().trim().equals("")
                    || jtf2.getText().trim().equals("")) {
                create.setEnabled(false);
                jcbtn.setEnabled(false);
            } else {
                jcbtn.setEnabled(true);
                create.setEnabled(true);
            }
        }

        public void insertUpdate(DocumentEvent e) {
            if (jtf1.getText().trim().equals("")
                    || jtf2.getText().trim().equals("")) {
                jcbtn.setEnabled(false);
                create.setEnabled(false);
            } else {
                jcbtn.setEnabled(true);
                create.setEnabled(true);
            }
        }

    }

    // envoi des messages
    public void sendMessage() {
        try {
            String message = jtextInputChat.getText().trim();
            if (message.equals("")) {
                return;
            }
            this.oldMsg = message;
            message = Utils.toString(encryption.encrypt(Utils.toByteArray(message)));
            output.println(message);
            jtextInputChat.requestFocus();
            jtextInputChat.setText(null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.exit(0);
        }
    }
    public void sendMessage(String message){
        output.println(message);
        //encrypt here
        //Output.println(cipheredText);
    }

    public static void main(String[] args) throws Exception {
        ClientGui client = new ClientGui();
    }

    // read new incoming messages
    class Read extends Thread {
    byte [] kb;
        public void run() {
            encryption = new Encryption();
            encryption.setKeyPair();
           // System.out.println("did this");
            String message;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    message = input.readLine();
                    if (message != null) {
                        if(message.startsWith("{{{")){
                          //  System.out.println(message);
                            String key = message.substring(3,message.length());
                            key = Utils.toHex(key);
                            byte[] keyBytes = Utils.toByteArray(key);
                          // System.out.println(Utils.toString(keyBytes));
                            encryption.setEncPubKey(keyBytes);
                             byte[] b = encryption.setKeyBlock();
                             
                            //READ THE SERVERS PUBLIC KEY
                            //SEND THEIR OWN PUBLIC KEY TO SERVER
                            sendMessage("{{{" + b);
                        }else{
                           message = Utils.toString(encryption.decrypt(Utils.toByteArray(message)));
                        
                        if (message.charAt(0) == '[') {
                            message = message.substring(1, message.length() - 1);
                            ArrayList<String> ListUser = new ArrayList<String>(
                                    Arrays.asList(message.split(", "))
                            );
                            jtextListUsers.setText(null);
                            for (String user : ListUser) {
                                appendToPane(jtextListUsers, "@" + user);
                            }
                        } else {
                            appendToPane(jtextFilDiscu, message);
                        }
                    }
                    }
                } catch (IOException ex) {
                    System.err.println("Failed to parse incoming message");
                } catch (Exception ex) {
                    Logger.getLogger(ClientGui.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    // send html to pane
    private void appendToPane(JTextPane tp, String msg) {
        HTMLDocument doc = (HTMLDocument) tp.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) tp.getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), msg, 0, 0, null);
            tp.setCaretPosition(doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
