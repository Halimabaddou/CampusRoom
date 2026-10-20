package ma.fpo.campusroom;

import static org.junit.jupiter.api.Assertions.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;
import jade.wrapper.AgentContainer;
import java.net.ServerSocket;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import ma.fpo.campusroom.containers.*;
import ma.fpo.campusroom.models.DemandeReservation;
import ma.fpo.campusroom.utils.*;
import org.junit.jupiter.api.*;

/** Vrais agents JADE et vrais messages ; aucune simulation de la bibliothèque. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationJadeTest {
  static AgentContainer main, rooms, coord;
  static final BlockingQueue<Commande> commandes = new LinkedBlockingQueue<>();
  static final BlockingQueue<ACLMessage> retours = new LinkedBlockingQueue<>();
  static final CountDownLatch pret = new CountDownLatch(1);

  record Commande(String id, DemandeReservation demande, int copies) {}

  public static class ClientTest extends Agent {
    protected void setup() {
      addBehaviour(
          new CyclicBehaviour() {
            boolean ready = false;

            public void action() {
              if (!ready) {
                try {
                  DFAgentDescription t = new DFAgentDescription();
                  ServiceDescription s = new ServiceDescription();
                  s.setType(Configuration.SERVICE);
                  t.addServices(s);
                  if (DFService.search(myAgent, t).length >= 3) {
                    ready = true;
                    pret.countDown();
                  } else {
                    block(50);
                    return;
                  }
                } catch (FIPAException e) {
                  block(50);
                  return;
                }
              }
              Commande c = commandes.poll();
              if (c != null)
                for (int i = 0; i < c.copies(); i++) {
                  ACLMessage m = MessageUtils.nouveau(ACLMessage.REQUEST, c.id());
                  m.setReplyWith(c.id());
                  m.addReceiver(new AID(Configuration.COORDINATEUR, AID.ISLOCALNAME));
                  m.setContent(MessageUtils.demande(c.demande()));
                  send(m);
                }
              ACLMessage m = receive(MessageTemplate.MatchOntology(Configuration.ONTOLOGY));
              if (m != null) retours.add(m);
              ACLMessage trace = receive(MessageTemplate.MatchOntology(Configuration.TRACE));
              if (c == null && m == null && trace == null) block(20);
            }
          });
    }
  }

  @BeforeAll
  static void start() throws Exception {
    try (ServerSocket s = new ServerSocket(0)) {
      System.setProperty("campus.port", "" + s.getLocalPort());
    }
    System.setProperty("campus.offres.ms", "600");
    System.setProperty("campus.confirmation.ms", "600");
    main = MainContainer.creer(false);
    rooms = SallesContainer.creer();
    coord = CoordinateurContainer.creer();
    main.createNewAgent("testeur", ClientTest.class.getName(), null).start();
    assertTrue(pret.await(10, TimeUnit.SECONDS), "Enregistrement DF");
  }

  static DemandeReservation d(int jour, int n, boolean p) {
    return new DemandeReservation(
        LocalDate.of(2026, 11, jour), LocalTime.of(10, 0), LocalTime.of(12, 0), n, p);
  }

  static String envoi(DemandeReservation d, int copies) {
    String id = UUID.randomUUID().toString();
    commandes.add(new Commande(id, d, copies));
    return id;
  }

  static Properties resultat(String id) throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
    while (System.nanoTime() < end) {
      ACLMessage m = retours.poll(1, TimeUnit.SECONDS);
      if (m != null && id.equals(m.getConversationId())) return MessageUtils.decode(m.getContent());
    }
    throw new AssertionError("Aucun résultat pour " + id);
  }

  @Test
  @Order(1)
  void reussiteEtMeilleureOffre() throws Exception {
    Properties p = resultat(envoi(d(1, 15, true), 1));
    assertEquals("CONFIRMEE", p.getProperty("statut"));
    assertEquals("Salle A", p.getProperty("salle"));
  }

  @Test
  @Order(2)
  void aucuneOffre() throws Exception {
    assertEquals("ECHEC", resultat(envoi(d(2, 70, true), 1)).getProperty("statut"));
  }

  @Test
  @Order(3)
  void deuxDemandesConcurrentes() throws Exception {
    String a = envoi(d(3, 30, true), 1), b = envoi(d(3, 30, true), 1);
    Map<String, String> r = new HashMap<>();
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
    while (r.size() < 2 && System.nanoTime() < end) {
      ACLMessage m = retours.poll(1, TimeUnit.SECONDS);
      if (m != null && (m.getConversationId().equals(a) || m.getConversationId().equals(b)))
        r.put(m.getConversationId(), MessageUtils.decode(m.getContent()).getProperty("statut"));
    }
    assertEquals(2, r.size());
    assertEquals(1, r.values().stream().filter("CONFIRMEE"::equals).count());
    assertEquals(1, r.values().stream().filter("ECHEC"::equals).count());
  }

  @Test
  @Order(4)
  void doublonRejoue() throws Exception {
    DemandeReservation d = d(4, 30, true);
    String id = envoi(d, 2);
    assertEquals("CONFIRMEE", resultat(id).getProperty("statut"));
    commandes.add(new Commande(id, d, 1));
    assertEquals("CONFIRMEE", resultat(id).getProperty("statut"));
    assertEquals("ECHEC", resultat(envoi(d, 1)).getProperty("statut"));
  }

  @Test
  @Order(5)
  void salleSilencieuse() throws Exception {
    rooms.getAgent("salleB").kill();
    rooms
        .createNewAgent(
            "salleMuette",
            "ma.fpo.campusroom.agents.SalleAgent",
            new Object[] {"Salle muette", 35, true, "silencieux"})
        .start();
    // Une barrière de disponibilité est effectuée côté test pour la nouvelle salle.
    awaitService("salleMuette");
    assertEquals("ECHEC", resultat(envoi(d(5, 30, true), 1)).getProperty("statut"));
  }

  @Test
  @Order(6)
  void confirmationPerdueResteIncertaine() throws Exception {
    rooms.getAgent("salleMuette").kill();
    rooms
        .createNewAgent(
            "salleIncertaine",
            "ma.fpo.campusroom.agents.SalleAgent",
            new Object[] {"Salle incertaine", 35, true, "confirmation-perdue"})
        .start();
    awaitService("salleIncertaine");
    assertEquals("INCERTAINE", resultat(envoi(d(6, 30, true), 1)).getProperty("statut"));
    assertEquals(
        "ECHEC",
        resultat(envoi(d(6, 30, true), 1)).getProperty("statut"),
        "La première réservation a été enregistrée malgré la confirmation perdue");
  }

  // Le test n'accède pas aux calendriers : barrière via un agent temporaire et le DF.
  static void awaitService(String name) throws Exception {
    CountDownLatch signal = new CountDownLatch(1);
    main.createNewAgent("barriere" + name, Barriere.class.getName(), new Object[] {name, signal})
        .start();
    assertTrue(signal.await(5, TimeUnit.SECONDS));
  }

  public static class Barriere extends Agent {
    protected void setup() {
      String name = (String) getArguments()[0];
      CountDownLatch signal = (CountDownLatch) getArguments()[1];
      addBehaviour(
          new CyclicBehaviour() {
            public void action() {
              try {
                DFAgentDescription t = new DFAgentDescription();
                t.setName(new AID(name, AID.ISLOCALNAME));
                if (DFService.search(myAgent, t).length > 0) {
                  signal.countDown();
                  doDelete();
                } else block(20);
              } catch (FIPAException e) {
                block(20);
              }
            }
          });
    }
  }

  @AfterAll
  static void stop() {
    jade.core.Runtime.instance().shutDown();
  }
}
