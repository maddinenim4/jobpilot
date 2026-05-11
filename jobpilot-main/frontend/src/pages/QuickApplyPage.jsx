import { useState } from 'react';
import { quickApply } from '../api/api';
import { Send, Loader2, CheckCircle, AlertCircle } from 'lucide-react';

const QuickApplyPage = () => {
  const [form, setForm] = useState({
    recruiterEmail: '',
    jobTitle: '',
    company: '',
    location: '',
    jobDescription: '',
  });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);

    try {
      const res = await quickApply(form);
      if (res.data.success) {
        setResult(res.data.data);
        setForm({
          recruiterEmail: '',
          jobTitle: '',
          company: '',
          location: '',
          jobDescription: '',
        });
      } else {
        setError(res.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Application failed. Check your settings.');
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
          <h1 className="text-3xl font-bold text-white tracking-tight">Quick Apply</h1>
          <p className="text-slate-400 mt-1">Paste a job description and send your application instantly</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 backdrop-blur border border-red-500/30 text-red-200 rounded-lg text-sm flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            {error}
          </div>
        )}

        {result && (
          <div className="mb-6 p-4 bg-green-500/20 backdrop-blur border border-green-500/30 rounded-lg">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle className="w-5 h-5 text-green-300" />
              <p className="text-green-200 font-medium">Application sent successfully!</p>
            </div>
            <div className="text-sm text-green-200/80 space-y-1 pl-7">
              <p>Job: {result.jobTitle} at {result.company}</p>
              <p>Sent to: {result.recruiterEmail}</p>
              <p>Resume used: {result.resumeUsed}</p>
            </div>
          </div>
        )}

        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-200 mb-2">
                  Recruiter Email <span className="text-red-400">*</span>
                </label>
                <input type="email" name="recruiterEmail" value={form.recruiterEmail} onChange={handleChange} required placeholder="recruiter@company.com" className={inputClass} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-200 mb-2">Job Title</label>
                <input type="text" name="jobTitle" value={form.jobTitle} onChange={handleChange} placeholder=".NET Developer" className={inputClass} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-200 mb-2">Company</label>
                <input type="text" name="company" value={form.company} onChange={handleChange} placeholder="Acme Corp" className={inputClass} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-200 mb-2">Location</label>
                <input type="text" name="location" value={form.location} onChange={handleChange} placeholder="Remote / New York, NY" className={inputClass} />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-200 mb-2">
                Job Description <span className="text-red-400">*</span>
              </label>
              <textarea
                name="jobDescription"
                value={form.jobDescription}
                onChange={handleChange}
                required
                rows={12}
                placeholder="Paste full job description here..."
                className="flex w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 py-3 text-sm text-white placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition resize-y"
              />
            </div>

            <div className="bg-blue-500/10 backdrop-blur border border-blue-500/20 rounded-lg p-3">
              <p className="text-xs text-blue-200">
                JobPilot will analyze JD, select best matching resume, generate a personalized email, and send it with your resume attached.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading || !form.jobDescription.trim() || !form.recruiterEmail.trim()}
              className="inline-flex items-center justify-center gap-2 rounded-lg text-sm font-medium transition-all disabled:opacity-50 bg-gradient-to-r from-red-600 to-red-500 text-white hover:from-red-500 hover:to-red-400 h-11 px-6 shadow-lg shadow-red-500/30"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Sending application...
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  Quick Apply
                </>
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default QuickApplyPage;
