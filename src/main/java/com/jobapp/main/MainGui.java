package com.jobapp.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.jobapp.dao.CVDAO;
import com.jobapp.dao.JobDAO;
import com.jobapp.ml.CVParser;
import com.jobapp.ml.JobMatcher;
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.RekruteScraper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainGui extends JFrame {

    // --- PALETTE "OBSIDIAN PRESTIGE" ---
    private final Color BG_DARK = new Color(15, 23, 42);       // Slate 900
    private final Color PANEL_GLASS = new Color(30, 41, 59, 200); // Slate 800 (Alpha)
    private final Color ACCENT_CYAN = new Color(6, 182, 212);  // Cyan 500
    private final Color ACCENT_PURPLE = new Color(139, 92, 246); // Violet 500
    private final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private final Color TEXT_SECONDARY = new Color(148, 163, 184);

    // Components
    private JTextField searchField;
    private JTextArea cvArea;
    private DefaultTableModel tableModel;
    private JTable resultsTable;
    private JLabel statusLabel;
    private ProgressBarCustom progressBar;

    // Backend Services
    private final JobDAO jobDao = new JobDAO();
    private final CVDAO cvDao = new CVDAO();
    private final JobMatcher matcher = new JobMatcher();
    private final CVParser cvParser = new CVParser();
    private List<Offre> currentJobs = new ArrayList<>();
    private final com.jobapp.ml.SmartCVParser smartParser = new com.jobapp.ml.SmartCVParser();

    public MainGui() {
        // Setup Theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Component.arc", 16);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.showHorizontalLines", false);
            UIManager.put("Table.showVerticalLines", false);
        } catch (Exception e) { e.printStackTrace(); }

        setTitle("JobHunter");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- MAIN LAYOUT ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_DARK);
        setContentPane(mainPanel);

        // 1. Sidebar (Controls)
        mainPanel.add(createSidebar(), BorderLayout.WEST);

        // 2. Dashboard (Results)
        mainPanel.add(createDashboard(), BorderLayout.CENTER);
    }

    // ---------------- UI SECTIONS ----------------

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(380, 0));
        sidebar.setBackground(BG_DARK); // Blend with background
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(new EmptyBorder(30, 30, 30, 30));

        // --- Container with Glass Effect ---
        JPanel glassContainer = new RoundedPanel(20, PANEL_GLASS);
        glassContainer.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 20, 15, 20);
        gbc.gridx = 0;

        // Logo
        JLabel logo = new JLabel("RECRUIT.AI");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logo.setForeground(ACCENT_CYAN);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(30, 20, 40, 20);
        glassContainer.add(logo, gbc);

        // Section: Market
        gbc.gridy++; gbc.insets = new Insets(0, 20, 5, 20);
        glassContainer.add(createLabel("MARKET INTELLIGENCE"), gbc);

        searchField = createStyledTextField("Java, Finance, Manager...");
        gbc.gridy++; gbc.insets = new Insets(0, 20, 15, 20);
        glassContainer.add(searchField, gbc);

        JButton scrapeBtn = createGradientButton("Sync Market Data", ACCENT_CYAN, new Color(59, 130, 246));
        scrapeBtn.addActionListener(e -> startScraping());
        gbc.gridy++;
        glassContainer.add(scrapeBtn, gbc);

        // Section: Candidate
        gbc.gridy++; gbc.insets = new Insets(30, 20, 5, 20);
        glassContainer.add(createLabel("CANDIDATE PROFILING"), gbc);

        JButton pdfBtn = createOutlinedButton("Import PDF Resume");
        pdfBtn.addActionListener(e -> importPDF());
        gbc.gridy++; gbc.insets = new Insets(0, 20, 10, 20);
        glassContainer.add(pdfBtn, gbc);

        cvArea = new JTextArea(8, 20);
        cvArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        cvArea.setBackground(new Color(15, 23, 42));
        cvArea.setForeground(Color.LIGHT_GRAY);
        cvArea.setCaretColor(Color.WHITE);
        cvArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cvArea.setLineWrap(true);
        JScrollPane scrollCv = new JScrollPane(cvArea);
        scrollCv.setBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)));
        gbc.gridy++;
        glassContainer.add(scrollCv, gbc);

        JButton matchBtn = createGradientButton("Run Neural Matching", ACCENT_PURPLE, new Color(192, 38, 211));
        matchBtn.addActionListener(e -> runMatching());
        gbc.gridy++; gbc.insets = new Insets(15, 20, 20, 20);
        glassContainer.add(matchBtn, gbc);

        sidebar.add(glassContainer, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel createDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.setOpaque(false);
        dashboard.setBorder(new EmptyBorder(30, 0, 30, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Live Opportunities");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRIMARY);

        statusLabel = new JLabel("System Ready");
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        progressBar = new ProgressBarCustom();
        progressBar.setPreferredSize(new Dimension(200, 4));
        progressBar.setVisible(false);

        header.add(title, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        header.add(progressBar, BorderLayout.SOUTH);

        // Table / Cards
        String[] cols = {"Data"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(110); // Card Height
        resultsTable.setDefaultRenderer(Object.class, new JobCardRenderer());
        resultsTable.setTableHeader(null); // No header needed for card view
        resultsTable.setBackground(BG_DARK);
        resultsTable.setShowGrid(false);
        resultsTable.setIntercellSpacing(new Dimension(0, 10));
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Click Listener for URL
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.getSelectedRow();
                if (row >= 0 && row < currentJobs.size()) {
                    try {
                        Desktop.getDesktop().browse(java.net.URI.create(currentJobs.get(row).getUrl()));
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);

        dashboard.add(header, BorderLayout.NORTH);
        dashboard.add(scroll, BorderLayout.CENTER);

        return dashboard;
    }

    // ---------------- LOGIC ----------------

    private void startScraping() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) keyword = "java";

        statusLabel.setText("Scraping active nodes...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        String finalKeyword = keyword;
        new Thread(() -> {
            List<Offre> rawJobs = new ArrayList<>();
            // Rekrute
            rawJobs.addAll(new RekruteScraper().scrape(finalKeyword));
            // Emploi.ma
            rawJobs.addAll(new EmploiMaScraper().scrape(finalKeyword));

            // Save DB
            for(Offre o : rawJobs) jobDao.saveJob(o);

            currentJobs = rawJobs;

            SwingUtilities.invokeLater(() -> {
                updateTable(null); // No matching yet, just show jobs
                statusLabel.setText("Found " + rawJobs.size() + " positions.");
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
            });
        }).start();
    }

    // Dans ta classe MainGui

    private void importPDF() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PDDocument doc = Loader.loadPDF(f)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String fullText = stripper.getText(doc);

                // 1. Afficher le texte extrait
                cvArea.setText(fullText.trim());

                // 2. --- NLP MAGIC ---
                // On demande au parser de deviner le job
                String aiGeneratedQuery = smartParser.extractSearchQuery(fullText);

                // 3. Mise à jour automatique de l'interface
                searchField.setText(aiGeneratedQuery);
                statusLabel.setText("CV Analysé. Profil détecté : " + aiGeneratedQuery);

                // 4. (Optionnel) Déclencher le scraping automatiquement ?
                int response = JOptionPane.showConfirmDialog(this,
                        "L'IA a détecté le profil : '" + aiGeneratedQuery + "'.\nLancer la recherche maintenant ?",
                        "Analyse NLP Terminée", JOptionPane.YES_NO_OPTION);

                if(response == JOptionPane.YES_OPTION) {
                    startScraping(); // Boum ! Tout est automatique.
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur lecture PDF");
            }
        }
    }

    private void runMatching() {
        String cvText = cvArea.getText();
        if (cvText.length() < 10) {
            JOptionPane.showMessageDialog(this, "Please enter a valid CV first.");
            return;
        }

        statusLabel.setText("Running Neural Matcher...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            String cleanCv = cvParser.parseAndRegroup(cvText);
            // Save Candidate
            int cvId = cvDao.saveCV("Unknown Candidate", "user@app.com", cvText, cleanCv);

            // Sort jobs by score
            currentJobs.sort((j1, j2) -> {
                double s1 = matcher.calculateScore(cleanCv, j1.getFullText());
                double s2 = matcher.calculateScore(cleanCv, j2.getFullText());
                return Double.compare(s2, s1); // Descending
            });

            // Save matches > 10%
            for(Offre j : currentJobs) {
                double score = matcher.calculateScore(cleanCv, j.getFullText());
                if(score > 10) jobDao.saveMatch(cvId, j.getUrl(), score);
            }

            SwingUtilities.invokeLater(() -> {
                updateTable(cleanCv);
                statusLabel.setText("Matching Complete. Top candidates surfaced.");
                progressBar.setVisible(false);
            });
        }).start();
    }

    private void updateTable(String cvText) {
        tableModel.setRowCount(0);
        for (Offre job : currentJobs) {
            double score = 0.0;
            if (cvText != null) {
                score = matcher.calculateScore(cvText, job.getFullText());
            }
            // Add Object wrapper to let Renderer handle it
            tableModel.addRow(new Object[]{ new JobWrapper(job, score) });
        }
    }

    // ---------------- CUSTOM COMPONENTS & RENDERERS ----------------

    // Wrapper to hold data for the renderer
    record JobWrapper(Offre job, double score) {}

    class JobCardRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JobWrapper data = (JobWrapper) value;
            Offre job = data.job;

            JPanel card = new RoundedPanel(15, PANEL_GLASS);
            card.setLayout(new BorderLayout(15, 0));
            card.setBorder(new EmptyBorder(15, 20, 15, 20));

            // Left: Score Indicator
            JPanel scorePanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Dynamic Color based on score
                    Color c = data.score > 70 ? Color.decode("#10B981") : (data.score > 40 ? Color.decode("#F59E0B") : Color.decode("#64748B"));

                    g2.setColor(new Color(30, 41, 59));
                    g2.fillOval(0,0, 60, 60);
                    g2.setColor(c);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawArc(5, 5, 50, 50, 90, -(int)(data.score * 3.6));

                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    String s = String.format("%.0f", data.score);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(s, 30 - fm.stringWidth(s)/2, 35);
                }
            };
            scorePanel.setPreferredSize(new Dimension(65, 60));
            scorePanel.setOpaque(false);

            // Center: Info
            JPanel infoPanel = new JPanel(new GridLayout(3, 1));
            infoPanel.setOpaque(false);

            JLabel title = new JLabel(job.getTitre());
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setForeground(Color.WHITE);

            JLabel company = new JLabel(job.getEntreprise() + "  •  " + job.getVille());
            company.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            company.setForeground(TEXT_SECONDARY);

            JLabel cat = new JLabel(job.getCategory());
            cat.setFont(new Font("Segoe UI", Font.BOLD, 11));
            cat.setForeground(ACCENT_CYAN);

            infoPanel.add(title);
            infoPanel.add(company);
            infoPanel.add(cat);

            // Right: Arrow
            JLabel arrow = new JLabel("➜");
            arrow.setFont(new Font("Segoe UI", Font.BOLD, 20));
            arrow.setForeground(isSelected ? ACCENT_CYAN : new Color(71, 85, 105));

            card.add(scorePanel, BorderLayout.WEST);
            card.add(infoPanel, BorderLayout.CENTER);
            card.add(arrow, BorderLayout.EAST);

            // Selection Highlight
            if(isSelected) {
                card.setBackground(new Color(30, 41, 59)); // Lighter when selected
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT_CYAN, 1),
                        new EmptyBorder(14, 19, 14, 19)
                ));
            }

            // Margin for spacing between rows
            JPanel container = new JPanel(new BorderLayout());
            container.setOpaque(false);
            container.add(card, BorderLayout.CENTER);
            container.setBorder(new EmptyBorder(0, 0, 10, 0)); // Bottom margin

            return container;
        }
    }

    // Helper: Styled Text Field
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField(placeholder) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(100, 116, 139));
                    g2.drawString(placeholder, getInsets().left, g.getFontMetrics().getAscent() + getInsets().top);
                }
            }
        };
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(15, 23, 42)); // Dark input
        field.setCaretColor(ACCENT_CYAN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return field;
    }

    // Helper: Gradient Button
    private JButton createGradientButton(String text, Color c1, Color c2) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 45));
        return btn;
    }

    private JButton createOutlinedButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(ACCENT_CYAN);
        btn.setBackground(null);
        btn.setOpaque(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createLineBorder(ACCENT_CYAN));
        btn.setPreferredSize(new Dimension(0, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(148, 163, 184)); // Slate 400
        l.setBorder(new EmptyBorder(0, 2, 0, 0));
        return l;
    }

    // Custom Rounded Panel
    static class RoundedPanel extends JPanel {
        private int radius;
        private Color bgColor;
        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
        }
    }

    // Simple thin progress bar
    static class ProgressBarCustom extends JProgressBar {
        public ProgressBarCustom() {
            setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
                @Override protected void paintDeterminate(Graphics g, JComponent c) {}
                @Override protected void paintIndeterminate(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(new Color(6, 182, 212)); // Cyan
                    int w = c.getWidth();
                    int h = c.getHeight();
                    // Simple infinite slide animation simulation
                    long time = System.currentTimeMillis();
                    int x = (int)((time % 2000) / 2000.0 * w);
                    g2.fillRect(x, 0, w/4, h);
                    g2.fillRect(x - w, 0, w/4, h);
                }
            });
            setBackground(new Color(30, 41, 59));
            setBorderPainted(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGui().setVisible(true));
    }
}