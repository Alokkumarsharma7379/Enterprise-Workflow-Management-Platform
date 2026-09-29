import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import AuthPage from "../pages/AuthPage";
import { authApi } from "../api";
vi.mock("../api", () => ({ authApi: { login: vi.fn(), register: vi.fn() } }));
beforeEach(() => vi.resetAllMocks());
describe("authentication forms", () => {
  it("submits login credentials through the API client", async () => {
    authApi.login.mockResolvedValue({});
    render(
      <MemoryRouter>
        <AuthPage />
      </MemoryRouter>,
    );
    await userEvent.type(
      screen.getByLabelText("Email address"),
      "alok@example.com",
    );
    await userEvent.type(
      screen.getByLabelText("Password"),
      "correct-password-123",
    );
    await userEvent.click(screen.getByRole("button", { name: /sign in/i }));
    await waitFor(() =>
      expect(authApi.login).toHaveBeenCalledWith({
        email: "alok@example.com",
        password: "correct-password-123",
      }),
    );
  });
  it("displays a failed login and allows another attempt", async () => {
    authApi.login.mockRejectedValue(
      new Error("Invalid or expired credentials"),
    );
    render(
      <MemoryRouter>
        <AuthPage />
      </MemoryRouter>,
    );
    await userEvent.type(
      screen.getByLabelText("Email address"),
      "alok@example.com",
    );
    await userEvent.type(screen.getByLabelText("Password"), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: /sign in/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Invalid or expired credentials",
    );
    expect(screen.getByRole("button", { name: /sign in/i })).toBeEnabled();
  });
  it("collects display name for registration", () => {
    render(
      <MemoryRouter>
        <AuthPage register />
      </MemoryRouter>,
    );
    expect(screen.getByLabelText("Full name")).toBeRequired();
    expect(screen.getByLabelText("Password")).toHaveAttribute(
      "minlength",
      "12",
    );
  });
});
