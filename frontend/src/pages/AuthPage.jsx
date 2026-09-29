import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Layers2, ArrowRight, Check } from "lucide-react";
import { authApi } from "../api";
import { ErrorMessage, Field } from "../components/UI";
export default function AuthPage({ register = false }) {
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const form = Object.fromEntries(new FormData(event.currentTarget));
    try {
      await (register ? authApi.register(form) : authApi.login(form));
      navigate("/dashboard");
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-page">
      <section className="auth-story">
        <div className="brand">
          <span className="brand-icon">
            <Layers2 size={22} />
          </span>{" "}
          workflow.
        </div>
        <div>
          <span className="eyebrow">LESS FRICTION. MORE PROGRESS.</span>
          <h1>
            Good work starts
            <br />
            with a clear plan.
          </h1>
          <p>
            Bring your people, projects, and next steps together in one focused
            workspace.
          </p>
          <div className="story-list">
            {[
              "A shared view of what matters",
              "Every task has a next step",
              "The context behind every change",
            ].map((text) => (
              <div key={text}>
                <Check size={17} />
                {text}
              </div>
            ))}
          </div>
        </div>
        <span className="muted">Built for thoughtful teams.</span>
      </section>
      <section className="auth-form">
        <div>
          <span className="eyebrow">YOUR WORK, CONNECTED</span>
          <h2>{register ? "Create your account" : "Welcome back"}</h2>
          <p className="muted">
            {register
              ? "Start with your account. Build your workspace next."
              : "Pick up where your team left off."}
          </p>
          <form onSubmit={submit}>
            <ErrorMessage error={error} />
            {register && (
              <Field label="Full name">
                <input
                  name="displayName"
                  autoComplete="name"
                  maxLength={100}
                  required
                />
              </Field>
            )}
            <Field label="Email address">
              <input
                name="email"
                type="email"
                autoComplete="email"
                maxLength={254}
                required
              />
            </Field>
            <Field label="Password">
              <input
                name="password"
                type="password"
                autoComplete={register ? "new-password" : "current-password"}
                minLength={register ? 12 : undefined}
                maxLength={72}
                required
              />
            </Field>
            {register && (
              <small className="muted">
                Use at least 12 characters, up to 72 UTF-8 bytes.
              </small>
            )}
            <button className="primary full" disabled={busy}>
              {busy ? "Please wait…" : register ? "Create account" : "Sign in"}
              <ArrowRight size={17} />
            </button>
          </form>
          <p className="auth-alternate">
            {register ? "Already have an account?" : "New to Workflow?"}{" "}
            <Link to={register ? "/login" : "/register"}>
              {register ? "Sign in" : "Create an account"}
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
