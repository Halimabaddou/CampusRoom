package ma.fpo.campusroom.containers;

import jade.wrapper.*;
import ma.fpo.campusroom.utils.Configuration;

public final class CoordinateurContainer {
  public static AgentContainer creer() throws StaleProxyException {
    AgentContainer c = MainContainer.secondaire("CoordinateurContainer");
    c.createNewAgent(Configuration.COORDINATEUR, "ma.fpo.campusroom.agents.CoordinateurAgent", null)
        .start();
    return c;
  }

  public static void main(String[] args) throws Exception {
    creer();
  }
}
