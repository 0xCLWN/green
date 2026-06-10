/* app.jsx — smol vpn hi-fi prototype shell */
const { useState, useEffect, useRef } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "#57E08A",
  "lockWhenConnected": true,
  "showGraph": true
}/*EDITMODE-END*/;

const SERVERS = [
  { id: 's1', name: 'zigga-maria', flag: '🇩🇪', region: 'frankfurt · de', ping: 42, uri: 'vless://bfa80d2c-f47a-4ed5-80c2-2b79de211ad4@zigger.isgd.net:443?type=tcp&security=reality&pbk=xK2…#zigga-maria' },
  { id: 's2', name: 'frank-01', flag: '🇳🇱', region: 'amsterdam · nl', ping: 88, uri: 'vless://7c91a02e-2b41-4f7a-9c3d-1ed5a4b80f12@fra.example.net:8443?type=ws&path=/v#frank-01' },
  { id: 's3', name: 'tokyo-relay', flag: '🇯🇵', region: 'tokyo · jp', ping: null, uri: 'vless://aa10ff2c-9c41-4ed5-80c2-d4b80f1212ad@tk.relay.io:443?type=grpc#tokyo-relay' },
];

function SplitLine({ readOnly, tunneled, onClick }) {
  const txt = tunneled === 0
    ? <>Whole-phone VPN · <b>all apps</b></>
    : <>Split tunnel · <b>{tunneled} apps</b> tunneled</>;
  return (
    <div className={'splitline' + (readOnly ? ' ro' : '')} onClick={readOnly ? undefined : onClick}>
      <div className="sic"><IconSplit size={17} stroke={readOnly ? '#bfffd5' : 'var(--accent)'} /></div>
      <div className="stx">{txt}</div>
      {!readOnly && <div className="sgo">manage ›</div>}
    </div>
  );
}

function ConnectedLayer({ server, tunneled, onDisconnect, onLocked, onSettings, lock, showGraph, shake }) {
  const [secs, setSecs] = useState(0);
  const [test, setTest] = useState(null);
  const [bars, setBars] = useState([40, 62, 48, 80, 55, 72, 50, 78, 60, 70]);
  useEffect(() => {
    const t = setInterval(() => setSecs(s => s + 1), 1000);
    const g = setInterval(() => setBars(b => [...b.slice(1), 30 + Math.round(Math.random() * 65)]), 1100);
    return () => { clearInterval(t); clearInterval(g); };
  }, []);
  const pad = n => String(n).padStart(2, '0');
  const h = Math.floor(secs / 3600), m = Math.floor((secs % 3600) / 60), s = secs % 60;
  const uptime = (h ? pad(h) + ':' : '') + pad(m) + ':' + pad(s);
  const runTest = () => { if (test === 'testing') return; setTest('testing'); setTimeout(() => setTest('ok'), 1500); };
  return (
    <div className={'layer' + (shake ? ' shake' : '')}>
      <div className="grab" />
      <div className="ltop">
        <div className="onpill"><span className="pulse" /> Connected · secure</div>
        {lock
          ? <div className="lockchip" onClick={onLocked} style={{ cursor: 'pointer' }}><IconLock size={13} stroke="#cdeed8" /> settings locked</div>
          : <div className="lockchip" onClick={onSettings} style={{ cursor: 'pointer' }}><IconGear size={13} stroke="#cdeed8" /> settings</div>}
      </div>
      <div className="bigserver">{server.name}</div>
      <div className="lmeta">{server.flag} {server.region} · vless · reality</div>

      <SplitLine readOnly tunneled={tunneled} />

      <div className="statgrid">
        <div className="sbox"><div className="k">Ping</div><div className="v">{server.ping || 41}<small> ms</small></div></div>
        <div className="sbox"><div className="k">Uptime</div><div className="v">{uptime}</div></div>
        <div className="sbox"><div className="k"><IconDown size={13} /> Download</div><div className="v">1.2<small> MB/s</small></div></div>
        <div className="sbox"><div className="k"><IconUp size={13} /> Upload</div><div className="v">0.3<small> MB/s</small></div></div>
        {showGraph && <div className="sbox span">
          <div className="k" style={{ display: 'flex', justifyContent: 'space-between' }}><span>Throughput</span><span style={{ fontFamily: 'var(--mono)', textTransform: 'none', letterSpacing: 0 }}>412 MB ↓ · 88 MB ↑</span></div>
          <div className="graph">{bars.map((b, i) => <i key={i} style={{ height: b + '%' }} />)}</div>
        </div>}
      </div>

      <div className="layeractions">
        <button className={'testbtn' + (test === 'ok' ? ' ok' : '')} onClick={runTest}>
          {test === 'testing' && <><span className="spin">⟳</span> re-testing route…</>}
          {test === 'ok' && <><IconBolt size={15} /> route healthy · 41 ms</>}
          {!test && <><IconBolt size={15} /> Test connection</>}
        </button>
        <button className="disconnect" onClick={onDisconnect}><IconPower size={18} /> Disconnect</button>
      </div>
    </div>
  );
}

function App({ t }) {
  const [connected, setConnected] = useState(false);
  const [selId, setSelId] = useState('s1');
  const [servers, setServers] = useState(SERVERS);
  const [tunneled, setTunneled] = useState(3);
  const [view, setView] = useState(null); // 'settings' | 'edit' | 'import'
  const [editId, setEditId] = useState('s1');
  const [settings, setSettings] = useState({ autoConnect: true, killSwitch: true, alwaysOn: false, notify: true });
  const [toast, setToast] = useState(null);
  const [shake, setShake] = useState(false);
  const toastT = useRef(null);

  const server = servers.find(s => s.id === selId) || servers[0];
  const editServer = servers.find(s => s.id === editId) || servers[0];

  const showToast = (msg, warn) => {
    setToast({ msg, warn });
    clearTimeout(toastT.current);
    toastT.current = setTimeout(() => setToast(null), 2100);
  };
  const nudge = (msg) => { setShake(true); setTimeout(() => setShake(false), 450); showToast(msg, true); };

  return (
    <div className={'app' + (connected ? ' connected' : '')}>
      {/* ---------- HOME (idle) ---------- */}
      <div className="screen">
        <div className="appbar">
          <div className="word"><span className="mark"><IconShield size={17} stroke="var(--accent)" /></span> smol vpn</div>
          <div className="iconbtn" onClick={() => setView('settings')}><IconGear size={20} /></div>
        </div>

        <div className="statusblock">
          <div className="shield"><IconShield stroke="var(--dim)" /></div>
          <h2>Not connected</h2>
          <p>Pick a server and tap connect to secure this device.</p>
        </div>

        <SplitLine tunneled={tunneled} onClick={() => setTunneled(t => (t === 0 ? 3 : 0))} />

        <div className="label" style={{ margin: '20px 2px 9px' }}>Servers</div>
        <div className="servers">
          {servers.map(s => (
            <div key={s.id} className={'scard' + (s.id === selId ? ' sel' : '')} onClick={() => setSelId(s.id)}>
              <div className="radio" />
              <div className="sinfo">
                <div className="nm"><span className="flag">{s.flag}</span>{s.name}</div>
                <div className="meta">{s.region}</div>
              </div>
              <div className={'ping' + (s.ping && s.ping < 60 ? ' good' : '')}>{s.ping ? s.ping + ' ms' : '— ms'}</div>
              <div className="dots" onClick={(e) => { e.stopPropagation(); setEditId(s.id); setView('edit'); }}><IconDots size={18} /></div>
            </div>
          ))}
          <div className="addcard" onClick={() => setView('import')}>
            <IconPlus size={18} /> Add server
          </div>
        </div>

        <button className="connect" onClick={() => setConnected(true)}><IconPower size={19} /> Connect</button>
      </div>

      {/* ---------- CONNECTED LAYER ---------- */}
      <ConnectedLayer server={server} tunneled={tunneled} shake={shake}
        lock={t.lockWhenConnected} showGraph={t.showGraph}
        onDisconnect={() => setConnected(false)}
        onSettings={() => setView('settings')}
        onLocked={() => nudge('Disconnect to change settings')} />

      {/* ---------- PUSHED SCREENS (only reachable while disconnected) ---------- */}
      <SettingsScreen show={view === 'settings'} onBack={() => setView(null)}
        settings={settings} setSettings={setSettings}
        onSplit={() => {}} onImport={() => setView('import')} />
      <EditServerScreen show={view === 'edit'} onBack={() => setView(null)} server={editServer}
        onSave={(srv) => { setServers(list => list.map(x => x.id === srv.id ? srv : x)); setView(null); showToast('Server saved'); }}
        onDelete={() => { setServers(list => list.filter(x => x.id !== editServer.id)); setView(null); showToast('Server deleted'); }} />
      <ImportScreen show={view === 'import'} onBack={() => setView(null)} />

      {/* ---------- TOAST ---------- */}
      <div className={'toast' + (toast ? ' show' : '')}>
        {toast && toast.warn && <span className="tl"><IconLock size={14} stroke="var(--warn)" /></span>}
        {toast && toast.msg}
      </div>
    </div>
  );
}

const ACCENTS = {
  '#57E08A': { press: '#46c576', soft: 'rgba(87,224,138,.14)', on: '#06200F', gA: '#1C7E50', gB: '#0E5536', glow: '#9affc0' },
  '#5AB0F0': { press: '#3f97da', soft: 'rgba(90,176,240,.14)', on: '#04202F', gA: '#1C5F7E', gB: '#0E3E55', glow: '#c3e4ff' },
  '#A98BF0': { press: '#8f6fe0', soft: 'rgba(169,139,240,.14)', on: '#1A0F33', gA: '#553C8A', gB: '#392A66', glow: '#ddccff' },
  '#F0B44A': { press: '#e0a233', soft: 'rgba(240,180,74,.14)', on: '#2A1C06', gA: '#8A6320', gB: '#5E4310', glow: '#ffe2ab' },
};

function Root() {
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
  useEffect(() => {
    const a = ACCENTS[t.accent] || ACCENTS['#57E08A'];
    const r = document.documentElement.style;
    r.setProperty('--accent', t.accent);
    r.setProperty('--accent-press', a.press);
    r.setProperty('--accent-soft', a.soft);
    r.setProperty('--on-accent', a.on);
    r.setProperty('--grad-a', a.gA);
    r.setProperty('--grad-b', a.gB);
    r.setProperty('--glow', a.glow);
  }, [t.accent]);
  return (
    <React.Fragment>
      <div id="scaler">
        <AndroidDevice dark width={400} height={840}>
          <App t={t} />
        </AndroidDevice>
      </div>
      <TweaksPanel>
        <TweakSection label="Appearance" />
        <TweakColor label="Accent" value={t.accent}
          options={Object.keys(ACCENTS)}
          onChange={(v) => setTweak('accent', v)} />
        <TweakSection label="Behavior" />
        <TweakToggle label="Lock settings while connected" value={t.lockWhenConnected}
          onChange={(v) => setTweak('lockWhenConnected', v)} />
        <TweakToggle label="Throughput graph" value={t.showGraph}
          onChange={(v) => setTweak('showGraph', v)} />
      </TweaksPanel>
    </React.Fragment>
  );
}

ReactDOM.createRoot(document.getElementById('vpnroot')).render(<Root />);

function fitDevice() {
  const s = document.getElementById('scaler');
  if (!s) return;
  const scale = Math.min(1, (window.innerHeight - 28) / 840, (window.innerWidth - 28) / 400);
  s.style.transform = 'scale(' + scale + ')';
}
window.addEventListener('resize', fitDevice);
setTimeout(fitDevice, 60);
