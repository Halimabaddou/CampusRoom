package ma.fpo.campusroom.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.*;
import java.util.*;
import ma.fpo.campusroom.behaviours.NegociationSalleBehaviour;
import ma.fpo.campusroom.models.DemandeReservation;
import ma.fpo.campusroom.utils.*;

public class CoordinateurAgent extends Agent {
  public final Set<String> conversationsActives = new HashSet<>();
  private final Map<String, DemandeReservation> demandes = new HashMap<>();
  private final Map<String, ACLMessage> terminees = new HashMap<>();

  @Override
  protected void setup() {
    addBehaviour(
        new CyclicBehaviour() {
          public void action() {
            ACLMessage m =
                receive(
                    MessageTemplate.and(
                        MessageTemplate.MatchOntology(Configuration.ONTOLOGY),
                        MessageUtils.type(ACLMessage.REQUEST)));
            if (m == null) {
              block();
              return;
            }
            try {
              UUID.fromString(m.getConversationId());
              DemandeReservation d = MessageUtils.demande(m.getContent());
              String key = cle(m);
              if (demandes.containsKey(key)) {
                if (!demandes.get(key).equals(d))
                  throw new IllegalArgumentException(
                      "Identifiant déjà utilisé pour une autre demande");
                ACLMessage r = terminees.get(key);
                if (r != null)
                  MessageUtils.envoyer(
                      myAgent, MessageUtils.reponse(m, r.getPerformative(), r.getContent()));
                return; // Doublon en cours : une seule négociation reste active.
              }
              demandes.put(key, d);
              addBehaviour(new NegociationSalleBehaviour(CoordinateurAgent.this, m, d));
            } catch (RuntimeException e) {
              MessageUtils.envoyer(
                  myAgent,
                  MessageUtils.reponse(
                      m,
                      ACLMessage.FAILURE,
                      MessageUtils.resultat("ECHEC", "", "Demande invalide : " + e.getMessage())));
            }
          }
        });
    // Les anciennes réponses sont retirées sans être attribuées à une nouvelle demande.
    addBehaviour(
        new CyclicBehaviour() {
          public void action() {
            ACLMessage ancien =
                receive(
                    new MessageTemplate(
                        m ->
                            Configuration.ONTOLOGY.equals(m.getOntology())
                                && m.getPerformative() != ACLMessage.REQUEST
                                && !conversationsActives.contains(m.getConversationId())));
            if (ancien == null) block();
            else System.out.println("Réponse tardive ignorée : " + ancien.getConversationId());
          }
        });
  }

  public static String cle(ACLMessage m) {
    return m.getSender().getName() + "|" + m.getConversationId();
  }

  public void terminer(ACLMessage demande, int type, String contenu) {
    ACLMessage r = MessageUtils.reponse(demande, type, contenu);
    terminees.put(cle(demande), r);
    MessageUtils.envoyer(this, r);
  }

  public void trace(ACLMessage demande, String text) {
    ACLMessage m = MessageUtils.reponse(demande, ACLMessage.INFORM, text);
    m.setOntology(Configuration.TRACE);
    send(m);
    System.out.println(text);
  }
}
