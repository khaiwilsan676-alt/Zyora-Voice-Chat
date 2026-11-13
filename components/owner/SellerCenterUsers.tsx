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
};

const valueOf = (...values: unknown[]) => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() && String(item).trim() !== 'null');
  return value === undefined ? '' : String(value).trim();
};

export default function SellerCenterUsers() {
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState<SellerUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState<string | null>(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let active = true;
    const search = async () => {
      const term = query.trim();
      if (!term) {
        setUsers([]);
        setLoading(false);
        return;
      }
      setLoading(true);
      setMessage('');
      try {
        // Uses the same canonical MongoDB user search endpoint and ID normalization as Manage Users.
        const response = await fetch(apiUrl('/api/users?q=' + encodeURIComponent(term)), {
          cache: 'no-store',
          headers: { Accept: 'application/json' }
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data?.error || 'User search failed');
        const rows = Array.isArray(data?.users) ? data.users : data?.user ? [data.user] : [];
        if (!active) return;
        setUsers(rows.map((user: any) => ({
          id: valueOf(user._id, user.id, user.uid, user.accountId),
          uid: valueOf(user.uid, user.id, user.appLongId, user._id),
          accountId: valueOf(user.accountId, user.accountNumber, user.displayAccountNumber, user.displayUserNumber, user.id, user.uid),
          name: valueOf(user.name, user.displayName, user.userName) || 'User',
          image: valueOf(user.image, user.dp, user.avatar, user.photoURL, user.photoUrl, user.profileImage, user.picture),
          active: user.coinSellerTag === true || user.coinSeller === true || user.sellerTag === true
        })));
      } catch (error) {
        if (active) {
          setUsers([]);
          setMessage(error instanceof Error ? error.message : 'User search failed');
        }
      } finally {
        if (active) setLoading(false);
      }
    };
    void search();
    return () => { active = false; };
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
      const data = await response.json();
      if (!response.ok) throw new Error(data?.error || 'Could not update Seller Center');
      setUsers((current) => current.map((item) => item.id === user.id ? { ...item, active: next } : item));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update Seller Center');
    } finally {
      setUpdating(null);
    }
  };

  const filteredUsers = useMemo(() => users, [users]);

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-8 py-6 pb-4 border-b border-gray-100 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Seller Center</h2>
          <p className="text-xs text-slate-400 mt-0.5">Search a user and activate Seller Center on their profile</p>
        </div>
      </div>
      <div className="p-8 flex-1 overflow-y-auto">
        <div className="relative w-full max-w-3xl mb-6">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by user ID..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-blue-400 font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
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
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {filteredUsers.map((user, index) => (
                  <tr key={user.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3 pl-4 pr-2 text-xs font-bold text-slate-500">{index + 1}</td>
                    <td className="py-3 px-2">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 bg-indigo-100 shrink-0 flex items-center justify-center text-indigo-600 font-bold">
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
                  </tr>
                ))}
                {!query.trim() && <tr><td colSpan={4} className="py-12 text-center text-sm font-medium text-slate-400">Search a user ID to display users</td></tr>}
                {!!query.trim() && !filteredUsers.length && !loading && <tr><td colSpan={4} className="py-12 text-center text-sm font-medium text-slate-400">No users found</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
