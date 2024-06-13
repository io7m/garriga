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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The type of AlertManager v4 alerts.
 */

// CHECKSTYLE:OFF

@JsonDeserialize
@JsonSerialize
public record GAlertV4(
  @JsonProperty(value = "status", required = true)
  String status,
  @JsonProperty(value = "labels", required = true)
  Map<String, String> labels,
  @JsonProperty(value = "annotations", required = true)
  Map<String, String> annotations,
  @JsonProperty(value = "startsAt", required = true)
  String startsAt,
  @JsonProperty(value = "endsAt", required = true)
  String endsAt,
  @JsonProperty(value = "generatorURL", required = true)
  String generatorURL,
  @JsonProperty(value = "fingerprint", required = true)
  String fingerprint)
{
  public GAlertV4
  {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(labels, "labels");
    Objects.requireNonNull(annotations, "annotations");
    Objects.requireNonNull(startsAt, "startsAt");
    Objects.requireNonNull(endsAt, "endsAt");
    Objects.requireNonNull(generatorURL, "generatorURL");
    Objects.requireNonNull(fingerprint, "fingerprint");
  }

  /**
   * @return This message formatted as plain text
   */

  public String formatText()
  {
    final var text = new StringBuilder(128);

    switch (this.status.toUpperCase(Locale.ROOT)) {
      case "FIRING" -> {
        text.append("ALERT FIRING!\n");
      }
      case "RESOLVED" -> {
        text.append("Alert resolved.\n");
      }
      default -> {
        text.append("Alert ");
        text.append(this.status);
        text.append(".\n");
      }
    }

    this.labels()
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(e -> {
        text.append(String.format("# %-16s : %s\n", e.getKey(), e.getValue()));
      });

    text.append("\n");

    this.annotations()
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(e -> {
        text.append(String.format("@ %-16s : %s\n", e.getKey(), e.getValue()));
      });

    return text.toString();
  }

  /**
   * @return This message formatted as HTML
   */

  public String formatHTML()
  {
    final var sb = new StringBuilder(128);

    switch (this.status.toUpperCase(Locale.ROOT)) {
      case "FIRING" -> {
        sb.append("<p>\uD83D\uDEA8 <b data-mx-color=\"#ff0000\">ALERT FIRING!</b></p>");
      }
      case "RESOLVED" -> {
        sb.append("<p>✅ <span data-mx-color=\"#00aa00\">Alert resolved.</b></p>");
      }
      default -> {
        sb.append("<p>Alert ");
        sb.append(StringEscapeUtils.escapeXml11(this.status));
        sb.append("</p>");
      }
    }

    sb.append("<p>");
    sb.append("<table>");
    this.labels()
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(e -> {
        sb.append("<tr>");
        sb.append("<td>");
        sb.append("<tt>");
        sb.append(StringEscapeUtils.escapeXml11(e.getKey()));
        sb.append("</tt>");
        sb.append("</td>");
        sb.append("<td>");
        sb.append("<tt>");
        sb.append(StringEscapeUtils.escapeXml11(e.getValue()));
        sb.append("</tt>");
        sb.append("</td>");
        sb.append("</tr>");
      });
    sb.append("</table>");
    sb.append("</p>");

    sb.append("<p>");
    sb.append("<table>");
    this.annotations()
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(e -> {
        sb.append("<tr>");
        sb.append("<td>");
        sb.append("<tt>");
        sb.append(StringEscapeUtils.escapeXml11(e.getKey()));
        sb.append("</tt>");
        sb.append("</td>");
        sb.append("<td>");
        sb.append("<tt>");
        sb.append(StringEscapeUtils.escapeXml11(e.getValue()));
        sb.append("</tt>");
        sb.append("</td>");
        sb.append("</tr>");
      });
    sb.append("</table>");
    sb.append("</p>");
    return sb.toString();
  }
}
