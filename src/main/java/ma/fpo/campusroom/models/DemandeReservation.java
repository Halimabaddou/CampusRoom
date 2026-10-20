package ma.fpo.campusroom.models;

import java.time.*;
import java.util.Objects;

/** Intervalle semi-ouvert : la fin n'appartient pas à la séance. */
public record DemandeReservation(
    LocalDate date, LocalTime debut, LocalTime fin, int participants, boolean projecteur) {
  public DemandeReservation {
    Objects.requireNonNull(date, "Date obligatoire");
    Objects.requireNonNull(debut, "Début obligatoire");
    Objects.requireNonNull(fin, "Fin obligatoire");
    if (!fin.isAfter(debut))
      throw new IllegalArgumentException("La fin doit être après le début, le même jour.");
    if (participants <= 0)
      throw new IllegalArgumentException("Le nombre de participants doit être positif.");
  }

  public boolean chevauche(DemandeReservation autre) {
    return date.equals(autre.date) && debut.isBefore(autre.fin) && autre.debut.isBefore(fin);
  }
}
