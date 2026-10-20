package ma.fpo.campusroom.models;

import java.util.Comparator;

public record PropositionSalle(String agent, String salle, int capacite) {
  public static Comparator<PropositionSalle> ordre() {
    // L'effectif est identique pour toutes les offres : trier la capacité minimise les places
    // libres.
    return Comparator.comparingInt(PropositionSalle::capacite)
        .thenComparing(PropositionSalle::salle)
        .thenComparing(PropositionSalle::agent);
  }
}
