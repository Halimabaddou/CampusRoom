package ma.fpo.campusroom;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import ma.fpo.campusroom.models.*;
import ma.fpo.campusroom.utils.MessageUtils;
import org.junit.jupiter.api.Test;

public class ReglesTest {
  private DemandeReservation d(int debut, int fin, int n) {
    return new DemandeReservation(
        LocalDate.of(2026, 11, 1), LocalTime.of(debut, 0), LocalTime.of(fin, 0), n, true);
  }

  @Test
  void validation() {
    assertThrows(IllegalArgumentException.class, () -> d(10, 10, 30));
    assertThrows(IllegalArgumentException.class, () -> d(12, 10, 30));
    assertThrows(IllegalArgumentException.class, () -> d(10, 12, 0));
    assertThrows(IllegalArgumentException.class, () -> d(10, 12, -1));
  }

  @Test
  void intervallesSemiOuverts() {
    assertTrue(d(10, 12, 30).chevauche(d(11, 13, 30)));
    assertTrue(d(10, 14, 30).chevauche(d(11, 12, 30)));
    assertFalse(d(10, 12, 30).chevauche(d(12, 14, 30)));
  }

  @Test
  void joursDifferents() {
    DemandeReservation a = d(10, 12, 30);
    assertFalse(
        a.chevauche(new DemandeReservation(a.date().plusDays(1), a.debut(), a.fin(), 30, true)));
  }

  @Test
  void contraintes() {
    assertFalse(new Salle("A", 20, true).disponible(d(10, 12, 30)));
    assertFalse(new Salle("C", 60, false).disponible(d(10, 12, 30)));
    assertTrue(new Salle("B", 35, true).disponible(d(10, 12, 30)));
  }

  @Test
  void classementDeterministe() {
    var offres =
        List.of(
            new PropositionSalle("c", "C", 60),
            new PropositionSalle("b", "B", 35),
            new PropositionSalle("a", "A", 35));
    assertEquals("A", offres.stream().min(PropositionSalle.ordre()).orElseThrow().salle());
  }

  @Test
  void doublonIdempotent() {
    Salle s = new Salle("B", 35, true);
    Reservation a = s.reserver("r1", d(10, 12, 30));
    assertSame(a, s.reserver("r1", d(10, 12, 30)));
    assertEquals(1, s.nombreReservations());
    assertThrows(IllegalArgumentException.class, () -> s.reserver("r1", d(12, 14, 30)));
  }

  @Test
  void concurrence() throws Exception {
    Salle s = new Salle("B", 35, true);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch go = new CountDownLatch(1);
    try {
      List<Future<Boolean>> f = new ArrayList<>();
      for (String id : List.of("a", "b"))
        f.add(
            pool.submit(
                () -> {
                  go.await();
                  try {
                    s.reserver(id, d(10, 12, 30));
                    return true;
                  } catch (IllegalStateException e) {
                    return false;
                  }
                }));
      go.countDown();
      int ok = 0;
      for (var x : f) if (x.get()) ok++;
      assertEquals(1, ok);
      assertEquals(1, s.nombreReservations());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void allerRetourMessage() {
    DemandeReservation a = d(10, 12, 30);
    assertEquals(a, MessageUtils.demande(MessageUtils.demande(a)));
    assertThrows(IllegalArgumentException.class, () -> MessageUtils.demande("date=incorrecte"));
  }
}
