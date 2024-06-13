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


package com.io7m.garriga.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.garriga.main.http.GAlertManagerRequestV4;
import com.io7m.garriga.main.http.GHTTPServerConfiguration;
import com.io7m.garriga.main.http.GHandlerV4;
import com.io7m.garriga.main.http.GMessageV4ObjectMappers;
import com.io7m.garriga.main.matrix.GMatrixServiceType;
import com.io7m.repetoir.core.RPServiceDirectory;
import io.helidon.common.uri.UriPath;
import io.helidon.http.HeaderNames;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.media.ReadableEntity;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public final class GHandlerV4Test
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHandlerV4Test.class);

  private ObjectMapper mappers;
  private RPServiceDirectory services;
  private GHandlerV4 handler;
  private GMatrixServiceType matrix;
  private ServerRequest request;
  private ServerResponse response;
  private HttpPrologue prologue;
  private ServerRequestHeaders headers;
  private ReadableEntity readable;

  private static InputStream resource(
    final String name)
    throws IOException
  {
    final var path =
      "/com/io7m/garriga/tests/%s".formatted(name);
    final var url =
      GServerConfigurationTest.class.getResource(path);

    return url.openStream();
  }

  @BeforeEach
  public void setup()
  {
    this.mappers =
      GMessageV4ObjectMappers.createMapper();
    this.services =
      new RPServiceDirectory();

    this.matrix =
      Mockito.mock(GMatrixServiceType.class);
    this.services.register(
      GMatrixServiceType.class,
      this.matrix
    );

    this.request =
      Mockito.mock(ServerRequest.class);
    this.response =
      Mockito.mock(ServerResponse.class);
    this.prologue =
      Mockito.mock(HttpPrologue.class);
    this.headers =
      Mockito.mock(ServerRequestHeaders.class);
    this.readable =
      Mockito.mock(ReadableEntity.class);

    Mockito.when(this.request.prologue())
      .thenReturn(this.prologue);
    Mockito.when(this.prologue.uriPath())
      .thenReturn(UriPath.create("/4/send"));
    Mockito.when(this.prologue.method())
      .thenReturn(Method.POST);
    Mockito.when(this.request.headers())
      .thenReturn(this.headers);

    this.handler =
      new GHandlerV4(
        this.services,
        new GHTTPServerConfiguration("::", 6000, "abcd"));
  }

  @Test
  public void testMessageParse()
    throws Exception
  {
    try (var s = resource("msg-0.json")) {
      final var data =
        s.readAllBytes();
      final var message =
        this.mappers.readValue(data, GAlertManagerRequestV4.class);

      assertEquals(4, message.version());

      for (final var alert : message.alerts()) {
        LOG.debug("{}", alert.formatText());
        LOG.debug("{}", alert.formatHTML());
      }
    }
  }

  @Test
  public void testAuthenticationFailed0()
  {
    this.handler.handle(this.request, this.response);

    Mockito.verifyNoMoreInteractions(this.matrix);
    Mockito.verify(this.response, new Times(1))
      .status(401);
  }

  @Test
  public void testAuthenticationFailed1()
  {
    Mockito.when(this.headers.value(HeaderNames.AUTHORIZATION))
      .thenReturn(Optional.empty());

    this.handler.handle(this.request, this.response);

    Mockito.verifyNoMoreInteractions(this.matrix);
    Mockito.verify(this.response, new Times(1))
      .status(401);
  }

  @Test
  public void testAuthenticationFailed2()
  {
    Mockito.when(this.headers.value(HeaderNames.AUTHORIZATION))
      .thenReturn(Optional.of("Bearer invalid"));

    this.handler.handle(this.request, this.response);

    Mockito.verifyNoMoreInteractions(this.matrix);
    Mockito.verify(this.response, new Times(1))
      .status(401);
  }

  @Test
  public void testTooLarge()
  {
    Mockito.when(this.headers.value(HeaderNames.AUTHORIZATION))
      .thenReturn(Optional.of("Bearer abcd"));
    Mockito.when(this.headers.contentLength())
      .thenReturn(OptionalLong.of(2000000L));

    this.handler.handle(this.request, this.response);

    Mockito.verifyNoMoreInteractions(this.matrix);
    Mockito.verify(this.response, new Times(1))
      .status(413);
  }

  @Test
  public void testOK()
    throws Exception
  {
    final byte[] data;
    try (var s = resource("msg-0.json")) {
      data = s.readAllBytes();
    }

    Mockito.when(this.request.content())
      .thenReturn(this.readable);
    Mockito.when(this.readable.inputStream())
      .thenReturn(new ByteArrayInputStream(data));

    Mockito.when(this.headers.value(HeaderNames.AUTHORIZATION))
      .thenReturn(Optional.of("Bearer abcd"));
    Mockito.when(this.headers.contentLength())
      .thenReturn(OptionalLong.of(data.length));

    this.handler.handle(this.request, this.response);

    Mockito.verify(this.matrix, new Times(2))
      .send(any());
    Mockito.verify(this.response, new Times(1))
      .status(200);
  }
}
