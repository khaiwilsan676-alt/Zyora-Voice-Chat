import React, { useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { socket } from '../src/lib/socket';

const REPORTS_DB_NAME = 'ZyoraReportsDB';
const REPORTS_STORE = 'userReports';
const PAGE_SIZE = 20;
const REPORT_STATUSES = ['Open', 'Ignored', 'Handled'] as const;

const openReportsDB = (): Promise<IDBDatabase> => new Promise((resolve, reject) => {
  const request = indexedDB.open(REPORTS_DB_NAME, 1);
  request.onerror = () => reject(request.error);
  request.onupgradeneeded = () => {
    const db = request.result;
    if (!db.objectStoreNames.contains(REPORTS_STORE)) {
      db.createObjectStore(REPORTS_STORE, { keyPath: 'id' });
    }
  };
  request.onsuccess = () => resolve(request.result);
});

const loadLocalReports = async (): Promise<any[]> => {
  if (typeof indexedDB === 'undefined') return [];
  try {
    const db = await openReportsDB();
    const items = await new Promise<any[]>((resolve, reject) => {
      const request = db.transaction(REPORTS_STORE, 'readonly').objectStore(REPORTS_STORE).getAll();
      request.onsuccess = () => resolve(request.result || []);
      request.onerror = () => reject(request.error);
    });
    db.close();
    return items;
  } catch {
    return [];
  }
};

const saveLocalReport = async (report: any) => {
  if (!report?.id || typeof indexedDB === 'undefined') return;
  try {
    const db = await openReportsDB();
    await new Promise<void>((resolve, reject) => {
      const transaction = db.transaction(REPORTS_STORE, 'readwrite');
      transaction.objectStore(REPORTS_STORE).put(report);
      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
    db.close();
  } catch (error) {
    console.error('Local report save failed:', error);
  }
};

const getReporterId = (report: any) => String(report.senderId || report.reporterId || report.reporter || 'N/A');
const getTargetId = (report: any) => String(report.reportedId || report.targetId || report.target || 'N/A');
const getReason = (report: any) => {
  const category = String(report.category || report.reason || 'Other');
  return ({ violence: 'Violence', abusing: 'Abusing', illegal: 'Illegal', others: "Other's", other: 'Other' } as Record<string, string>)[category.toLowerCase()] || category;
};
const getStatus = (report: any) => REPORT_STATUSES.includes(report.status) ? report.status : 'Open';

export default function Reports() {
  const [reports, setReports] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [appliedSearchQuery, setAppliedSearchQuery] = useState('');
  const [appliedStatusFilter, setAppliedStatusFilter] = useState('All');
  const [filterOpen, setFilterOpen] = useState(false);
  const [selectedReport, setSelectedReport] = useState<any>(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    let mounted = true;
    void loadLocalReports().then((items) => {
      if (mounted) setReports(items.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)));
    });

    if (!socket.connected) socket.connect();
    const handleNewReport = (incoming: any) => {
      if (!mounted || !incoming?.id) return;
      const report = { ...incoming, status: getStatus(incoming) };
      void saveLocalReport(report);
      setReports((previous) => {
        const next = [report, ...previous.filter((item) => item.id !== report.id)];
        return next.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
      });
    };
    socket.on('user_report', handleNewReport);
    return () => {
      mounted = false;
      socket.off('user_report', handleNewReport);
    };
  }, []);

  const filteredReports = useMemo(() => {
    const query = appliedSearchQuery.trim().toLowerCase();
    return reports.filter((report) => {
      const matchesSearch = !query || [
        getReporterId(report), getTargetId(report), getReason(report),
        report.senderName, report.reportedName, report.description,
      ].some((value) => String(value || '').toLowerCase().includes(query));
      const matchesStatus = appliedStatusFilter === 'All' || getStatus(report) === appliedStatusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [reports, appliedSearchQuery, appliedStatusFilter]);

  const pageCount = Math.max(1, Math.ceil(filteredReports.length / PAGE_SIZE));
  const visibleReports = filteredReports.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  useEffect(() => { setPage(1); }, [appliedSearchQuery, appliedStatusFilter]);

  const updateStatus = (report: any, status: typeof REPORT_STATUSES[number]) => {
    const updated = { ...report, status };
    setReports((previous) => previous.map((item) => item.id === report.id ? updated : item));
    setSelectedReport(updated);
    void saveLocalReport(updated);
  };

  return (
    <div className="flex h-full flex-col overflow-y-auto bg-slate-50 p-5 md:p-8">
      <div className="mx-auto w-full max-w-7xl space-y-5">
        <h2 className="text-2xl font-bold text-slate-800">Report's</h2>

        <div className="flex flex-col gap-3 sm:flex-row">
          <input
            type="search"
            placeholder="Search reports..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-800 outline-none focus:border-blue-500"
          />
          <div className="relative">
            <button
              type="button"
              onClick={() => setFilterOpen((open) => !open)}
              className="flex w-full items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 sm:min-w-40"
            >
              {statusFilter} <ChevronDown size={16} />
            </button>
            {filterOpen && (
              <div className="absolute right-0 z-20 mt-2 w-full min-w-40 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
                {['All', ...REPORT_STATUSES].map((status) => (
                  <button
                    key={status}
                    type="button"
                    onClick={() => { setStatusFilter(status); setFilterOpen(false); }}
                    className={`block w-full px-4 py-2.5 text-left text-sm hover:bg-blue-50 ${statusFilter === status ? 'font-bold text-blue-600' : 'text-slate-700'}`}
                  >
                    {status}
                  </button>
                ))}
              </div>
            )}
          </div>
          <button
            type="button"
            onClick={() => { setPage(1); setFilterOpen(false); }}
            className="rounded-xl bg-green-800 px-6 py-3 text-sm font-bold text-white hover:bg-green-900"
          >
            Filter
          </button>
        </div>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4">
            <h3 className="text-lg font-bold text-slate-800">Reports</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[950px] text-left">
              <thead className="bg-slate-50 text-[11px] font-extrabold uppercase tracking-wide text-slate-500">
                <tr>
                  {['S.No', 'Reporter', 'Target', 'Reason', 'Status', 'Created At', 'Action'].map((heading) => (
                    <th key={heading} className="px-4 py-3">{heading}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visibleReports.map((report, index) => (
                  <tr key={report.id} className="text-sm text-slate-700 hover:bg-slate-50">
                    <td className="px-4 py-4 font-semibold">{(page - 1) * PAGE_SIZE + index + 1}</td>
                    <td className="px-4 py-4 font-semibold">{getReporterId(report)}</td>
                    <td className="px-4 py-4 font-semibold">{getTargetId(report)}</td>
                    <td className="px-4 py-4">{getReason(report)}</td>
                    <td className="px-4 py-4">
                      <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${getStatus(report) === 'Handled' ? 'bg-emerald-50 text-emerald-700' : getStatus(report) === 'Ignored' ? 'bg-slate-100 text-slate-600' : 'bg-blue-50 text-blue-700'}`}>
                        {getStatus(report)}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-4 text-xs text-slate-500">
                      {report.timestamp ? new Date(report.timestamp).toLocaleString() : '—'}
                    </td>
                    <td className="px-4 py-4">
                      <button
                        type="button"
                        onClick={() => setSelectedReport(report)}
                        className="rounded-lg bg-green-800 px-4 py-2 text-xs font-bold text-white hover:bg-green-900"
                      >
                        Open
                      </button>
                    </td>
                  </tr>
                ))}
                {visibleReports.length === 0 && (
                  <tr><td colSpan={7} className="px-4 py-14 text-center text-sm font-medium text-slate-400">No reports found.</td></tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-center gap-5 border-t border-slate-100 px-4 py-4">
            <button
              type="button"
              aria-label="Previous page"
              disabled={page <= 1}
              onClick={() => setPage((current) => Math.max(1, current - 1))}
              className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-30"
            >
              <ChevronLeft size={20} />
            </button>
            <span className="text-sm font-semibold text-slate-600">Previous&nbsp;&nbsp; {page} / {pageCount} &nbsp;&nbsp;Next</span>
            <button
              type="button"
              aria-label="Next page"
              disabled={page >= pageCount}
              onClick={() => setPage((current) => Math.min(pageCount, current + 1))}
              className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-30"
            >
              <ChevronRight size={20} />
            </button>
          </div>
        </section>
      </div>

      {selectedReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={() => setSelectedReport(null)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="report-detail-title"
            className="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              aria-label="Close report"
              onClick={() => setSelectedReport(null)}
              className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
            >
              <X size={19} />
            </button>
            <h3 id="report-detail-title" className="mb-5 text-xl font-bold text-slate-800">Report</h3>
            <div className="space-y-4">
              {[
                ['Reporter ID', getReporterId(selectedReport)],
                ['Target', getTargetId(selectedReport)],
                ['Status', getStatus(selectedReport)],
              ].map(([label, value]) => (
                <div key={label}>
                  <div className="mb-1 text-xs font-bold text-slate-400">{label}</div>
                  <div className="rounded-lg bg-slate-50 px-3 py-2.5 text-sm font-semibold text-slate-800">{value}</div>
                </div>
              ))}
              <div>
                <div className="mb-1 text-xs font-bold text-slate-400">Description</div>
                <div className="min-h-24 whitespace-pre-wrap break-words rounded-lg bg-slate-50 px-3 py-3 text-sm text-slate-800">
                  {selectedReport.description || 'No description provided'}
                </div>
              </div>
              <div>
                <div className="mb-2 text-xs font-bold text-slate-400">Update Status</div>
                <div className="flex flex-wrap gap-2">
                  {REPORT_STATUSES.map((status) => (
                    <button
                      key={status}
                      type="button"
                      onClick={() => updateStatus(selectedReport, status)}
                      className={`rounded-lg px-3 py-2 text-xs font-bold ${getStatus(selectedReport) === status ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-blue-50'}`}
                    >
                      {status}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
