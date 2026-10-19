import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, X } from 'lucide-react';
import { socket } from '../src/lib/socket';

export default function Reports() {
  const [reports, setReports] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedReport, setSelectedReport] = useState<any>(null);

  useEffect(() => {
    let isMounted = true;

    if (!socket.connected) {
      socket.connect();
    }

    socket.emit('user_report_history_request');

    const handleHistoryResponse = (data: any) => {
      if (!isMounted || !Array.isArray(data?.reports)) return;
      setReports(data.reports.sort((a: any, b: any) => (b.timestamp || 0) - (a.timestamp || 0)));
    };

    const handleNewReport = (data: any) => {
      if (!isMounted || !data?.id) return;
      setReports((prev) => {
        if (prev.some((m) => m.id === data.id)) return prev;
        return [data, ...prev].sort((a: any, b: any) => (b.timestamp || 0) - (a.timestamp || 0));
      });
    };

    socket.on('user_report_history_response', handleHistoryResponse);
    socket.on('user_report', handleNewReport);

    return () => {
      isMounted = false;
      socket.off('user_report_history_response', handleHistoryResponse);
      socket.off('user_report', handleNewReport);
    };
  }, []);

  const filteredReports = reports.filter((report) => {
    const q = searchQuery.toLowerCase();
    return (
      (report.senderName || '').toLowerCase().includes(q) ||
      (report.senderId || '').toLowerCase().includes(q) ||
      (report.reportedName || '').toLowerCase().includes(q) ||
      (report.reportedId || '').toLowerCase().includes(q)
    );
  });

  return (
    <div className="flex flex-col h-full bg-slate-50 overflow-y-auto p-6 md:p-8">
      <div className="max-w-5xl mx-auto w-full space-y-6">
        <h2 className="text-2xl font-bold text-slate-800">Report</h2>

        <div className="w-full">
          <input
            type="text"
            placeholder="Search by ID or Name..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white shadow-sm"
          />
        </div>

        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden flex flex-col h-[600px]">
          <div className="hidden md:flex items-center gap-4 px-8 py-3 text-[11px] font-extrabold text-slate-500 uppercase tracking-wider bg-slate-50 border-b border-slate-100 shrink-0">
            <span className="w-10">S.No</span>
            <span className="w-12">Report Sender Avtar</span>
            <span className="w-40">Report Sender ID</span>
            <span className="w-12">Reported Avtar</span>
            <span className="flex-1">Reported ID</span>
            <span className="w-40 text-right">Data Time</span>
          </div>

          <div className="flex-1 overflow-y-auto">
            {filteredReports.map((report, idx) => (
              <div
                key={report.id}
                onClick={() => setSelectedReport(report)}
                className="flex items-center gap-4 px-8 py-3.5 border-b border-slate-100 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                <div className="w-10 text-xs font-bold text-slate-500">{idx + 1}</div>

                <div className="w-12 shrink-0">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-bold flex items-center justify-center shadow-sm overflow-hidden border border-indigo-100">
                    {report.senderPhoto ? (
                      <img src={report.senderPhoto} alt={report.senderName} className="w-full h-full object-cover" />
                    ) : (
                      <span className="text-sm">{(report.senderName || 'U').charAt(0).toUpperCase()}</span>
                    )}
                  </div>
                </div>

                <div className="w-40 shrink-0 min-w-0">
                  <div className="font-bold text-sm text-slate-800 truncate">{report.senderId}</div>
                  <div className="text-xs text-slate-400 truncate">{report.senderName}</div>
                </div>

                <div className="w-12 shrink-0">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-r from-red-500 to-pink-600 text-white font-bold flex items-center justify-center shadow-sm overflow-hidden border border-red-100">
                    {report.reportedPhoto ? (
                      <img src={report.reportedPhoto} alt={report.reportedName} className="w-full h-full object-cover" />
                    ) : (
                      <span className="text-sm">{(report.reportedName || 'U').charAt(0).toUpperCase()}</span>
                    )}
                  </div>
                </div>

                <div className="flex-1 min-w-0">
                  <div className="font-bold text-sm text-slate-800 truncate">{report.reportedId}</div>
                  <div className="text-xs text-slate-400 truncate">{report.reportedName}</div>
                </div>

                <div className="w-40 shrink-0 text-right">
                  <span className="text-[11px] text-slate-400 font-medium">
                    {report.timestamp
                      ? new Date(report.timestamp).toLocaleString([], {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })
                      : ''}
                  </span>
                </div>
              </div>
            ))}

            {filteredReports.length === 0 && (
              <div className="py-16 text-center text-slate-400 text-sm font-medium">
                <p className="font-bold text-slate-700">No reports found.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {selectedReport && (
        <div className="fixed inset-0 z-50 bg-[#f0f2f5] flex flex-col font-sans">
          {/* ----- Header Area ----- */}
          <div
            className="relative pt-safe pb-4 flex items-center justify-center"
            style={{
              background: 'linear-gradient(to bottom, #3b82f6 0%, #f0f2f5 100%)',
              paddingTop: 'calc(env(safe-area-inset-top, 0px) + 8px)'
            }}
          >
            <button
              onClick={() => setSelectedReport(null)}
              className="absolute left-2 top-auto p-2 hover:bg-white/20 rounded-full transition-colors"
            >
              <ArrowLeft size={24} className="text-gray-900" />
            </button>
            <h1 className="text-xl font-bold text-gray-900 tracking-wide">
              Report Details
            </h1>
          </div>

          {/* ----- Main Scrollable Content ----- */}
          <div className="flex-1 overflow-y-auto px-4 pb-6">
            <h2 className="text-base font-bold text-gray-800 mt-4 mb-2">Types</h2>
            {/* Category Cards (No borders, no shadows) */}
            <div className="mt-2 grid grid-cols-2 gap-3">
              {[
                { id: 'violence', label: 'Violence' },
                { id: 'abusing', label: 'Abusing' },
                { id: 'illegal', label: 'Illegal' },
                { id: 'others', label: "Other's" },
              ].map((cat) => {
                const isSelected = selectedReport.category === cat.id;
                return (
                  <button
                    key={cat.id}
                    disabled
                    className={`py-4 px-2 rounded-md text-sm font-semibold transition-all duration-200 ${
                      isSelected
                        ? 'bg-blue-100 text-blue-600'
                        : 'bg-white text-gray-700 opacity-50'
                    }`}
                  >
                    {cat.label}
                  </button>
                );
              })}
            </div>

            {/* Description Section (No borders, no shadows) */}
            <div className="mt-6">
              <h2 className="text-base font-bold text-gray-800 mb-2">Description</h2>
              <textarea
                value={selectedReport.description || ''}
                readOnly
                placeholder="No description provided"
                className="w-full bg-white rounded-md p-3 text-sm text-gray-800 outline-none min-h-[120px] resize-none"
              />
            </div>

            {/* Attach Proof Section (Sirf dashed border rakha hai, shadow hata di) */}
            <div className="mt-6">
              <h2 className="text-base font-bold text-gray-800 mb-2">Uploaded Image</h2>
              <div
                className="w-full max-h-[500px] bg-white rounded-md flex items-center justify-center relative overflow-hidden"
              >
                {selectedReport.proofImage ? (
                  <img src={selectedReport.proofImage} alt="Proof" className="w-full h-auto max-h-[500px] object-contain" />
                ) : (
                  <span className="text-xs text-gray-400 py-10">No Image</span>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
