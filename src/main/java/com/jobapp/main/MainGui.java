package com.jobapp.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.jobapp.dao.JobDAO;
import com.jobapp.ml.SmartCVParser;
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.MarocAnnoncesScraper;
import com.jobapp.scraping.RekruteScraper;
import com.jobapp.service.NeuralMatchingEngine;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainGui extends JFrame {

    // --- COLORS ---
    private final Color BG_DARK = new Color(15, 23, 42);
    private final Color PANEL_GLASS = new Color(30, 41, 59, 255);
    private final Color ACCENT_CYAN = new Color(6, 182, 212);
    private final Color ACCENT_BLUE = new Color(59, 130, 246);
    private final Color ACCENT_PURPLE = new Color(139, 92, 246);
    private final Color BUTTON_INACTIVE = new Color(67, 56, 202);
    private final Color TEXT_SECONDARY = new Color(148, 163, 184);

    // Components
    private JTextField searchField;
    private JTextArea cvArea;
    private DefaultTableModel tableModel;
    private JTable resultsTable;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton btnMatch;

    // Filter Components
    private JComboBox<String> comboCategory;
    private JComboBox<String> comboCity;

    // Backend Services
    private final JobDAO jobDao = new JobDAO();
    private final NeuralMatchingEngine neuralMatcher = new NeuralMatchingEngine();
    private final SmartCVParser smartParser = new SmartCVParser();

    // Data State
    private List<Offre> allJobs = new ArrayList<>();
    private List<Offre> displayedJobs = new ArrayList<>();
    private String currentCVText = null; // Holds the uploaded CV text

    public MainGui() {
        setupTheme();
        setTitle("JobHunter - Intelligent Recruitment Platform");
        setSize(1440, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // 1. Top Section
        mainPanel.add(createTopBar(), BorderLayout.NORTH);

        // 2. Middle Section (Split View)
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(createCandidatePanel());
        contentPanel.add(createResultsPanel());
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 3. Bottom Section
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);

        // 4. AUTO-LOAD DATA ON STARTUP
        loadJobsFromDb();
    }

    // --- INITIAL DATA LOADING ---
    private void loadJobsFromDb() {
        new Thread(() -> {
            // Ensure you added getAllJobs() to JobDAO as discussed previously!
            List<Offre> savedJobs = jobDao.getAllJobs();

            SwingUtilities.invokeLater(() -> {
                if (!savedJobs.isEmpty()) {
                    allJobs = savedJobs;
                    displayedJobs = new ArrayList<>(savedJobs);
                    populateFilters();
                    updateTable(null); // Show jobs with 0% score initially
                    statusLabel.setText("Loaded " + allJobs.size() + " jobs from database.");
                } else {
                    statusLabel.setText("Database empty. Please click 'Synchroniser Marché'.");
                }
            });
        }).start();
    }

    // --- TOP BAR ---
    private JPanel createTopBar() {
        JPanel header = new RoundedPanel(15, PANEL_GLASS);
        header.setLayout(new BorderLayout(20, 10));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        header.setPreferredSize(new Dimension(0, 120));

        // Row 1: Logo + Search
        JPanel topRow = new JPanel(new BorderLayout(20, 0));
        topRow.setOpaque(false);

        JLabel logo = new JLabel("JobHunter");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(ACCENT_CYAN);

        JPanel searchContainer = new JPanel(new BorderLayout(10, 0));
        searchContainer.setOpaque(false);

        searchField = new JTextField("Importez un CV pour auto-générer les mots-clés...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchField.setBackground(BG_DARK);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(ACCENT_CYAN);
        searchField.setBorder(new CompoundBorder(
                new LineBorder(new Color(71, 85, 105)),
                new EmptyBorder(10, 15, 10, 15)));

        JButton btnScrape = createButton("🌍 Synchroniser Marché", ACCENT_CYAN);
        btnScrape.addActionListener(e -> startScraping());

        searchContainer.add(searchField, BorderLayout.CENTER);
        searchContainer.add(btnScrape, BorderLayout.EAST);

        topRow.add(logo, BorderLayout.WEST);
        topRow.add(searchContainer, BorderLayout.CENTER);

        // Row 2: Filters
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterRow.setOpaque(false);

        JLabel lblFilter = new JLabel("Filtrer par :");
        lblFilter.setForeground(TEXT_SECONDARY);
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));

        comboCategory = createStyledComboBox("Toutes les Catégories");
        comboCity = createStyledComboBox("Toutes les Villes");

        JButton btnApplyFilter = createButton("Appliquer", ACCENT_BLUE);
        btnApplyFilter.setPreferredSize(new Dimension(120, 35));
        btnApplyFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnApplyFilter.addActionListener(e -> applyFilters());

        JButton btnReset = createButton("↺", BUTTON_INACTIVE);
        btnReset.setPreferredSize(new Dimension(40, 35));
        btnReset.addActionListener(e -> resetFilters());

        filterRow.add(lblFilter);
        filterRow.add(comboCategory);
        filterRow.add(comboCity);
        filterRow.add(btnApplyFilter);
        filterRow.add(btnReset);

        header.add(topRow, BorderLayout.NORTH);
        header.add(filterRow, BorderLayout.CENTER);

        return header;
    }

    // --- CANDIDATE PANEL ---
    private JPanel createCandidatePanel() {
        JPanel panel = new RoundedPanel(15, PANEL_GLASS);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("PROFIL CANDIDAT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(TEXT_SECONDARY);
        panel.add(lblTitle, BorderLayout.NORTH);

        cvArea = new JTextArea();
        cvArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        cvArea.setBackground(BG_DARK);
        cvArea.setForeground(new Color(167, 243, 208));
        cvArea.setCaretColor(Color.WHITE);
        cvArea.setLineWrap(true);
        cvArea.setWrapStyleWord(true);
        cvArea.setText("// Importez un PDF pour commencer le matching...");

        JScrollPane scroll = new JScrollPane(cvArea);
        scroll.setBorder(new LineBorder(new Color(71, 85, 105)));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);

        JButton btnImport = createButton("📂 Importer PDF", ACCENT_BLUE);
        btnImport.setPreferredSize(new Dimension(0, 50));
        btnImport.addActionListener(e -> importPDF());

        btnMatch = createButton("⚡ Recalculer Match", BUTTON_INACTIVE);
        btnMatch.setEnabled(false);
        btnMatch.setForeground(new Color(165, 180, 252));
        btnMatch.setPreferredSize(new Dimension(0, 50));
        btnMatch.addActionListener(e -> runMatching());

        actions.add(btnImport);
        actions.add(btnMatch);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    // --- RESULTS PANEL ---
    private JPanel createResultsPanel() {
        JPanel panel = new RoundedPanel(15, PANEL_GLASS);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("OPPORTUNITÉS CIBLÉES");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(TEXT_SECONDARY);
        panel.add(lblTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Offre"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(90);
        resultsTable.setDefaultRenderer(Object.class, new JobCardRenderer());
        resultsTable.setTableHeader(null);
        resultsTable.setBackground(BG_DARK);
        resultsTable.setShowGrid(false);
        resultsTable.setIntercellSpacing(new Dimension(0, 5));

        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.getSelectedRow();
                    if (row >= 0) {
                        try {
                            JobWrapper w = (JobWrapper) tableModel.getValueAt(row, 0);
                            Desktop.getDesktop().browse(java.net.URI.create(w.job.getUrl()));
                        } catch(Exception ex) {}
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        statusLabel = new JLabel("Système prêt.");
        statusLabel.setForeground(TEXT_SECONDARY);
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 5));
        progressBar.setForeground(ACCENT_CYAN);
        progressBar.setBackground(BG_DARK);
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(progressBar, BorderLayout.EAST);
        return bar;
    }

    // ================= BUSINESS LOGIC =================

    private void startScraping() {
        String query = searchField.getText();
        if (query.contains("Importez")) query = ""; // Scraping global

        statusLabel.setText("Scan du marché en cours...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        String finalQuery = query;
        new Thread(() -> {
            List<Offre> jobs = new ArrayList<>();
            // Robot 1
            try { jobs.addAll(new RekruteScraper().scrape(finalQuery)); } catch(Exception e){}
            // Robot 2
            try { jobs.addAll(new EmploiMaScraper().scrape(finalQuery)); } catch(Exception e){}
            // Robot 3
            try { jobs.addAll(new MarocAnnoncesScraper().scrape(finalQuery)); } catch(Exception e){}

            // Save new jobs
            for (Offre j : jobs) try { jobDao.saveJob(j); } catch(Exception e){}

            // Reload FULL list from DB to include history
            List<Offre> totalJobs = jobDao.getAllJobs();

            SwingUtilities.invokeLater(() -> {
                this.allJobs = totalJobs;
                this.displayedJobs = new ArrayList<>(totalJobs);
                populateFilters();

                // Keep the current match scores if a CV is already uploaded
                updateTable(this.currentCVText);

                statusLabel.setText("Sync terminée. " + totalJobs.size() + " offres en base.");
                progressBar.setVisible(false);
            });
        }).start();
    }

    private void populateFilters() {
        Set<String> categories = new HashSet<>();
        Set<String> cities = new HashSet<>();
        String[] blacklist = {"CDI", "Salaire", "Mission", "Profil", "Bac", "Année", "Expérience"};

        for (Offre j : allJobs) {
            if (isValidData(j.getCategory(), 40, blacklist))
                categories.add(formatData(j.getCategory()));
            if (isValidData(j.getVille(), 25, blacklist))
                cities.add(formatData(j.getVille()));
        }

        comboCategory.removeAllItems();
        comboCategory.addItem("Toutes les Catégories");
        categories.stream().sorted().forEach(comboCategory::addItem);

        comboCity.removeAllItems();
        comboCity.addItem("Toutes les Villes");
        cities.stream().sorted().forEach(comboCity::addItem);
    }

    private boolean isValidData(String text, int maxLength, String[] blacklist) {
        if (text == null || text.trim().isEmpty()) return false;
        if (text.length() > maxLength) return false;
        for (String badWord : blacklist) {
            if (text.toLowerCase().contains(badWord.toLowerCase())) return false;
        }
        return true;
    }

    private String formatData(String text) {
        String clean = text.trim().toLowerCase();
        if (clean.isEmpty()) return "";
        return clean.substring(0, 1).toUpperCase() + clean.substring(1);
    }

    private void applyFilters() {
        String selectedCat = (String) comboCategory.getSelectedItem();
        String selectedCity = (String) comboCity.getSelectedItem();
        if (selectedCat == null || selectedCity == null) return;

        displayedJobs = allJobs.stream()
                .filter(j -> {
                    String jCat = formatData(j.getCategory() == null ? "" : j.getCategory());
                    String jCity = formatData(j.getVille() == null ? "" : j.getVille());
                    boolean matchCat = selectedCat.equals("Toutes les Catégories") || selectedCat.equals(jCat);
                    boolean matchCity = selectedCity.equals("Toutes les Villes") || selectedCity.equals(jCity);
                    return matchCat && matchCity;
                })
                .collect(Collectors.toList());

        updateTable(this.currentCVText);
        statusLabel.setText(displayedJobs.size() + " offres filtrées.");
    }

    private void resetFilters() {
        comboCategory.setSelectedIndex(0);
        comboCity.setSelectedIndex(0);
        displayedJobs = new ArrayList<>(allJobs);
        applyFilters();
    }

    // --- IMPORT PDF (This updates scores instantly) ---
    private void importPDF() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PDDocument doc = Loader.loadPDF(f)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);

                // 1. Update UI
                cvArea.setText(text.trim());
                cvArea.setCaretPosition(0);

                // 2. Save text for matching
                this.currentCVText = text;

                // 3. NLP Prediction
                String predictedQuery = smartParser.extractSearchQuery(text);
                searchField.setText(predictedQuery);

                // 4. Update Controls
                btnMatch.setEnabled(true);
                btnMatch.setBackground(ACCENT_PURPLE);
                btnMatch.setForeground(Color.WHITE);
                btnMatch.setText("⚡ Matcher : " + predictedQuery);

                statusLabel.setText("CV importé. Calcul des scores en cours...");

                // 5. RUN MATCHING IMMEDIATELY
                runMatching();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erreur lecture PDF");
            }
        }
    }

    private void runMatching() {
        if (currentCVText == null || currentCVText.startsWith("//")) return;

        statusLabel.setText("Optimisation de l'algorithme neuronal...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            // Sort the DISPLAYED jobs (so filtering is respected)
            displayedJobs.sort((j1, j2) -> {
                int s1 = neuralMatcher.calculateMatchScore(currentCVText, j1.getFullText());
                int s2 = neuralMatcher.calculateMatchScore(currentCVText, j2.getFullText());
                return Integer.compare(s2, s1);
            });

            SwingUtilities.invokeLater(() -> {
                updateTable(currentCVText);
                statusLabel.setText("Top candidats affichés.");
                progressBar.setVisible(false);
            });
        }).start();
    }

    private void updateTable(String cvText) {
        tableModel.setRowCount(0);
        for (Offre job : displayedJobs) {
            double score = 0;
            // Only calculate score if we have a valid CV
            if (cvText != null && !cvText.startsWith("//")) {
                score = neuralMatcher.calculateMatchScore(cvText, job.getFullText());
            }
            tableModel.addRow(new Object[]{ new JobWrapper(job, score) });
        }
    }

    // ================= UI HELPERS =================

    private void setupTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("ComboBox.selectionBackground", new Color(40, 55, 75));
            UIManager.put("ComboBox.background", BG_DARK);
            UIManager.put("ComboBox.foreground", Color.WHITE);
            UIManager.put("ComboBox.borderColor", new Color(71, 85, 105));
        } catch (Exception e) {}
    }

    private JComboBox<String> createStyledComboBox(String firstItem) {
        JComboBox<String> box = new JComboBox<>();
        box.addItem(firstItem);
        box.setPreferredSize(new Dimension(180, 35));
        box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return box;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(bg == Color.WHITE ? BG_DARK : Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    record JobWrapper(Offre job, double score) {}

    class JobCardRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus, int row, int col) {
            JobWrapper w = (JobWrapper) value;
            JPanel card = new RoundedPanel(10, isSel ? new Color(40, 55, 75) : new Color(30, 41, 59));
            card.setLayout(new BorderLayout(15, 0));
            card.setBorder(new EmptyBorder(10, 15, 10, 15));

            // Show score only if > 0
            JLabel lblScore = new JLabel(w.score > 0 ? String.format("%.0f%%", w.score) : "--");
            lblScore.setFont(new Font("Segoe UI", Font.BOLD, 18));
            Color scoreColor = w.score > 60 ? new Color(16, 185, 129) : (w.score > 20 ? new Color(245, 158, 11) : Color.GRAY);
            lblScore.setForeground(scoreColor);
            lblScore.setPreferredSize(new Dimension(60, 0));
            lblScore.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel info = new JPanel(new GridLayout(2, 1));
            info.setOpaque(false);
            JLabel title = new JLabel(w.job.getTitre());
            title.setFont(new Font("Segoe UI", Font.BOLD, 15));
            title.setForeground(Color.WHITE);
            JLabel details = new JLabel(w.job.getEntreprise() + " - " + w.job.getVille());
            details.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            details.setForeground(TEXT_SECONDARY);
            info.add(title);
            info.add(details);

            card.add(lblScore, BorderLayout.WEST);
            card.add(info, BorderLayout.CENTER);

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(card, BorderLayout.CENTER);
            wrapper.setBorder(new EmptyBorder(0, 0, 5, 0));
            return wrapper;
        }
    }

    static class RoundedPanel extends JPanel {
        private int r; private Color c;
        public RoundedPanel(int r, Color c) { this.r=r; this.c=c; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),r,r);
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGui().setVisible(true));
    }
}