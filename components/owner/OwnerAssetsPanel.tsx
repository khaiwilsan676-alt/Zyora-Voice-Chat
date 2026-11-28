'use client';

import React, { useEffect, useMemo, useState } from 'react';
import { apiUrl } from '@/src/lib/api';

type AssetType = 'Chat bubble' | 'Theme' | 'Frame' | 'Vehicle' | 'Level Entry' | 'Level Frame' | 'Level Medal' | 'Family Reward' | 'Daily Check-in';
type Asset = { id: string; name: string; tab: string; image: string; video?: string; type: AssetType; source?: 'zyora' };

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
  { id: 'a9', name: 'Official Frame', tab: 'Avatar Frame', type: 'Frame', image: 'IMG_20261126_214336.png', video: 'VID_20261126_065126_533_bsl.mp4', source: 'zyora' },
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

  // Level page rewards (same images and previews used by native Level.kt).
  { id: 'le25', name: 'Lv.25 Entry', tab: 'Vehicle', type: 'Level Entry', image: 'IMG_20261025_124343.png', video: 'alioss_16_2025080802181154988~2.mp4', source: 'zyora' },
  { id: 'le45', name: 'Lv.45 Entry', tab: 'Vehicle', type: 'Level Entry', image: 'IMG_20261025_124403.png', video: 'alioss_17_2025110614082840769~2.mp4', source: 'zyora' },
  { id: 'le60', name: 'Lv.60 Entry', tab: 'Vehicle', type: 'Level Entry', image: 'IMG_20261025_124423.png', video: 'alioss_17_2025111717234047534~2.mp4', source: 'zyora' },
  { id: 'le75', name: 'Lv.75 Entry', tab: 'Vehicle', type: 'Level Entry', image: 'IMG_20261025_124548.png', video: 'alioss_17_2025110916541444937~2.mp4', source: 'zyora' },
  { id: 'le90', name: 'Lv.90 Entry', tab: 'Vehicle', type: 'Level Entry', image: 'IMG_20261025_124608.png', video: 'alioss_17_2025103016391722282~2.mp4', source: 'zyora' },
  { id: 'lf5', name: 'Lv.5 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_135250.png', source: 'zyora' },
  { id: 'lf10', name: 'Lv.10 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_135305.png', source: 'zyora' },
  { id: 'lf25', name: 'Lv.25 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_135331.png', source: 'zyora' },
  { id: 'lf40', name: 'Lv.40 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_135358.png', source: 'zyora' },
  { id: 'lf55', name: 'Lv.55 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_135714.png', source: 'zyora' },
  { id: 'lf80', name: 'Lv.80 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_135736.png', source: 'zyora' },
  { id: 'lf100', name: 'Lv.100 Frame', tab: 'Avatar Frame', type: 'Level Frame', image: 'IMG_20261025_140657.png', source: 'zyora' },
  { id: 'lm5', name: 'Lv.5 Medal', tab: 'Medal', type: 'Level Medal', image: 'IMG_20261024_153426.png', video: '1000220021-background.mp4', source: 'zyora' },
  { id: 'lm15', name: 'Lv.15 Medal', tab: 'Medal', type: 'Level Medal', image: 'file_00000000bd988211840a0dcbe09101ee.png', video: '1000220022-background.mp4', source: 'zyora' },
  { id: 'lm30', name: 'Lv.30 Medal', tab: 'Medal', type: 'Level Medal', image: 'IMG_20261024_153407.png', video: '1000220023-background.mp4', source: 'zyora' },
  { id: 'lm50', name: 'Lv.50 Medal', tab: 'Medal', type: 'Level Medal', image: 'IMG_20261024_153322.png', video: '1000220024-background.mp4', source: 'zyora' },
  { id: 'lm65', name: 'Lv.65 Medal', tab: 'Medal', type: 'Level Medal', image: 'IMG_20261024_153346.png', video: '1000220025-background.mp4', source: 'zyora' },
  { id: 'lm75', name: 'Lv.75 Medal', tab: 'Medal', type: 'Level Medal', image: 'IMG_20261024_153248.png', video: '1000220026-background.mp4', source: 'zyora' },
  { id: 'lm90', name: 'Lv.90 Medal', tab: 'Medal', type: 'Level Medal', image: 'IMG_20261024_153322.png', video: '1000220027-background.mp4', source: 'zyora' },
  // Family leaderboard rewards.
  { id: 'fr1f', name: 'Family TOP1 Frame 7 Days', tab: 'Avatar Frame', type: 'Family Reward', image: 'IMG_20261117_222458.png', video: 'VID_20261119_100449_690_bsl.mp4', source: 'zyora' },
  { id: 'fr1m', name: 'Family TOP1 Medal 7 Days', tab: 'Medal', type: 'Family Reward', image: 'IMG_20261117_210225.png', source: 'zyora' },
  { id: 'fr1t', name: 'Family TOP1 Tag 7 Days', tab: 'Family Tag', type: 'Family Reward', image: 'IMG_20261117_193617.png', source: 'zyora' },
  { id: 'fr1e', name: 'Family TOP1 Entry 7 Days', tab: 'Vehicle', type: 'Family Reward', image: 'IMG_20260918_141104.jpg', video: 'gemini_generated_video_e407ad86~2.mp4', source: 'zyora' },
  { id: 'fr1c', name: 'Family TOP1 5 Lakh Coins', tab: 'Family Reward', type: 'Family Reward', image: 'file_00000000b2d481fd8cd233482dbeb9ef.png', source: 'zyora' },
  { id: 'fr1th', name: 'Family TOP1 Room Theme 7 Days', tab: 'Theme', type: 'Family Reward', image: 'IMG-20260914-WA0043.jpg', source: 'zyora' },
  { id: 'fr2f', name: 'Family TOP2 Frame 7 Days', tab: 'Avatar Frame', type: 'Family Reward', image: 'IMG_20261117_222526.png', video: 'VID_20261119_100523_352_bsl.mp4', source: 'zyora' },
  { id: 'fr2m', name: 'Family TOP2 Medal 7 Days', tab: 'Medal', type: 'Family Reward', image: 'IMG_20261117_210250.png', source: 'zyora' },
  { id: 'fr2t', name: 'Family TOP2 Tag 7 Days', tab: 'Family Tag', type: 'Family Reward', image: 'IMG_20261117_193700.png', source: 'zyora' },
  { id: 'fr2th', name: 'Family TOP2 Room Theme 7 Days', tab: 'Theme', type: 'Family Reward', image: 'IMG-20260914-WA0045.jpg', source: 'zyora' },
  { id: 'fr3f', name: 'Family TOP3 Frame 7 Days', tab: 'Avatar Frame', type: 'Family Reward', image: 'IMG_20261117_222701.png', video: 'VID_20261119_100548_604_bsl.mp4', source: 'zyora' },
  { id: 'fr3m', name: 'Family TOP3 Medal 7 Days', tab: 'Medal', type: 'Family Reward', image: 'IMG_20261117_210313.png', source: 'zyora' },
  { id: 'fr3t', name: 'Family TOP3 Tag 7 Days', tab: 'Family Tag', type: 'Family Reward', image: 'IMG_20261117_193640.png', source: 'zyora' },
  { id: 'fr3th', name: 'Family TOP3 Room Theme 7 Days', tab: 'Theme', type: 'Family Reward', image: 'IMG-20260914-WA0046.jpg', source: 'zyora' },
  { id: 'fr410f', name: 'Family TOP4-10 Frame 3 Days', tab: 'Avatar Frame', type: 'Family Reward', image: 'IMG_20261117_222718.png', video: 'VID_20261119_100632_615_bsl.mp4', source: 'zyora' },
  { id: 'fr410m', name: 'Family TOP4-10 Medal 3 Days', tab: 'Medal', type: 'Family Reward', image: 'IMG_20261117_210200.png', source: 'zyora' },
  // Daily check-in rewards, including the Day 3 x2 reward and Days 5-7 bonus items.
  { id: 'dc1coin', name: 'Daily Check-in Coins', tab: 'Daily Check-in', type: 'Daily Check-in', image: 'file_00000000b2d481fd8cd233482dbeb9ef.png', source: 'zyora' },
  { id: 'dc3x2', name: 'Daily Check-in Day 3 ×2 Days', tab: 'Daily Check-in', type: 'Daily Check-in', image: 'file_00000000882882088d745d793bd43e06.png', source: 'zyora' },
  { id: 'dc5', name: 'Daily Check-in Day 5 ×1 Day', tab: 'Daily Check-in', type: 'Daily Check-in', image: 'IMG_20260903_141944.png', source: 'zyora' },
  { id: 'dc6', name: 'Daily Check-in Day 6 ×2 Days', tab: 'Daily Check-in', type: 'Daily Check-in', image: 'IMG-20260903-WA0076.jpg', source: 'zyora' },
  { id: 'dc7a', name: 'Daily Check-in Day 7 ×3 Days', tab: 'Daily Check-in', type: 'Daily Check-in', image: 'file_0000000044388211996656afc9ce9c03.png', source: 'zyora' },
  { id: 'dc7b', name: 'Daily Check-in Day 7 Bonus ×3 Days', tab: 'Daily Check-in', type: 'Daily Check-in', image: 'IMG-20260903-WA0077.jpg', source: 'zyora' },
]

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
  const [typeMenuOpen, setTypeMenuOpen] = useState(false);
  const [assetNameQuery, setAssetNameQuery] = useState('');
  const [title, setTitle] = useState('Chat bubble');

  const visibleAssets = useMemo(() => ASSETS.filter((asset) => asset.type === type), [type]);
  const matchedAsset = useMemo(() => {
    const query = assetNameQuery.trim().toLowerCase();
    if (!query) return null;
    return visibleAssets.find((asset) => asset.name.toLowerCase() === query)
      || visibleAssets.find((asset) => asset.name.toLowerCase().includes(query))
      || null;
  }, [assetNameQuery, visibleAssets]);
  const assetUrl = (asset: Asset) => (asset.source === 'zyora' ? 'https://raw.githubusercontent.com/khaiwilsan676-alt/Zyora-Voice-Chat/main/public/' : ASSET_BASE) + asset.image;

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
    const assetToSend = matchedAsset || selected;
    if (!user?.accountId || !assetToSend || !/^\d{1,4}$/.test(days) || Number(days) < 1 || Number(days) > 3650) {
      setNotice('Select a user, asset and valid days (1–3650).'); return;
    }
    setBusy(true); setNotice('');
    try {
      const response = await fetch(apiUrl('/api/owner/assets/grant'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-owner-key': ownerKey },
        body: JSON.stringify({ recipientAccountId: user.accountId, itemId: assetToSend.id, name: assetToSend.name, tab: assetToSend.tab, image: assetToSend.image, video: assetToSend.video || '', days: Number(days) })
      });
      const data = await response.json();
      if (!response.ok || !data.success) throw new Error(data.error || 'Could not send asset');
      setNotice(data.notification === 'delivered'
        ? 'Asset sent successfully. Zyora System message delivered.'
        : data.notification === 'queued'
          ? 'Asset sent successfully. Zyora System message queued.'
          : 'Asset granted, but System message could not be queued. Check Render Redis connection.');
      await loadHistory();
    } catch (error: any) { setNotice(error?.message || 'Could not send asset.'); }
    finally { setBusy(false); }
  };

  return (
    <section className="w-full max-w-none rounded-xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <div className="space-y-4">
        <input value={userQuery} onChange={(event) => setUserQuery(event.target.value)} placeholder="Search User ID" className="h-12 w-full max-w-xs rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm font-semibold text-slate-900 outline-none focus:border-[#197477]" inputMode="numeric" />
        {user && <div className="flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-3">
          <img src={user.avatar || '/default-avatar.png'} alt="" className="h-10 w-10 rounded-full object-cover" />
          <div className="min-w-0"><div className="truncate text-sm font-bold text-slate-800">{user.name}</div><div className="text-xs text-slate-500">ID: {user.accountId}</div></div>
        </div>}
        <div className="relative space-y-2">
          <label className="block text-xs font-bold text-slate-600" htmlFor="owner-asset-title">Title</label>
          <div className="relative w-full max-w-sm">
            <input id="owner-asset-title" value={title} readOnly placeholder="Select title" className="h-12 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 pr-12 text-sm font-semibold text-slate-900 outline-none focus:border-[#197477]" />
            <button type="button" aria-label="Choose title" aria-expanded={typeMenuOpen} onClick={() => setTypeMenuOpen((open) => !open)} className="absolute inset-y-0 right-0 flex w-11 items-center justify-center border-l border-slate-200 text-lg text-slate-600">⌄</button>
            {typeMenuOpen && <div className="absolute left-0 top-[52px] z-30 max-h-64 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl">
              {(['Chat bubble', 'Theme', 'Frame', 'Vehicle', 'Level Entry', 'Level Frame', 'Level Medal', 'Family Reward', 'Daily Check-in'] as AssetType[]).map((value) => (
                <button key={value} type="button" onClick={() => { setType(value); setTitle(value); setSelected(null); setAssetNameQuery(''); setTypeMenuOpen(false); }} className={`block w-full rounded-lg px-3 py-2.5 text-left text-sm font-semibold ${type === value ? 'bg-[#197477] text-white' : 'text-slate-700 hover:bg-slate-100'}`}>{value}</button>
              ))}
            </div>}
          </div>
        </div>
        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-600" htmlFor="owner-asset-name">Assets Name</label>
          <div className="flex items-center gap-3">
            <input id="owner-asset-name" value={assetNameQuery} onChange={(event) => { setAssetNameQuery(event.target.value); setSelected(null); }} placeholder="Type asset name" list="owner-asset-name-options" className="h-12 w-full max-w-sm rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-semibold text-slate-900 outline-none focus:border-[#197477]" />
            {matchedAsset && <img src={assetUrl(matchedAsset)} alt={matchedAsset.name} title={matchedAsset.name} className="h-14 w-14 shrink-0 rounded-lg border border-slate-200 bg-white object-contain p-1" />}
          </div>
          <datalist id="owner-asset-name-options">
            {visibleAssets.map((asset) => <option key={asset.id} value={asset.name} />)}
          </datalist>
        </div>
        <div className="flex items-center gap-3">
          <label className="w-20 shrink-0 text-xs font-bold text-slate-600" htmlFor="owner-asset-days">Days</label>
          <input id="owner-asset-days" value={days} onChange={(event) => setDays(event.target.value)} inputMode="numeric" className="h-12 w-24 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-sm font-semibold text-slate-900 outline-none focus:border-[#197477]" />
        </div>
        <button type="button" onClick={sendAsset} disabled={busy || !user || !(matchedAsset || selected)} className="w-full rounded-xl bg-[#197477] px-4 py-3 text-sm font-extrabold text-white disabled:cursor-not-allowed disabled:opacity-40">{busy ? 'Sending…' : 'Send'}</button>
        {notice && <p className="text-xs font-semibold text-slate-600">{notice}</p>}
      </div>

      <div className="mt-8 w-full overflow-x-auto rounded-xl border border-slate-200 bg-white">
        <table className="w-full min-w-[920px] border-collapse text-left">
          <thead><tr className="border-b border-slate-100 text-[10px] font-semibold text-slate-500 uppercase tracking-wide"><th className="py-3 pl-4 pr-2">User</th><th className="py-3 px-2">ID</th><th className="py-3 px-2">Assets Type</th><th className="py-3 px-2">Assets</th><th className="py-3 pr-4">Days</th></tr></thead>
          <tbody className="divide-y divide-slate-100">
            {history.map((item) => <tr key={item.id} className="hover:bg-slate-50">
              <td className="py-3 pl-4 pr-2"><div className="flex items-center gap-2.5"><img src={item.avatar || '/default-avatar.png'} alt="" className="h-9 w-9 rounded-full object-cover" /><span className="max-w-36 truncate text-xs font-bold text-slate-800">{item.name}</span></div></td>
              <td className="px-2 py-3 text-xs text-slate-600 tabular-nums">{item.accountId}</td>
              <td className="px-2 py-3 text-xs text-slate-600">{item.tab}</td>
              <td className="px-2 py-3"><img src={item.image ? (ASSETS.find((asset) => asset.image === item.image) ? assetUrl(ASSETS.find((asset) => asset.image === item.image)!) : (String(item.image).startsWith('http') ? item.image : ASSET_BASE + item.image)) : '/default-avatar.png'} alt={item.assetName || item.name || 'Sent asset'} className="h-9 w-9 rounded-lg border border-slate-100 bg-white object-contain p-1" /></td>
              <td className="py-3 pr-4 text-xs text-slate-600">{item.days}</td>
            </tr>)}
            {history.length === 0 && <tr><td colSpan={5} className="px-4 py-8 text-center text-xs text-slate-400">No sent assets</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  );
}
