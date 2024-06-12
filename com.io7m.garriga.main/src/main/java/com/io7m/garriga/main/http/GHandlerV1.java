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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.garriga.main.matrix.GMatrixMessage;
import com.io7m.garriga.main.matrix.GMatrixService;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The v1 handler.
 */

public final class GHandlerV1 implements Handler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHandlerV1.class);

  private final GMatrixService matrixClient;
  private final ObjectMapper mapper;

  /**
   * The v1 handler.
   *
   * @param inServices The services
   */

  public GHandlerV1(
    final RPServiceDirectoryType inServices)
  {
    this.matrixClient =
      inServices.requireService(GMatrixService.class);
    this.mapper =
      GMessageV1ObjectMappers.createMapper();
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

    try {
      try (var stream = request.content().inputStream()) {
        final var message =
          this.mapper.readValue(stream, GMessageV1.class);
        LOG.debug("{}", message);
        this.matrixClient.send(
          new GMatrixMessage(
            message.title(),
            "<pre>%s</pre>".formatted(message.title())
          )
        );
      }

      response.status(200);
      response.header(HeaderNames.CONTENT_TYPE, "text/plain");
      response.send("OK\r\n");
    } catch (final IOException e) {
      LOG.error("I/O: ", e);
      response.status(500);
      response.header(HeaderNames.CONTENT_TYPE, "text/plain");
      response.send(e.getMessage());
    }
  }
}
