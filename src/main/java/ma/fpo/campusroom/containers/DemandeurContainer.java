package ma.fpo.campusroom.containers;

import jade.wrapper.*;

public final class DemandeurContainer {
  public static AgentContainer creer() throws StaleProxyException {
    AgentContainer c = MainContainer.secondaire("DemandeurContainer");
    c.createNewAgent("demandeur", "ma.fpo.campusroom.agents.DemandeurAgent", null).start();
    return c;
  }

  public static void main(String[] args) throws Exception {
    creer();
  }
}
