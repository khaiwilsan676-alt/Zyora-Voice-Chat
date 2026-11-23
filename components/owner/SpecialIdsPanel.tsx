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
  ownerImageTag?: string;
  ownerImageTagExpiresAt?: number;
  vipDays?: number | string;
  specialIdDays?: number | string;
  officialHeadTag?: boolean; officialTag?: boolean; adminTag?: boolean; serviceTag?: boolean;
  hostTag?: boolean; coinSellerTag?: boolean; vipTag?: boolean; premiumTag?: boolean;
  officialHeadTagExpiresAt?: number; officialTagExpiresAt?: number; adminTagExpiresAt?: number;
  serviceTagExpiresAt?: number; hostTagExpiresAt?: number; coinSellerTagExpiresAt?: number;
  vipTagExpiresAt?: number; premiumTagExpiresAt?: number;
};

const PROFILE_TAGS = [
  { key: 'officialHeadTag', label: 'Official Head', image: '/file_000000005c388246a5d95dba880d7eb9.png' },
  { key: 'officialTag', label: 'Official', image: '/IMG_20261027_102250.png' },
  { key: 'adminTag', label: 'Admin', image: '/IMG_20261027_114117.png' },
  { key: 'serviceTag', label: 'Service', image: '/IMG_20261027_102207.png' },
  { key: 'hostTag', label: 'Host', image: '/IMG_20261028_154914.png' },
  { key: 'coinSellerTag', label: 'Coin Seller', image: '/IMG_20261027_105359.png' },
  { key: 'vipTag', label: 'VIP', image: '/IMG_20261027_102250.png' },
  { key: 'premiumTag', label: 'Premium', image: '/IMG_20261027_102250.png' },
] as const;
const OFFICIAL_IMAGE_TAG = '/IMG_20261101_125212.png';

export default function SpecialIdsPanel({ users = [], ownerKey }: { users?: UserRow[]; ownerKey: string }) {
  const [userQuery, setUserQuery] = useState('');
  const [specialId, setSpecialId] = useState('');
  const [days, setDays] = useState('');
  const [kind, setKind] = useState<'id' | 'tag'>('id');
  const [tagImage, setTagImage] = useState(OFFICIAL_IMAGE_TAG);
  const [tagKey, setTagKey] = useState<string>('ownerImageTag');
  const [permanent, setPermanent] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserRow | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState('');

  const matchingUsers = useMemo(() => {
    const query = userQuery.trim().toLowerCase();
    if (!query) return [];
    return users.filter(user => [user.name, user.hurryId, user.id, user.vipId, (user as any).vipID, (user as any).specialId]
      .some(value => String(value || '').toLowerCase().includes(query))).slice(0, 8);
  }, [users, userQuery]);

  // Show users with an active Special ID or owner-assigned image tag.
  const assignedUsers = useMemo(() => users.filter(user => {
    const idActive = Boolean(user.vipId && (!user.vipIdExpiresAt || user.vipIdExpiresAt > Date.now()));
    const tagActive = Boolean(user.ownerImageTag && (!user.ownerImageTagExpiresAt || user.ownerImageTagExpiresAt > Date.now()));
    const profileTagActive = PROFILE_TAGS.some(tag => Boolean(user[tag.key] && (!(user as any)[tag.key + 'ExpiresAt'] || (user as any)[tag.key + 'ExpiresAt'] > Date.now())));
    return idActive || tagActive || profileTagActive;
  }), [users]);

  const assignId = async () => {
    if (!selectedUser || !selectedUser.hurryId || (kind === 'id' ? !/^\d+$/.test(specialId.trim()) : (tagKey === 'ownerImageTag' ? !tagImage : !PROFILE_TAGS.some(tag => tag.key === tagKey))) || (!permanent && (!/^\d{1,4}$/.test(days) || Number(days) < 1 || Number(days) > 3650))) {
      setStatus('Select a user and enter a valid ID/tag and duration.');
      return;
    }
    setSaving(true);
    setStatus('');
    try {
      const response = await fetch(apiUrl('/api/owner/special-ids/assign'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-owner-key': ownerKey },
        body: JSON.stringify({
          recipientAccountId: selectedUser.hurryId,
          assignmentType: kind === 'id' ? 'id' : tagKey === 'ownerImageTag' ? 'imageTag' : 'profileTag',
          specialId: specialId.trim(),
          tagImage,
          tagKey,
          permanent,
          days: permanent ? 0 : Number(days),
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
      setStatus(`${kind === 'id' ? 'Special ID' : 'Image tag'} ${permanent ? 'permanently' : 'for ' + days + ' days'} assigned.`);
      setSpecialId(''); setTagImage(OFFICIAL_IMAGE_TAG); setDays(''); setPermanent(false);
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
          className="h-11 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-semibold text-slate-900 outline-none focus:border-[#197477]"
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
        <label className="block text-xs font-bold text-slate-600">Special ID / Tags</label>
        <div className="flex gap-2"><button type="button" onClick={() => setKind("id")} className="rounded-lg bg-slate-100 px-3 py-2 text-xs font-bold">Special ID</button><button type="button" onClick={() => setKind("tag")} className="rounded-lg bg-slate-100 px-3 py-2 text-xs font-bold">Image Tag</button></div>
        <div className="flex flex-wrap items-center gap-2">
          {kind === "id" ? <input
            id="special-id-value"
            value={specialId}
            onChange={event => setSpecialId(event.target.value.replace(/[^0-9]/g, ''))}
            placeholder="Enter ID number"
            inputMode="numeric"
            className="h-10 w-40 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none"
          /> : <div className="flex flex-wrap items-center gap-2">
            <select value={tagKey} onChange={event => setTagKey(event.target.value)} className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700">
              <option value="ownerImageTag">Official Image Tag</option>
              {PROFILE_TAGS.map(tag => <option key={tag.key} value={tag.key}>{tag.label}</option>)}
            </select>
            {tagKey === 'ownerImageTag' ? (
              <div className="flex h-12 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3">
                <img src="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261101_125212.png" alt="Official Image Tag preview" className="h-8 max-w-[76px] object-contain" />
                <span className="text-[10px] font-extrabold uppercase tracking-wider text-slate-700">Official Image Tag</span>
              </div>
            ) : (() => {
              const selectedTag = PROFILE_TAGS.find(tag => tag.key === tagKey);
              return selectedTag ? <div className="flex h-12 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3">
                <img src={"https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/" + (selectedTag.image.startsWith('/') ? selectedTag.image.slice(1) : selectedTag.image)} alt={selectedTag.label} className="h-8 max-w-[90px] object-contain" />
                <span className="text-[10px] font-extrabold uppercase tracking-wider text-slate-700">{selectedTag.label}</span>
              </div> : null;
            })()}
          </div>}
          {kind === 'id' && <div className="relative">
            <button type="button" onClick={() => setMenuOpen(value => !value)} className="flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700">
              <BadgeCheck className="h-4 w-4" /> ID Badge <ChevronDown className="h-4 w-4" />
            </button>
            {menuOpen && (
              <div className="absolute left-0 top-11 z-20 min-w-36 rounded-lg border border-slate-200 bg-white p-1 shadow-xl">
                <button type="button" onClick={() => setMenuOpen(false)} className="w-full rounded-md px-3 py-2 text-left text-xs font-semibold hover:bg-slate-50">ID Badge</button>
              </div>
            )}
          </div>}
          <label className="flex h-10 items-center gap-1 text-xs font-semibold"><input type="checkbox" checked={permanent} onChange={event => setPermanent(event.target.checked)} /> Permanent</label>
          {!permanent && <input
            value={days}
            onChange={event => setDays(event.target.value.replace(/[^0-9]/g, ''))}
            placeholder="Days"
            inputMode="numeric"
            className="h-10 w-20 rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none"
          />}
          <button type="button" onClick={assignId} disabled={saving || !selectedUser} className="h-10 rounded-lg bg-[#197477] px-4 text-xs font-bold text-white disabled:opacity-40">
            {saving ? 'Saving…' : kind === 'id' ? 'Give ID' : 'Give Image Tag'}
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
              <th className="px-3 py-3">Assigned ID / Image Tag</th>
              <th className="px-3 py-3">Duration</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {assignedUsers.map((user, index) => {
              const idActive = Boolean(user.vipId && (!user.vipIdExpiresAt || user.vipIdExpiresAt > Date.now()));
              const tagActive = Boolean(user.ownerImageTag && (!user.ownerImageTagExpiresAt || user.ownerImageTagExpiresAt > Date.now()));
              const activeProfileTags = PROFILE_TAGS.filter(tag => Boolean(user[tag.key] && (!(user as any)[tag.key + 'ExpiresAt'] || (user as any)[tag.key + 'ExpiresAt'] > Date.now())));
              const expiry = idActive ? user.vipIdExpiresAt : tagActive ? user.ownerImageTagExpiresAt : (activeProfileTags[0] ? (user as any)[activeProfileTags[0].key + 'ExpiresAt'] : 0);
              const remaining = expiry ? Math.max(0, Math.ceil((expiry - Date.now()) / 86400000)) : null;
              return (
                <tr key={user.id} className="text-sm hover:bg-slate-50/60">
                  <td className="px-3 py-3 text-xs font-bold text-slate-500">{index + 1}</td>
                  <td className="px-3 py-3"><img src={user.image || '/default-avatar.png'} alt="" className="h-9 w-9 rounded-full border border-slate-200 bg-slate-100 object-cover" /></td>
                  <td className="px-3 py-3 font-semibold text-slate-800">{user.name || 'User'}</td>
                  <td className="px-3 py-3 font-semibold text-slate-700">{user.hurryId || '—'}</td>
                  <td className="px-3 py-3 font-semibold text-slate-700">
                    <span className="inline-flex items-center gap-2">
                      {idActive && <><img src="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261101_125212.png" alt="Official ID" className="h-5 w-auto object-contain" />{user.vipId}</>}
                      {tagActive && <span className="inline-flex items-center gap-1.5 rounded-md border border-violet-200 bg-violet-50 px-2 py-1"><img src="https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/IMG_20261101_125212.png" alt="Official Image Tag" className="h-6 max-w-[90px] object-contain" /><span className="text-[9px] font-extrabold uppercase tracking-wide text-violet-700">Official Image Tag</span></span>}
                      {activeProfileTags.map(tag => <span key={tag.key} className="inline-flex items-center gap-1.5 rounded-md border border-slate-200 bg-slate-50 px-2 py-1"><img src={"https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/" + (tag.image.startsWith('/') ? tag.image.slice(1) : tag.image)} alt={tag.label} className="h-6 max-w-[90px] object-contain" /><span className="text-[9px] font-extrabold uppercase tracking-wide text-slate-700">{tag.label}</span></span>)}
                    </span>
                  </td>
                  <td className="px-3 py-3 text-slate-600">{expiry ? `${remaining ?? 0} days` : 'Permanent'}</td>
                </tr>
              );
            })}
            {assignedUsers.length === 0 && (
              <tr><td colSpan={6} className="px-3 py-10 text-center text-sm font-medium text-slate-400">No users have an active VIP ID or profile tag yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
