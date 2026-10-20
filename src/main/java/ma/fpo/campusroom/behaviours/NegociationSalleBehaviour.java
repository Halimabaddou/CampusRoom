package ma.fpo.campusroom.behaviours;

import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;
import java.util.*;
import ma.fpo.campusroom.agents.CoordinateurAgent;
import ma.fpo.campusroom.models.*;
import ma.fpo.campusroom.utils.*;

/** Machine à états : découverte, collecte, attente confirmation, fin. */
public class NegociationSalleBehaviour extends Behaviour {
  private final CoordinateurAgent coord;
  private final ACLMessage requete;
  private final DemandeReservation demande;
  // Un ID interne isole aussi deux demandeurs qui auraient utilisé le même ID externe.
  private final String id = UUID.randomUUID().toString(),
      cfpToken = UUID.randomUUID().toString(),
      acceptToken = UUID.randomUUID().toString();
  private final Set<String> attendus = new HashSet<>(), repondus = new HashSet<>();
  private final Map<String, ACLMessage> messages = new HashMap<>();
  private final List<PropositionSalle> offres = new ArrayList<>();
  private int etat = 0;
  private long echeance;
  private PropositionSalle choisie;

  public NegociationSalleBehaviour(
      CoordinateurAgent a, ACLMessage requete, DemandeReservation demande) {
    super(a);
    coord = a;
    this.requete = requete;
    this.demande = demande;
    coord.conversationsActives.add(id);
  }

  @Override
  public void action() {
    if (etat == 0) {
      try {
        DFAgentDescription modele = new DFAgentDescription();
        ServiceDescription s = new ServiceDescription();
        s.setType(Configuration.SERVICE);
        modele.addServices(s);
        DFAgentDescription[] salles = DFService.search(myAgent, modele);
        ACLMessage cfp = MessageUtils.nouveau(ACLMessage.CFP, id);
        cfp.setContent(MessageUtils.demande(demande));
        cfp.setReplyWith(cfpToken);
        echeance = System.currentTimeMillis() + Configuration.OFFRES_MS;
        cfp.setReplyByDate(new Date(echeance));
        for (DFAgentDescription salle : salles) {
          cfp.addReceiver(salle.getName());
          attendus.add(salle.getName().getName());
        }
        if (attendus.isEmpty()) {
          fin("ECHEC", "", "Aucune salle découverte dans le DF.");
          return;
        }
        coord.trace(requete, "CFP envoyé à " + attendus.size() + " salle(s).");
        MessageUtils.envoyer(myAgent, cfp);
        etat = 1;
      } catch (FIPAException e) {
        fin("ECHEC", "", "Recherche DF impossible : " + e.getMessage());
      }
    } else if (etat == 1) {
      if (System.currentTimeMillis() >= echeance) {
        selectionner();
        return;
      }
      ACLMessage m =
          myAgent.receive(
              MessageTemplate.and(
                  MessageUtils.conversation(id), MessageTemplate.MatchInReplyTo(cfpToken)));
      if (m == null) {
        block(Math.max(1, echeance - System.currentTimeMillis()));
        return;
      }
      String sender = m.getSender().getName();
      if (!attendus.contains(sender) || repondus.contains(sender)) return;
      if (m.getPerformative() != ACLMessage.PROPOSE
          && m.getPerformative() != ACLMessage.REFUSE
          && m.getPerformative() != ACLMessage.FAILURE) return;
      repondus.add(sender);
      if (m.getPerformative() == ACLMessage.PROPOSE) {
        try {
          Properties p = MessageUtils.decode(m.getContent());
          int cap = Integer.parseInt(MessageUtils.required(p, "capacite"));
          if (cap < demande.participants() || m.getReplyWith() == null)
            throw new IllegalArgumentException();
          PropositionSalle o = new PropositionSalle(sender, MessageUtils.required(p, "salle"), cap);
          offres.add(o);
          messages.put(sender, m);
          coord.trace(requete, "PROPOSE : " + o.salle() + ", " + cap + " places.");
        } catch (RuntimeException e) {
          coord.trace(requete, "Proposition invalide ignorée.");
        }
      } else coord.trace(requete, "REFUSE/FAILURE : " + m.getSender().getLocalName());
      if (repondus.size() == attendus.size()) selectionner();
    } else if (etat == 2) {
      if (System.currentTimeMillis() >= echeance) {
        fin(
            "INCERTAINE",
            choisie.salle(),
            "Confirmation manquante. Ne pas refaire la demande : vérifier auprès de la salle.");
        return;
      }
      MessageTemplate t =
          MessageTemplate.and(
              MessageUtils.conversation(id),
              MessageTemplate.and(
                  MessageTemplate.MatchSender(new AID(choisie.agent(), AID.ISGUID)),
                  MessageTemplate.MatchInReplyTo(acceptToken)));
      ACLMessage m = myAgent.receive(t);
      if (m == null) {
        block(Math.max(1, echeance - System.currentTimeMillis()));
        return;
      }
      if (m.getPerformative() == ACLMessage.INFORM) {
        try {
          Properties p = MessageUtils.decode(m.getContent());
          if (!"CONFIRMEE".equals(p.getProperty("statut"))
              || !choisie.salle().equals(p.getProperty("salle")))
            throw new IllegalArgumentException();
          fin(
              "CONFIRMEE",
              choisie.salle(),
              "Salle compatible avec le moins de places inutilisées : "
                  + (choisie.capacite() - demande.participants())
                  + ". Confirmation reçue.");
        } catch (RuntimeException e) {
          fin(
              "INCERTAINE",
              choisie.salle(),
              "Confirmation illisible. Vérification manuelle nécessaire.");
        }
      } else if (m.getPerformative() == ACLMessage.FAILURE)
        fin(
            "ECHEC",
            choisie.salle(),
            "La salle a refusé la réservation au dernier contrôle. Aucune réaffectation"
                + " automatique.");
    }
  }

  private void selectionner() {
    coord.trace(
        requete, "Collecte terminée : " + repondus.size() + "/" + attendus.size() + " réponses.");
    if (offres.isEmpty()) {
      fin("ECHEC", "", "Aucune offre compatible reçue dans le délai.");
      return;
    }
    choisie = offres.stream().min(PropositionSalle.ordre()).orElseThrow();
    echeance = System.currentTimeMillis() + Configuration.CONFIRMATION_MS;
    for (PropositionSalle o : offres) {
      boolean ok = o.equals(choisie);
      ACLMessage m =
          MessageUtils.reponse(
              messages.get(o.agent()),
              ok ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL,
              MessageUtils.demande(demande));
      if (ok) {
        m.setReplyWith(acceptToken);
        m.setReplyByDate(new Date(echeance));
      }
      MessageUtils.envoyer(myAgent, m);
    }
    coord.trace(requete, "ACCEPT_PROPOSAL : " + choisie.salle() + " ; attente de confirmation.");
    etat = 2;
  }

  private void fin(String statut, String salle, String raison) {
    coord.terminer(
        requete,
        statut.equals("CONFIRMEE") ? ACLMessage.INFORM : ACLMessage.FAILURE,
        MessageUtils.resultat(statut, salle, raison));
    etat = 3;
  }

  @Override
  public int onEnd() {
    coord.conversationsActives.remove(id);
    return 0;
  }

  @Override
  public boolean done() {
    return etat == 3;
  }
}
