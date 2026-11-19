'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { apiUrl } from '@/src/lib/api';

type AssetType = 'Chat bubble' | 'Theme' | 'Frame' | 'Vehicle';
type Asset = { id: string; name: string; tab: string; image: string; video?: string; type: AssetType };

const ASSET_BASE = 'https://raw.githubusercontent.com/khaiwilsan676-alt/Hurry-Voice-Chat/main/public/';
const ASSETS: Asset[] = [
  { id: 'c1', name: '1', tab: 'Chat Bubble', type: 'Chat bubble', image: 'file_000000003d888211822aa6837fe5013c.png' },
  { id: 'c2', name: '2', tab: 'Chat Bubble', type: 'Chat bubble', image: 'file_000000006044821186ff566329797142.png' },
  { id: 'c3', name: '3', tab: 'Chat Bubble', type: 'Chat bubble', image: 'file_00000000c44c81f598f62ae8a45e13a7.png' },
  { id: 'c4', name: '4', tab: 'Chat Bubble', type: 'Chat bubble', image: 'IMG_20260920_122831.png' },
  { id: 'c5', name: '5', tab: 'Chat Bubble', type: 'Chat bubble', image: 'IMG_20260920_122743.png' },
  { id: 'c6', name: '6', tab: 'Chat Bubble', type: 'Chat bubble', image: 'IMG_20260920_121752.png' },
  { id: 't1', name: 'Seafood', tab: 'Theme', type: 'Theme', image: 'IMG-20260904-WA0004.jpg' },
  { id: 't2', name: 'Night Sky', tab: 'Theme', type: 'Theme', image: 'IMG-20260904-WA0005.jpg' },
  { id: 't3', name: 'Seafood', tab: 'Theme', type: 'Theme', image: 'IMG-20260904-WA0006.jpg' },
  { id: 't4', name: 'Night Sky', tab: 'Theme', type: 'Theme', image: 'IMG-20260904-WA0007.jpg' },
  { id: 't5', name: 'Seafood', tab: 'Theme', type: 'Theme', image: 'IMG-20260904-WA0040.jpg' },
  { id: 't6', name: 'Night Sky', tab: 'Theme', type: 'Theme', image: 'IMG-20260904-WA0041.jpg' },
  { id: 't7', name: 'Painted Skies', tab: 'Theme', type: 'Theme', image: 'IMG_20261024-WA0021.jpg' },
  { id: 't8', name: 'Solace nature', tab: 'Theme', type: 'Theme', image: 'IMG_20261024-WA0022.jpg' },
  { id: 't9', name: 'Time light', tab: 'Theme', type: 'Theme', image: 'IMG_20261024-WA0023.jpg' },
  { id: 'a1', name: 'Crown Wings', tab: 'Avatar Frame', type: 'Frame', image: 'IMG_20261021_192009.png', video: 'VID_20260905_024534_955_bsl.mp4' },
  { id: 'a2', name: 'Host Wings', tab: 'Avatar Frame', type: 'Frame', image: 'IMG_20261021_192044.png', video: 'VID_20260905_024726_660_bsl.mp4' },
  { id: 'a3', name: 'Mystic Wings', tab: 'Avatar Frame', type: 'Frame', image: 'file_00000000c67c81fba9294a82cd14d3ca.png', video: 'VID_20260905_083446_619_bsl.mp4' },
  { id: 'a4', name: 'Ninja Neo', tab: 'Avatar Frame', type: 'Frame', image: 'file_00000000b8048211813166b471b0cdf6.png' },
  { id: 'a5', name: 'Gemshine', tab: 'Avatar Frame', type: 'Frame', image: 'file_00000000a77c8211a913aef88c4d9a4d.png' },
  { id: 'a6', name: 'Love Beat', tab: 'Avatar Frame', type: 'Frame', image: 'file_00000000445482119241b60988ca201a.png' },
  { id: 'a7', name: 'Golden Aura', tab: 'Avatar Frame', type: 'Frame', image: 'file_00000000406482119e4e67fbbe323a54.png' },
  { id: 'a8', name: 'Happy birthday', tab: 'Avatar Frame', type: 'Frame', image: 'file_00000000c21482118a018264cecee76c.png', video: 'gemini_generated_video_c02dfa3d~2.mp4' },
  { id: 'v1', name: 'Leopard Roar', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_124625.png', video: 'VID_20260905_095157_269_bsl.mp4' },
  { id: 'v10', name: 'Shadow Beast', tab: 'Vehicle', type: 'Vehicle', image: 'file_0000000009a482118ab17c8d0629d193.png', video: 'Screenrecorder-2026-10-23-16-01-27-317~2.mp4' },
  { id: 'v2', name: 'Speedster', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125302.png', video: 'alioss_19_2025090317293759938~2.mp4' },
  { id: 'v3', name: 'Blaze Runner', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125318.png', video: 'alioss_19_2025120517555513292~2.mp4' },
  { id: 'v4', name: 'Star Rider', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125335.png', video: 'alioss_19_2025121720314422497~2.mp4' },
  { id: 'v5', name: 'Sky Drift', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125357.png', video: 'alioss_19_2026021110563339968~2.mp4' },
  { id: 'v6', name: 'Neon Flash', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125133.png', video: 'alioss_16_2025092202374953873~2.mp4' },
  { id: 'v7', name: 'Royal Cruiser', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125152.png', video: 'alioss_16_2025080802251364763~2.mp4' },
  { id: 'v8', name: 'Shadow Wing', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125210.png', video: 'alioss_16_2025092202291646319~2.mp4' },
  { id: 'v9', name: 'Galaxy Surfer', tab: 'Vehicle', type: 'Vehicle', image: 'IMG_20261023_125416.png', video: 'alioss_17_2025041211424496804~2.mp4' },
];

const inputClass = 'w-full rounded-xl border border-slate-200 bg-slate-100 px-4 py-3 text-sm font-semibold text-slate-800 outline-none focus:border-slate-400';
const tableHead = 'px-4 py-3 text-left text-[10px] font-extrabold uppercase tracking-wider text-slate-500';

export default function OwnerAssetsPanel({ ownerKey }: { ownerKey: string }) {
  const [userQuery, setUserQuery] = useState('');
  const [user, setUser] = useState<any>(null);
  const [userMatches, setUserMatches] = useState<any[]>([]);
  const [type, setType] = useState<AssetType>('Chat bubble');
  const [selected, setSelected] = useState<Asset | null>(null);
  const [days, setDays] = useState('30');
  const [history, setHistory] = useState<any[]>([]);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');

  const visibleAssets = useMemo(() => ASSETS.filter((asset) => asset.type === type), [type]);

  const loadHistory = async () => {
    try {
      const response = await fetch(apiUrl('/api/owner/assets/sent'), { headers: { 'x-owner-key': ownerKey }, cache: 'no-store' });
      if (response.ok) {
        const data = await response.json();
        setHistory(Array.isArray(data.items) ? data.items : []);
      }
    } catch { /* keep last visible history */ }
  };

  useEffect(() => { void loadHistory(); }, []);

  useEffect(() => {
    const query = userQuery.trim();
    if (!/^\d{4,20}$/.test(query)) { setUser(null); setUserMatches([]); return; }
    let active = true;
    const timer = window.setTimeout(async () => {
      try {
        const response = await fetch(apiUrl('/api/seller/user/' + encodeURIComponent(query)), { cache: 'no-store' });
        const data = await response.json();
        if (!active) return;
        if (response.ok && data.user) { setUser(data.user); setUserMatches([data.user]); }
        else { setUser(null); setUserMatches([]); }
      } catch { if (active) { setUser(null); setUserMatches([]); } }
    }, 250);
    return () => { active = false; window.clearTimeout(timer); };
  }, [userQuery]);

  const sendAsset = async () => {
    if (!user?.accountId || !selected || !/^\d{1,4}$/.test(days) || Number(days) < 1 || Number(days) > 3650) {
      setNotice('Select a user, asset and valid days (1–3650).'); return;
    }
    setBusy(true); setNotice('');
    try {
      const response = await fetch(apiUrl('/api/owner/assets/grant'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-owner-key': ownerKey },
        body: JSON.stringify({ recipientAccountId: user.accountId, itemId: selected.id, name: selected.name, tab: selected.tab, image: selected.image, video: selected.video || '', days: Number(days) })
      });
      const data = await response.json();
      if (!response.ok || !data.success) throw new Error(data.error || 'Could not send asset');
      setNotice('Asset sent successfully.');
      await loadHistory();
    } catch (error: any) { setNotice(error?.message || 'Could not send asset.'); }
    finally { setBusy(false); }
  };

  return (
    <section className="mt-10 border-t border-slate-200 pt-8">
      <div className="space-y-3">
        <input value={userQuery} onChange={(event) => setUserQuery(event.target.value)} placeholder="Search User ID" className={inputClass} inputMode="numeric" />
        {user && <div className="flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-3">
          <img src={user.avatar || '/default-avatar.png'} alt="" className="h-10 w-10 rounded-full object-cover" />
          <div className="min-w-0"><div className="truncate text-sm font-bold text-slate-800">{user.name}</div><div className="text-xs text-slate-500">ID: {user.accountId}</div></div>
        </div>}
        <div className="rounded-xl bg-slate-100 p-3">
          <div className="mb-2 text-xs font-bold text-slate-600">Assets Type</div>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {(['Chat bubble', 'Theme', 'Frame', 'Vehicle'] as AssetType[]).map((value) => (
              <button key={value} type="button" onClick={() => { setType(value); setSelected(null); }} className={`rounded-lg px-3 py-3 text-xs font-bold transition-colors ${type === value ? 'bg-slate-800 text-white' : 'bg-white text-slate-600 hover:bg-slate-200'}`}>{value}</button>
            ))}
          </div>
        </div>
        <div className="grid grid-cols-4 gap-2 sm:grid-cols-6 lg:grid-cols-8">
          {visibleAssets.map((asset) => (
            <button key={asset.id} type="button" onClick={() => setSelected(asset)} aria-label={asset.name} className={`aspect-square overflow-hidden rounded-lg border bg-slate-100 p-1 ${selected?.id === asset.id ? 'border-indigo-500 ring-2 ring-indigo-200' : 'border-slate-200'}`}>
              <img src={ASSET_BASE + asset.image} alt="" className="h-full w-full object-contain" loading="lazy" />
            </button>
          ))}
        </div>
        <div className="rounded-xl bg-slate-100 p-3">
          <div className="mb-2 text-xs font-bold text-slate-600">Assets Name</div>
          <select value={selected?.id || ''} onChange={(event) => setSelected(ASSETS.find((asset) => asset.id === event.target.value) || null)} className={inputClass}>
            <option value="">Select asset</option>
            {visibleAssets.map((asset) => <option key={asset.id} value={asset.id}>{asset.name}</option>)}
          </select>
        </div>
        <div className="rounded-xl bg-slate-100 p-3">
          <div className="mb-2 text-xs font-bold text-slate-600">Days</div>
          <input value={days} onChange={(event) => setDays(event.target.value)} inputMode="numeric" className={inputClass} />
        </div>
        <div className="flex min-h-28 items-center justify-center rounded-xl bg-slate-100 p-3">
          {selected && <img src={ASSET_BASE + selected.image} alt="" className="h-28 w-28 object-contain" />}
        </div>
        <button type="button" onClick={sendAsset} disabled={busy || !user || !selected} className="w-full rounded-xl bg-slate-900 px-4 py-3 text-sm font-extrabold text-white disabled:cursor-not-allowed disabled:opacity-40">{busy ? 'Sending…' : 'Send'}</button>
        {notice && <p className="text-xs font-semibold text-slate-600">{notice}</p>}
      </div>

      <div className="mt-8 overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full min-w-[650px] border-collapse text-left">
          <thead><tr className="border-b border-slate-100 bg-slate-50"><th className={tableHead}>User</th><th className={tableHead}>ID</th><th className={tableHead}>Assets Type</th><th className={tableHead}>Days</th></tr></thead>
          <tbody className="divide-y divide-slate-100">
            {history.map((item) => <tr key={item.id} className="hover:bg-slate-50">
              <td className="px-4 py-3"><div className="flex items-center gap-2"><img src={item.avatar || '/default-avatar.png'} alt="" className="h-8 w-8 rounded-full object-cover" /><span className="max-w-36 truncate text-xs font-bold text-slate-800">{item.name}</span></div></td>
              <td className="px-4 py-3 text-xs font-semibold text-slate-600">{item.accountId}</td>
              <td className="px-4 py-3 text-xs font-semibold text-slate-700">{item.tab}</td>
              <td className="px-4 py-3 text-xs font-semibold text-slate-600">{item.days}</td>
            </tr>)}
            {history.length === 0 && <tr><td colSpan={4} className="px-4 py-8 text-center text-xs text-slate-400">No sent assets</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
