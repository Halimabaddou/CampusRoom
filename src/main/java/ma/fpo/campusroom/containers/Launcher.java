package ma.fpo.campusroom.containers;

/** Lanceur complet : lancer cette classe, pas jade.Boot avec un conteneur comme agent. */
public final class Launcher {
  public static void main(String[] args) {
    try {
      MainContainer.creer(true);
      SallesContainer.creer();
      CoordinateurContainer.creer();
      DemandeurContainer.creer();
    } catch (Exception e) {
      System.err.println(
          "Démarrage impossible. Vérifier le port " + ma.fpo.campusroom.utils.Configuration.port());
      e.printStackTrace();
      jade.core.Runtime.instance().shutDown();
    }
  }
}
