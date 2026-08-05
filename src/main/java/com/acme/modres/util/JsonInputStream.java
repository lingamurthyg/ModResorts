package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * JsonInputStream supports parsing JSON from either a {@link File} (legacy)
 * or a raw {@link InputStream} / {@link ByteArrayInputStream} (cloud-native,
 * used when content is loaded from Amazon S3 or classpath bytes).
 *
 * <p>Implements {@link Closeable} so it can be used in try-with-resources
 * blocks, preventing resource leaks in containerized AWS environments.</p>
 */
public class JsonInputStream implements Closeable {

  private InputStream inputStream;
  private File file; // kept for backward-compatibility with File-based callers

  /**
   * Construct from a {@link File} (backward-compatible constructor).
   */
  public JsonInputStream(File file) throws FileNotFoundException {
    this.file = file;
    this.inputStream = new FileInputStream(file);
  }

  /**
   * Construct from an {@link InputStream} – used when content is sourced from
   * Amazon S3 or classpath bytes, eliminating local temporary file creation.
   */
  public JsonInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * Parse the JSON content of this stream as the given class.
   */
  public Object parseJsonAs(Class<?> cls) {
    // For File-based streams, honour the legacy file.exists() guard
    if (file != null && !file.exists()) {
      return null;
    }
    try {
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      return gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    if (inputStream != null) {
      inputStream.close();
    }
  }
}
