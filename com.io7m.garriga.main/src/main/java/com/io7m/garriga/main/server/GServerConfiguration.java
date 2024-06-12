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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.garriga.main.http.GHTTPServerConfiguration;
import com.io7m.garriga.main.matrix.GMatrixServiceConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * The server configuration.
 *
 * @param schema                  The schema type
 * @param httpServerConfiguration The HTTP server configuration
 * @param matrixConfiguration     The matrix client configuration
 */

@JsonDeserialize
@JsonSerialize
public record GServerConfiguration(
  @JsonProperty(value = "%schema", required = true)
  String schema,
  @JsonProperty(required = true, value = "HTTPServer")
  GHTTPServerConfiguration httpServerConfiguration,
  @JsonProperty(required = true, value = "MatrixClient")
  GMatrixServiceConfiguration matrixConfiguration)
{
  /**
   * The server configuration.
   *
   * @param schema                  The schema type
   * @param httpServerConfiguration The HTTP server configuration
   * @param matrixConfiguration     The matrix client configuration
   */

  public GServerConfiguration
  {
    Objects.requireNonNull(schema, "schema");
    Objects.requireNonNull(httpServerConfiguration, "serverConfiguration");
    Objects.requireNonNull(matrixConfiguration, "matrixConfiguration");
  }

  /**
   * Parse a configuration.
   *
   * @param stream The input stream
   *
   * @return The configuration
   *
   * @throws IOException On I/O errors
   */

  public static GServerConfiguration open(
    final InputStream stream)
    throws IOException
  {
    final var mapper = createMapper();
    return mapper.readValue(stream, GServerConfiguration.class);
  }

  private static ObjectMapper createMapper()
  {
    final JsonMapper mapper =
      JsonMapper.builder()
        .disable(FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    final var deserializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(GServerConfiguration.class)
        .allowClass(GHTTPServerConfiguration.class)
        .allowClass(GMatrixServiceConfiguration.class)
        .allowClass(URI.class)
        .allowClass(int.class)
        .allowClass(String.class)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(deserializers);
    mapper.registerModule(simpleModule);
    return mapper;
  }
}
