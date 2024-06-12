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

import com.io7m.garriga.main.matrix.GMatrixServiceStatusType.UnhealthyType.Failed;
import com.io7m.repetoir.core.RPServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.io7m.garriga.main.matrix.GMatrixServiceStatusType.HealthyType.Connected.CONNECTED;
import static com.io7m.garriga.main.matrix.GMatrixServiceStatusType.UnhealthyType.Starting.STARTING;

/**
 * The matrix client service.
 */

public final class GMatrixService
  implements RPServiceType, AutoCloseable, Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(GMatrixService.class);

  private final ExecutorService executor;
  private final GMatrixServiceConfiguration configuration;
  private final HttpClient httpClient;
  private final AtomicBoolean closed;
  private final AtomicReference<GMatrixServiceStatusType> status;
  private final LinkedBlockingQueue<GMatrixMessage> messageQueue;
  private GMatrixClient client;
  private String token;
  private String roomId;

  private GMatrixService(
    final ExecutorService inExecutor,
    final GMatrixServiceConfiguration inConfiguration,
    final HttpClient inHttpClient)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "httpClient");
    this.closed =
      new AtomicBoolean(false);
    this.status =
      new AtomicReference<>(STARTING);
    this.messageQueue =
      new LinkedBlockingQueue<GMatrixMessage>();
  }

  /**
   * Create a matrix service.
   *
   * @param configuration The configuration
   *
   * @return A matrix service
   */

  public static GMatrixService create(
    final GMatrixServiceConfiguration configuration)
  {
    final var executor =
      Executors.newVirtualThreadPerTaskExecutor();
    final var httpClient =
      HttpClient.newHttpClient();
    final var service =
      new GMatrixService(executor, configuration, httpClient);

    executor.execute(service::run);
    return service;
  }

  @Override
  public String description()
  {
    return "Matrix client service.";
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Integer.toUnsignedString(this.hashCode(), 16)
    );
  }

  @Override
  public void close()
  {
    if (this.closed.compareAndSet(false, true)) {
      this.executor.close();
    }
  }

  @Override
  public void run()
  {
    while (!this.closed.get()) {
      try {
        this.connect();
      } catch (final Exception e) {
        LOG.error("Failed to connect: ", e);
        this.fail(e);
        this.pause();
      }

      this.status.set(CONNECTED);

      try {
        while (!this.closed.get()) {
          final var message =
            this.messageQueue.poll(1L, TimeUnit.SECONDS);

          if (message != null) {
            this.client.roomSendMessage(
              this.token,
              this.roomId,
              message
            );
          }
        }
      } catch (final Exception e) {
        LOG.error("Failed to process message: ", e);
        this.fail(e);
        this.pause();
      }
    }
  }

  private void connect()
    throws IOException, InterruptedException
  {
    this.login();
    this.fetchRoom();
    this.joinRoom();
  }

  private void joinRoom()
    throws IOException, InterruptedException
  {
    this.client.roomJoin(this.token, this.roomId);
    LOG.info("Joined room.");
  }

  private void fetchRoom()
    throws IOException, InterruptedException
  {
    final var response =
      this.client.roomResolveAlias(
        this.token,
        this.configuration.matrixChannel());

    switch (response) {
      case final GMatrixJSON.MError r -> {
        throw new IOException(
          "Matrix server said: %s %s".formatted(r.errorCode, r.errorMessage)
        );
      }
      case final GMatrixJSON.MLoginResponse r -> {
        throw new IOException(
          "Matrix responded with an unexpected message: %s".formatted(r)
        );
      }
      case final GMatrixJSON.MRoomResolveAliasResponse r -> {
        this.roomId = r.roomId;
      }
    }
  }

  private void login()
    throws IOException, InterruptedException
  {
    LOG.info(
      "Connecting to matrix server {}",
      this.configuration.matrixServerBase()
    );

    this.client =
      GMatrixClient.create(
        this.httpClient,
        this.configuration.matrixServerBase()
      );

    final var response =
      this.client.login(
        this.configuration.matrixUser(),
        this.configuration.matrixPassword()
      );

    switch (response) {
      case final GMatrixJSON.MError r -> {
        throw new IOException(
          "Matrix server said: %s %s".formatted(r.errorCode, r.errorMessage)
        );
      }
      case final GMatrixJSON.MLoginResponse r -> {
        this.token = r.accessToken;
        LOG.info("Logged in to matrix server.");
      }
      case final GMatrixJSON.MRoomResolveAliasResponse r -> {
        throw new IOException(
          "Matrix responded with an unexpected message: %s".formatted(r)
        );
      }
    }
  }

  private <E extends Exception> void fail(final E e)
  {
    this.status.set(new Failed(e.getMessage()));
  }

  private void pause()
  {
    try {
      Thread.sleep(1_000L);
    } catch (final InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * @return The matrix client service status.
   */

  public GMatrixServiceStatusType status()
  {
    return this.status.get();
  }

  /**
   * Send a message.
   *
   * @param message The message
   */

  public void send(
    final GMatrixMessage message)
  {
    this.messageQueue.add(Objects.requireNonNull(message, "message"));
  }
}
