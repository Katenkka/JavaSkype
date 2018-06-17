package fr.delthas.skype;

/**
 * A listener for new messages sent to a Skype account.
 */
@FunctionalInterface
public interface GroupFileListener {
  /**
   * Called when a message is sent from a user to a group the Skype account is in while it is connected.
   *
   * @param group   The group in which the message has been sent.
   * @param user  The sender of the message.
   * @param file The file sent.
   */
  void fileReceived(Group group, User user, SkypeFile file);
}
