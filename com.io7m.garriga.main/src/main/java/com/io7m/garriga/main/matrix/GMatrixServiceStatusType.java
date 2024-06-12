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


package com.io7m.garriga.main.matrix;

/**
 * The status of the matrix service.
 */

public sealed interface GMatrixServiceStatusType
{
  /**
   * @return The status message
   */

  String message();

  /**
   * The status of the service is healthy.
   */

  sealed interface HealthyType
    extends GMatrixServiceStatusType
  {
    /**
     * The status of the service is healthy.
     */

    enum Connected implements HealthyType
    {
      /**
       * The status of the service is healthy.
       */

      CONNECTED;

      @Override
      public String message()
      {
        return "Connected.";
      }
    }
  }

  /**
   * The status of the service is unhealthy.
   */

  sealed interface UnhealthyType
    extends GMatrixServiceStatusType
  {
    /**
     * The status of the service is unhealthy.
     */

    enum Starting implements UnhealthyType
    {
      /**
       * The status of the service is unhealthy.
       */

      STARTING;

      @Override
      public String message()
      {
        return "Starting up...";
      }
    }

    /**
     * The status of the service is unhealthy.
     *
     * @param message The failure message
     */

    record Failed(String message)
      implements UnhealthyType
    {

    }
  }
}
