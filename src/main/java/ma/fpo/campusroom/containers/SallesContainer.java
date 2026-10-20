package ma.fpo.campusroom.containers;

import jade.wrapper.*;

public final class SallesContainer {
  public static AgentContainer creer() throws StaleProxyException {
    AgentContainer c = MainContainer.secondaire("SallesContainer");
    c.createNewAgent(
            "salleA",
            "ma.fpo.campusroom.agents.SalleAgent",
            new Object[] {"Salle A", 20, true, System.getProperty("campus.salleA.mode", "normal")})
        .start();
    c.createNewAgent(
            "salleB",
            "ma.fpo.campusroom.agents.SalleAgent",
            new Object[] {"Salle B", 35, true, System.getProperty("campus.salleB.mode", "normal")})
        .start();
    c.createNewAgent(
            "salleC",
            "ma.fpo.campusroom.agents.SalleAgent",
            new Object[] {"Salle C", 60, false, System.getProperty("campus.salleC.mode", "normal")})
        .start();
    return c;
  }

  public static void main(String[] args) throws Exception {
    creer();
  }
}
