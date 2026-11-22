'use client';

import React, { useMemo, useState } from 'react';
import { BadgeCheck, ChevronDown } from 'lucide-react';
const SPECIAL_ID_ASSIGN_URL = 'https://zyora-server.onrender.com/api/owner/special-ids/assign';

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

  const matchingUsers = useMemo(() => {
    const query = userQuery.trim().toLowerCase();
    if (!query) return [];
    return users.filter(user => [user.name, user.hurryId, user.id, user.vipId]
      .some(value => String(value || '').toLowerCase().includes(query))).slice(0, 8);
  }, [users, userQuery]);

  // The history table is intentionally limited to users who currently have a VIP ID.
  const assignedUsers = useMemo(() => users.filter(user => {
    if (!String(user.vipId || '').trim()) return false;
    return !user.vipIdExpiresAt || user.vipIdExpiresAt > Date.now();
  }), [users]);

  const assignId = async () => {
    if (!selectedUser || !selectedUser.hurryId || !/^\d+$/.test(specialId.trim()) ||
        !/^\d{1,4}$/.test(days) || Number(days) < 1 || Number(days) > 3650) {
      setStatus('Select a user and enter a numeric VIP ID and valid days (1–3650).');
      return;
    }
    setSaving(true);
    setStatus('');
    try {
      const response = await fetch(SPECIAL_ID_ASSIGN_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-owner-key': ownerKey },
        body: JSON.stringify({
          recipientAccountId: selectedUser.hurryId,
          specialId: specialId.trim(),
          days: Number(days),
          badgeType: 'ID Badge'
        })
      });
      const responseText = await response.text();
      let result: any;
      try {
        result = JSON.parse(responseText);
      } catch {
        const isHtml = /^\s*<!doctype\s+html|^\s*<html/i.test(responseText);
        throw new Error(isHtml
          ? `Zyora API returned an HTML page (HTTP ${response.status}). The Render API route is unavailable or not deployed; check the Zyora-Server deployment.`
          : `Zyora API returned an invalid response (HTTP ${response.status}).`);
      }
      if (!response.ok || !result?.success) throw new Error(result?.error || `Could not assign ID (HTTP ${response.status})`);
      setStatus(result.notification === 'delivered'
        ? `ID ${specialId.trim()} assigned and Zyora System message delivered to ${selectedUser.name || 'user'}.`
        : result.notification === 'queued'
          ? `ID ${specialId.trim()} assigned; Zyora System message queued for ${selectedUser.name || 'user'}.`
          : `ID ${specialId.trim()} saved, but System message could not be queued. Check Render Redis connection.`);
      setSpecialId('');
      setDays('');
      setUserQuery('');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Could not assign ID');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="w-full max-w-5xl space-y-4">
      <div className="relative w-full max-w-xs">
        <input
          value={userQuery}
          onChange={event => setUserQuery(event.target.value)}
          placeholder="Search user by App ID or name"
          inputMode="search"
          className="h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none focus:border-slate-400"
        />
        {matchingUsers.length > 0 && (
          <div className="absolute left-0 right-0 top-12 z-30 max-h-64 overflow-y-auto rounded-lg border border-slate-200 bg-white p-1 shadow-xl">
            {matchingUsers.map(user => (
              <button
                key={user.id}
                type="button"
                onClick={() => {
                  setSelectedUser(user);
                  setUserQuery('');
                  setStatus('');
                }}
                className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left hover:bg-slate-50"
              >
                <img src={user.image || '/default-avatar.png'} alt="" className="h-8 w-8 rounded-full object-cover" />
                <span className="min-w-0 flex-1 truncate text-xs font-semibold text-slate-800">{user.name || 'User'}</span>
                <span className="text-xs text-slate-500">{user.hurryId || '—'}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="space-y-2">
        <label htmlFor="special-id-value" className="block text-xs font-bold text-slate-600">Special ID</label>
        <div className="flex flex-wrap items-center gap-2">
          <input
            id="special-id-value"
            value={specialId}
            onChange={event => setSpecialId(event.target.value.replace(/[^0-9]/g, ''))}
            placeholder="Enter ID number"
            inputMode="numeric"
            className="h-10 w-40 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none"
          />
          <div className="relative">
            <button type="button" onClick={() => setMenuOpen(value => !value)} className="flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700">
              <BadgeCheck className="h-4 w-4" /> ID Badge <ChevronDown className="h-4 w-4" />
            </button>
            {menuOpen && (
              <div className="absolute left-0 top-11 z-20 min-w-36 rounded-lg border border-slate-200 bg-white p-1 shadow-xl">
                <button type="button" onClick={() => setMenuOpen(false)} className="w-full rounded-md px-3 py-2 text-left text-xs font-semibold hover:bg-slate-50">ID Badge</button>
              </div>
            )}
          </div>
          <input
            value={days}
            onChange={event => setDays(event.target.value.replace(/[^0-9]/g, ''))}
            placeholder="Days"
            inputMode="numeric"
            className="h-10 w-20 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none"
          />
          <button type="button" onClick={assignId} disabled={saving || !selectedUser} className="h-10 rounded-lg bg-slate-900 px-4 text-xs font-bold text-white disabled:opacity-40">
            {saving ? 'Sending…' : 'Give ID'}
          </button>
        </div>
        {selectedUser && (
          <p className="text-xs text-slate-500">
            Selected: {selectedUser.name || 'User'} · App ID {selectedUser.hurryId || '—'}
            <button type="button" onClick={() => setSelectedUser(null)} className="ml-2 font-semibold text-slate-700 underline">Change</button>
          </p>
        )}
        {status && <p className="text-xs font-semibold text-slate-600">{status}</p>}
      </div>

      <div className="w-full overflow-x-auto">
        <table className="w-full min-w-[760px] border-collapse text-left">
          <thead>
            <tr className="border-b border-slate-100 text-[10px] font-extrabold uppercase tracking-wider text-slate-500">
              <th className="px-3 py-3">S.No</th>
              <th className="px-3 py-3">User Avatar</th>
              <th className="px-3 py-3">Name</th>
              <th className="px-3 py-3">App ID</th>
              <th className="px-3 py-3">VIP ID</th>
              <th className="px-3 py-3">Days</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {assignedUsers.map((user, index) => {
              const remaining = user.vipIdExpiresAt
                ? Math.max(0, Math.ceil((user.vipIdExpiresAt - Date.now()) / 86400000))
                : null;
              return (
                <tr key={user.id} className="text-sm hover:bg-slate-50/60">
                  <td className="px-3 py-3 text-xs font-bold text-slate-500">{index + 1}</td>
                  <td className="px-3 py-3"><img src={user.image || '/default-avatar.png'} alt="" className="h-9 w-9 rounded-full border border-slate-200 bg-slate-100 object-cover" /></td>
                  <td className="px-3 py-3 font-semibold text-slate-800">{user.name || 'User'}</td>
                  <td className="px-3 py-3 font-semibold text-slate-700">{user.hurryId || '—'}</td>
                  <td className="px-3 py-3 font-semibold text-slate-700">
                    <span className="inline-flex items-center gap-1">
                      <img src="/IMG_20261101_125212.png" alt="ID Badge" className="h-5 w-auto object-contain" />
                      {user.vipId}
                    </span>
                  </td>
                  <td className="px-3 py-3 text-slate-600">{remaining ?? user.vipIdDays ?? user.vipDays ?? user.specialIdDays ?? '—'}</td>
                </tr>
              );
            })}
            {assignedUsers.length === 0 && (
              <tr><td colSpan={6} className="px-3 py-10 text-center text-sm font-medium text-slate-400">No users have an active VIP ID yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
