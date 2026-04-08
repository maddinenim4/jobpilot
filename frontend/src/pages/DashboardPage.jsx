import { useEffect, useState } from 'react';
import { getStats, getAppliedJobs } from '../api/api';
import { Briefcase, CheckCircle, XCircle, SkipForward, Mail, Building2, MapPin, Calendar } from 'lucide-react';

const DashboardPage = () => {
  const [stats, setStats] = useState({ total: 0, applied: 0, skipped: 0, failed: 0 });
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const [statsRes, jobsRes] = await Promise.all([getStats(), getAppliedJobs()]);
        if (statsRes.data.success) setStats(statsRes.data.data);
        if (jobsRes.data.success) setJobs(jobsRes.data.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const statCards = [
    { label: 'Total Found', value: stats.total, icon: Briefcase, gradient: 'from-blue-500 to-blue-600', glow: 'shadow-blue-500/30' },
    { label: 'Applied', value: stats.applied, icon: CheckCircle, gradient: 'from-green-500 to-emerald-600', glow: 'shadow-green-500/30' },
    { label: 'Skipped', value: stats.skipped, icon: SkipForward, gradient: 'from-amber-500 to-orange-600', glow: 'shadow-amber-500/30' },
    { label: 'Failed', value: stats.failed, icon: XCircle, gradient: 'from-red-500 to-red-600', glow: 'shadow-red-500/30' },
  ];

  return (
    <div className="min-h-screen relative bg-slate-950">
      {/* Background gradients */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 -left-40 w-[500px] h-[500px] bg-red-600/20 rounded-full blur-[120px]"></div>
        <div className="absolute top-1/3 right-0 w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-[120px]"></div>
      </div>

      <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff05_1px,transparent_1px),linear-gradient(to_bottom,#ffffff05_1px,transparent_1px)] bg-[size:4rem_4rem] pointer-events-none"></div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white tracking-tight">Dashboard</h1>
          <p className="text-slate-400 mt-1">Track your automated job applications</p>
        </div>

        {/* Stat cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {statCards.map((stat) => {
            const Icon = stat.icon;
            return (
              <div key={stat.label} className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6 hover:bg-white/10 transition-all">
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${stat.gradient} flex items-center justify-center mb-4 shadow-lg ${stat.glow}`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <p className="text-3xl font-bold text-white">{stat.value}</p>
                <p className="text-sm text-slate-400 mt-1">{stat.label}</p>
              </div>
            );
          })}
        </div>

        {/* Applied jobs list */}
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-white/10">
            <h2 className="text-lg font-semibold text-white">Recent Applications</h2>
            <p className="text-sm text-slate-400 mt-0.5">Your latest job applications sent via JobPilot</p>
          </div>

          {loading ? (
            <div className="p-12 text-center text-slate-400">Loading...</div>
          ) : jobs.length === 0 ? (
            <div className="p-12 text-center">
              <Briefcase className="w-12 h-12 text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400">No applications yet</p>
              <p className="text-sm text-slate-500 mt-1">Run a scan or use Quick Apply to get started</p>
            </div>
          ) : (
            <div className="divide-y divide-white/5">
              {jobs.map((job) => (
                <div key={job.id} className="px-6 py-4 hover:bg-white/5 transition-colors">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <h3 className="text-sm font-semibold text-white truncate">{job.jobTitle}</h3>
                      <div className="flex items-center gap-4 mt-2 text-xs text-slate-400">
                        <div className="flex items-center gap-1">
                          <Building2 className="w-3 h-3" />
                          {job.company}
                        </div>
                        <div className="flex items-center gap-1">
                          <MapPin className="w-3 h-3" />
                          {job.location || 'Remote'}
                        </div>
                        <div className="flex items-center gap-1">
                          <Mail className="w-3 h-3" />
                          {job.recruiterEmail}
                        </div>
                        {job.appliedAt && (
                          <div className="flex items-center gap-1">
                            <Calendar className="w-3 h-3" />
                            {new Date(job.appliedAt).toLocaleDateString()}
                          </div>
                        )}
                      </div>
                    </div>
                    <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-green-500/20 text-green-300 border border-green-500/30 whitespace-nowrap">
                      {job.status || 'APPLIED'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
