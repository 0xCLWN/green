/* icons.jsx — inline stroke icons for smol vpn */
const I = ({ d, size = 20, sw = 1.8, fill = 'none', stroke = 'currentColor', children, vb = 24 }) => (
  <svg width={size} height={size} viewBox={`0 0 ${vb} ${vb}`} fill={fill} stroke={stroke}
    strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round">
    {d ? <path d={d} /> : children}
  </svg>
);

const IconGear = (p) => <I {...p}><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" /></I>;
const IconShield = ({ size = 46, ...p }) => <I size={size} sw={1.6} {...p}><path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3z" /></I>;
const IconShieldCheck = ({ size = 46, stroke = '#9affc0', ...p }) => <I size={size} sw={1.8} stroke={stroke} {...p}><path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3z" /><path d="M9 11.5l2 2 4-4" /></I>;
const IconChevron = (p) => <I {...p} d="M9 6l6 6-6 6" />;
const IconBack = (p) => <I {...p} d="M15 6l-6 6 6 6" />;
const IconDots = (p) => <I {...p}><circle cx="12" cy="5" r="1.4" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none" /><circle cx="12" cy="19" r="1.4" fill="currentColor" stroke="none" /></I>;
const IconPlus = (p) => <I {...p}><path d="M12 5v14M5 12h14" /></I>;
const IconSplit = (p) => <I {...p}><path d="M6 3v6a4 4 0 0 0 4 4h8" /><path d="M6 21v-6" /><path d="M15 9l3-3-3-3" /><path d="M15 17l3-3-3-3" /></I>;
const IconBolt = (p) => <I {...p} fill="currentColor" stroke="none" d="M13 2L4.5 13H11l-1 9 8.5-11H12l1-9z" />;
const IconPower = (p) => <I {...p}><path d="M12 4v8" /><path d="M7 6.5a8 8 0 1 0 10 0" /></I>;
const IconQR = (p) => <I {...p}><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><path d="M14 14h3v3M21 14v.01M21 21v-4M14 21h3" /></I>;
const IconClip = (p) => <I {...p}><rect x="6" y="4" width="12" height="17" rx="2" /><path d="M9 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1" /></I>;
const IconLink = (p) => <I {...p}><path d="M10 13a5 5 0 0 0 7 0l2-2a5 5 0 0 0-7-7l-1 1" /><path d="M14 11a5 5 0 0 0-7 0l-2 2a5 5 0 0 0 7 7l1-1" /></I>;
const IconPencil = (p) => <I {...p}><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" /></I>;
const IconTrash = (p) => <I {...p}><path d="M4 7h16M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" /></I>;
const IconDown = (p) => <I {...p}><path d="M12 4v13M6 11l6 6 6-6" /></I>;
const IconUp = (p) => <I {...p}><path d="M12 20V7M6 13l6-6 6 6" /></I>;
const IconClose = (p) => <I {...p}><path d="M6 6l12 12M18 6L6 18" /></I>;
const IconLock = (p) => <I {...p}><rect x="5" y="11" width="14" height="9" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></I>;

Object.assign(window, {
  IconGear, IconShield, IconShieldCheck, IconChevron, IconBack, IconDots, IconPlus,
  IconSplit, IconBolt, IconPower, IconQR, IconClip, IconLink, IconPencil, IconTrash,
  IconDown, IconUp, IconClose, IconLock,
});
