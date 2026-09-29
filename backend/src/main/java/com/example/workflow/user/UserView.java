package com.example.workflow.user;

import java.util.UUID;

public record UserView(UUID id, String displayName) {
  public static UserView of(AppUser user) {
    return new UserView(user.getId(), user.getDisplayName());
  }
}
