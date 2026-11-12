import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, LogOut, User, Landmark, Activity, FileText } from 'lucide-react';

const Navbar = ({ activeTab, setActiveTab }) => {
  const { user, logout } = useAuth();

  if (!user) return null;

  const getRoleBadge = (role) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return <span className="bg-purple-950/80 text-purple-300 border border-purple-800 text-xs px-2.5 py-0.5 rounded-full font-semibold">Admin</span>;
      case 'ROLE_AUDITOR':
        return <span className="bg-amber-950/80 text-amber-300 border border-amber-800 text-xs px-2.5 py-0.5 rounded-full font-semibold">Auditor</span>;
      default:
        return <span className="bg-emerald-950/80 text-emerald-300 border border-emerald-800 text-xs px-2.5 py-0.5 rounded-full font-semibold">Customer</span>;
    }
  };

  return (
    <header className="border-b border-slate-800 bg-slate-900/90 backdrop-blur sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Brand */}
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => setActiveTab('main')}>
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <Landmark className="h-5 w-5 text-slate-950" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-extrabold text-lg tracking-tight text-white">Ledger<span className="text-emerald-400">Line</span></span>
              <ShieldCheck className="h-4 w-4 text-emerald-400" />
            </div>
            <p className="text-xs text-slate-400 font-medium">Secure Digital Ledger &amp; Money Transfer</p>
          </div>
        </div>

        {/* Role Navigation Links */}
        <div className="hidden md:flex items-center space-x-2">
          {user.role === 'ROLE_CUSTOMER' && (
            <>
              <button
                onClick={() => setActiveTab('overview')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'overview' ? 'bg-slate-800 text-emerald-400 shadow-sm' : 'text-slate-400 hover:text-white'
                }`}
              >
                Account Overview
              </button>
              <button
                onClick={() => setActiveTab('statement')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'statement' ? 'bg-slate-800 text-emerald-400 shadow-sm' : 'text-slate-400 hover:text-white'
                }`}
              >
                Passbook Statement
              </button>
            </>
          )}

          {user.role === 'ROLE_ADMIN' && (
            <>
              <button
                onClick={() => setActiveTab('admin_users')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'admin_users' ? 'bg-slate-800 text-purple-400' : 'text-slate-400 hover:text-white'
                }`}
              >
                Users &amp; Accounts
              </button>
              <button
                onClick={() => setActiveTab('admin_txns')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'admin_txns' ? 'bg-slate-800 text-purple-400' : 'text-slate-400 hover:text-white'
                }`}
              >
                All Transactions
              </button>
            </>
          )}

          {(user.role === 'ROLE_AUDITOR' || user.role === 'ROLE_ADMIN') && (
            <>
              <button
                onClick={() => setActiveTab('audit_recon')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'audit_recon' ? 'bg-slate-800 text-amber-400' : 'text-slate-400 hover:text-white'
                }`}
              >
                Reconciliation
              </button>
              <button
                onClick={() => setActiveTab('audit_logs')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'audit_logs' ? 'bg-slate-800 text-amber-400' : 'text-slate-400 hover:text-white'
                }`}
              >
                Audit Logs
              </button>
              <button
                onClick={() => setActiveTab('audit_ledger')}
                className={`px-3 py-1.5 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === 'audit_ledger' ? 'bg-slate-800 text-amber-400' : 'text-slate-400 hover:text-white'
                }`}
              >
                Double-Entry Ledger
              </button>
            </>
          )}
        </div>

        {/* User Profile & Logout */}
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-3 text-right">
            <div className="hidden sm:block">
              <div className="flex items-center justify-end space-x-2">
                <span className="font-semibold text-sm text-slate-200">{user.fullName || user.username}</span>
                {getRoleBadge(user.role)}
              </div>
              <p className="text-xs text-slate-400 font-mono">{user.email}</p>
            </div>
            <div className="h-9 w-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
              <User className="h-4 w-4" />
            </div>
          </div>

          <button
            onClick={logout}
            title="Sign Out"
            className="p-2 text-slate-400 hover:text-rose-400 hover:bg-rose-950/30 rounded-lg transition-colors"
          >
            <LogOut className="h-5 w-5" />
          </button>
        </div>

      </div>
    </header>
  );
};

export default Navbar;
