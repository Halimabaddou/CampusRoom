package ma.fpo.campusroom.utils;

import jade.core.Agent;
import jade.lang.acl.*;
import java.io.*;
import java.time.*;
import java.util.*;
import ma.fpo.campusroom.models.*;

/** Contenu texte Properties : pas de désérialisation d'objets Java. */
public final class MessageUtils {
  private MessageUtils() {}

  public static String encode(String... pairs) {
    Properties p = new Properties();
    for (int i = 0; i < pairs.length; i += 2) p.setProperty(pairs[i], pairs[i + 1]);
    try {
      StringWriter s = new StringWriter();
      p.store(s, null);
      return s.toString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Properties decode(String text) {
    if (text == null || text.length() > 16000)
      throw new IllegalArgumentException("Contenu absent ou trop long");
    try {
      Properties p = new Properties();
      p.load(new StringReader(text));
      return p;
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String required(Properties p, String key) {
    String s = p.getProperty(key);
    if (s == null) throw new IllegalArgumentException("Champ absent : " + key);
    return s;
  }

  public static String demande(DemandeReservation d) {
    return encode(
        "date",
        d.date().toString(),
        "debut",
        d.debut().toString(),
        "fin",
        d.fin().toString(),
        "participants",
        "" + d.participants(),
        "projecteur",
        "" + d.projecteur());
  }

  public static DemandeReservation demande(String s) {
    Properties p = decode(s);
    String v = required(p, "projecteur");
    if (!v.equals("true") && !v.equals("false"))
      throw new IllegalArgumentException("Booléen invalide");
    return new DemandeReservation(
        LocalDate.parse(required(p, "date")),
        LocalTime.parse(required(p, "debut")),
        LocalTime.parse(required(p, "fin")),
        Integer.parseInt(required(p, "participants")),
        Boolean.parseBoolean(v));
  }

  public static String resultat(String statut, String salle, String raison) {
    return encode("statut", statut, "salle", salle, "raison", raison);
  }

  public static ACLMessage nouveau(int type, String id) {
    ACLMessage m = new ACLMessage(type);
    m.setOntology(Configuration.ONTOLOGY);
    m.setLanguage("properties-v1");
    m.setConversationId(id);
    return m;
  }

  public static ACLMessage reponse(ACLMessage original, int type, String contenu) {
    ACLMessage m = original.createReply();
    m.setPerformative(type);
    m.setContent(contenu);
    return m;
  }

  public static MessageTemplate type(int type) {
    return MessageTemplate.MatchPerformative(type);
  }

  public static MessageTemplate conversation(String id) {
    return MessageTemplate.and(
        MessageTemplate.MatchOntology(Configuration.ONTOLOGY),
        MessageTemplate.MatchConversationId(id));
  }

  public static void envoyer(Agent a, ACLMessage m) {
    System.out.println(
        a.getLocalName()
            + " -> "
            + ACLMessage.getPerformative(m.getPerformative())
            + " ["
            + m.getConversationId()
            + "]");
    a.send(m);
  }
}
