package ma.fpo.campusroom.utils;

public final class Configuration {
  private Configuration() {}

  public static final String SERVICE = "reservation-salle",
      ONTOLOGY = "campusroom-v1",
      TRACE = "campusroom-trace-v1",
      COORDINATEUR = "coordinateur";
  public static final long OFFRES_MS = Long.getLong("campus.offres.ms", 3000),
      CONFIRMATION_MS = Long.getLong("campus.confirmation.ms", 3000),
      CLIENT_MS = Long.getLong("campus.client.ms", 15000);

  public static String host() {
    return System.getProperty("campus.host", "127.0.0.1");
  }

  public static String port() {
    return System.getProperty("campus.port", "1099");
  }
}
