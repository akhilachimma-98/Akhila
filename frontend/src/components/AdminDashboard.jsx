import React, { useState, useEffect } from 'react';
import api from '../api/client';
import {
  Users, Landmark, ShieldAlert, ArrowLeftRight, CheckCircle2,
  Lock, Unlock, DollarSign, Activity, AlertCircle, RefreshCw
} from 'lucide-react';

const AdminDashboard = ({ activeTab }) => {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [notification, setNotification] = useState(null);

  // Freeze/Unfreeze modal
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [targetStatus, setTargetStatus] = useState('FROZEN');
  const [reason, setReason] = useState('Administrative compliance review');

  useEffect(() => {
    fetchStats();
    fetchUsers();
    fetchTransactions();
  }, []);

  const fetchStats = async () => {
    try {
      const res = await api.get('/admin/stats');
      if (res.data.success) {
        setStats(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load stats', err);
    }
  };

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await api.get('/admin/users?size=25');
      if (res.data.success) {
        setUsers(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load users', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchTransactions = async () => {
    try {
      const res = await api.get('/admin/transactions?size=25');
      if (res.data.success) {
        setTransactions(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load transactions', err);
    }
  };

  const handleUpdateAccountStatus = async (e) => {
    e.preventDefault();
    if (!selectedAccount) return;

    setActionLoading(true);
    setNotification(null);

    try {
      const res = await api.patch(`/admin/accounts/${selectedAccount.accountNumber}/status`, {
        status: targetStatus,
        reason: reason || 'Admin operation',
      });

      if (res.data.success) {
        setNotification({
          type: 'success',
          message: `Account ${selectedAccount.accountNumber} status changed to ${targetStatus}`,
        });
        setSelectedAccount(null);
        fetchUsers();
        fetchStats();
      }
    } catch (err) {
      setNotification({
        type: 'error',
        message: err.response?.data?.message || 'Status update failed',
      });
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      
      {/* Top Banner & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight flex items-center space-x-2">
            <span>Bank Administration Console</span>
            <span className="bg-purple-950 text-purple-400 border border-purple-800 text-xs px-2.5 py-0.5 rounded-full font-bold">
              ROLE_ADMIN
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">Manage customers, freeze suspicious accounts, and inspect volume</p>
        </div>

        <button
          onClick={() => {
            fetchStats();
            fetchUsers();
            fetchTransactions();
          }}
          className="self-start sm:self-auto px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl flex items-center space-x-2 border border-slate-700 transition"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Refresh Data</span>
        </button>
      </div>

      {notification && (
        <div
          className={`p-4 rounded-xl flex items-center justify-between border ${
            notification.type === 'success'
              ? 'bg-emerald-950/40 border-emerald-800 text-emerald-200'
              : 'bg-rose-950/40 border-rose-800 text-rose-200'
          }`}
        >
          <div className="flex items-center space-x-3">
            {notification.type === 'success' ? (
              <CheckCircle2 className="h-5 w-5 text-emerald-400" />
            ) : (
              <AlertCircle className="h-5 w-5 text-rose-400" />
            )}
            <span className="text-sm font-medium">{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-xs uppercase font-bold tracking-wider"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* System Statistics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Active Balance</span>
            <DollarSign className="h-4 w-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">
            ${stats ? parseFloat(stats.totalActiveBalance).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Across all customer accounts</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Deposits</span>
            <Landmark className="h-4 w-4 text-teal-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">
            ${stats ? parseFloat(stats.totalSystemDeposits).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Lifetime incoming reserve</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Transfer Volume</span>
            <ArrowLeftRight className="h-4 w-4 text-purple-400" />
          </div>
          <div className="text-2xl font-bold text-white font-mono">
            ${stats ? parseFloat(stats.totalTransfersVolume).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Account-to-account velocity</p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Accounts Status</span>
            <ShieldAlert className="h-4 w-4 text-amber-400" />
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-bold text-emerald-400">{stats?.activeAccounts ?? 0}</span>
            <span className="text-xs text-slate-400">Active</span>
            <span className="text-slate-600">/</span>
            <span className="text-2xl font-bold text-rose-400">{stats?.frozenAccounts ?? 0}</span>
            <span className="text-xs text-slate-400">Frozen</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Total {stats?.totalAccounts ?? 0} bank accounts</p>
        </div>
      </div>

      {/* View Toggle: Users & Accounts or All Transactions */}
      {activeTab === 'admin_txns' ? (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="p-6 border-b border-slate-800 flex items-center justify-between">
            <h3 className="text-base font-bold text-white">All System Transactions</h3>
            <span className="text-xs text-slate-400">{transactions.length} records</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4">Reference</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Source</th>
                  <th className="py-3 px-4">Target</th>
                  <th className="py-3 px-4 text-right">Amount</th>
                  <th className="py-3 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-slate-200">
                {transactions.map((txn) => (
                  <tr key={txn.transactionReference} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 font-mono text-slate-400">{new Date(txn.createdAt).toLocaleString()}</td>
                    <td className="py-3 px-4 font-mono font-medium text-slate-200">{txn.transactionReference}</td>
                    <td className="py-3 px-4 font-semibold">{txn.type}</td>
                    <td className="py-3 px-4 font-mono text-slate-400">{txn.sourceAccountNumber || 'External'}</td>
                    <td className="py-3 px-4 font-mono text-slate-400">{txn.targetAccountNumber || 'External'}</td>
                    <td className="py-3 px-4 text-right font-mono font-bold text-white">${parseFloat(txn.amount).toFixed(2)}</td>
                    <td className="py-3 px-4 text-center">
                      <span className="bg-emerald-950 text-emerald-400 border border-emerald-800 text-[10px] px-2 py-0.5 rounded font-bold">
                        {txn.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="p-6 border-b border-slate-800 flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-white">Registered Users &amp; Accounts Management</h3>
              <p className="text-xs text-slate-400">View customer holdings, freeze accounts on suspicion, and unfreeze upon verification</p>
            </div>
            <span className="text-xs text-slate-400">{users.length} users registered</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">User</th>
                  <th className="py-3 px-4">Role</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4 text-right">Balance</th>
                  <th className="py-3 px-4 text-center">Account Status</th>
                  <th className="py-3 px-4 text-right">Administrative Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-slate-200">
                {users.map((u) => {
                  const account = u.accounts && u.accounts.length > 0 ? u.accounts[0] : null;
                  return (
                    <tr key={u.id} className="hover:bg-slate-800/40 transition">
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-white">{u.fullName || u.username}</div>
                        <div className="text-[11px] text-slate-400 font-mono">{u.email}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="text-slate-300 font-mono text-[11px]">{u.role.replace('ROLE_', '')}</span>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          u.status === 'ACTIVE' ? 'bg-emerald-950 text-emerald-400' : 'bg-rose-950 text-rose-400'
                        }`}>
                          {u.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-300">
                        {account ? account.accountNumber : <span className="text-slate-500">None</span>}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold text-emerald-400">
                        {account ? `$${parseFloat(account.balance).toFixed(2)}` : '-'}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        {account ? (
                          <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase border ${
                            account.status === 'ACTIVE'
                              ? 'bg-emerald-950/80 text-emerald-300 border-emerald-800'
                              : 'bg-rose-950/80 text-rose-300 border-rose-800'
                          }`}>
                            {account.status}
                          </span>
                        ) : '-'}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        {account && (
                          account.status === 'ACTIVE' ? (
                            <button
                              onClick={() => {
                                setSelectedAccount(account);
                                setTargetStatus('FROZEN');
                                setReason('Suspicious transaction review');
                              }}
                              className="px-3 py-1.5 bg-rose-950/60 hover:bg-rose-900 border border-rose-800 text-rose-200 rounded-lg text-xs font-semibold inline-flex items-center space-x-1.5 transition"
                            >
                              <Lock className="h-3 w-3" />
                              <span>Freeze Account</span>
                            </button>
                          ) : (
                            <button
                              onClick={() => {
                                setSelectedAccount(account);
                                setTargetStatus('ACTIVE');
                                setReason('Verification cleared by admin');
                              }}
                              className="px-3 py-1.5 bg-emerald-950/60 hover:bg-emerald-900 border border-emerald-800 text-emerald-200 rounded-lg text-xs font-semibold inline-flex items-center space-x-1.5 transition"
                            >
                              <Unlock className="h-3 w-3" />
                              <span>Unfreeze Account</span>
                            </button>
                          )
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Freeze / Unfreeze Confirmation Modal */}
      {selectedAccount && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2 flex items-center space-x-2">
              {targetStatus === 'FROZEN' ? (
                <>
                  <Lock className="h-5 w-5 text-rose-400" />
                  <span>Freeze Bank Account</span>
                </>
              ) : (
                <>
                  <Unlock className="h-5 w-5 text-emerald-400" />
                  <span>Unfreeze Bank Account</span>
                </>
              )}
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Target Account: <strong className="font-mono text-white">{selectedAccount.accountNumber}</strong>
            </p>

            <form onSubmit={handleUpdateAccountStatus} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Administrative Reason (Logged in Audit Trail)</label>
                <input
                  type="text"
                  required
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="e.g. KYC compliance or suspicious login"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-white focus:border-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedAccount(null)}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold py-2.5 rounded-xl text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className={`flex-1 font-bold py-2.5 rounded-xl text-sm text-slate-950 transition ${
                    targetStatus === 'FROZEN' ? 'bg-rose-500 hover:bg-rose-400' : 'bg-emerald-500 hover:bg-emerald-400'
                  }`}
                >
                  {actionLoading ? 'Updating...' : `Confirm ${targetStatus}`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default AdminDashboard;
