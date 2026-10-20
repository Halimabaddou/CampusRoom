package ma.fpo.campusroom.models;

import java.util.*;

/** État local d'un agent salle. Aucun accès du coordinateur à cet objet. */
public final class Salle {
  private final String nom;
  private final int capacite;
  private final boolean projecteur;
  private final Map<String, Reservation> reservations = new LinkedHashMap<>();

  public Salle(String nom, int capacite, boolean projecteur) {
    this.nom = nom;
    this.capacite = capacite;
    this.projecteur = projecteur;
  }

  public String nom() {
    return nom;
  }

  public int capacite() {
    return capacite;
  }

  public synchronized boolean disponible(DemandeReservation d) {
    return d.participants() <= capacite
        && (!d.projecteur() || projecteur)
        && reservations.values().stream().noneMatch(r -> r.demande().chevauche(d));
  }

  public synchronized Reservation reserver(String cle, DemandeReservation d) {
    Reservation existante = reservations.get(cle);
    if (existante != null) {
      if (!existante.demande().equals(d))
        throw new IllegalArgumentException("Identifiant réutilisé avec une autre demande.");
      return existante; // Idempotence : le doublon retrouve le résultat initial.
    }
    if (!disponible(d))
      throw new IllegalStateException("Salle indisponible lors de la confirmation.");
    Reservation r = new Reservation(cle, nom, d);
    reservations.put(cle, r);
    return r;
  }

  public synchronized int nombreReservations() {
    return reservations.size();
  }
}
