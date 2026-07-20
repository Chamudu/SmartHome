import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { isFirebaseConfigured } from './firebase'
import { useOutletSimulator } from './useOutletSimulator'
import type { DeviceStatus } from './types'

const statusOptions: DeviceStatus[] = ['ON', 'OFF', 'ERROR', 'DISCONNECTED']

function App() {
  if (!isFirebaseConfigured) {
    return <ConfigurationRequired />
  }

  return <SimulatorConsole />
}

function ConfigurationRequired() {
  return (
    <main className="shell shell--centered">
      <section className="setup-card">
        <p className="eyebrow">Hardware simulator</p>
        <h1>Firebase configuration required</h1>
        <p>
          Copy <code>.env.example</code> to <code>.env.local</code>, then provide
          the Firebase web application and seeded outlet identifiers.
        </p>
        <p className="muted">
          Local environment files are excluded from source control.
        </p>
      </section>
    </main>
  )
}

function SimulatorConsole() {
  const simulator = useOutletSimulator()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function submitSignIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void simulator.signIn(email, password)
  }

  if (!simulator.user) {
    return (
      <main className="shell shell--centered">
        <form className="setup-card sign-in" onSubmit={submitSignIn}>
          <p className="eyebrow">Hardware simulator</p>
          <h1>Simulator access</h1>
          <label>
            Email
            <input
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <label>
            Password
            <input
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {simulator.error && <p className="error-message">{simulator.error}</p>}
          <button className="primary-button" type="submit" disabled={simulator.busy}>
            {simulator.busy ? 'Connecting…' : 'Sign in'}
          </button>
        </form>
      </main>
    )
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Smart Home</p>
          <h1>Hardware simulator</h1>
        </div>
        <div className="session">
          <span className={`connection-dot ${simulator.listenerConnected ? 'is-online' : ''}`} />
          <span>{simulator.listenerConnected ? 'Cloud listener active' : 'Connecting…'}</span>
          <button className="text-button" type="button" onClick={() => void simulator.signOut()}>
            Sign out
          </button>
        </div>
      </header>

      <section className="workspace">
        <aside className="sidebar">
          <p className="sidebar-label">Environment</p>
          <strong>Primary home</strong>
          <span>Ground floor</span>
          <div className="sidebar-rule" />
          <p className="sidebar-label">Connected identity</p>
          <strong>{simulator.user.email ?? 'Simulator account'}</strong>
          <span>Hardware simulator</span>
        </aside>

        <section className="content">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Ground floor / Utility</p>
              <h2>Outlet diagnostic</h2>
            </div>
            <span className={`status-pill status-${simulator.device?.reported.status.toLowerCase() ?? 'loading'}`}>
              {simulator.device?.reported.status ?? 'LOADING'}
            </span>
          </div>

          {simulator.error && <p className="error-banner">{simulator.error}</p>}

          {!simulator.device ? (
            <section className="device-card empty-state">
              <h3>Waiting for seeded outlet</h3>
              <p>The listener is active, but the configured device document is not available yet.</p>
            </section>
          ) : (
            <OutletCard simulator={simulator} />
          )}
        </section>
      </section>
    </main>
  )
}

type OutletCardProps = {
  simulator: ReturnType<typeof useOutletSimulator>
}

function OutletCard({ simulator }: OutletCardProps) {
  const device = simulator.device
  if (!device) return null

  const hasPendingRequest =
    device.desired.requestId !== null &&
    device.desired.requestId !== device.reported.requestId

  return (
    <section className="device-card">
      <div className="device-identity">
        <div className="outlet-glyph" aria-hidden="true">
          <span />
          <span />
        </div>
        <div>
          <h3>{device.name}</h3>
          <p>{device.profile.replace('_', ' ')}</p>
        </div>
      </div>

      <dl className="telemetry-grid">
        <div>
          <dt>Desired</dt>
          <dd>{device.desired.status}</dd>
        </div>
        <div>
          <dt>Reported</dt>
          <dd>{device.reported.status}</dd>
        </div>
        <div>
          <dt>Command</dt>
          <dd>{hasPendingRequest ? 'PENDING' : device.commandState}</dd>
        </div>
        <div>
          <dt>Request ID</dt>
          <dd className="request-id">{device.desired.requestId ?? '—'}</dd>
        </div>
      </dl>

      <div className="control-panel">
        <div>
          <p className="control-title">Report physical state</p>
          <p className="muted">Simulates a state observation sent by hardware.</p>
        </div>
        <div className="button-row">
          {statusOptions.map((status) => (
            <button
              key={status}
              className={device.reported.status === status ? 'state-button is-selected' : 'state-button'}
              type="button"
              disabled={simulator.busy}
              onClick={() => void simulator.reportStatus(status)}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      <footer className="device-footer">
        <span>Automatic command acknowledgement: enabled</span>
        <span>{simulator.busy ? 'Writing state…' : 'Ready'}</span>
      </footer>
    </section>
  )
}

export default App
