'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { apiUrl } from '@/src/lib/api';

type SellerUser = {
  id: string;
  uid: string;
  accountId: string;
  name: string;
  image: string;
  active: boolean;
  coins?: number;
};

const valueOf = (...values: unknown[]) => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() && String(item).trim() !== 'null');
  return value === undefined ? '' : String(value).trim();
};


async function readApiResponse(response: Response) {
  const raw = await response.text();
  if (!raw.trim()) throw new Error(`Server returned an empty response (HTTP ${response.status})`);
  let data: any;
  try {
    data = JSON.parse(raw);
  } catch {
    const contentType = response.headers.get('content-type') || '';
    const hint = contentType.includes('text/html') || /^<!doctype html|^<html/i.test(raw.trim())
      ? 'Backend returned an HTML page instead of JSON. Check the backend URL/deployment.'
      : 'Backend returned an invalid JSON response.';
    throw new Error(`${hint} (HTTP ${response.status})`);
  }
  if (!response.ok) throw new Error(data?.error || `Request failed (HTTP ${response.status})`);
  return data;
}

type SellerCenterUsersProps = { onOpenUser: (user: SellerUser) => void | Promise<void> };

export default function SellerCenterUsers({ onOpenUser }: SellerCenterUsersProps) {
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<SellerUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    const loadActiveSellers = async () => {
      setLoading(true);
      setMessage('');
      try {
        const response = await fetch(apiUrl('/api/owner/seller-center'), {
          cache: 'no-store',
          headers: { Accept: 'application/json' }
        });
        const data = await readApiResponse(response);
        const rows = Array.isArray(data?.users) ? data.users : [];
        if (!mounted) return;
        setUsers(rows.map((user: any) => ({
          id: valueOf(user._id, user.id, user.uid, user.accountId),
          uid: valueOf(user.uid, user.id, user.appLongId, user._id),
          accountId: valueOf(user.accountId, user.accountNumber, user.displayAccountNumber, user.displayUserNumber, user.id, user.uid),
          name: valueOf(user.name, user.displayName, user.userName) || 'User',
          image: valueOf(user.image, user.dp, user.avatar, user.photoURL, user.photoUrl, user.profileImage, user.picture),
          active: user.coinSellerTag === true || user.coinSeller === true || user.sellerTag === true
        })).filter((user: SellerUser) => user.active));
      } catch (error) {
        if (mounted) {
          setUsers([]);
          setMessage(error instanceof Error ? error.message : 'Could not load Seller Center users');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };
    void loadActiveSellers();
    return () => { mounted = false; };
  }, []);

  // Search the complete user directory, not only users already enabled as sellers.
  useEffect(() => {
    const term = query.trim();
    if (!term) return;
    let mounted = true;
    const timer = window.setTimeout(async () => {
      setLoading(true);
      setMessage('');
      try {
        const response = await fetch(apiUrl(`/api/users?q=${encodeURIComponent(term)}`), {
          cache: 'no-store',
          headers: { Accept: 'application/json' }
        });
        const data = await readApiResponse(response);
        const rows = Array.isArray(data?.users) ? data.users : data?.user ? [data.user] : [];
        if (!mounted) return;
        setUsers(rows.map((user: any) => ({
          id: valueOf(user._id, user.id, user.uid, user.accountId),
          uid: valueOf(user.uid, user.id, user.appLongId, user._id),
          accountId: valueOf(user.accountId, user.accountNumber, user.displayAccountNumber, user.displayUserNumber, user.id, user.uid),
          name: valueOf(user.name, user.displayName, user.userName) || 'User',
          image: valueOf(user.image, user.dp, user.avatar, user.photoURL, user.photoUrl, user.profileImage, user.picture),
          active: user.coinSellerTag === true || user.coinSeller === true || user.sellerTag === true
        })));
      } catch (error) {
        if (mounted) {
          setUsers([]);
          setMessage(error instanceof Error && !/404/.test(error.message) ? error.message : '');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    }, 250);
    return () => { mounted = false; window.clearTimeout(timer); };
  }, [query]);

  const toggleSeller = async (user: SellerUser) => {
    const next = !user.active;
    setUpdating(user.id);
    setMessage('');
    try {
      const response = await fetch(apiUrl('/api/owner/seller-center'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ uid: user.uid, accountId: user.accountId, active: next })
      });
      const data = await readApiResponse(response);
      setUsers((current) => next ? current.map((item) => item.id === user.id ? { ...item, active: true } : item) : current.filter((item) => item.id !== user.id));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update Seller Center');
    } finally {
      setUpdating(null);
    }
  };

  const filteredUsers = users;

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-8 py-6 pb-4 border-b border-gray-100 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Seller Center</h2>
          <p className="text-xs text-slate-400 mt-0.5">Users with Seller Center active</p>
        </div>
      </div>
      <div className="p-8 flex-1 overflow-y-auto">
        <div className="relative w-full max-w-3xl mb-6">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search active seller users..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-[#197477] font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
          />
        </div>
        {message && <p className="mb-4 text-sm font-medium text-red-500">{message}</p>}
        {loading ? (
          <div className="py-12 text-center text-slate-400 font-medium text-sm">Searching users...</div>
        ) : (
          <div className="w-full overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[680px]">
              <thead>
                <tr className="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="py-4 pl-4 pr-2 w-12">S.No</th>
                  <th className="py-4 px-2">User</th>
                  <th className="py-4 px-2">User ID</th>
                  <th className="py-4 px-2">Seller Center</th>
                  <th className="py-4 pr-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {filteredUsers.map((user, index) => (
                  <tr key={user.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3 pl-4 pr-2 text-xs font-bold text-slate-500">{index + 1}</td>
                    <td className="py-3 px-2">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 bg-indigo-100 shrink-0 flex items-center justify-center text-[#197477] font-bold">
                          {user.image ? <img src={user.image} alt={user.name} className="w-full h-full object-cover" /> : user.name.charAt(0).toUpperCase()}
                        </div>
                        <span className="text-sm font-bold text-slate-800">{user.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-2"><span className="text-sm font-bold text-[#8a92ff] tracking-wide">{user.accountId || '—'}</span></td>
                    <td className="py-3 px-2">
                      <button
                        type="button"
                        disabled={updating === user.id}
                        onClick={() => void toggleSeller(user)}
                        className={`px-4 py-2 rounded-lg text-xs font-bold transition-colors disabled:opacity-50 ${user.active ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-slate-100 text-slate-600 border border-slate-200'}`}
                      >
                        {updating === user.id ? 'Updating...' : user.active ? 'Active' : 'Active Seller Center'}
                      </button>
                    </td>
                    <td className="py-3 pr-4 text-right">
                      <button type="button" onClick={async () => {
                        try {
                          const response = await fetch(apiUrl(`/api/lucky-bag/wallet/${encodeURIComponent(user.uid || user.accountId)}`), {
                            cache: 'no-store',
                            headers: { Accept: 'application/json', 'Cache-Control': 'no-cache' }
                          });
                          const wallet = await readApiResponse(response);
                          await onOpenUser({ ...user, coins: Math.max(0, Number(wallet?.balance ?? 0)) });
                        } catch (error) {
                          setMessage(error instanceof Error ? error.message : 'Could not load Seller Center wallet');
                        }
                      }} className="px-3 py-1.5 rounded-md bg-[#197477] hover:bg-[#17696c] text-white text-[10px] font-semibold transition-colors">Open</button>
                    </td>
                  </tr>
                ))}
                {!query.trim() && <tr><td colSpan={4} className="py-12 text-center text-sm font-medium text-slate-400">No active Seller Center users</td></tr>}
                {!!query.trim() && !filteredUsers.length && !loading && <tr><td colSpan={4} className="py-12 text-center text-sm font-medium text-slate-400">No users found</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
