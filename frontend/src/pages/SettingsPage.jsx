import { useState, useEffect } from 'react';
import { getProfile, updateSettings } from '../api/api';
import { Save, Eye, EyeOff, Loader2 } from 'lucide-react';

const SettingsPage = () => {
  const [form, setForm] = useState({
    gmailApiKey: '',
    aiApiKey: '',
    aiModelType: 'claude',
    emailSignature: '',
    schedulerEnabled: false,
    schedulerIntervalHours: 60,
    schedulerStartTime: '07:00',
    schedulerEndTime: '17:00',
    schedulerKeywords: '.NET Developer',
    schedulerPortals: 'Nvoids',
    schedulerTitleFilter: '',
  });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const [showGmail, setShowGmail] = useState(false);
  const [showAi, setShowAi] = useState(false);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const res = await getProfile();
        if (res.data.success) {
          const data = res.data.data;
          setForm({
            gmailApiKey: '',
            aiApiKey: '',
            aiModelType: data.aiModelType || 'claude',
            emailSignature: data.emailSignature || '',
            schedulerEnabled: data.schedulerEnabled || false,
            schedulerIntervalHours: data.schedulerIntervalHours || 60,
            schedulerStartTime: data.schedulerStartTime || '07:00',
            schedulerEndTime: data.schedulerEndTime || '17:00',
            schedulerKeywords: data.schedulerKeywords || '.NET Developer',
            schedulerPortals: data.schedulerPortals || 'Nvoids',
            schedulerTitleFilter: data.schedulerTitleFilter || '',
          });
        }
      } catch (err) {
        console.error(err);
      }
    };
    loadProfile();
  }, []);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      const payload = { ...form };
      if (!payload.gmailApiKey) delete payload.gmailApiKey;
      if (!payload.aiApiKey) delete payload.aiApiKey;
      const res = await updateSettings(payload);
      if (res.data.success) {
        setSuccess('Settings saved successfully!');
        setForm({ ...form, gmailApiKey: '', aiApiKey: '' });
      } else {
        setError(res.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save settings.');
    } finally {
      setLoading(false);
    }
  };

  const inputClass = "flex h-11 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 text-sm text-white placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition";

  return (
    <div className="min-h-screen relative bg-slate-950">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 -left-40 w-[500px] h-[500px] bg-red-600/20 rounded-full blur-[120px]"></div>
        <div className="absolute top-1/3 right-0 w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-[120px]"></div>
      </div>
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff05_1px,transparent_1px),linear-gradient(to_bottom,#ffffff05_1px,transparent_1px)] bg-[size:4rem_4rem] pointer-events-none"></div>

      <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white tracking-tight">Settings</h1>
          <p className="text-slate-400 mt-1">Configure your API keys and preferences</p>
        </div>

        {success && (
          <div className="mb-4 p-3 bg-green-500/20 backdrop-blur border border-green-500/30 text-green-300 rounded-lg text-sm">
            {success}
          </div>
        )}
        {error && (
          <div className="mb-4 p-3 bg-red-500/20 backdrop-blur border border-red-500/30 text-red-200 rounded-lg text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Gmail */}
          <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
            <h2 className="text-lg font-semibold text-white mb-4">Gmail Configuration</h2>
            <label className="block text-sm font-medium text-slate-200 mb-2">Gmail App Password</label>
            <div className="relative">
              <input
                type={showGmail ? 'text' : 'password'}
                name="gmailApiKey"
                value={form.gmailApiKey}
                onChange={handleChange}
                placeholder="Enter new app password (leave blank to keep current)"
                className={inputClass + ' pr-10'}
              />
              <button type="button" onClick={() => setShowGmail(!showGmail)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white">
                {showGmail ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            <p className="text-xs text-slate-500 mt-2">
              Generate at: Google Account → Security → 2-Step Verification → App passwords
            </p>
          </div>

          {/* AI */}
          <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
            <h2 className="text-lg font-semibold text-white mb-4">AI Configuration</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-200 mb-2">AI Provider</label>
                <select
                  name="aiModelType"
                  value={form.aiModelType}
                  onChange={handleChange}
                  className={inputClass}
                >
                  <option value="claude" className="bg-slate-900">Claude (Anthropic)</option>
                  <option value="openai" className="bg-slate-900">GPT (OpenAI)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-200 mb-2">API Key</label>
                <div className="relative">
                  <input
                    type={showAi ? 'text' : 'password'}
                    name="aiApiKey"
                    value={form.aiApiKey}
                    onChange={handleChange}
                    placeholder="Enter new API key (leave blank to keep current)"
                    className={inputClass + ' pr-10'}
                  />
                  <button type="button" onClick={() => setShowAi(!showAi)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white">
                    {showAi ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Signature */}
          <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
            <h2 className="text-lg font-semibold text-white mb-4">Email Signature</h2>
            <textarea
              name="emailSignature"
              value={form.emailSignature}
              onChange={handleChange}
              rows={4}
              placeholder={"Your name\nPhone\nLinkedIn"}
              className="flex w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 py-3 text-sm text-white placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition resize-y"
            />
          </div>

          {/* Scheduler */}
          <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
            <h2 className="text-lg font-semibold text-white mb-4">Scheduler</h2>
            <div className="space-y-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  name="schedulerEnabled"
                  checked={form.schedulerEnabled}
                  onChange={handleChange}
                  className="w-4 h-4 rounded accent-red-500"
                />
                <span className="text-sm font-medium text-slate-200">Enable auto-scan scheduler</span>
              </label>

              {form.schedulerEnabled && (
                <div className="space-y-4 pt-2 pl-7">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-200 mb-2">Start Time</label>
                      <input type="time" name="schedulerStartTime" value={form.schedulerStartTime} onChange={handleChange} className={inputClass} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-200 mb-2">End Time</label>
                      <input type="time" name="schedulerEndTime" value={form.schedulerEndTime} onChange={handleChange} className={inputClass} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-200 mb-2">Interval</label>
                      <select name="schedulerIntervalHours" value={form.schedulerIntervalHours} onChange={handleChange} className={inputClass}>
                        <option value={10} className="bg-slate-900">Every 10 minutes</option>
                        <option value={30} className="bg-slate-900">Every 30 minutes</option>
                        <option value={60} className="bg-slate-900">Every 1 hour</option>
                        <option value={120} className="bg-slate-900">Every 2 hours</option>
                        <option value={240} className="bg-slate-900">Every 4 hours</option>
                      </select>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-200 mb-2">Keywords (comma-separated)</label>
                    <input type="text" name="schedulerKeywords" value={form.schedulerKeywords} onChange={handleChange} placeholder=".NET Developer, C# Engineer" className={inputClass} />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-200 mb-2">Portals (comma-separated)</label>
                    <input type="text" name="schedulerPortals" value={form.schedulerPortals} onChange={handleChange} placeholder="Nvoids, Hiring42, Dice" className={inputClass} />
                    <p className="mt-1.5 text-xs text-slate-500">
                      Available: Nvoids, Hiring42, Robert Half, Randstad, Insight Global, TEKsystems, Apex Systems, Kforce, Collabera, Beacon Hill, Dice
                    </p>
                  </div>
                  <div>
                  <label className="block text-sm font-medium text-slate-200 mb-2">
                    Title Filter (strict match)
                  </label>
                  <input type="text" name="schedulerTitleFilter" value={form.schedulerTitleFilter || ''} onChange={handleChange} placeholder="e.g. .net developer" className={inputClass} />
                  <p className="mt-1.5 text-xs text-slate-500">
                    Only jobs whose title contains all these words will be applied to. Leave blank to apply to all matched jobs.
                  </p>
                </div>
                </div>
              )}
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-lg text-sm font-medium transition-all disabled:opacity-50 bg-gradient-to-r from-red-600 to-red-500 text-white hover:from-red-500 hover:to-red-400 h-11 px-6 shadow-lg shadow-red-500/30"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
            Save Settings
          </button>
        </form>
      </div>
    </div>
  );
};

export default SettingsPage;
