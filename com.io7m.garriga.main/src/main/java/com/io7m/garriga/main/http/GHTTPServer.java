/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.garriga.main.http;

import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.repetoir.core.RPServiceType;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;

import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.SO_REUSEPORT;

/**
 * The web server.
 */

public final class GHTTPServer implements AutoCloseable, RPServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHTTPServer.class);

  private final WebServer webServer;

  private GHTTPServer(
    final WebServer inWebServer)
  {
    this.webServer =
      Objects.requireNonNull(inWebServer, "webServer");
  }

  /**
   * Create and start a web server.
   *
   * @param services      The services
   * @param configuration The configuration
   *
   * @return The server
   *
   * @throws Exception On errors
   */

  public static GHTTPServer create(
    final RPServiceDirectoryType services,
    final GHTTPServerConfiguration configuration)
    throws Exception
  {
    final var routing =
      HttpRouting.builder()
        .get("/health", new GHandlerHealth(services))
        .post("/4/send", new GHandlerV4(services, configuration));

    final var webServerBuilder =
      WebServerConfig.builder();

    final var address =
      InetAddress.getByName(configuration.listenAddress());

    final var webServer =
      webServerBuilder
        .port(configuration.listenPort())
        .address(address)
        .listenerSocketOptions(Map.ofEntries(
          Map.entry(SO_REUSEADDR, Boolean.TRUE),
          Map.entry(SO_REUSEPORT, Boolean.TRUE)
        ))
        .routing(routing)
        .build();

    webServer.start();
    LOG.info("[{}] Server started", address);
    return new GHTTPServer(webServer);
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Integer.toUnsignedString(this.hashCode(), 16)
    );
  }

  @Override
  public void close()
  {
    this.webServer.stop();
  }

  @Override
  public String description()
  {
    return "HTTP server service.";
  }
}
