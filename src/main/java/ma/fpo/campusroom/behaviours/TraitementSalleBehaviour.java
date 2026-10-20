package ma.fpo.campusroom.behaviours;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.*;
import java.util.*;
import ma.fpo.campusroom.models.*;
import ma.fpo.campusroom.utils.*;

public class TraitementSalleBehaviour extends CyclicBehaviour {
  private final Salle salle;
  private final String mode;
  private final Map<String, Offre> offres = new HashMap<>();
  private final Map<String, Resultat> resultats = new HashMap<>();

  private record Offre(DemandeReservation demande, String token, long expiration) {}

  private record Resultat(int type, String contenu) {}

  public TraitementSalleBehaviour(Agent a, Salle salle, String mode) {
    super(a);
    this.salle = salle;
    this.mode = mode;
  }

  @Override
  public void action() {
    ACLMessage m = myAgent.receive(MessageTemplate.MatchOntology(Configuration.ONTOLOGY));
    if (m == null) {
      block();
      return;
    }
    if (m.getSender() == null || m.getConversationId() == null) return;
    String cle = m.getSender().getName() + "|" + m.getConversationId();
    try {
      if (m.getPerformative() == ACLMessage.CFP) {
        if (mode.equals("silencieux")) return;
        DemandeReservation d = MessageUtils.demande(m.getContent());
        Offre ancienne = offres.get(cle);
        if (ancienne != null && !ancienne.demande().equals(d))
          throw new IllegalArgumentException("Demande modifiée");
        long now = System.currentTimeMillis();
        if (m.getReplyByDate() == null || now > m.getReplyByDate().getTime()) return;
        if (resultats.containsKey(cle) || !salle.disponible(d)) {
          MessageUtils.envoyer(
              myAgent,
              MessageUtils.reponse(
                  m, ACLMessage.REFUSE, "Contraintes ou calendrier incompatibles"));
          return;
        }
        String token = ancienne == null ? UUID.randomUUID().toString() : ancienne.token();
        offres.put(
            cle, new Offre(d, token, m.getReplyByDate().getTime() + Configuration.CONFIRMATION_MS));
        ACLMessage rep =
            MessageUtils.reponse(
                m,
                ACLMessage.PROPOSE,
                MessageUtils.encode("salle", salle.nom(), "capacite", "" + salle.capacite()));
        rep.setReplyWith(token);
        MessageUtils.envoyer(myAgent, rep);
      } else if (m.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
        Offre o = offres.get(cle);
        if (o != null && Objects.equals(m.getInReplyTo(), o.token())) {
          offres.remove(cle);
          resultats.putIfAbsent(
              cle,
              new Resultat(
                  ACLMessage.FAILURE,
                  MessageUtils.resultat("ECHEC", salle.nom(), "Offre clôturée")));
        }
      } else if (m.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
        Offre o = offres.get(cle);
        if (o == null || !Objects.equals(m.getInReplyTo(), o.token()))
          throw new IllegalArgumentException("Offre inconnue ou jeton invalide");
        if (!o.demande().equals(MessageUtils.demande(m.getContent())))
          throw new IllegalArgumentException("Demande différente de l'offre");
        Resultat r = resultats.get(cle);
        if (r == null) {
          try {
            if (System.currentTimeMillis() > o.expiration())
              throw new IllegalStateException("Offre expirée");
            salle.reserver(cle, o.demande());
            r =
                new Resultat(
                    ACLMessage.INFORM,
                    MessageUtils.resultat(
                        "CONFIRMEE", salle.nom(), "Réservation confirmée par la salle"));
          } catch (IllegalStateException e) {
            r =
                new Resultat(
                    ACLMessage.FAILURE,
                    MessageUtils.resultat("ECHEC", salle.nom(), e.getMessage()));
          }
          resultats.put(cle, r);
        }
        if (!mode.equals("confirmation-perdue"))
          MessageUtils.envoyer(myAgent, MessageUtils.reponse(m, r.type(), r.contenu()));
      }
    } catch (RuntimeException e) {
      MessageUtils.envoyer(
          myAgent,
          MessageUtils.reponse(
              m,
              ACLMessage.FAILURE,
              MessageUtils.resultat("ECHEC", salle.nom(), "Message invalide : " + e.getMessage())));
    }
  }
}
