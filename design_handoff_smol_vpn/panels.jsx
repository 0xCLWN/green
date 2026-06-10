/* panels.jsx — Settings, Edit Server, Import (pushed screens) */
const { useState } = React;

function PushScreen({ show, title, onBack, children, rightSlot }) {
  return (
    <div className={'push' + (show ? ' show' : '')}>
      <div className="pushhead">
        <div className="iconbtn" onClick={onBack}><IconBack /></div>
        <h3>{title}</h3>
        <div className="spacer" />
        {rightSlot}
      </div>
      <div className="pushbody">{children}</div>
    </div>
  );
}

function Toggle({ on, onChange }) {
  return <div className={'toggle' + (on ? ' on' : '')} onClick={() => onChange(!on)} />;
}

function SettingsScreen({ show, onBack, settings, setSettings, onSplit, onImport }) {
  const set = (k, v) => setSettings(s => ({ ...s, [k]: v }));
  const Row = ({ title, sub, children, onClick, only }) => (
    <div className={'setrow' + (only ? ' only' : '')} onClick={onClick} style={{ cursor: onClick ? 'pointer' : 'default' }}>
      <div><div className="st">{title}</div>{sub && <div className="ss">{sub}</div>}</div>
      <div className="spacer" />
      {children}
    </div>
  );
  return (
    <PushScreen show={show} title="Settings" onBack={onBack}>
      <div className="setsec">
        <div className="label">Connection</div>
        <Row title="Auto-connect on launch" sub="Connect the last server at startup">
          <Toggle on={settings.autoConnect} onChange={v => set('autoConnect', v)} />
        </Row>
        <Row title="Kill switch" sub="Block all traffic if the tunnel drops">
          <Toggle on={settings.killSwitch} onChange={v => set('killSwitch', v)} />
        </Row>
        <Row title="Always-on VPN" sub="Keep the tunnel running in background">
          <Toggle on={settings.alwaysOn} onChange={v => set('alwaysOn', v)} />
        </Row>
      </div>

      <div className="setsec">
        <div className="label">Routing</div>
        <Row title="Split tunneling" sub="Choose which apps use the VPN" onClick={onSplit}>
          <div className="val"><b style={{ color: 'var(--accent)', fontFamily: 'var(--mono)' }}>3 apps</b><IconChevron size={16} stroke="var(--dim)" /></div>
        </Row>
        <Row title="DNS" onClick={() => {}}>
          <div className="val">Automatic<IconChevron size={16} stroke="var(--dim)" /></div>
        </Row>
        <Row title="Protocol" onClick={() => {}}>
          <div className="val">VLESS · TCP<IconChevron size={16} stroke="var(--dim)" /></div>
        </Row>
      </div>

      <div className="setsec">
        <div className="label">Data</div>
        <Row title="Import / subscriptions" sub="QR, clipboard or a subscription link" onClick={onImport}>
          <IconChevron size={16} stroke="var(--dim)" />
        </Row>
      </div>

      <div className="setsec">
        <div className="label">General</div>
        <Row title="Connection notifications">
          <Toggle on={settings.notify} onChange={v => set('notify', v)} />
        </Row>
        <Row title="App version" only>
          <div className="val">1.2.0</div>
        </Row>
      </div>
    </PushScreen>
  );
}

function EditServerScreen({ show, onBack, server, onSave, onDelete }) {
  const [name, setName] = useState(server.name);
  const [uri, setUri] = useState(server.uri);
  const [tested, setTested] = useState(null); // null | 'testing' | 'ok'
  React.useEffect(() => { setName(server.name); setUri(server.uri); setTested(null); }, [server]);
  const test = () => {
    setTested('testing');
    setTimeout(() => setTested('ok'), 1400);
  };
  return (
    <PushScreen show={show} title="Edit server" onBack={onBack}
      rightSlot={<div className="btn primary" style={{ flex: 'none', padding: '9px 18px' }} onClick={() => onSave({ ...server, name, uri })}>Save</div>}>
      <div className="field">
        <div className="label">Name</div>
        <input value={name} onChange={e => setName(e.target.value)} />
      </div>
      <div className="field">
        <div className="label">VLESS URI</div>
        <textarea rows={4} value={uri} onChange={e => setUri(e.target.value)} />
      </div>
      <div className="field">
        <div className="label">Reachability</div>
        <button className={'testbtn' + (tested === 'ok' ? ' ok' : '')} style={{ width: '100%', background: 'var(--surface)', border: '1px solid var(--border2)', color: tested === 'ok' ? 'var(--accent)' : 'var(--text)' }} onClick={test}>
          {tested === 'testing' && <><span className="spin">⟳</span> testing route…</>}
          {tested === 'ok' && <><IconBolt size={15} /> route ok · 41 ms</>}
          {!tested && <><IconBolt size={15} /> Test connection</>}
        </button>
      </div>
      <div className="spacer" style={{ minHeight: 8 }} />
      <button className="btn danger" style={{ width: '100%' }} onClick={onDelete}><IconTrash size={17} /> Delete server</button>
    </PushScreen>
  );
}

function ImportScreen({ show, onBack }) {
  const opts = [
    { ic: <IconQR size={22} stroke="var(--accent)" />, t: 'Scan QR code', s: 'Point the camera at a VLESS QR' },
    { ic: <IconClip size={20} stroke="var(--accent)" />, t: 'Paste from clipboard', s: 'vless:// link detected' },
    { ic: <IconLink size={20} stroke="var(--accent)" />, t: 'Subscription link', s: 'A group that auto-updates' },
    { ic: <IconPencil size={19} stroke="var(--accent)" />, t: 'Enter manually', s: 'Type or paste the config' },
  ];
  return (
    <PushScreen show={show} title="Add a server" onBack={onBack}>
      <div className="qr">
        {[1,1,1,0,1,1,1, 1,0,1,1,0,0,1, 1,1,0,1,1,0,1, 0,0,1,0,1,1,0, 1,1,0,1,0,1,1, 1,0,1,1,1,0,0, 1,1,0,1,0,1,1].map((b, i) => <i key={i} className={b ? '' : 'o'} />)}
      </div>
      <div className="label" style={{ textAlign: 'center', marginBottom: 14 }}>or add another way</div>
      {opts.map((o, i) => (
        <div className="impopt" key={i}>
          <div className="ig">{o.ic}</div>
          <div style={{ flex: 1 }}><div className="it">{o.t}</div><div className="is">{o.s}</div></div>
          <IconChevron size={18} className="ar" />
        </div>
      ))}
    </PushScreen>
  );
}

Object.assign(window, { SettingsScreen, EditServerScreen, ImportScreen, PushScreen, Toggle });
