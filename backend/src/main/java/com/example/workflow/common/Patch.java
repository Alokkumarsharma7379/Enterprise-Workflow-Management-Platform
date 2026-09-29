package com.example.workflow.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.*;

/** Presence-aware PATCH parsing: absent means unchanged; null only clears nullable fields. */
public final class Patch {

  private final JsonNode json;

  public Patch(JsonNode json, String... allowed) {
    if (json == null || !json.isObject()) throw ApiException.invalid(
      "Expected a JSON object"
    );
    this.json = json;
    Set<String> fields = new HashSet<>(Arrays.asList(allowed));
    json.fieldNames().forEachRemaining(f -> {
      if (!fields.contains(f)) throw ApiException.invalid(
        "Unknown field: " + f
      );
    });
  }

  public boolean has(String field) {
    return json.has(field);
  }

  public String text(String field, int max, boolean nullable) {
    JsonNode value = json.get(field);
    if (value == null || value.isNull()) {
      if (nullable) return null;
      throw ApiException.invalid(field + " is required");
    }
    if (!value.isTextual()) throw ApiException.invalid(field + " must be text");
    String result = value.asText().trim();
    if (
      (!nullable && result.isEmpty()) || result.length() > max
    ) throw ApiException.invalid("Invalid length for " + field);
    return result;
  }

  public UUID uuid(String field) {
    String value = text(field, 36, true);
    try {
      return value == null ? null : UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw ApiException.invalid("Invalid " + field);
    }
  }

  public LocalDate date(String field) {
    String value = text(field, 10, true);
    try {
      return value == null ? null : LocalDate.parse(value);
    } catch (Exception e) {
      throw ApiException.invalid("Invalid " + field);
    }
  }

  public <E extends Enum<E>> E enumeration(String field, Class<E> type) {
    try {
      return Enum.valueOf(type, text(field, 30, false));
    } catch (IllegalArgumentException e) {
      throw ApiException.invalid("Invalid " + field);
    }
  }

  public void version(long actual) {
    JsonNode value = json.get("version");
    if (
      value == null || !value.isIntegralNumber() || !value.canConvertToLong()
    ) throw ApiException.invalid("version is required");
    if (value.longValue() != actual) throw ApiException.conflict(
      "This record changed; refresh before saving"
    );
  }
}
