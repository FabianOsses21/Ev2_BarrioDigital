package cl.barriodigital.common;
public final class MessagingNames {
  private MessagingNames(){}
  public static final String EX_DIRECT="cmd.direct";
  public static final String EX_TOPIC="cmd.topic";
  public static final String EX_DEAD="cmd.dead.dlx";
  public static final String Q_EMAIL="q.cmd.email";
  public static final String Q_AUDIT="q.cmd.2";
  public static final String Q_TRACKING="q.cmd.3";
  public static final String RK_EMAIL="email.send";
  public static final String RK_AUDIT="tramite.audit.";
  public static final String RK_TRACKING="tramite.estado.";
  public static final String BK_AUDIT="tramite.audit.#";
  public static final String BK_TRACKING="tramite.estado.#";
  public static final String DL_EMAIL="email.dead";
  public static final String DL_AUDIT="cmd.2.dead";
  public static final String DL_TRACKING="cmd.3.dead";
}
