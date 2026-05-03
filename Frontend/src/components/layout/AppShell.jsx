import React, { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import Topbar from '@/components/layout/Topbar';
import CommandPalette from '@/components/layout/CommandPalette';
import useShortcuts from '@/hooks/useShortcuts';

export default function AppShell() {
  const [cmdkOpen, setCmdkOpen] = useState(false);
  const navigate = useNavigate();
  useShortcuts({ navigate, setCmdkOpen });
  return (
    <div className="app">
      <Sidebar />
      <Topbar onOpenCmdk={() => setCmdkOpen(true)} />
      <main className="main"><Outlet /></main>
      {cmdkOpen && <CommandPalette onClose={() => setCmdkOpen(false)} />}
    </div>
  );
}
