import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { isFirebaseConfigured } from './firebase'
import { useDeviceSimulator } from './useDeviceSimulator'
import type { DeviceConfig, DeviceProfile, DeviceStatus, DeviceTwin } from './types'

const statusOptions: DeviceStatus[] = ['ON', 'OFF', 'ERROR', 'DISCONNECTED']
const profileOptions: Array<DeviceProfile | 'ALL'> = [
  'ALL', 'OUTLET', 'MULTI_SWITCH', 'SAFETY_OUTLET', 'LIGHT', 'CAMERA',
]

function App() {
  return isFirebaseConfigured ? <SimulatorConsole /> : <ConfigurationRequired />
}

function ConfigurationRequired() {
  return (
    <main className="shell shell--centered">
      <section className="setup-card">
        <p className="eyebrow">Hardware simulator</p>
        <h1>Firebase configuration required</h1>
        <p>
          Copy <code>.env.example</code> to <code>.env.local</code>, then provide
          the Firebase web application values and home identifier.
        </p>
      </section>
    </main>
  )
}

function SimulatorConsole() {
  const simulator = useDeviceSimulator()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [floorFilter, setFloorFilter] = useState('ALL')
  const [profileFilter, setProfileFilter] = useState<DeviceProfile | 'ALL'>('ALL')

  const floors = useMemo(
    () => [...new Set(simulator.devices.map((device) => device.floorId))].sort(),
    [simulator.devices],
  )
  const visibleDevices = simulator.devices.filter((device) =>
    (floorFilter === 'ALL' || device.floorId === floorFilter) &&
    (profileFilter === 'ALL' || device.profile === profileFilter),
  )

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
          <label>Email<input type="email" autoComplete="username" required value={email}
            onChange={(event) => setEmail(event.target.value)} /></label>
          <label>Password<input type="password" autoComplete="current-password" required value={password}
            onChange={(event) => setPassword(event.target.value)} /></label>
          {simulator.error && <p className="error-message">{simulator.error}</p>}
          <button className="primary-button" type="submit" disabled={simulator.authBusy}>
            {simulator.authBusy ? 'Connecting…' : 'Sign in'}
          </button>
        </form>
      </main>
    )
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div><p className="eyebrow">Smart Home</p><h1>Hardware simulator</h1></div>
        <div className="session">
          <span className={`connection-dot ${simulator.listenerConnected ? 'is-online' : ''}`} />
          <span>{simulator.listenerConnected ? `${simulator.devices.length} devices connected` : 'Connecting…'}</span>
          <button className="text-button" type="button" onClick={() => void simulator.signOut()}>Sign out</button>
        </div>
      </header>

      <section className="workspace">
        <aside className="sidebar">
          <p className="sidebar-label">Floor</p>
          {['ALL', ...floors].map((floor) => (
            <button key={floor} className={floorFilter === floor ? 'filter-button is-selected' : 'filter-button'}
              type="button" onClick={() => setFloorFilter(floor)}>
              {floor === 'ALL' ? 'All floors' : floor}
            </button>
          ))}
          <div className="sidebar-rule" />
          <p className="sidebar-label">Profile</p>
          <select value={profileFilter} onChange={(event) => setProfileFilter(event.target.value as DeviceProfile | 'ALL')}>
            {profileOptions.map((profile) => <option key={profile} value={profile}>{formatProfile(profile)}</option>)}
          </select>
          <div className="sidebar-rule" />
          <p className="sidebar-label">Identity</p>
          <strong>{simulator.user.email ?? 'Simulator account'}</strong>
          <span>SIMULATOR role</span>
        </aside>

        <section className="content">
          <div className="section-heading">
            <div><p className="eyebrow">Realtime hardware state</p><h2>Device diagnostics</h2></div>
            <span className="status-pill">{visibleDevices.length} shown</span>
          </div>
          {simulator.error && <p className="error-banner">{simulator.error}</p>}
          {visibleDevices.length === 0 ? (
            <section className="device-card empty-state"><h3>No matching devices</h3>
              <p>Create devices in Android or change the simulator filters.</p></section>
          ) : (
            <div className="device-list">
              {visibleDevices.map((device) => (
                <DeviceCard key={device.id} device={device}
                  busy={simulator.busyDeviceIds.has(device.id)}
                  busyChannelKeys={simulator.busyChannelKeys}
                  onReport={(status) => void simulator.reportStatus(device.id, status)}
                  onReportChannel={(channelId, status) =>
                    void simulator.reportChannelStatus(device.id, channelId, status)} />
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  )
}

function DeviceCard({ device, busy, busyChannelKeys, onReport, onReportChannel }: {
  device: DeviceTwin
  busy: boolean
  busyChannelKeys: ReadonlySet<string>
  onReport: (status: DeviceStatus) => void
  onReportChannel: (channelId: string, status: DeviceStatus) => void
}) {
  const pending = device.desired.requestId !== null && device.desired.requestId !== device.reported.requestId
  const channels = device.profile === 'MULTI_SWITCH' && 'channels' in device.config
    ? device.config.channels
    : []
  return (
    <article className="device-card">
      <div className="device-identity">
        <div className="profile-glyph" aria-hidden="true">{profileGlyph(device.profile)}</div>
        <div><h3>{device.name}</h3><p>{formatProfile(device.profile)}</p></div>
        <span className={`status-pill status-${device.reported.status.toLowerCase()}`}>{device.reported.status}</span>
      </div>
      <dl className="telemetry-grid">
        <div><dt>Desired</dt><dd>{device.desired.status}</dd></div>
        <div><dt>Reported</dt><dd>{device.reported.status}</dd></div>
        <div><dt>Command</dt><dd>{pending ? 'PENDING' : device.commandState}</dd></div>
        <div><dt>Position</dt><dd>{device.position.column}, {device.position.row}</dd></div>
      </dl>
      <p className="config-summary">{configurationSummary(device.profile, device.config)}</p>
      {channels.map((channel) => {
        const channelBusy = busyChannelKeys.has(`${device.id}:${channel.id}`)
        const channelPending = (channel.reportedStatus === 'ON' || channel.reportedStatus === 'OFF') &&
          channel.desiredStatus !== channel.reportedStatus
        return (
          <div className="control-panel" key={channel.id}>
            <div>
              <p className="control-title">{channel.name}</p>
              <p className="muted">Desired {channel.desiredStatus} · Reported {channel.reportedStatus}
                {channelPending ? ' · PENDING' : ''}</p>
            </div>
            <div className="button-row">
              {statusOptions.map((status) => (
                <button key={status}
                  className={channel.reportedStatus === status ? 'state-button is-selected' : 'state-button'}
                  type="button" disabled={channelBusy}
                  onClick={() => onReportChannel(channel.id, status)}>{status}</button>
              ))}
            </div>
          </div>
        )
      })}
      <div className="control-panel">
        <div><p className="control-title">Report physical state</p>
          <p className="muted">Writes only simulator-authorized reported fields.</p></div>
        <div className="button-row">
          {statusOptions.map((status) => (
            <button key={status} className={device.reported.status === status ? 'state-button is-selected' : 'state-button'}
              type="button" disabled={busy} onClick={() => onReport(status)}>{status}</button>
          ))}
        </div>
      </div>
      <footer className="device-footer"><span>{device.floorId} / {device.roomId ?? 'Unassigned room'}</span>
        <span>{busy ? 'Writing state…' : 'Ready'}</span></footer>
    </article>
  )
}

function formatProfile(profile: DeviceProfile | 'ALL') {
  return profile === 'ALL' ? 'All profiles' : profile.replaceAll('_', ' ')
}

function profileGlyph(profile: DeviceProfile) {
  return ({ OUTLET: '◉', MULTI_SWITCH: '≡', SAFETY_OUTLET: '⚠', LIGHT: '✦', CAMERA: '▣' })[profile]
}

function configurationSummary(profile: DeviceProfile, config: DeviceConfig) {
  if (profile === 'MULTI_SWITCH' && 'channels' in config) return `${config.channels.length} independent channels`
  if (profile === 'SAFETY_OUTLET' && 'maxOnDurationSeconds' in config) return `Maximum active time: ${config.maxOnDurationSeconds / 60} minutes`
  if (profile === 'LIGHT' && 'schedule' in config) return `Schedule ${config.schedule.enabled ? 'enabled' : 'disabled'} · ${config.schedule.timezone}`
  if (profile === 'CAMERA' && 'mediaUri' in config) return `${config.mediaType} · ${config.mediaUri}`
  return 'Continuous single-channel power'
}

export default App
