import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login } from '../api/api';
import { Sparkles, Zap, Mail, FileCheck } from 'lucide-react';

const LoginPage = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { loginUser } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await login({ username, password });
      if (res.data.success) {
        const { token, username: uname, fullName } = res.data.data;
        loginUser(token, { username: uname, fullName });
        navigate('/dashboard');
      } else {
        setError(res.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen relative overflow-hidden bg-slate-950">
      <div className="absolute inset-0">
        <div className="absolute top-0 -left-40 w-[500px] h-[500px] bg-red-600/30 rounded-full blur-[120px] animate-pulse"></div>
        <div className="absolute top-1/3 right-0 w-[600px] h-[600px] bg-blue-600/20 rounded-full blur-[120px] animate-pulse" style={{ animationDelay: '1s' }}></div>
        <div className="absolute bottom-0 left-1/3 w-[500px] h-[500px] bg-purple-600/20 rounded-full blur-[120px] animate-pulse" style={{ animationDelay: '2s' }}></div>
      </div>

      <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff08_1px,transparent_1px),linear-gradient(to_bottom,#ffffff08_1px,transparent_1px)] bg-[size:4rem_4rem]"></div>

      <div className="relative min-h-screen grid lg:grid-cols-2">
        <div className="hidden lg:flex flex-col justify-between p-12 text-white">
          <div className="flex items-center gap-2 text-3xl font-black tracking-tight" style={{ fontFamily: "'Arial Black', 'Helvetica Neue', sans-serif" }}>
            <span className="text-white">I</span>
            <span className="text-red-500 inline-block" style={{ transform: 'scaleX(-1)' }}>R</span>
            <span className="text-white">S</span>
            <span className="text-red-500 ml-1">Job</span>
            <span className="text-white">Pilot</span>
          </div>

          <div className="space-y-8">
            <div>
              <h1 className="text-5xl font-bold leading-tight mb-4">
                Apply to jobs <br />
                <span className="bg-gradient-to-r from-red-400 to-red-600 bg-clip-text text-transparent">while you sleep.</span>
              </h1>
              <p className="text-slate-300 text-lg max-w-md">
                Let AI find jobs, match your resume, and send personalized emails to recruiters — automatically.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-4 max-w-md">
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 hover:bg-white/10 transition-all">
                <Sparkles className="w-5 h-5 text-red-500 mb-2" />
                <p className="text-sm font-medium">AI Matching</p>
                <p className="text-xs text-slate-400 mt-1">Smart resume selection</p>
              </div>
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 hover:bg-white/10 transition-all">
                <Mail className="w-5 h-5 text-red-500 mb-2" />
                <p className="text-sm font-medium">Auto Email</p>
                <p className="text-xs text-slate-400 mt-1">Personalized outreach</p>
              </div>
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 hover:bg-white/10 transition-all">
                <Zap className="w-5 h-5 text-red-500 mb-2" />
                <p className="text-sm font-medium">11 Portals</p>
                <p className="text-xs text-slate-400 mt-1">One-click scanning</p>
              </div>
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl p-4 hover:bg-white/10 transition-all">
                <FileCheck className="w-5 h-5 text-red-500 mb-2" />
                <p className="text-sm font-medium">Scheduler</p>
                <p className="text-xs text-slate-400 mt-1">Set & forget</p>
              </div>
            </div>
          </div>

          <div className="text-sm text-slate-500">© 2026 JobPilot. Automating careers.</div>
        </div>

        <div className="flex items-center justify-center p-8">
          <div className="w-full max-w-md">
            <div className="lg:hidden mb-8 text-center text-white">
              <div className="flex items-center justify-center gap-2 text-3xl font-black tracking-tight" style={{ fontFamily: "'Arial Black', 'Helvetica Neue', sans-serif" }}>
                <span>I</span>
                <span className="text-red-500 inline-block" style={{ transform: 'scaleX(-1)' }}>R</span>
                <span>S</span>
                <span className="text-red-500 ml-1">Job</span>
                <span>Pilot</span>
              </div>
            </div>

            <div className="bg-white/10 backdrop-blur-2xl border border-white/20 rounded-2xl shadow-2xl p-8 relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent pointer-events-none"></div>

              <div className="relative">
                <div className="mb-8">
                  <h2 className="text-3xl font-bold tracking-tight text-white">Welcome back</h2>
                  <p className="text-slate-300 mt-2">Sign in to continue your job search</p>
                </div>

                {error && (
                  <div className="mb-4 p-3 bg-red-500/20 backdrop-blur border border-red-500/30 text-red-200 rounded-lg text-sm">
                    {error}
                  </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-5">
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-200">Email</label>
                    <input
                      type="text"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      required
                      className="flex h-11 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 py-2 text-sm text-white placeholder:text-slate-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition"
                      placeholder="you@example.com"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-200">Password</label>
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      className="flex h-11 w-full rounded-lg border border-white/20 bg-white/5 backdrop-blur px-4 py-2 text-sm text-white placeholder:text-slate-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:border-red-500/50 transition"
                      placeholder="••••••••"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-all disabled:opacity-50 bg-gradient-to-r from-red-600 to-red-500 text-white hover:from-red-500 hover:to-red-400 h-11 px-4 py-2 w-full shadow-lg shadow-red-500/30"
                  >
                    {loading ? 'Signing in...' : 'Sign In'}
                  </button>
                </form>

                <div className="mt-8 text-center">
                  <p className="text-sm text-slate-300">
                    Don't have an account?{' '}
                    <Link to="/register" className="font-medium text-red-400 hover:text-red-300 transition-colors">
                      Sign up for free
                    </Link>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
