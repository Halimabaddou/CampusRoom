package ma.fpo.campusroom.gui;

import jade.gui.GuiEvent;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import ma.fpo.campusroom.agents.DemandeurAgent;
import ma.fpo.campusroom.models.DemandeReservation;

public class CampusRoomFrame extends JFrame {
  private final JTextField date = new JTextField(LocalDate.now().plusDays(1).toString(), 10),
      debut = new JTextField("10:00", 5),
      fin = new JTextField("12:00", 5),
      effectif = new JTextField("30", 4);
  private final JCheckBox projecteur = new JCheckBox("Vidéoprojecteur", true);
  private final JTextArea journal = new JTextArea();
  private final JLabel etat = new JLabel("Prêt — choisissez une date et un créneau.");
  private final DefaultTableModel table =
      new DefaultTableModel(
          new String[] {"ID", "Date", "Début", "Fin", "Effectif", "Salle", "Statut"}, 0) {
        public boolean isCellEditable(int r, int c) {
          return false;
        }
      };

  public CampusRoomFrame(DemandeurAgent agent) {
    super("CampusRoom — Réservation multi-agents");
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setSize(1100, 680);
    setLocationRelativeTo(null);
    JPanel contenu = new JPanel(new BorderLayout(12, 12));
    contenu.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
    setContentPane(contenu);
    JPanel haut = new JPanel(new BorderLayout(8, 12));
    JLabel titre = new JLabel("CampusRoom | Réserver une salle");
    titre.setFont(new Font("SansSerif", Font.BOLD, 23));
    haut.add(titre, BorderLayout.NORTH);
    JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
    form.add(new JLabel("Date (AAAA-MM-JJ)"));
    form.add(date);
    form.add(new JLabel("Début"));
    form.add(debut);
    form.add(new JLabel("Fin"));
    form.add(fin);
    form.add(new JLabel("Participants"));
    form.add(effectif);
    form.add(projecteur);
    JButton reserver = new JButton("Rechercher et réserver");
    form.add(reserver);
    haut.add(form, BorderLayout.CENTER);
    haut.add(etat, BorderLayout.SOUTH);
    contenu.add(haut, BorderLayout.NORTH);
    journal.setEditable(false);
    journal.setLineWrap(true);
    journal.setWrapStyleWord(true);
    JScrollPane log = new JScrollPane(journal);
    log.setBorder(BorderFactory.createTitledBorder("Explications et échanges des agents"));
    JScrollPane resultats = new JScrollPane(new JTable(table));
    resultats.setBorder(
        BorderFactory.createTitledBorder(
            "Résultats de cette session — seules les lignes CONFIRMEE sont réservées"));
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultats, log);
    split.setResizeWeight(.55);
    contenu.add(split, BorderLayout.CENTER);
    contenu.add(
        new JLabel(
            "Données en mémoire : perdues à l’arrêt. Statut INCERTAINE : vérifier avant de"
                + " recommencer."),
        BorderLayout.SOUTH);
    reserver.addActionListener(
        e -> {
          try {
            DemandeReservation d =
                new DemandeReservation(
                    LocalDate.parse(date.getText().trim()),
                    LocalTime.parse(debut.getText().trim()),
                    LocalTime.parse(fin.getText().trim()),
                    Integer.parseInt(effectif.getText().trim()),
                    projecteur.isSelected());
            GuiEvent event = new GuiEvent(this, 1);
            event.addParameter(d);
            agent.postGuiEvent(event);
            etat.setText("Demande envoyée aux agents…");
          } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Vérifie la date, les heures et l’effectif.\n" + ex.getMessage(),
                "Saisie invalide",
                JOptionPane.ERROR_MESSAGE);
          }
        });
    addWindowListener(
        new WindowAdapter() {
          public void windowClosed(WindowEvent e) {
            agent.postGuiEvent(new GuiEvent(this, 2));
          }
        });
  }

  public void journal(String text) {
    journal.append(text + "\n");
    journal.setCaretPosition(journal.getDocument().getLength());
  }

  public void resultat(
      String id, DemandeReservation d, String statut, String salle, String raison) {
    table.addRow(
        new Object[] {
          id.substring(0, 8), d.date(), d.debut(), d.fin(), d.participants(), salle, statut
        });
    etat.setText(statut + " — " + salle);
    journal(id.substring(0, 8) + " : " + raison);
  }
}
