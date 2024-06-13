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

import com.io7m.garriga.main.server.GServerConfiguration;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class GServerConfigurationTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GServerConfigurationTest.class);

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

  @TestFactory
  public Stream<DynamicTest> testParseErrors()
    throws Exception
  {
    return Stream.of(
        "error-config-0.json",
        "error-config-1.json",
        "error-config-2.json",
        "error-config-3.json",
        "error-config-4.json")
      .map(file -> {
        return DynamicTest.dynamicTest("testParseError_" + file, () -> {
          parseError(file);
        });
      });
  }

  @Test
  public void testOK()
    throws Exception
  {
    try (var s = resource("ok-config-0.json")) {
      final var c = GServerConfiguration.open(s);
      final var http = c.httpServerConfiguration();
      assertEquals("::", http.listenAddress());
      assertEquals(6000, http.listenPort());

      final var matrix = c.matrixConfiguration();
      assertEquals("#lobby:matrix.example.com", matrix.matrixChannel());
      assertEquals("5ad02f638d672e0004d2614e9a37a96ccdf8fd5d1edce3247f41fdbbf5d4a475", matrix.matrixPassword());
      assertEquals("@someone:matrix.example.com", matrix.matrixUser());
      assertEquals(URI.create("https://matrix.example.com"), matrix.matrixServerBase());
    }
  }

  private static void parseError(
    final String name)
    throws IOException
  {
    try (var s = resource(name)) {
      final var ex =
        assertThrows(Exception.class, () -> GServerConfiguration.open(s));
      LOG.debug("", ex);
    }
  }
}
