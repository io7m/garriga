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
import com.io7m.garriga.main.matrix.GMatrixServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The v1 handler.
 */

public final class GHandlerV4 implements Handler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHandlerV4.class);

  private final GMatrixServiceType matrixClient;
  private final GHTTPServerConfiguration configuration;
  private final ObjectMapper mapper;

  /**
   * The v1 handler.
   *
   * @param inServices      The services
   * @param inConfiguration The configuration
   */

  public GHandlerV4(
    final RPServiceDirectoryType inServices,
    final GHTTPServerConfiguration inConfiguration)
  {
    this.matrixClient =
      inServices.requireService(GMatrixServiceType.class);
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.mapper =
      GMessageV4ObjectMappers.createMapper();
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
      final var headers =
        request.headers();

      final var authorizationOpt =
        headers.value(HeaderNames.AUTHORIZATION);

      if (authorizationOpt.isEmpty()) {
        LOG.error("Authentication failed.");
        response.status(401);
        response.header(HeaderNames.CONTENT_TYPE, "text/plain");
        response.send("Authentication failed.");
        return;
      }

      final var authorization = authorizationOpt.get().trim();
      if (!authorization.equals(this.bearerToken())) {
        LOG.error("Authentication failed.");
        response.status(401);
        response.header(HeaderNames.CONTENT_TYPE, "text/plain");
        response.send("Authentication failed.");
        return;
      }

      final var length =
        headers.contentLength()
          .orElse(Long.MAX_VALUE);

      if (length >= 1_000_000L) {
        LOG.error("Request too large: {}", length);
        response.status(413);
        response.header(HeaderNames.CONTENT_TYPE, "text/plain");
        response.send("Request too large.");
        return;
      }

      final byte[] jsonBytes;
      try (var stream = request.content().inputStream()) {
        jsonBytes = stream.readAllBytes();
      }

      if (LOG.isTraceEnabled()) {
        // CHECKSTYLE:OFF
        LOG.trace("{}", new String(jsonBytes, StandardCharsets.UTF_8));
        // CHECKSTYLE:ON
      }

      final var message =
        this.mapper.readValue(jsonBytes, GAlertManagerRequestV4.class);

      for (final var alert : message.alerts()) {
        this.matrixClient.send(
          new GMatrixMessage(alert.formatText(), alert.formatHTML())
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

  private String bearerToken()
  {
    return "Bearer %s".formatted(this.configuration.authenticationToken()).trim();
  }
}
