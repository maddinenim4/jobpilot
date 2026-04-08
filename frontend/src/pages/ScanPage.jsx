import { useState, useEffect } from 'react';
import { scanJobs, getPortals, updateSettings, getProfile } from '../api/api';
import { Search, Loader2, CheckCircle, XCircle, SkipForward, ChevronDown } from 'lucide-react';

const ScanPage = () => {
  const [keywords, setKeywords] = useState('.NET Developer');
  const [titleFilter, setTitleFilter] = useState('');
  const [postedWithinHours, setPostedWithinHours] = useState(0);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [portals, setPortals] = useState([]);
  const [selectedPortals, setSelectedPortals] = useState([]);
  const [showPortalDropdown, setShowPortalDropdown] = useState(false);
  const [scheduler, setScheduler] = useState({
    schedulerEnabled: false,
    schedulerIntervalHours: 60,
    schedulerStartTime: '07:00',
    schedulerEndTime: '17:00',
  });
  const [schedulerSaving, setSchedulerSaving] = useState(false);
  const [schedulerMsg, setSchedulerMsg] = useState('');

  useEffect(() => {
    const fetchPortals = async () => {
      try {
        const res = await getPortals();
        if (res.data.success) {
          const portalList = ['Nvoids', 'Hiring42', 'Robert Half', 'Randstad', 'Insight Global',
                           'TEKsystems', 'Apex Systems', 'Kforce', 'Collabera', 'Beacon Hill', 'Dice'];
          setPortals(portalList);
          setSelectedPortals(['Nvoids']);
        }
      } catch (err) {
        console.error('Failed to load portals:', err);
        setPortals(['Nvoids', 'Hiring42', 'Robert Half', 'Randstad', 'Insight Global',
                           'TEKsystems', 'Apex Systems', 'Kforce', 'Collabera', 'Beacon Hill', 'Dice']);
      }
    };
    fetchPortals();
  }, []);

  const loadScheduler = async () => {
      try {
        const res = await getProfile();
        if (res.data.success) {
          const d = res.data.data;
          setScheduler({
            schedulerEnabled: d.schedulerEnabled || false,
            schedulerIntervalHours: d.schedulerIntervalHours || 60,
            schedulerStartTime: d.schedulerStartTime || '07:00',
            schedulerEndTime: d.schedulerEndTime || '17:00',
          });
        }
      } catch (err) { console.error(err); }
    };
    loadScheduler();

  const saveScheduler = async (updates) => {
    setSchedulerSaving(true);
    setSchedulerMsg('');
    try {
      const payload = {
        ...updates,
        schedulerKeywords: keywords,
        schedulerPortals: selectedPortals.join(','),
      };
      const res = await updateSettings(payload);
      if (res.data.success) {
        setSchedulerMsg(updates.schedulerEnabled ? '✓ Scheduler ON — running in background' : '✓ Scheduler OFF');
        setTimeout(() => setSchedulerMsg(''), 4000);
      }
    } catch (err) {
      setSchedulerMsg('Failed to save');
    } finally {
      setSchedulerSaving(false);
    }
  };

  const toggleScheduler = () => {
    const newState = { ...scheduler, schedulerEnabled: !scheduler.schedulerEnabled };
    setScheduler(newState);
    saveScheduler(newState);
  };

  const togglePortal = (portal) => {
    if (selectedPortals.includes(portal)) {
      setSelectedPortals(selectedPortals.filter((p) => p !== portal));
    } else if (selectedPortals.length < 5) {
      setSelectedPortals([...selectedPortals, portal]);
    }
  };

  const handleScan = async (e) => {
    e.preventDefault();
    if (selectedPortals.length === 0) {
      setError('Please select at least one portal.');
      return;
    }
    setError('');
    setResult(null);
    setLoading(true);

    try {
      const keywordList = keywords.split(',').map((k) => k.trim()).filter(Boolean);
      const res = await scanJobs({
        keywords: keywordList,
        timePeriodHours: 24,
        jobSiteUrls: [],
        portals: selectedPortals,
        titleFilter: titleFilter,
        postedWithinHours: postedWithinHours,
      });

      if (res.data.success) {
        setResult(res.data.data);
      } else {
        setError(res.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Scan failed. Check your settings.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status) => {
    if (status.startsWith('APPLIED')) return <CheckCircle className="w-3.5 h-3.5" />;
    if (status.startsWith('SKIPPED')) return <SkipForward className="w-3.5 h-3.5" />;
    return <XCircle className="w-3.5 h-3.5" />;
  };

  const getStatusColor = (status) => {
    if (status.startsWith('APPLIED')) return 'bg-green-500/20 text-green-300 border-green-500/30';
    if (status.startsWith('SKIPPED')) return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
    return 'bg-red-500/20 text-red-300 border-red-500/30';
  };

  return (
    <div className="min-h-screen relative bg-slate-950">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 -left-40 w-[500px] h-[500px] bg-red-600/20 rounded-full blur-[120px]"></div>
        <div className="absolute top-1/3 right-0 w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-[120px]"></div>
      </div>
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff05_1px,transparent_1px),linear-gradient(to_bottom,#ffffff05_1px,transparent_1px)] bg-[size:4rem_4rem] pointer-events-none"></div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white tracking-tight">Scan & Apply</h1>
          <p className="text-slate-400 mt-1">Search job portals and auto-apply with AI</p>
        </div>

        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 mb-8">
          <form onSubmit={handleScan} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-200 mb-2">
                Keywords (comma-separated)
              </label>
              <input
                type="text"
                value={keywords}
                onChange={(e) => setKeywords(e.target.value)}
                className="flex h-11 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 text-sm text-white placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition"
                placeholder=".NET Developer, C# Engineer"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-200 mb-2">
                Title Filter (optional)
              </label>
              <input
                type="text"
                value={titleFilter}
                onChange={(e) => setTitleFilter(e.target.value)}
                className="flex h-11 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 text-sm text-white placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition"
                placeholder="e.g. .net developer (only jobs with this in title)"
              />
              <p className="mt-1.5 text-xs text-slate-500">Leave blank to apply to all matched jobs</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-200 mb-2">
                Posted Within
              </label>
              <select
                value={postedWithinHours}
                onChange={(e) => setPostedWithinHours(Number(e.target.value))}
                className="flex h-11 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition"
              >
                <option value={0} className="bg-slate-900">Any time</option>
                <option value={1} className="bg-slate-900">Last 1 hour</option>
                <option value={2} className="bg-slate-900">Last 2 hours</option>
                <option value={4} className="bg-slate-900">Last 4 hours</option>
                <option value={8} className="bg-slate-900">Last 8 hours</option>
                <option value={12} className="bg-slate-900">Last 12 hours</option>
                <option value={24} className="bg-slate-900">Last 24 hours</option>
                <option value={48} className="bg-slate-900">Last 2 days</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-200 mb-2">
                Job Portals (select up to 5)
              </label>
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowPortalDropdown(!showPortalDropdown)}
                  className="flex h-11 w-full items-center justify-between rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition"
                >
                  <span className={selectedPortals.length === 0 ? 'text-slate-500' : ''}>
                    {selectedPortals.length === 0 ? 'Select portals...' : selectedPortals.join(', ')}
                  </span>
                  <ChevronDown className="w-4 h-4 text-slate-400" />
                </button>

                {showPortalDropdown && (
                  <div className="absolute z-20 mt-2 w-full bg-slate-900/95 backdrop-blur-xl border border-white/20 rounded-lg shadow-2xl max-h-60 overflow-y-auto">
                    {portals.map((portal) => (
                      <label key={portal} className="flex items-center px-4 py-2.5 hover:bg-white/5 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          checked={selectedPortals.includes(portal)}
                          onChange={() => togglePortal(portal)}
                          disabled={!selectedPortals.includes(portal) && selectedPortals.length >= 5}
                          className="w-4 h-4 rounded mr-3 accent-red-500"
                        />
                        <span className={`text-sm ${
                          !selectedPortals.includes(portal) && selectedPortals.length >= 5
                            ? 'text-slate-600'
                            : 'text-slate-200'
                        }`}>
                          {portal}
                        </span>
                      </label>
                    ))}
                  </div>
                )}
              </div>
              <p className="mt-1.5 text-xs text-slate-500">{selectedPortals.length}/5 portals selected</p>
            </div>

            <button
              type="submit"
              disabled={loading || !keywords.trim() || selectedPortals.length === 0}
              className="inline-flex items-center justify-center gap-2 rounded-lg text-sm font-medium transition-all disabled:opacity-50 bg-gradient-to-r from-red-600 to-red-500 text-white hover:from-red-500 hover:to-red-400 h-11 px-6 shadow-lg shadow-red-500/30"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Scanning {selectedPortals.length} portal{selectedPortals.length > 1 ? 's' : ''}...
                </>
              ) : (
                <>
                  <Search className="w-4 h-4" />
                  Start Scan
                </>
              )}
            </button>
          </form>
        </div>

        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 mb-8">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-semibold text-white">Auto Scheduler</h2>
              <p className="text-xs text-slate-400 mt-0.5">Runs scans automatically in background using keywords and portals above</p>
            </div>
            <button
              type="button"
              onClick={toggleScheduler}
              disabled={schedulerSaving}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                scheduler.schedulerEnabled ? 'bg-green-500' : 'bg-slate-600'
              }`}
            >
              <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                scheduler.schedulerEnabled ? 'translate-x-6' : 'translate-x-1'
              }`} />
            </button>
          </div>

          {scheduler.schedulerEnabled && (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Start Time</label>
                <input
                  type="time"
                  value={scheduler.schedulerStartTime}
                  onChange={(e) => {
                    const s = { ...scheduler, schedulerStartTime: e.target.value };
                    setScheduler(s);
                    saveScheduler(s);
                  }}
                  className="h-10 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-3 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">End Time</label>
                <input
                  type="time"
                  value={scheduler.schedulerEndTime}
                  onChange={(e) => {
                    const s = { ...scheduler, schedulerEndTime: e.target.value };
                    setScheduler(s);
                    saveScheduler(s);
                  }}
                  className="h-10 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-3 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Interval</label>
                <select
                  value={scheduler.schedulerIntervalHours}
                  onChange={(e) => {
                    const s = { ...scheduler, schedulerIntervalHours: Number(e.target.value) };
                    setScheduler(s);
                    saveScheduler(s);
                  }}
                  className="h-10 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-3 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500"
                >
                  <option value={10} className="bg-slate-900">Every 10 min</option>
                  <option value={30} className="bg-slate-900">Every 30 min</option>
                  <option value={60} className="bg-slate-900">Every 1 hour</option>
                  <option value={120} className="bg-slate-900">Every 2 hours</option>
                  <option value={240} className="bg-slate-900">Every 4 hours</option>
                </select>
              </div>
            </div>
          )}

          {schedulerMsg && (
            <p className="text-xs text-green-400 mt-3">{schedulerMsg}</p>
          )}
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 backdrop-blur border border-red-500/30 text-red-200 rounded-lg text-sm flex items-center gap-2">
            <XCircle className="w-4 h-4" />
            {error}
          </div>
        )}

        {result && (
          <>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
              {[
                { label: 'Found', value: result.totalFound, color: 'text-white' },
                { label: 'Matched', value: result.matched, color: 'text-white' },
                { label: 'Applied', value: result.applied, color: 'text-green-400' },
                { label: 'Skipped', value: result.skipped, color: 'text-amber-400' },
                { label: 'Failed', value: result.failed, color: 'text-red-400' },
              ].map((s) => (
                <div key={s.label} className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 text-center">
                  <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
                  <p className="text-xs text-slate-400 mt-1">{s.label}</p>
                </div>
              ))}
            </div>

            <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
              <div className="px-6 py-4 border-b border-white/10">
                <h2 className="text-lg font-semibold text-white">Results</h2>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="text-left text-xs uppercase tracking-wide text-slate-400 border-b border-white/10">
                      <th className="px-6 py-3 font-medium">Job Title</th>
                      <th className="px-6 py-3 font-medium">Company</th>
                      <th className="px-6 py-3 font-medium">Location</th>
                      <th className="px-6 py-3 font-medium">Recruiter</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.results.map((item, idx) => (
                      <tr key={idx} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                        <td className="px-6 py-3 text-sm font-medium text-white">
                          {item.sourceUrl ? (
                            <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer" className="text-red-400 hover:text-red-300 transition-colors">
                              {item.jobTitle}
                            </a>
                          ) : item.jobTitle}
                        </td>
                        <td className="px-6 py-3 text-sm text-slate-300">{item.company}</td>
                        <td className="px-6 py-3 text-sm text-slate-300">{item.location}</td>
                        <td className="px-6 py-3 text-sm text-slate-300">{item.recruiterEmail || '—'}</td>
                        <td className="px-6 py-3">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full border ${getStatusColor(item.status)}`}>
                            {getStatusIcon(item.status)}
                            {item.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default ScanPage;
