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

import com.io7m.garriga.main.matrix.GMatrixService;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.HealthyType;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.UnhealthyType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The health handler.
 */

public final class GHandlerHealth implements Handler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHandlerHealth.class);

  private final GMatrixService matrixClient;

  /**
   * The health handler.
   *
   * @param inServices The services
   */

  public GHandlerHealth(
    final RPServiceDirectoryType inServices)
  {
    this.matrixClient =
      inServices.requireService(GMatrixService.class);
  }

  @Override
  public void handle(
    final ServerRequest request,
    final ServerResponse response)
  {
    LOG.info(
      "{} {}",
      request.prologue().uriPath(),
      request.prologue().method()
    );

    switch (this.matrixClient.status()) {
      case final HealthyType ignored -> {
        response.status(200);
        response.header(HeaderNames.CONTENT_TYPE, "text/plain");
        response.send("OK\r\n");
      }
      case final UnhealthyType unhealthy -> {
        response.status(500);
        response.header(HeaderNames.CONTENT_TYPE, "text/plain");
        response.send("UNHEALTHY: %s\r\n".formatted(unhealthy.message()));
      }
    }
  }
}
