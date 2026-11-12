import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import {
  CreditCard, ArrowUpRight, ArrowDownLeft, Send, History, FileText,
  AlertTriangle, CheckCircle, RefreshCw, KeyRound, Lock
} from 'lucide-react';

const CustomerDashboard = ({ activeTab }) => {
  const { user, refreshProfile } = useAuth();
  const [loading, setLoading] = useState(false);
  const [statementLoading, setStatementLoading] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [ledgerEntries, setLedgerEntries] = useState([]);
  const [notification, setNotification] = useState(null);

  // Modal controls
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [showDepositModal, setShowDepositModal] = useState(false);
  const [showWithdrawModal, setShowWithdrawModal] = useState(false);

  // Form states
  const [targetAccount, setTargetAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');

  // Primary account info
  const accountNumber = user?.accountNumber || (user?.accounts?.[0]?.accountNumber) || 'N/A';
  const balance = user?.accountBalance ?? (user?.accounts?.[0]?.balance) ?? 0;
  const status = user?.accountStatus || (user?.accounts?.[0]?.status) || 'ACTIVE';

  useEffect(() => {
    if (accountNumber && accountNumber !== 'N/A') {
      fetchHistory();
      fetchStatement();
    }
  }, [accountNumber]);

  const fetchHistory = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/transactions/history/${accountNumber}?size=10`);
      if (res.data.success) {
        setTransactions(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load transaction history', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchStatement = async () => {
    try {
      setStatementLoading(true);
      const res = await api.get(`/transactions/statement/${accountNumber}?size=15`);
      if (res.data.success) {
        setLedgerEntries(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load passbook statement', err);
    } finally {
      setStatementLoading(false);
    }
  };

  const openTransferModal = () => {
    // Generate new UUID for idempotency
    const newKey = 'IDEM-' + Math.random().toString(36).substring(2, 10).toUpperCase() + '-' + Date.now();
    setIdempotencyKey(newKey);
    setTargetAccount('');
    setAmount('');
    setDescription('Payment');
    setShowTransferModal(true);
  };

  const handleTransfer = async (e) => {
    e.preventDefault();
    setLoading(true);
    setNotification(null);

    try {
      const res = await api.post(
        '/transactions/transfer',
        {
          sourceAccountNumber: accountNumber,
          targetAccountNumber: targetAccount.trim(),
          amount: parseFloat(amount),
          description: description || 'Fund Transfer',
          idempotencyKey,
        },
        {
          headers: {
            'Idempotency-Key': idempotencyKey,
          },
        }
      );

      if (res.data.success) {
        setNotification({ type: 'success', message: `Transferred $${amount} to ${targetAccount} successfully!` });
        setShowTransferModal(false);
        await refreshProfile();
        fetchHistory();
        fetchStatement();
      }
    } catch (err) {
      setNotification({
        type: 'error',
        message: err.response?.data?.message || err.message || 'Transfer failed',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDeposit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setNotification(null);

    try {
      const res = await api.post('/transactions/deposit', {
        accountNumber,
        amount: parseFloat(amount),
        description: description || 'Self Deposit',
      });

      if (res.data.success) {
        setNotification({ type: 'success', message: `Deposited $${amount} successfully!` });
        setShowDepositModal(false);
        setAmount('');
        await refreshProfile();
        fetchHistory();
        fetchStatement();
      }
    } catch (err) {
      setNotification({ type: 'error', message: err.response?.data?.message || 'Deposit failed' });
    } finally {
      setLoading(false);
    }
  };

  const handleWithdraw = async (e) => {
    e.preventDefault();
    setLoading(true);
    setNotification(null);

    try {
      const res = await api.post('/transactions/withdraw', {
        accountNumber,
        amount: parseFloat(amount),
        description: description || 'ATM Withdrawal',
      });

      if (res.data.success) {
        setNotification({ type: 'success', message: `Withdrew $${amount} successfully!` });
        setShowWithdrawModal(false);
        setAmount('');
        await refreshProfile();
        fetchHistory();
        fetchStatement();
      }
    } catch (err) {
      setNotification({ type: 'error', message: err.response?.data?.message || 'Withdrawal failed' });
    } finally {
      setLoading(false);
    }
  };

  const isFrozen = status === 'FROZEN';

  return (
    <div className="space-y-8">
      
      {/* Alerts & Notifications */}
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
              <CheckCircle className="h-5 w-5 text-emerald-400 shrink-0" />
            ) : (
              <AlertTriangle className="h-5 w-5 text-rose-400 shrink-0" />
            )}
            <span className="text-sm font-medium">{notification.message}</span>
          </div>
          <button
            onClick={() => setNotification(null)}
            className="text-xs opacity-70 hover:opacity-100 uppercase tracking-wider font-bold"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Account Balance Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 border border-slate-800 p-8 shadow-xl">
          <div className="absolute top-0 right-0 w-80 h-80 bg-emerald-500/5 rounded-full blur-3xl pointer-events-none" />

          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center space-x-3">
              <div className="p-2.5 bg-slate-800 rounded-xl text-emerald-400 border border-slate-700">
                <CreditCard className="h-6 w-6" />
              </div>
              <div>
                <p className="text-xs uppercase tracking-wider text-slate-400 font-semibold">LedgerLine Primary Savings</p>
                <p className="text-sm font-mono text-slate-200 font-bold">{accountNumber}</p>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <span
                className={`text-xs px-3 py-1 rounded-full font-bold uppercase tracking-wider border ${
                  status === 'ACTIVE'
                    ? 'bg-emerald-950/80 text-emerald-300 border-emerald-800'
                    : 'bg-rose-950/80 text-rose-300 border-rose-800'
                }`}
              >
                {status}
              </span>
              <button
                onClick={async () => {
                  await refreshProfile();
                  fetchHistory();
                  fetchStatement();
                }}
                className="p-1.5 text-slate-400 hover:text-white rounded-lg transition"
                title="Refresh Balances"
              >
                <RefreshCw className="h-4 w-4" />
              </button>
            </div>
          </div>

          <div className="my-6">
            <p className="text-xs text-slate-400 mb-1 font-medium">Available Ledger Balance</p>
            <div className="text-4xl sm:text-5xl font-extrabold text-white tracking-tight flex items-baseline">
              <span className="text-emerald-400 mr-1">$</span>
              <span>{parseFloat(balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
              <span className="text-sm text-slate-400 ml-2 font-mono font-medium">USD</span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap gap-3 pt-4 border-t border-slate-800">
            <button
              onClick={openTransferModal}
              disabled={isFrozen}
              className="bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold px-5 py-2.5 rounded-xl text-sm flex items-center space-x-2 transition shadow-lg shadow-emerald-500/20 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <Send className="h-4 w-4" />
              <span>Send Money</span>
            </button>

            <button
              onClick={() => {
                setAmount('');
                setShowDepositModal(true);
              }}
              disabled={isFrozen}
              className="bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold px-4 py-2.5 rounded-xl text-sm flex items-center space-x-2 border border-slate-700 transition disabled:opacity-40"
            >
              <ArrowDownLeft className="h-4 w-4 text-emerald-400" />
              <span>Deposit</span>
            </button>

            <button
              onClick={() => {
                setAmount('');
                setShowWithdrawModal(true);
              }}
              disabled={isFrozen}
              className="bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold px-4 py-2.5 rounded-xl text-sm flex items-center space-x-2 border border-slate-700 transition disabled:opacity-40"
            >
              <ArrowUpRight className="h-4 w-4 text-rose-400" />
              <span>Withdraw</span>
            </button>
          </div>

          {isFrozen && (
            <div className="mt-4 p-3 bg-rose-950/30 border border-rose-900 rounded-xl flex items-center space-x-2 text-rose-300 text-xs">
              <Lock className="h-4 w-4 shrink-0" />
              <span>This account is currently <strong>FROZEN</strong> by Bank Administration. Outgoing and incoming transfers are locked.</span>
            </div>
          )}
        </div>

        {/* Security & Idempotency Info Card */}
        <div className="rounded-2xl bg-slate-900 border border-slate-800 p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center space-x-2 text-emerald-400 mb-3">
              <KeyRound className="h-5 w-5" />
              <h3 className="font-bold text-sm text-white">Financial Integrity Safeguards</h3>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed mb-4">
              Your money is protected by banking-grade transactional isolation:
            </p>
            <ul className="text-xs text-slate-300 space-y-2">
              <li className="flex items-start space-x-2">
                <span className="text-emerald-400 font-bold">•</span>
                <span><strong>Pessimistic Locking:</strong> Balances are serialized with row-level database locks.</span>
              </li>
              <li className="flex items-start space-x-2">
                <span className="text-emerald-400 font-bold">•</span>
                <span><strong>Append-Only Double Entry:</strong> Immutable debit and credit lines prove every penny.</span>
              </li>
              <li className="flex items-start space-x-2">
                <span className="text-emerald-400 font-bold">•</span>
                <span><strong>Replay Idempotency:</strong> Double clicking "Send" cannot deduct your money twice.</span>
              </li>
            </ul>
          </div>
          <div className="pt-4 border-t border-slate-800 text-[11px] text-slate-400">
            Account Owner: <span className="text-slate-200 font-semibold">{user?.fullName}</span>
          </div>
        </div>
      </div>

      {/* Main Content: Passbook Statement or Recent Transactions */}
      {activeTab === 'statement' ? (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="p-6 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <FileText className="h-5 w-5 text-emerald-400" />
              <div>
                <h3 className="text-base font-bold text-white">Immutable Passbook Statement</h3>
                <p className="text-xs text-slate-400">Double-entry ledger audit lines with running balance snapshots</p>
              </div>
            </div>
            <button
              onClick={fetchStatement}
              className="text-xs font-medium text-emerald-400 hover:underline"
            >
              Refresh
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Date &amp; Time</th>
                  <th className="py-3 px-4">Txn Reference</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4 text-right">Amount</th>
                  <th className="py-3 px-4 text-right">Balance After</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-slate-200">
                {statementLoading ? (
                  <tr>
                    <td colSpan="6" className="py-8 text-center text-slate-400">Loading ledger entries...</td>
                  </tr>
                ) : ledgerEntries.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="py-8 text-center text-slate-400">No ledger entries recorded yet.</td>
                  </tr>
                ) : (
                  ledgerEntries.map((entry) => (
                    <tr key={entry.id} className="hover:bg-slate-800/40 transition">
                      <td className="py-3.5 px-4 font-mono text-slate-400">
                        {new Date(entry.createdAt).toLocaleString()}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-300 font-medium">
                        {entry.transactionReference}
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`px-2 py-0.5 rounded font-bold uppercase text-[10px] ${
                            entry.entryType === 'CREDIT'
                              ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                              : 'bg-rose-950 text-rose-400 border border-rose-800'
                          }`}
                        >
                          {entry.entryType}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-300">{entry.description}</td>
                      <td className={`py-3.5 px-4 text-right font-mono font-bold ${
                        entry.entryType === 'CREDIT' ? 'text-emerald-400' : 'text-rose-400'
                      }`}>
                        {entry.entryType === 'CREDIT' ? '+' : '-'}${parseFloat(entry.amount).toFixed(2)}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold text-white">
                        ${parseFloat(entry.balanceAfter).toFixed(2)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="p-6 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <History className="h-5 w-5 text-emerald-400" />
              <div>
                <h3 className="text-base font-bold text-white">Recent Transactions</h3>
                <p className="text-xs text-slate-400">Latest fund transfers, deposits, and cash withdrawals</p>
              </div>
            </div>
            <button
              onClick={fetchHistory}
              className="text-xs font-medium text-emerald-400 hover:underline"
            >
              Refresh
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4">Reference</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Counterparty</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4 text-right">Amount</th>
                  <th className="py-3 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-slate-200">
                {loading ? (
                  <tr>
                    <td colSpan="7" className="py-8 text-center text-slate-400">Loading transactions...</td>
                  </tr>
                ) : transactions.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="py-8 text-center text-slate-400">No transactions performed yet.</td>
                  </tr>
                ) : (
                  transactions.map((txn) => {
                    const isOutgoing = txn.sourceAccountNumber === accountNumber;
                    return (
                      <tr key={txn.transactionReference} className="hover:bg-slate-800/40 transition">
                        <td className="py-3.5 px-4 font-mono text-slate-400">
                          {new Date(txn.createdAt).toLocaleDateString()}
                        </td>
                        <td className="py-3.5 px-4 font-mono text-slate-300 font-medium">
                          {txn.transactionReference}
                        </td>
                        <td className="py-3.5 px-4">
                          <span className="font-semibold text-slate-300">{txn.type}</span>
                        </td>
                        <td className="py-3.5 px-4 font-mono text-slate-400">
                          {txn.type === 'TRANSFER'
                            ? (isOutgoing ? `To: ${txn.targetAccountNumber}` : `From: ${txn.sourceAccountNumber}`)
                            : 'Self'}
                        </td>
                        <td className="py-3.5 px-4 text-slate-300">{txn.description}</td>
                        <td className={`py-3.5 px-4 text-right font-mono font-bold ${
                          isOutgoing ? 'text-rose-400' : 'text-emerald-400'
                        }`}>
                          {isOutgoing ? '-' : '+'}${parseFloat(txn.amount).toFixed(2)}
                        </td>
                        <td className="py-3.5 px-4 text-center">
                          <span className="bg-emerald-950 text-emerald-400 border border-emerald-800 text-[10px] px-2 py-0.5 rounded font-bold uppercase">
                            {txn.status}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Transfer Modal */}
      {showTransferModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-1 flex items-center space-x-2">
              <Send className="h-5 w-5 text-emerald-400" />
              <span>Send Money Transfer</span>
            </h3>
            <p className="text-xs text-slate-400 mb-4">Atomic account-to-account transfer with concurrency protection</p>

            <form onSubmit={handleTransfer} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Source Account (Locked)</label>
                <input
                  type="text"
                  disabled
                  value={accountNumber}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2 text-xs font-mono text-slate-400"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Target Account Number</label>
                <input
                  type="text"
                  required
                  value={targetAccount}
                  onChange={(e) => setTargetAccount(e.target.value)}
                  placeholder="e.g. LL..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-white font-mono focus:border-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Amount ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="100.00"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-emerald-400 focus:border-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Note</label>
                  <input
                    type="text"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Lunch / Rent"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-sm text-white focus:border-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              {/* Idempotency Key View */}
              <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl text-left">
                <div className="flex items-center justify-between text-[11px] mb-1">
                  <span className="font-semibold text-slate-400">Idempotency-Key (Replay Protection)</span>
                  <span className="text-[10px] text-emerald-400 font-mono">ACTIVE</span>
                </div>
                <div className="text-[10px] font-mono text-slate-300 truncate">{idempotencyKey}</div>
                <p className="text-[10px] text-slate-400 mt-1">Guarantees that double-clicks or retries execute strictly once.</p>
              </div>

              <div className="flex space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowTransferModal(false)}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold py-2.5 rounded-xl text-sm transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold py-2.5 rounded-xl text-sm transition disabled:opacity-50"
                >
                  {loading ? 'Processing...' : 'Confirm Transfer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Deposit Modal */}
      {showDepositModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-sm p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2 flex items-center space-x-2">
              <ArrowDownLeft className="h-5 w-5 text-emerald-400" />
              <span>Deposit Funds</span>
            </h3>

            <form onSubmit={handleDeposit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Amount ($)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="200.00"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-base font-bold text-emerald-400 focus:border-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowDepositModal(false)}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold py-2.5 rounded-xl text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold py-2.5 rounded-xl text-sm disabled:opacity-50"
                >
                  {loading ? 'Depositing...' : 'Deposit'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Withdraw Modal */}
      {showWithdrawModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-sm p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2 flex items-center space-x-2">
              <ArrowUpRight className="h-5 w-5 text-rose-400" />
              <span>Withdraw Funds</span>
            </h3>

            <form onSubmit={handleWithdraw} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Amount ($)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="50.00"
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-base font-bold text-rose-400 focus:border-rose-500 focus:outline-none"
                />
              </div>

              <div className="flex space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowWithdrawModal(false)}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold py-2.5 rounded-xl text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-rose-500 hover:bg-rose-400 text-slate-950 font-bold py-2.5 rounded-xl text-sm disabled:opacity-50"
                >
                  {loading ? 'Withdrawing...' : 'Withdraw'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default CustomerDashboard;
