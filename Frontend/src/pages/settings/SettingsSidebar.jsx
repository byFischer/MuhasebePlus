import Icon from '@/components/mp/Icon';

export default function SettingsSidebar({ active, onSelect, tabs }) {
  return (
    <nav className="settings-sidebar">
      {Object.entries(tabs).map(([key, tab]) => (
        <button
          key={key}
          className={`settings-nav-item ${active === key ? 'on' : ''}`}
          onClick={() => onSelect(key)}
        >
          <Icon name={tab.icon} size={15} />
          <span>{tab.label}</span>
        </button>
      ))}
    </nav>
  );
}
