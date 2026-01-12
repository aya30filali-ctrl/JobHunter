package com.jobapp.main;

import com.formdev.flatlaf.FlatDarkLaf;
import com.jobapp.dao.CVDAO;
import com.jobapp.dao.JobDAO;
import com.jobapp.ml.SmartCVParser;
import com.jobapp.model.Offre;
import com.jobapp.scraping.EmploiMaScraper;
import com.jobapp.scraping.MarocAnnoncesScraper;
import com.jobapp.scraping.RekruteScraper;
import com.jobapp.service.NeuralMatchingEngine;
import com.jobapp.service.ScraperService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
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

    // --- PALETTE DE COULEURS PRO (V15) ---
    private final Color BG_MAIN = new Color(15, 23, 42);
    private final Color BG_PANEL = new Color(30, 41, 59);

    private final Color BTN_SYNC = new Color(79, 70, 229);
    private final Color BTN_IMPORT = new Color(14, 165, 233);
    private final Color BTN_MATCH = new Color(124, 58, 237); // Deep Purple
    private final Color BTN_RESET = new Color(239, 68, 68);

    private final Color TEXT_PRI = new Color(241, 245, 249);
    private final Color TEXT_SEC = new Color(148, 163, 184);
    private final Color TEXT_ACCENT = new Color(139, 92, 246);

    private JTextArea cvArea;
    private DefaultTableModel tableModel;
    private JTable resultsTable;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton btnMatch;
    private JLabel lblMatchStatus;

    private JComboBox<String> comboCategory;
    private JComboBox<String> comboCity;

    private final JobDAO jobDao = new JobDAO();
    private final CVDAO cvDao = new CVDAO();
    private final NeuralMatchingEngine neuralMatcher = new NeuralMatchingEngine();
    private final SmartCVParser smartParser = new SmartCVParser();
    private final ScraperService scraperService = new ScraperService();

    private List<Offre> allJobs = new ArrayList<>();
    private List<Offre> displayedJobs = new ArrayList<>();
    private String currentCVText = null;
    private int currentCvId = -1;

    public MainGui() {
        setupTheme();
        setTitle("JobHunter Pro - Dashboard");
        setSize(1440, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BG_MAIN);
        setContentPane(mainContainer);

        mainContainer.add(createHeader(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSidebar(), createMainContent());
        splitPane.setDividerLocation(360);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);
        mainContainer.add(splitPane, BorderLayout.CENTER);

        mainContainer.add(createStatusBar(), BorderLayout.SOUTH);

        loadJobsFromDb();
        scraperService.startAutoUpdate();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_MAIN);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));
        header.setPreferredSize(new Dimension(0, 80));

        JLabel logo = new JLabel(" ◈ JOBHUNTER");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        logo.setForeground(TEXT_ACCENT);

        JButton btnSync = createButton("Actualiser le Marché", BTN_SYNC);
        btnSync.setPreferredSize(new Dimension(220, 42));
        btnSync.addActionListener(e -> startGlobalScraping());

        header.add(logo, BorderLayout.WEST);
        header.add(btnSync, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 15));
        sidebar.setBackground(BG_MAIN);
        sidebar.setBorder(new EmptyBorder(0, 20, 20, 10));

        JLabel lblTitle = new JLabel("MON ESPACE CANDIDAT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(TEXT_SEC);
        lblTitle.setBorder(new EmptyBorder(0, 5, 5, 0));
        sidebar.add(lblTitle, BorderLayout.NORTH);

        cvArea = new JTextArea(
                "\n" +
                        "   Bienvenue dans votre espace.\n\n" +
                        "   Ce terminal utilise notre moteur neural V-Final\n" +
                        "   pour scanner le marché caché.\n\n" +
                        "   1.  Chargez votre profil professionnel (PDF).\n" +
                        "   2.  Activez l'algorithme de matching.\n" +
                        "   3.  Accédez aux opportunités haute précision.\n\n" +
                        "   Système en attente..."
        );
        cvArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cvArea.setBackground(BG_PANEL);
        cvArea.setForeground(TEXT_SEC);
        cvArea.setLineWrap(true);
        cvArea.setWrapStyleWord(true);
        cvArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        cvArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(cvArea);
        scroll.setBorder(new LineBorder(new Color(60, 70, 90), 1));
        sidebar.add(scroll, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new GridLayout(2, 1, 0, 12));
        actionsPanel.setOpaque(false);

        JButton btnImport = createButton("📂 IMPORTER MON CV", BTN_IMPORT);
        btnImport.setPreferredSize(new Dimension(0, 45));
        btnImport.addActionListener(e -> importPDF());

        btnMatch = createButton("LANCER L'ANALYSE", new Color(60, 70, 90));
        btnMatch.setEnabled(false);
        btnMatch.setPreferredSize(new Dimension(0, 45));
        btnMatch.addActionListener(e -> runMatching());

        lblMatchStatus = new JLabel("Aucun CV chargé.");
        lblMatchStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblMatchStatus.setForeground(TEXT_SEC);
        lblMatchStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        actionsPanel.add(btnImport);
        actionsPanel.add(btnMatch);

        JPanel bottomWrapper = new JPanel(new BorderLayout(0, 10));
        bottomWrapper.setOpaque(false);
        bottomWrapper.add(actionsPanel, BorderLayout.CENTER);
        bottomWrapper.add(lblMatchStatus, BorderLayout.SOUTH);

        sidebar.add(bottomWrapper, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setBackground(BG_MAIN);
        content.setBorder(new EmptyBorder(0, 15, 20, 30));

        JPanel filterBar = new RoundedPanel(12, BG_PANEL);
        filterBar.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 12));

        JLabel lblFilter = new JLabel("Filtrer par :");
        lblFilter.setForeground(TEXT_SEC);
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));

        comboCategory = createStyledComboBox("Toutes les Catégories");
        comboCity = createStyledComboBox("Toutes les Villes");

        JButton btnApply = createButton("OK", BTN_SYNC);
        btnApply.setPreferredSize(new Dimension(50, 32));
        btnApply.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnApply.addActionListener(e -> applyFilters());

        JButton btnReset = createButton("Reset", BTN_RESET);
        btnReset.setPreferredSize(new Dimension(60, 32));
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnReset.addActionListener(e -> resetFilters());

        filterBar.add(lblFilter);
        filterBar.add(comboCategory);
        filterBar.add(comboCity);
        filterBar.add(btnApply);
        filterBar.add(btnReset);

        content.add(filterBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Data"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(90);
        resultsTable.setDefaultRenderer(Object.class, new JobCardRenderer());
        resultsTable.setTableHeader(null);
        resultsTable.setBackground(BG_MAIN);
        resultsTable.setShowGrid(false);
        resultsTable.setIntercellSpacing(new Dimension(0, 10));

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
        scroll.getViewport().setBackground(BG_MAIN);

        content.add(scroll, BorderLayout.CENTER);

        return content;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_MAIN);
        bar.setBorder(new EmptyBorder(5, 30, 8, 30));

        statusLabel = new JLabel("Système prêt.");
        statusLabel.setForeground(TEXT_SEC);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 4));
        progressBar.setForeground(TEXT_ACCENT);
        progressBar.setBackground(BG_PANEL);
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(progressBar, BorderLayout.EAST);
        return bar;
    }

    private void loadJobsFromDb() {
        new Thread(() -> {
            List<Offre> saved = jobDao.getAllJobs();
            SwingUtilities.invokeLater(() -> {
                if (!saved.isEmpty()) {
                    allJobs = saved;
                    displayedJobs = new ArrayList<>(saved);
                    populateFilters();
                    updateTable(null);
                    statusLabel.setText(allJobs.size() + " offres chargées depuis la base.");
                } else {
                    statusLabel.setText("Aucune offre en base. Cliquez sur 'Actualiser le Marché'.");
                }
            });
        }).start();
    }

    private void startGlobalScraping() {
        statusLabel.setText("Connexion aux portails d'emploi en cours...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            String[] keywords = {"informatique", "finance", "commercial", "rh"};
            List<Offre> newJobs = new ArrayList<>();

            for(String kw : keywords) {
                try { newJobs.addAll(new RekruteScraper().scrape(kw)); } catch(Exception e){}
                try { newJobs.addAll(new EmploiMaScraper().scrape(kw)); } catch(Exception e){}
                try { newJobs.addAll(new MarocAnnoncesScraper().scrape(kw)); } catch(Exception e){}
            }

            int addedCount = 0;
            for (Offre j : newJobs) {
                try { if(jobDao.saveJob(j)) addedCount++; } catch(Exception e){}
            }

            List<Offre> total = jobDao.getAllJobs();
            int finalAddedCount = addedCount;

            SwingUtilities.invokeLater(() -> {
                this.allJobs = total;
                this.displayedJobs = new ArrayList<>(total);
                populateFilters();
                if(currentCVText != null) runMatching(); else updateTable(null);

                statusLabel.setText("Synchronisation terminée : " + total.size() + " offres disponibles.");
                progressBar.setVisible(false);

                if (finalAddedCount == 0) {
                    JOptionPane.showMessageDialog(this, "Vous êtes déjà à jour avec le marché !", "Information", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, finalAddedCount + " nouvelles offres ajoutées.", "Succès", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }).start();
    }

    private void importPDF() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PDDocument doc = Loader.loadPDF(f)) {
                String text = new PDFTextStripper().getText(doc);

                // UI Mise à jour texte
                cvArea.setText("\n   ✅ Profil Chargé : " + f.getName() + "\n\n   Analyse des compétences terminée.\n   Prêt pour le calcul vectoriel.");
                cvArea.setForeground(new Color(16, 185, 129));

                this.currentCVText = text;

                // SAUVEGARDE DB ASYNCHRONE
                new Thread(() -> {
                    this.currentCvId = cvDao.saveCV(text);
                }).start();

                String cat = smartParser.extractSearchQuery(text);
                lblMatchStatus.setText("Profil identifié : " + cat.toUpperCase());
                lblMatchStatus.setForeground(TEXT_ACCENT);

                btnMatch.setEnabled(true);
                btnMatch.setBackground(BTN_MATCH);
                btnMatch.setText("LANCER L'ANALYSE ⚡");

                addHoverEffect(btnMatch, BTN_MATCH);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la lecture du PDF.");
            }
        }
    }

    private void runMatching() {
        if (currentCVText == null) return;
        statusLabel.setText("Calcul des scores & Sauvegarde en cours...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        new Thread(() -> {
            displayedJobs.sort((j1, j2) -> {
                int s1 = neuralMatcher.calculateMatchScore(currentCVText, j1.getTitre(), j1.getDescription());
                int s2 = neuralMatcher.calculateMatchScore(currentCVText, j2.getTitre(), j2.getDescription());
                return Integer.compare(s2, s1);
            });

            if (this.currentCvId != -1) {
                for (Offre job : displayedJobs) {
                    int score = neuralMatcher.calculateMatchScore(currentCVText, job.getTitre(), job.getDescription());
                    if (score > 0) {
                        jobDao.saveMatch(this.currentCvId, job.getUrl(), score);
                    }
                }
            }

            SwingUtilities.invokeLater(() -> {
                updateTable(currentCVText);
                statusLabel.setText("Analyse terminée. Historique sauvegardé.");
                progressBar.setVisible(false);
            });
        }).start();
    }

    private void updateTable(String cvText) {
        tableModel.setRowCount(0);
        for (Offre job : displayedJobs) {
            double score = 0;
            if (cvText != null && !cvText.startsWith("\n")) {
                score = neuralMatcher.calculateMatchScore(cvText, job.getTitre(), job.getDescription());
            }
            tableModel.addRow(new Object[]{ new JobWrapper(job, score) });
        }
    }

    private void applyFilters() {
        String cat = (String) comboCategory.getSelectedItem();
        String city = (String) comboCity.getSelectedItem();
        displayedJobs = allJobs.stream().filter(j -> {
            boolean mCat = cat.equals("Toutes les Catégories") || (j.getCategory() != null && cat.equalsIgnoreCase(format(j.getCategory())));
            boolean mCity = city.equals("Toutes les Villes") || (j.getVille() != null && city.equalsIgnoreCase(format(j.getVille())));
            return mCat && mCity;
        }).collect(Collectors.toList());
        if(currentCVText != null) runMatching(); else updateTable(null);
    }

    private void resetFilters() {
        comboCategory.setSelectedIndex(0);
        comboCity.setSelectedIndex(0);
        displayedJobs = new ArrayList<>(allJobs);
        if(currentCVText != null) runMatching(); else updateTable(null);
    }

    private void populateFilters() {
        Set<String> cats = new HashSet<>();
        Set<String> cities = new HashSet<>();
        for(Offre j : allJobs) {
            if(j.getCategory() != null) cats.add(format(j.getCategory()));
            if(j.getVille() != null) cities.add(format(j.getVille()));
        }
        comboCategory.removeAllItems(); comboCategory.addItem("Toutes les Catégories");
        cats.stream().sorted().forEach(comboCategory::addItem);
        comboCity.removeAllItems(); comboCity.addItem("Toutes les Villes");
        cities.stream().sorted().forEach(comboCity::addItem);
    }
    private String format(String s) { return s.trim().substring(0, 1).toUpperCase() + s.trim().substring(1).toLowerCase(); }

    private void setupTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 12);
        } catch (Exception e) {}
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addHoverEffect(btn, bg);
        return btn;
    }

    private void addHoverEffect(JButton btn, Color originalColor) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if(btn.isEnabled()) btn.setBackground(originalColor.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if(btn.isEnabled()) btn.setBackground(originalColor);
            }
        });
    }

    private JComboBox<String> createStyledComboBox(String first) {
        JComboBox<String> box = new JComboBox<>();
        box.addItem(first);
        box.setPreferredSize(new Dimension(200, 32));
        return box;
    }

    record JobWrapper(Offre job, double score) {}

    class JobCardRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus, int row, int col) {
            JobWrapper w = (JobWrapper) value;
            JPanel card = new RoundedPanel(15, isSel ? new Color(51, 65, 85) : BG_PANEL);
            card.setLayout(new BorderLayout(20, 0));
            card.setBorder(new EmptyBorder(12, 20, 12, 20));

            JLabel lblScore = new JLabel(w.score > 0 ? String.format("%.0f%%", w.score) : "--");
            lblScore.setFont(new Font("Segoe UI", Font.BOLD, 22));
            Color cScore = Color.GRAY;
            if(w.score >= 70) cScore = BTN_MATCH;
            else if(w.score >= 40) cScore = TEXT_ACCENT;
            else if(w.score > 0) cScore = Color.ORANGE;
            lblScore.setForeground(cScore);
            lblScore.setPreferredSize(new Dimension(65, 0));
            lblScore.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
            info.setOpaque(false);

            JLabel title = new JLabel(w.job.getTitre());
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setForeground(TEXT_PRI);

            JLabel sub = new JLabel(w.job.getEntreprise() + "  •  " + w.job.getVille() + "  •  " + w.job.getCategory());
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            sub.setForeground(TEXT_SEC);

            info.add(title);
            info.add(sub);

            card.add(lblScore, BorderLayout.WEST);
            card.add(info, BorderLayout.CENTER);

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(card, BorderLayout.CENTER);
            wrapper.setBorder(new EmptyBorder(0, 0, 8, 0));
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