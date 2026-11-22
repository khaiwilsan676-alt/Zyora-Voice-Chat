'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { apiUrl } from '@/src/lib/api';

type RoomRecord = {
  id: string; name: string; dp: string; roomId: string; ownerId: string;
  ownerName: string; ownerImage: string; raw: any;
};

type RoomCache = { rooms: RoomRecord[]; expiresAt: number };
let roomsCache: RoomCache | null = null;
const ROOM_CACHE_TTL = 20_000;

const textValue = (...values: unknown[]) => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() && String(item).trim() !== 'null');
  return value === undefined ? '' : String(value).trim();
};
const firstValue = (source: any, keys: string[]) => textValue(...keys.map((key) => source?.[key]));
const numberValue = (source: any, keys: string[]) => {
  for (const key of keys) {
    const value = Number(source?.[key]);
    if (source?.[key] !== undefined && source?.[key] !== null && Number.isFinite(value)) return value.toLocaleString('en-IN');
  }
  return '0';
};

export default function ManageRooms() {
  const [rooms, setRooms] = useState<RoomRecord[]>(() => roomsCache?.rooms || []);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(() => !roomsCache || roomsCache.expiresAt <= Date.now());
  const [selectedRoom, setSelectedRoom] = useState<RoomRecord | null>(null);
  const [editValues, setEditValues] = useState({ name: '', admin: '', micMode: '15', theme: '', password: '' });
  const [dailyCoins, setDailyCoins] = useState('0');
  const [weeklyCoins, setWeeklyCoins] = useState('0');
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');

  useEffect(() => {
    if (!selectedRoom) return;
    setEditValues({
      name: selectedRoom.name,
      admin: (Array.isArray(selectedRoom.raw.admin) ? selectedRoom.raw.admin : Array.isArray(selectedRoom.raw.admins) ? selectedRoom.raw.admins : []).join(', '),
      micMode: String(selectedRoom.raw.micMode ?? 15),
      theme: firstValue(selectedRoom.raw, ['theme', 'roomTheme', 'themeName', 'room_theme']) || 'default',
      password: ''
    });
    setSaveMessage('');
    let active = true;
    const loadTotals = async () => {
      try {
        const [daily, weekly] = await Promise.all(['daily', 'weekly'].map(period =>
          fetch(apiUrl(`/api/rooms/${encodeURIComponent(selectedRoom.roomId)}/contributions?period=${period}`), { cache: 'no-store', headers: { Accept: 'application/json' } })
        ));
        const [dailyData, weeklyData] = await Promise.all([
          daily.ok ? daily.json() : Promise.resolve({ totalRoomCoins: 0 }),
          weekly.ok ? weekly.json() : Promise.resolve({ totalRoomCoins: 0 })
        ]);
        if (active) {
          setDailyCoins(Number(dailyData.totalRoomCoins || 0).toLocaleString('en-IN'));
          setWeeklyCoins(Number(weeklyData.totalRoomCoins || 0).toLocaleString('en-IN'));
        }
      } catch { if (active) { setDailyCoins('0'); setWeeklyCoins('0'); } }
    };
    void loadTotals();
    return () => { active = false; };
  }, [selectedRoom]);

  const saveRoomSettings = async () => {
    if (!selectedRoom || saving) return;
    setSaving(true); setSaveMessage('');
    try {
      const admin = editValues.admin.split(',').map(value => value.trim()).filter(Boolean);
      const payload: Record<string, unknown> = {
        accountId: selectedRoom.roomId,
        dp: selectedRoom.dp || '/default-avatar.png',
        name: editValues.name.trim(),
        admin,
        micMode: Number(editValues.micMode) || 15,
        theme: editValues.theme.trim() || 'default'
      };
      if (editValues.password.trim()) payload.roomPassword = editValues.password.trim();
      const response = await fetch(apiUrl('/api/rooms'), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || !data.success) throw new Error(data.error || 'Could not save room settings');
      const updated = { ...selectedRoom, name: editValues.name.trim(), raw: { ...selectedRoom.raw, ...payload } };
      setSelectedRoom(updated);
      setRooms(current => current.map(room => room.roomId === updated.roomId ? updated : room));
      roomsCache = { rooms: rooms.map(room => room.roomId === updated.roomId ? updated : room), expiresAt: Date.now() + ROOM_CACHE_TTL };
      setSaveMessage('Saved');
    } catch (error) {
      setSaveMessage(error instanceof Error ? error.message : 'Save failed');
    } finally { setSaving(false); }
  };

  useEffect(() => {
    let active = true;
    const loadRooms = async () => {
      // Refresh the owner profile/avatar join on every panel entry; cached room
      // rows may predate a user's latest profile photo.
      try {
        // Resolve the room's stored owner UID/account ID against canonical user profiles.
        const [response, usersResponse] = await Promise.all([
          fetch(apiUrl('/api/rooms?ownerPanel=true'), { cache: 'no-store', headers: { Accept: 'application/json' } }),
          fetch(apiUrl('/api/users'), { cache: 'no-store', headers: { Accept: 'application/json' } }).catch(() => null)
        ]);
        if (!response.ok) throw new Error(`Rooms API failed: ${response.status}`);
        const data = await response.json();
        const rows = Array.isArray(data?.rooms) ? data.rooms : [];
        let users: any[] = [];
        if (usersResponse?.ok) {
          const usersData = await usersResponse.json().catch(() => ({}));
          users = Array.isArray(usersData?.users) ? usersData.users : [];
        }
        const identityValues = (source: any) => [
          source?.uid, source?.firebaseUid, source?.firebaseUID, source?.userId,
          source?.id, source?._id, source?.appLongId, source?.accountId,
          source?.accountNumber, source?.displayAccountNumber, source?.displayUserNumber,
          source?.publicUserId, source?.hurryId, source?.zyoraId,
          source?.['Account ID'], source?.['Account Number']
        ].map(value => textValue(value)).filter(Boolean);
        const ownerImageOf = (source: any) => textValue(
          source?.image, source?.dp, source?.photo, source?.photoURL, source?.photoUrl,
          source?.avatar, source?.avatarUrl, source?.profileImage, source?.profileImageUrl,
          source?.profile_image, source?.imageUrl, source?.picture,
          source?.profile?.image, source?.profile?.avatar, source?.profile?.photo,
          source?.userData?.image, source?.userData?.avatar
        );
        const mapped: RoomRecord[] = rows.map((room: any, index: number) => {
          const ownerId = textValue(
            room.ownerUid, room.ownerId, room.ownerAccountId, room.owner?.uid,
            room.owner?.id, room.owner?.accountId, room.roomOwner?.uid,
            room.roomOwner?.id, room.roomOwner?.accountId, room.roomAdmin,
            room['Room Admin']
          );
          const owner = users.find(user => ownerId && identityValues(user).includes(ownerId));
          return {
            id: textValue(room._id, room.id, room.accountId, room.roomId, index + 1),
            name: textValue(room.name, room.roomName, room['Room Name']) || 'Room',
            dp: textValue(room.dp, room.image, room.roomDp, room['Room dp']),
            roomId: textValue(room.roomId, room.accountId, room.id, room['Room Admin']) || '—',
            ownerId: ownerId || '—',
            ownerName: textValue(room.ownerName, room.roomOwnerName, room.owner?.name, room.owner?.displayName, room.roomOwner?.name, owner?.name, owner?.displayName, owner?.userName) || '—',
            ownerImage: textValue(room.ownerImage, room.owner?.image, room.owner?.avatar, room.roomOwner?.image, ownerImageOf(owner)),
            raw: room
          };
        });
        roomsCache = { rooms: mapped, expiresAt: Date.now() + ROOM_CACHE_TTL };
        if (active) setRooms(mapped);
      } catch (error) {
        console.error('Owner Manage Rooms load failed:', error);
        if (active) setRooms([]);
      } finally {
        if (active) setLoading(false);
      }
    };
    void loadRooms();
    return () => { active = false; };
  }, []);

  const filteredRooms = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return rooms;
    return rooms.filter((room) => [room.name, room.roomId, room.ownerId, room.ownerName].some((value) => value.toLowerCase().includes(query)));
  }, [rooms, searchQuery]);

  const detailFields = selectedRoom ? [
    { label: 'Room name', value: editValues.name, key: 'name' },
    { label: 'Room ID', value: selectedRoom.roomId, key: 'roomId' },
    { label: 'Admin', value: editValues.admin, key: 'admin' },
    { label: 'Mic Mode', value: editValues.micMode, key: 'micMode' },
    { label: 'Room theme', value: editValues.theme, key: 'theme' },
    { label: 'Room password', value: editValues.password, key: 'password' }
  ] : [];

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-8 py-6 pb-4 border-b border-gray-100 flex items-center justify-between">
        <h2 className="text-xl font-bold text-slate-800">Rooms</h2>
        <span className="text-xs font-semibold px-3 py-1 bg-slate-100 text-slate-600 rounded-full">Total Rooms: {rooms.length}</span>
      </div>
      <div className="p-8 flex-1 overflow-y-auto">
        <div className="relative w-full max-w-3xl mb-6">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input type="text" value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Search by room name, room ID, owner name or owner ID..." className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-blue-400 font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900" />
        </div>
        {loading ? <div className="py-12 text-center text-slate-400 font-medium text-sm">Loading rooms...</div> : (
          <div className="w-full overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[850px]">
              <thead><tr className="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                <th className="py-4 pl-4 pr-2 w-12">S.No</th><th className="py-4 px-2 w-1/3">Room Profile & DP</th><th className="py-4 px-2">Room ID</th><th className="py-4 px-2">Room Owner</th><th className="py-4 px-3 text-right"> </th>
              </tr></thead>
              <tbody className="divide-y divide-slate-50">
                {filteredRooms.map((room, index) => <tr key={room.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-3 pl-4 pr-2 text-xs font-bold text-slate-500">{index + 1}</td>
                  <td className="py-3 px-2"><div className="flex items-center gap-3.5">
                    <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 bg-indigo-100 shrink-0 flex items-center justify-center text-indigo-600 font-bold">{room.dp ? <img src={room.dp} alt={room.name} className="w-full h-full object-cover" /> : room.name.charAt(0).toUpperCase()}</div>
                    <span className="text-sm font-bold text-slate-800">{room.name}</span>
                  </div></td>
                  <td className="py-3 px-2"><span className="text-sm font-bold text-black tracking-wide">{room.roomId}</span></td>
                  <td className="py-3 px-2"><div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full overflow-hidden border border-slate-200 bg-indigo-100 shrink-0 flex items-center justify-center text-indigo-600 font-bold">{room.ownerImage ? <img src={room.ownerImage} alt={room.ownerName} className="w-full h-full object-cover" /> : room.ownerName === '—' ? '?' : room.ownerName.charAt(0).toUpperCase()}</div>
                    <div className="flex flex-col"><span className="text-sm font-bold text-slate-800">{room.ownerName}</span><span className="text-[11px] text-slate-400 font-medium">{room.ownerId}</span></div>
                  </div></td>
                  <td className="py-3 px-3 text-right"><button type="button" onClick={() => setSelectedRoom(room)} className="rounded-lg bg-slate-900 px-4 py-2 text-xs font-bold text-white hover:bg-black">Open</button></td>
                </tr>)}
                {!filteredRooms.length && <tr><td colSpan={5} className="py-12 text-center text-sm font-medium text-slate-400">No rooms found</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedRoom && <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/50 p-3 sm:p-6" onClick={() => setSelectedRoom(null)}>
        <section className="w-full max-w-4xl max-h-[92vh] overflow-y-auto rounded-2xl bg-white p-5 sm:p-7 shadow-2xl" onClick={(event) => event.stopPropagation()}>
          <div className="mb-6 flex items-center justify-between"><h2 className="text-xl font-extrabold text-slate-900">{selectedRoom.name}</h2><button type="button" onClick={() => setSelectedRoom(null)} className="rounded-lg px-3 py-1.5 text-sm font-bold text-slate-600 hover:bg-slate-100">Close</button></div>
          <div className="grid grid-cols-3 gap-3 sm:gap-4">
            {detailFields.map((field) => <label key={field.label} className="min-w-0"><span className="mb-1.5 block truncate text-[11px] font-bold text-slate-500 sm:text-xs">{field.label}</span><input readOnly={field.key === 'roomId'} type={field.key === 'password' ? 'password' : 'text'} placeholder={field.key === 'password' ? 'Set new password' : ''} value={field.value || ''} onChange={(event) => setEditValues(current => ({ ...current, [field.key === 'name' ? 'name' : field.key === 'admin' ? 'admin' : field.key === 'micMode' ? 'micMode' : field.key === 'theme' ? 'theme' : 'password']: event.target.value }))} className={`w-full min-w-0 rounded-lg border border-slate-200 bg-slate-50 px-2 py-2.5 text-xs font-semibold text-slate-900 outline-none focus:border-slate-400 sm:px-3 sm:text-sm ${field.label === 'Mic Mode' ? 'text-[11px]' : ''}`} /></label>)}
          </div>
          <div className="mt-7"><h3 className="mb-3 text-base font-extrabold text-slate-900">Dashboard</h3><div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl border border-slate-200 p-4"><p className="text-xs font-semibold text-slate-500">Room daily coins receiving</p><p className="mt-2 text-xl font-extrabold text-slate-900">{dailyCoins}</p></div>
            <div className="rounded-xl border border-slate-200 p-4"><p className="text-xs font-semibold text-slate-500">Room weekly coins receiving</p><p className="mt-2 text-xl font-extrabold text-slate-900">{weeklyCoins}</p></div>
          </div></div>
          <div className="mt-6 flex items-center justify-end gap-3"><span className="text-xs font-semibold text-slate-500">{saveMessage}</span><button type="button" disabled={saving} onClick={saveRoomSettings} className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50">{saving ? 'Saving...' : 'Save'}</button></div>
        </section>
      </div>}
    </div>
  );
}
