import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import CustomerDashboard from './components/CustomerDashboard';
import AdminDashboard from './components/AdminDashboard';
import AuditorDashboard from './components/AuditorDashboard';

const MainApp = () => {
  const { user, loading } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950 text-emerald-400">
        <div className="text-center space-y-3">
          <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-xs font-mono uppercase tracking-widest text-slate-400">Loading LedgerLine...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <AuthModal />;
  }

  const renderDashboard = () => {
    // If Auditor
    if (user.role === 'ROLE_AUDITOR') {
      return <AuditorDashboard activeTab={activeTab} />;
    }

    // If Admin
    if (user.role === 'ROLE_ADMIN') {
      if (activeTab === 'audit_recon' || activeTab === 'audit_logs' || activeTab === 'audit_ledger') {
        return <AuditorDashboard activeTab={activeTab} />;
      }
      return <AdminDashboard activeTab={activeTab} />;
    }

    // Default: Customer
    return <CustomerDashboard activeTab={activeTab} />;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {renderDashboard()}
      </main>
      <footer className="border-t border-slate-900 py-6 text-center text-xs text-slate-500">
        LedgerLine – Secure Digital Ledger &amp; Money Transfer System • Snovasys Fresher Hiring Challenge 2026
      </footer>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

export default App;
