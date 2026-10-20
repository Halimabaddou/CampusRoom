package ma.fpo.campusroom.agents;

import jade.gui.*;
import javax.swing.SwingUtilities;
import ma.fpo.campusroom.behaviours.DemandeReservationBehaviour;
import ma.fpo.campusroom.gui.CampusRoomFrame;
import ma.fpo.campusroom.models.DemandeReservation;

public class DemandeurAgent extends GuiAgent {
  private CampusRoomFrame frame;

  @Override
  protected void setup() {
    SwingUtilities.invokeLater(
        () -> {
          frame = new CampusRoomFrame(this);
          frame.setVisible(true);
        });
  }

  @Override
  protected void onGuiEvent(GuiEvent event) {
    if (event.getType() == 1)
      addBehaviour(
          new DemandeReservationBehaviour(this, (DemandeReservation) event.getParameter(0)));
    if (event.getType() == 2) doDelete();
  }

  public void journal(String text) {
    SwingUtilities.invokeLater(
        () -> {
          if (frame != null) frame.journal(text);
        });
  }

  public void resultat(
      String id, DemandeReservation d, String statut, String salle, String raison) {
    SwingUtilities.invokeLater(
        () -> {
          if (frame != null) frame.resultat(id, d, statut, salle, raison);
        });
  }

  @Override
  protected void takeDown() {
    SwingUtilities.invokeLater(
        () -> {
          if (frame != null) frame.dispose();
        });
  }
}
