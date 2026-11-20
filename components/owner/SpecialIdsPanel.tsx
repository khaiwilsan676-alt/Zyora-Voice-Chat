'use client';

import React, { useMemo, useState } from 'react';
import { BadgeCheck, ChevronDown } from 'lucide-react';
import { apiUrl } from '@/src/lib/api';

type UserRow = {
  id: string;
  name?: string;
  hurryId?: string;
  image?: string;
  vipId?: string;
  vipIdDays?: number | string;
  vipIdExpiresAt?: number;
  vipIdBadge?: boolean;
  vipDays?: number | string;
  specialIdDays?: number | string;
};

export default function SpecialIdsPanel({ users = [], ownerKey }: { users?: UserRow[]; ownerKey: string }) {
  const [userQuery, setUserQuery] = useState('');
  const [specialId, setSpecialId] = useState('');
  const [days, setDays] = useState('');
  const [selectedUser, setSelectedUser] = useState<UserRow | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState('');

  const visibleUsers = useMemo(() => {
    const query = userQuery.trim().toLowerCase();
    return users.filter(user => !query || [user.name, user.hurryId, user.id, user.vipId]
      .some(value => String(value || '').toLowerCase().includes(query)));
  }, [users, userQuery]);

  const assignId = async () => {
    if (!selectedUser || !/^\d+$/.test(specialId.trim()) || !/^\d{1,4}$/.test(days) || Number(days) < 1 || Number(days) > 3650) {
      setStatus('Select a user and enter a VIP ID using digits only, plus valid days.');
      return;
    }
    setSaving(true);
    setStatus('');
    try {
      const response = await fetch(apiUrl('/api/owner/special-ids/assign'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-owner-key': ownerKey },
        body: JSON.stringify({ recipientAccountId: selectedUser.hurryId, specialId: specialId.trim(), days: Number(days), badgeType: 'ID Badge' })
      });
      const result = await response.json();
      if (!response.ok || !result?.success) throw new Error(result?.error || 'Could not assign ID');
      setStatus(result.notification === 'delivered'
        ? `ID ${specialId.trim()} assigned and Zyora System message delivered to ${selectedUser.name || 'user'}.`
        : result.notification === 'queued'
          ? `ID ${specialId.trim()} assigned; Zyora System message queued for ${selectedUser.name || 'user'}.`
          : `ID ${specialId.trim()} saved, but System message could not be queued. Check Render Redis connection.`);
      setSpecialId('');
      setDays('');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Could not assign ID');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="w-full max-w-5xl space-y-4">
      <input value={userQuery} onChange={event => setUserQuery(event.target.value)} placeholder="Search User ID" inputMode="numeric"
        className="h-11 w-full max-w-xs rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none focus:border-slate-400" />

      <div className="space-y-2">
        <label htmlFor="special-id-value" className="block text-xs font-bold text-slate-600">Special ID</label>
        <div className="flex flex-wrap items-center gap-2">
          <input id="special-id-value" value={specialId} onChange={event => setSpecialId(event.target.value.replace(/[^0-9]/g, ''))}
            placeholder="Enter ID number" inputMode="numeric" className="h-10 w-40 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none" />
          <div className="relative">
            <button type="button" onClick={() => setMenuOpen(value => !value)} className="flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700">
              <BadgeCheck className="h-4 w-4" /> ID Badge <ChevronDown className="h-4 w-4" />
            </button>
            {menuOpen && <div className="absolute left-0 top-11 z-20 min-w-36 rounded-lg border border-slate-200 bg-white p-1 shadow-xl">
              <button type="button" onClick={() => setMenuOpen(false)} className="w-full rounded-md px-3 py-2 text-left text-xs font-semibold hover:bg-slate-50">ID Badge</button>
            </div>}
          </div>
          <input value={days} onChange={event => setDays(event.target.value.replace(/[^0-9]/g, ''))} placeholder="Days" inputMode="numeric"
            className="h-10 w-20 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none" />
          <button type="button" onClick={assignId} disabled={saving || !selectedUser} className="h-10 rounded-lg bg-slate-900 px-4 text-xs font-bold text-white disabled:opacity-40">
            {saving ? 'Sending…' : 'Give ID'}
          </button>
        </div>
        {selectedUser && <p className="text-xs text-slate-500">Selected: {selectedUser.name || 'User'} · App ID {selectedUser.hurryId || '—'}</p>}
        {status && <p className="text-xs font-semibold text-slate-600">{status}</p>}
      </div>

      <div className="w-full overflow-x-auto">
        <table className="w-full min-w-[760px] border-collapse text-left">
          <thead><tr className="border-b border-slate-100 text-[10px] font-extrabold uppercase tracking-wider text-slate-500">
            <th className="px-3 py-3">S.No</th><th className="px-3 py-3">User Avatar</th><th className="px-3 py-3">Name</th>
            <th className="px-3 py-3">App ID</th><th className="px-3 py-3">VIP ID</th><th className="px-3 py-3">Day's</th>
          </tr></thead>
          <tbody className="divide-y divide-slate-50">
            {visibleUsers.map((user, index) => {
              const remaining = user.vipIdExpiresAt ? Math.max(0, Math.ceil((user.vipIdExpiresAt - Date.now()) / 86400000)) : null;
              const active = Boolean(user.vipId && (remaining === null || remaining > 0));
              return <tr key={user.id} onClick={() => setSelectedUser(user)} className={`cursor-pointer text-sm hover:bg-slate-50/60 ${selectedUser?.id === user.id ? 'bg-blue-50' : ''}`}>
                <td className="px-3 py-3 text-xs font-bold text-slate-500">{index + 1}</td>
                <td className="px-3 py-3"><img src={user.image || '/default-avatar.png'} alt="" className="h-9 w-9 rounded-full border border-slate-200 bg-slate-100 object-cover" /></td>
                <td className="px-3 py-3 font-semibold text-slate-800">{user.name || 'User'}</td>
                <td className="px-3 py-3 font-semibold text-slate-700">{user.hurryId || '—'}</td>
                <td className="px-3 py-3 font-semibold text-slate-700">{active ? <span className="inline-flex items-center gap-1"><img src="/IMG_20261101_125212.png" alt="ID Badge" className="h-5 w-auto object-contain" />{user.vipId}</span> : '—'}</td>
                <td className="px-3 py-3 text-slate-600">{active ? (remaining ?? user.vipIdDays ?? user.vipDays ?? user.specialIdDays ?? '—') : '—'}</td>
              </tr>;
            })}
            {visibleUsers.length === 0 && <tr><td colSpan={6} className="px-3 py-10 text-center text-sm font-medium text-slate-400">No users found</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
