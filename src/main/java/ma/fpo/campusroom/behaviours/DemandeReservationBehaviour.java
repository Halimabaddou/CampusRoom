package ma.fpo.campusroom.behaviours;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.*;
import java.util.*;
import ma.fpo.campusroom.agents.DemandeurAgent;
import ma.fpo.campusroom.models.DemandeReservation;
import ma.fpo.campusroom.utils.*;

public class DemandeReservationBehaviour extends Behaviour {
  private final DemandeurAgent agent;
  private final DemandeReservation demande;
  private final String id = UUID.randomUUID().toString();
  private long echeance;
  private boolean envoye = false, termine = false;

  public DemandeReservationBehaviour(DemandeurAgent a, DemandeReservation d) {
    super(a);
    agent = a;
    demande = d;
  }

  public void action() {
    if (!envoye) {
      ACLMessage m = MessageUtils.nouveau(ACLMessage.REQUEST, id);
      m.addReceiver(new AID(Configuration.COORDINATEUR, AID.ISLOCALNAME));
      m.setContent(MessageUtils.demande(demande));
      m.setReplyWith(id);
      MessageUtils.envoyer(agent, m);
      agent.journal("REQUEST " + id + " : " + demande);
      echeance = System.currentTimeMillis() + Configuration.CLIENT_MS;
      envoye = true;
    }
    if (System.currentTimeMillis() >= echeance) {
      agent.resultat(
          id,
          demande,
          "INCERTAINE",
          "",
          "Coordinateur sans réponse : vérifier avant de renouveler.");
      termine = true;
      return;
    }
    MessageTemplate t =
        MessageTemplate.and(
            MessageTemplate.MatchConversationId(id),
            MessageTemplate.and(
                MessageTemplate.MatchSender(new AID(Configuration.COORDINATEUR, AID.ISLOCALNAME)),
                MessageTemplate.or(
                    MessageTemplate.MatchOntology(Configuration.ONTOLOGY),
                    MessageTemplate.MatchOntology(Configuration.TRACE))));
    ACLMessage m = agent.receive(t);
    if (m == null) {
      block(Math.max(1, echeance - System.currentTimeMillis()));
      return;
    }
    if (Configuration.TRACE.equals(m.getOntology())) {
      agent.journal(m.getContent());
      return;
    }
    if (m.getPerformative() != ACLMessage.INFORM && m.getPerformative() != ACLMessage.FAILURE)
      return;
    try {
      Properties p = MessageUtils.decode(m.getContent());
      agent.resultat(
          id,
          demande,
          MessageUtils.required(p, "statut"),
          p.getProperty("salle", ""),
          MessageUtils.required(p, "raison"));
    } catch (RuntimeException e) {
      agent.resultat(id, demande, "INCERTAINE", "", "Résultat illisible : vérifier manuellement.");
    }
    termine = true;
  }

  public boolean done() {
    return termine;
  }
}
