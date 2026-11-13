'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { apiUrl } from '@/src/lib/api';

type RoomRecord = {
  id: string;
  name: string;
  dp: string;
  roomId: string;
  ownerId: string;
  ownerName: string;
  ownerImage: string;
};

type UserRecord = {
  id?: string;
  uid?: string;
  accountId?: string;
  accountNumber?: string;
  name?: string;
  displayName?: string;
  userName?: string;
  image?: string;
  avatar?: string;
  dp?: string;
};

const textValue = (...values: unknown[]) => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() && String(item).trim() !== 'null');
  return value === undefined ? '' : String(value).trim();
};

export default function ManageRooms() {
  const [rooms, setRooms] = useState<RoomRecord[]>([]);
  const [users, setUsers] = useState<UserRecord[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const loadRooms = async () => {
      try {
        setLoading(true);
        const [roomResponse, userResponse] = await Promise.all([
          fetch(apiUrl('/api/rooms?ownerPanel=true'), { cache: 'no-store', headers: { Accept: 'application/json' } }),
          fetch(apiUrl('/api/users'), { cache: 'no-store', headers: { Accept: 'application/json' } })
        ]);
        if (!roomResponse.ok) throw new Error(`Rooms API failed: ${roomResponse.status}`);
        const roomData = await roomResponse.json();
        const userData = userResponse.ok ? await userResponse.json() : {};
        const roomRows = Array.isArray(roomData?.rooms) ? roomData.rooms : [];
        const userRows: UserRecord[] = Array.isArray(userData?.users) ? userData.users : [];
        if (!active) return;
        setUsers(userRows);
        setRooms(roomRows.map((room: any, index: number) => {
          const ownerId = textValue(room.ownerId, room.ownerAccountId, room.accountId, room.roomAdmin, room['Room Admin'], room.id, room.roomId);
          const owner = userRows.find((user) => [user.accountId, user.accountNumber, user.id, user.uid].some((id) => id && String(id) === ownerId));
          return {
            id: textValue(room._id, room.id, room.accountId, room.roomId, index + 1),
            name: textValue(room.name, room.roomName, room['Room Name']) || 'Room',
            dp: textValue(room.dp, room.image, room.roomDp, room['Room dp']),
            roomId: textValue(room.roomId, room.accountId, room.id, room['Room Admin']) || '—',
            ownerId: ownerId || '—',
            ownerName: textValue(room.ownerName, room.roomOwnerName, room.owner?.name, room.owner?.displayName, room.roomOwner?.name, owner?.name, owner?.displayName, owner?.userName) || '—',
            ownerImage: textValue(room.ownerImage, room.owner?.image, room.owner?.avatar, room.roomOwner?.image, owner?.image, owner?.avatar, owner?.dp)
          };
        }));
      } catch (error) {
        console.error('Owner Manage Rooms load failed:', error);
        if (active) {
          setRooms([]);
          setUsers([]);
        }
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
    return rooms.filter((room) =>
      [room.name, room.roomId, room.ownerId, room.ownerName].some((value) => value.toLowerCase().includes(query))
    );
  }, [rooms, searchQuery]);

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="px-8 py-6 pb-4 border-b border-gray-100 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Rooms</h2>
          <p className="text-xs text-slate-400 mt-0.5">Registered rooms</p>
        </div>
        <span className="text-xs font-semibold px-3 py-1 bg-slate-100 text-slate-600 rounded-full">
          Total Rooms: {rooms.length}
        </span>
      </div>
      <div className="p-8 flex-1 overflow-y-auto">
        <div className="flex flex-col gap-4 mb-6">
          <div className="relative w-full max-w-3xl">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              type="text"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search by room name, room ID, owner name or owner ID..."
              className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-blue-400 font-medium text-slate-900 placeholder:text-slate-400 caret-slate-900"
            />
          </div>
        </div>
        {loading ? (
          <div className="py-12 text-center text-slate-400 font-medium text-sm">Loading rooms...</div>
        ) : (
          <div className="w-full overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[800px]">
              <thead>
                <tr className="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="py-4 pl-4 pr-2 w-12">S.No</th>
                  <th className="py-4 px-2 w-1/3">Room Profile & DP</th>
                  <th className="py-4 px-2">Room ID</th>
                  <th className="py-4 px-2">Room Owner</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {filteredRooms.map((room, index) => (
                  <tr key={room.id} className="hover:bg-slate-50/50 transition-colors group">
                    <td className="py-3 pl-4 pr-2 text-xs font-bold text-slate-500">{index + 1}</td>
                    <td className="py-3 px-2">
                      <div className="flex items-center gap-3.5">
                        <div className="w-10 h-10 rounded-full overflow-hidden border border-slate-200 bg-indigo-100 shrink-0 flex items-center justify-center text-indigo-600 font-bold">
                          {room.dp ? <img src={room.dp} alt={room.name} className="w-full h-full object-cover" /> : room.name.charAt(0).toUpperCase()}
                        </div>
                        <span className="text-sm font-bold text-slate-800">{room.name}</span>
                      </div>
                    </td>
                    <td className="py-3 px-2">
                      <span className="text-sm font-bold text-[#8a92ff] tracking-wide">{room.roomId}</span>
                    </td>
                    <td className="py-3 px-2">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full overflow-hidden border border-slate-200 bg-indigo-100 shrink-0 flex items-center justify-center text-indigo-600 font-bold">
                          {room.ownerImage ? <img src={room.ownerImage} alt={room.ownerName} className="w-full h-full object-cover" /> : room.ownerName === '—' ? '?' : room.ownerName.charAt(0).toUpperCase()}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-sm font-bold text-slate-800">{room.ownerName}</span>
                          <span className="text-[11px] text-slate-400 font-medium">{room.ownerId}</span>
                        </div>
                      </div>
                    </td>
                  </tr>
                ))}
                {!filteredRooms.length && (
                  <tr><td colSpan={4} className="py-12 text-center text-sm font-medium text-slate-400">No rooms found</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
