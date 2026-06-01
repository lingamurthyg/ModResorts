package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * Cloud-ready JSON input stream that works with any InputStream.
 * Compatible with classpath resources and S3 streams.
 */
public class JsonInputStream extends InputStream {

  private InputStream inputStream;

  public JsonInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public int read() throws IOException {
    return inputStream.read();
  }

  @Override
  public void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
  }

  /**
   * Parse JSON from the input stream into the specified class.
   * Uses try-with-resources for automatic resource management.
   */
  public Object parseJsonAs(Class<?> cls) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      Gson gson = new Gson();
      return gson.fromJson(reader, cls);
    } catch (IOException e) {
      System.err.println("Failed to parse JSON: " + e.getMessage());
      e.printStackTrace();
      return null;
    } catch (Exception e) {
      System.err.println("Unexpected error parsing JSON: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

}
