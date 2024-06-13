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
import com.io7m.garriga.main.http.GHandlerHealth;
import com.io7m.garriga.main.http.GHandlerV4;
import com.io7m.garriga.main.http.GMessageV4ObjectMappers;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.HealthyType;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.HealthyType.Connected;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.UnhealthyType;
import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.UnhealthyType.Failed;
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

public final class GHandlerHealthTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GHandlerHealthTest.class);

  private ObjectMapper mappers;
  private RPServiceDirectory services;
  private GHandlerHealth handler;
  private GMatrixServiceType matrix;
  private ServerRequest request;
  private ServerResponse response;
  private HttpPrologue prologue;
  private ServerRequestHeaders headers;

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

    Mockito.when(this.request.prologue())
      .thenReturn(this.prologue);
    Mockito.when(this.prologue.uriPath())
      .thenReturn(UriPath.create("/health"));
    Mockito.when(this.prologue.method())
      .thenReturn(Method.GET);
    Mockito.when(this.request.headers())
      .thenReturn(this.headers);

    this.handler =
      new GHandlerHealth(this.services);
  }

  @Test
  public void testUnhealthy()
  {
    Mockito.when(this.matrix.status())
      .thenReturn(new Failed("FAILED!"));

    this.handler.handle(this.request, this.response);

    Mockito.verify(this.response, new Times(1))
      .status(500);
  }

  @Test
  public void testHealthy()
  {
    Mockito.when(this.matrix.status())
      .thenReturn(Connected.CONNECTED);

    this.handler.handle(this.request, this.response);

    Mockito.verify(this.response, new Times(1))
      .status(200);
  }
}
