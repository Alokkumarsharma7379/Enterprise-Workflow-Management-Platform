package com.example.workflow.common;

import java.util.List;
import org.springframework.data.domain.*;

public final class Pages {

  private Pages() {}

  public static Pageable request(int page, int size) {
    if (page < 0 || size < 1 || size > 100) throw ApiException.invalid(
      "page must be nonnegative; size must be 1 to 100"
    );
    return PageRequest.of(
      page,
      size,
      Sort.by("createdAt").descending().and(Sort.by("id"))
    );
  }

  public record Result<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
  ) {
    public static <T> Result<T> of(Page<T> page) {
      return new Result<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages()
      );
    }
  }
}
