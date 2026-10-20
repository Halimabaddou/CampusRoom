package ma.fpo.campusroom.agents;

import jade.core.Agent;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import ma.fpo.campusroom.behaviours.TraitementSalleBehaviour;
import ma.fpo.campusroom.models.Salle;
import ma.fpo.campusroom.utils.Configuration;

public class SalleAgent extends Agent {
  @Override
  protected void setup() {
    Object[] a = getArguments();
    Salle salle =
        new Salle(
            (String) a[0],
            Integer.parseInt(a[1].toString()),
            Boolean.parseBoolean(a[2].toString()));
    DFAgentDescription d = new DFAgentDescription();
    d.setName(getAID());
    ServiceDescription s = new ServiceDescription();
    s.setType(Configuration.SERVICE);
    s.setName(salle.nom());
    d.addServices(s);
    try {
      DFService.register(this, d);
    } catch (FIPAException e) {
      System.err.println(e.getMessage());
      doDelete();
      return;
    }
    // Modes facultatifs réservés aux démonstrations de panne.
    String mode = a.length > 3 ? a[3].toString() : "normal";
    addBehaviour(new TraitementSalleBehaviour(this, salle, mode));
  }

  @Override
  protected void takeDown() {
    try {
      DFService.deregister(this);
    } catch (FIPAException e) {
      System.err.println(e.getMessage());
    }
  }
}
