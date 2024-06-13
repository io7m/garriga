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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The type of AlertManager v4 messages.
 */

// CHECKSTYLE:OFF

@JsonDeserialize
@JsonSerialize
public record GAlertManagerRequestV4(
  @JsonProperty(value = "version", required = true)
  int version,
  @JsonProperty(value = "groupKey", required = true)
  String groupKey,
  @JsonProperty(value = "truncatedAlerts", required = true)
  int truncatedAlerts,
  @JsonProperty(value = "status", required = true)
  String status,
  @JsonProperty(value = "receiver", required = true)
  String receiver,
  @JsonProperty(value = "groupLabels", required = true)
  Map<String, String> groupLabels,
  @JsonProperty(value = "commonLabels", required = true)
  Map<String, String> commonLabels,
  @JsonProperty(value = "commonAnnotations", required = true)
  Map<String, String> commonAnnotations,
  @JsonProperty(value = "externalURL", required = true)
  String externalURL,
  @JsonProperty(value = "alerts", required = true)
  List<GAlertV4> alerts)
{
  public GAlertManagerRequestV4
  {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(groupKey, "groupKey");
    Objects.requireNonNull(receiver, "receiver");
    Objects.requireNonNull(groupLabels, "groupLabels");
    Objects.requireNonNull(commonLabels, "commonLabels");
    Objects.requireNonNull(commonAnnotations, "commonAnnotations");
    Objects.requireNonNull(externalURL, "externalURL");
    Objects.requireNonNull(alerts, "alerts");
  }
}
