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


package com.io7m.garriga.main.server;

import com.io7m.garriga.main.http.GHTTPServer;
import com.io7m.garriga.main.matrix.GMatrixService;
import com.io7m.garriga.main.matrix.GMatrixServiceType;
import com.io7m.repetoir.core.RPServiceDirectory;
import com.io7m.repetoir.core.RPServiceDirectoryWritableType;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main server.
 */

public final class GServer implements AutoCloseable
{
  private final GServerConfiguration configuration;
  private RPServiceDirectoryWritableType services;
  private final AtomicBoolean closed;

  private GServer(
    final GServerConfiguration inConfiguration,
    final RPServiceDirectoryWritableType inServices)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.closed =
      new AtomicBoolean(true);
  }

  /**
   * Create a server.
   *
   * @param configuration The configuration
   *
   * @return The server
   */

  public static GServer create(
    final GServerConfiguration configuration)
  {
    return new GServer(
      configuration,
      new RPServiceDirectory()
    );
  }

  /**
   * Start the server.
   *
   * @throws Exception On errors
   */

  public void start()
    throws Exception
  {
    if (this.closed.compareAndSet(true, false)) {
      this.services = new RPServiceDirectory();
      this.services.register(
        GMatrixServiceType.class,
        GMatrixService.create(
          this.configuration.matrixConfiguration()
        )
      );
      this.services.register(
        GHTTPServer.class,
        GHTTPServer.create(
          this.services,
          this.configuration.httpServerConfiguration()
        )
      );
    }
  }

  @Override
  public void close()
    throws Exception
  {
    if (this.closed.compareAndSet(false, true)) {
      this.services.close();
    }
  }
}
