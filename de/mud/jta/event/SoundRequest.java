/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Mei�ner 1996-2002. All Rights Reserved.
 *
 * The software is licensed under the terms and conditions in the
 * license agreement included in the software distribution package.
 *
 * You should have received a copy of the license along with this
 * software; see the file license.txt. If not, navigate to the 
 * URL http://javatelnet.org/ and view the "License Agreement".
 *
 */
package de.mud.jta.event;

import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;
import de.mud.jta.event.SoundListener;

import java.net.URL;

/**
 * Play a sound.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Mei�ner
 */
public class SoundRequest implements PluginMessage {
  protected URL audioClip;

  public SoundRequest(URL audioClip) {
    this.audioClip = audioClip;
  }

  /**
   * Notify all listeners that they may play the sound.
   * @param pl the list of plugin message listeners
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof SoundListener) {
      ((SoundListener)pl).playSound(audioClip);
    }
    return null;
  }
}
