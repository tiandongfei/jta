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

package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.PluginConfig;
import de.mud.jta.FilterPlugin;
import de.mud.jta.PluginBus;

import de.mud.jta.event.OnlineStatusListener;
import de.mud.jta.event.ConfigurationListener;
import de.mud.jta.event.TelnetCommandListener;
import de.mud.jta.event.TerminalTypeRequest;
import de.mud.jta.event.WindowSizeRequest;
import de.mud.jta.event.LocalEchoRequest;
import de.mud.jta.event.EndOfRecordRequest;
import de.mud.jta.event.EndOfRecordRequest;

import de.mud.telnet.TelnetProtocolHandler;

import java.awt.Dimension;

import java.io.IOException;

/**
 * The telnet plugin utilizes a telnet protocol handler to filter
 * telnet negotiation requests from the data stream.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Mei�ner
 */
public class Telnet extends Plugin implements FilterPlugin {

  protected FilterPlugin source;
  protected TelnetProtocolHandler handler;

  private final static int debug = 0;

  /**
   * Create a new telnet plugin.
   */
  public Telnet(final PluginBus bus, String id) {
    super(bus, id);

    // create a new telnet protocol handler
    handler = new TelnetProtocolHandler() {
      /** get the current terminal type */
      public String getTerminalType() {
        return (String)bus.broadcast(new TerminalTypeRequest());
      }
      /** get the current window size */
      public Dimension getWindowSize() {
        return (Dimension)bus.broadcast(new WindowSizeRequest());
      }
      /** notify about local echo */
      public void setLocalEcho(boolean echo) {
        bus.broadcast(new LocalEchoRequest(echo));
      }
      /** notify about EOR end of record */
      public void notifyEndOfRecord() {
        bus.broadcast(new EndOfRecordRequest());
      }
      /** write data to our back end */
      public void write(byte[] b) throws IOException {
        source.write(b);
      }
    };

    // reset the telnet protocol handler just in case :-)
    bus.registerPluginListener(new OnlineStatusListener() {
      public void online() {
        handler.reset();
        try {
          handler.startup();
        } catch(java.io.IOException e) {
        }

        bus.broadcast(new LocalEchoRequest(true));
      }
      public void offline() {
        handler.reset();
        bus.broadcast(new LocalEchoRequest(true));
      }
    });

    bus.registerPluginListener(new ConfigurationListener() {
      public void setConfiguration(PluginConfig config) {
        configure(config);
      }
    });
    bus.registerPluginListener(new TelnetCommandListener() {
      public void sendTelnetCommand(byte command) throws IOException {
        handler.sendTelnetControl(command);
      }
    });

  }

  public void configure(PluginConfig cfg) {
    String crlf = cfg.getProperty("Telnet",id,"crlf");	// on \n
    if (crlf != null) handler.setCRLF(crlf);

    String cr = cfg.getProperty("Telnet",id,"cr");	// on \r
    if (cr != null) handler.setCR(cr);
  }

  public void setFilterSource(FilterPlugin source) {
    if(debug>0) System.err.println("Telnet: connected to: "+source);
    this.source = source;
  }

  public FilterPlugin getFilterSource() {
    return source;
  }

  public int read(byte[] b) throws IOException {
    // We just don't pass read() down, since negotiate() might call other
    // functions and we need transaction points.
    int n;

    n = handler.negotiate(b);	// we still have stuff buffered ...
    if (n>0)
      return n;

    while (true) {
      n = source.read(b);
      if (n <= 0 )
	return n;

      handler.inputfeed(b,n);
      n = 0;
      while (true) {
	n = handler.negotiate(b);
	if (n>0)
	  return n;
	if (n==-1) // buffer empty.
	  break;
      }
      return 0;
    }
  }

  public void write(byte[] b) throws IOException {
    handler.transpose(b); // transpose 0xff or \n and send data
  }
}
