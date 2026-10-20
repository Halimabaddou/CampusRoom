package ma.fpo.campusroom.containers;

import jade.core.*;
import jade.wrapper.AgentContainer;
import ma.fpo.campusroom.utils.Configuration;

public final class MainContainer {
  private MainContainer() {}

  public static AgentContainer creer(boolean gui) {
    ProfileImpl p = new ProfileImpl();
    p.setParameter(Profile.MAIN_HOST, Configuration.host());
    p.setParameter(Profile.MAIN_PORT, Configuration.port());
    p.setParameter(Profile.LOCAL_HOST, Configuration.host());
    p.setParameter(Profile.GUI, "" + gui);
    p.setParameter(Profile.NO_MTP, "true");
    return jade.core.Runtime.instance().createMainContainer(p);
  }

  public static AgentContainer secondaire(String nom) {
    ProfileImpl p = new ProfileImpl();
    p.setParameter(Profile.MAIN_HOST, Configuration.host());
    p.setParameter(Profile.MAIN_PORT, Configuration.port());
    p.setParameter(Profile.LOCAL_HOST, Configuration.host());
    p.setParameter(Profile.LOCAL_PORT, "0");
    p.setParameter(Profile.CONTAINER_NAME, nom);
    return jade.core.Runtime.instance().createAgentContainer(p);
  }

  public static void main(String[] args) {
    creer(true);
  }
}
