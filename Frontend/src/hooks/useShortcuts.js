import { useEffect } from 'react';
import { ROUTE_META } from '@/lib/routes';

export default function useShortcuts({ navigate, setCmdkOpen }) {
  useEffect(() => {
    const handler = (e) => {
      const meta = e.metaKey || e.ctrlKey;
      if (meta && e.key === 'k') { e.preventDefault(); setCmdkOpen(o => !o); return; }
      // find matching shortcut
      if (meta) {
        for (const [path, info] of Object.entries(ROUTE_META)) {
          if (info.shortcut && e.key === info.shortcut && !(e.key === 's' && e.shiftKey)) {
            e.preventDefault();
            navigate(path);
            return;
          }
        }
      }
      if (e.key === 'Escape') setCmdkOpen(false);
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [navigate, setCmdkOpen]);
}
