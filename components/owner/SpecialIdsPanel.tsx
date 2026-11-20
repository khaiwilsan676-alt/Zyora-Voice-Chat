'use client';

import React, { useMemo, useState } from 'react';

type UserRow = {
  id: string;
  name?: string;
  hurryId?: string;
  image?: string;
  vipId?: string;
  vipIdDays?: number | string;
  vipDays?: number | string;
  specialIdDays?: number | string;
};

export default function SpecialIdsPanel({ users = [] }: { users?: UserRow[] }) {
  const [userQuery, setUserQuery] = useState('');
  const [specialId, setSpecialId] = useState('');

  const visibleUsers = useMemo(() => {
    const query = userQuery.trim().toLowerCase();
    return users
      .filter((user) => String(user.vipId || '').trim())
      .filter((user) => !query || [user.name, user.hurryId, user.id, user.vipId]
        .some((value) => String(value || '').toLowerCase().includes(query)));
  }, [users, userQuery]);

  return (
    <section className="w-full max-w-5xl space-y-4">
      <input
        value={userQuery}
        onChange={(event) => setUserQuery(event.target.value)}
        placeholder="Search User ID"
        inputMode="numeric"
        className="h-12 w-full max-w-xs rounded-lg border border-slate-200 bg-white px-3 py-3 text-sm font-semibold text-slate-800 outline-none focus:border-slate-400"
      />

      <div className="space-y-2">
        <label htmlFor="special-id-value" className="block text-xs font-bold text-slate-600">Special ID</label>
        <input
          id="special-id-value"
          value={specialId}
          onChange={(event) => setSpecialId(event.target.value)}
          placeholder="Enter Special ID"
          inputMode="numeric"
          className="h-12 w-full max-w-sm rounded-lg border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-800 outline-none focus:border-slate-400"
        />
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
              <th className="px-3 py-3">Day's</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {visibleUsers.map((user, index) => (
              <tr key={user.id} className="text-sm hover:bg-slate-50/60">
                <td className="px-3 py-3 text-xs font-bold text-slate-500">{index + 1}</td>
                <td className="px-3 py-3">
                  <img
                    src={user.image || '/default-avatar.png'}
                    alt=""
                    className="h-9 w-9 rounded-full border border-slate-200 bg-slate-100 object-cover"
                  />
                </td>
                <td className="px-3 py-3 font-semibold text-slate-800">{user.name || 'User'}</td>
                <td className="px-3 py-3 font-semibold text-slate-700">{user.hurryId || '—'}</td>
                <td className="px-3 py-3 font-semibold text-slate-700">{user.vipId}</td>
                <td className="px-3 py-3 text-slate-600">{user.vipIdDays ?? user.vipDays ?? user.specialIdDays ?? '—'}</td>
              </tr>
            ))}
            {visibleUsers.length === 0 && (
              <tr>
                <td colSpan={6} className="px-3 py-10 text-center text-sm font-medium text-slate-400">
                  No Special ID users found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
