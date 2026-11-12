import React, { useState, useEffect } from 'react';
import api from '../api/client';
import {
  ShieldCheck, Calculator, FileSpreadsheet, History,
  CheckCircle2, AlertTriangle, RefreshCw, Eye
} from 'lucide-react';

const AuditorDashboard = ({ activeTab }) => {
  const [reconciliation, setReconciliation] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [ledgerEntries, setLedgerEntries] = useState([]);
  const [loading, setLoading] = useState(false);
  const [reconLoading, setReconLoading] = useState(false);

  useEffect(() => {
    fetchReconciliation();
    fetchAuditLogs();
    fetchLedger();
  }, []);

  const fetchReconciliation = async () => {
    try {
      setReconLoading(true);
      const res = await api.get('/audit/reconciliation');
      if (res.data.success) {
        setReconciliation(res.data.data);
      }
    } catch (err) {
      console.error('Failed to run reconciliation', err);
    } finally {
      setReconLoading(false);
    }
  };

  const fetchAuditLogs = async () => {
    try {
      setLoading(true);
      const res = await api.get('/audit/logs?size=30');
      if (res.data.success) {
        setAuditLogs(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load audit logs', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchLedger = async () => {
    try {
      const res = await api.get('/audit/ledger?size=30');
      if (res.data.success) {
        setLedgerEntries(res.data.data.content || []);
      }
    } catch (err) {
      console.error('Failed to load system ledger', err);
    }
  };

  const isReconciled = reconciliation?.reconciliationStatus === 'RECONCILED';

  return (
    <div className="space-y-8">
      
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight flex items-center space-x-2">
            <span>Financial Auditor &amp; Compliance Center</span>
            <span className="bg-amber-950 text-amber-400 border border-amber-800 text-xs px-2.5 py-0.5 rounded-full font-bold">
              ROLE_AUDITOR (READ-ONLY)
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">Mathematical balance verification and immutable audit trail inspection</p>
        </div>

        <button
          onClick={() => {
            fetchReconciliation();
            fetchAuditLogs();
            fetchLedger();
          }}
          className="self-start sm:self-auto px-3.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl flex items-center space-x-2 border border-slate-700 transition"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          <span>Refresh Audit</span>
        </button>
      </div>

      {/* Bank Reconciliation Card */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-850 to-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 border-b border-slate-800 gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded-xl">
              <Calculator className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Automated Bank Balance Reconciliation</h3>
              <p className="text-xs text-slate-400">Formula: (Lifetime Total Deposits − Lifetime Total Withdrawals) == Sum of Active Customer Balances</p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <div className={`px-4 py-2 rounded-xl border flex items-center space-x-2 ${
              isReconciled
                ? 'bg-emerald-950/60 border-emerald-800 text-emerald-300'
                : 'bg-rose-950/60 border-rose-800 text-rose-300'
            }`}>
              {isReconciled ? <CheckCircle2 className="h-5 w-5 text-emerald-400" /> : <AlertTriangle className="h-5 w-5 text-rose-400" />}
              <span className="font-extrabold text-xs tracking-wider">{reconciliation?.reconciliationStatus || 'CHECKING...'}</span>
            </div>

            <button
              onClick={fetchReconciliation}
              disabled={reconLoading}
              className="px-3 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold rounded-xl text-xs transition disabled:opacity-50"
            >
              {reconLoading ? 'Verifying...' : 'Re-verify Math'}
            </button>
          </div>
        </div>

        {/* Reconciliation Figures */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
          <div className="p-4 bg-slate-950/60 border border-slate-800 rounded-xl">
            <span className="text-[11px] text-slate-400 uppercase font-semibold">Total Deposits</span>
            <div className="text-xl font-bold font-mono text-emerald-400 mt-1">
              ${reconciliation ? parseFloat(reconciliation.totalDeposits).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>

          <div className="p-4 bg-slate-950/60 border border-slate-800 rounded-xl">
            <span className="text-[11px] text-slate-400 uppercase font-semibold">Total Withdrawals</span>
            <div className="text-xl font-bold font-mono text-rose-400 mt-1">
              ${reconciliation ? parseFloat(reconciliation.totalWithdrawals).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>

          <div className="p-4 bg-slate-950/60 border border-slate-800 rounded-xl">
            <span className="text-[11px] text-slate-400 uppercase font-semibold">Expected System Reserve</span>
            <div className="text-xl font-bold font-mono text-white mt-1">
              ${reconciliation ? parseFloat(reconciliation.expectedSystemReserve).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>

          <div className="p-4 bg-slate-950/60 border border-slate-800 rounded-xl">
            <span className="text-[11px] text-slate-400 uppercase font-semibold">Actual Balances Sum</span>
            <div className="text-xl font-bold font-mono text-white mt-1">
              ${reconciliation ? parseFloat(reconciliation.actualAccountsBalanceSum).toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>
        </div>

        <div className="mt-4 text-right">
          <span className="text-xs text-slate-400">
            Mathematical Discrepancy: <strong className={`font-mono ${reconciliation?.discrepancy === 0 ? 'text-emerald-400' : 'text-slate-200'}`}>${reconciliation ? parseFloat(reconciliation.discrepancy).toFixed(2) : '0.00'} USD</strong>
          </span>
        </div>
      </div>

      {/* Tabs: Double-Entry Ledger OR Audit Logs */}
      {activeTab === 'audit_ledger' ? (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="p-6 border-b border-slate-800 flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <FileSpreadsheet className="h-5 w-5 text-amber-400" />
                <span>System-Wide Double-Entry Ledger Lines</span>
              </h3>
              <p className="text-xs text-slate-400">Immutable credit and debit lines proving balance state transitions</p>
            </div>
            <span className="text-xs text-slate-400">{ledgerEntries.length} entries</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Timestamp</th>
                  <th className="py-3 px-4">Transaction Ref</th>
                  <th className="py-3 px-4">Account Number</th>
                  <th className="py-3 px-4 text-center">Entry Type</th>
                  <th className="py-3 px-4 text-right">Amount</th>
                  <th className="py-3 px-4 text-right">Balance After</th>
                  <th className="py-3 px-4">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-slate-200">
                {ledgerEntries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 font-mono text-slate-400">{new Date(entry.createdAt).toLocaleString()}</td>
                    <td className="py-3 px-4 font-mono font-medium text-slate-300">{entry.transactionReference}</td>
                    <td className="py-3 px-4 font-mono text-slate-300">{entry.accountNumber}</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`px-2 py-0.5 rounded font-bold uppercase text-[10px] ${
                        entry.entryType === 'CREDIT' ? 'bg-emerald-950 text-emerald-400 border border-emerald-800' : 'bg-rose-950 text-rose-400 border border-rose-800'
                      }`}>
                        {entry.entryType}
                      </span>
                    </td>
                    <td className={`py-3 px-4 text-right font-mono font-bold ${
                      entry.entryType === 'CREDIT' ? 'text-emerald-400' : 'text-rose-400'
                    }`}>
                      {entry.entryType === 'CREDIT' ? '+' : '-'}${parseFloat(entry.amount).toFixed(2)}
                    </td>
                    <td className="py-3 px-4 text-right font-mono font-bold text-white">
                      ${parseFloat(entry.balanceAfter).toFixed(2)}
                    </td>
                    <td className="py-3 px-4 text-slate-300">{entry.description}</td>
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
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <History className="h-5 w-5 text-amber-400" />
                <span>Immutable Security &amp; Operational Audit Trail</span>
              </h3>
              <p className="text-xs text-slate-400">Tamper-evident logs of administrative freezes, registrations, and large transfers</p>
            </div>
            <span className="text-xs text-slate-400">{auditLogs.length} logged events</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Date &amp; Time</th>
                  <th className="py-3 px-4">Actor</th>
                  <th className="py-3 px-4">Role</th>
                  <th className="py-3 px-4">Action</th>
                  <th className="py-3 px-4">Target Entity</th>
                  <th className="py-3 px-4">Details</th>
                  <th className="py-3 px-4">IP</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-slate-200">
                {auditLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 font-mono text-slate-400">{new Date(log.createdAt).toLocaleString()}</td>
                    <td className="py-3 px-4 font-medium text-white">{log.actorUsername}</td>
                    <td className="py-3 px-4 font-mono text-[11px] text-slate-400">{log.actorRole}</td>
                    <td className="py-3 px-4">
                      <span className="bg-slate-800 border border-slate-700 px-2 py-0.5 rounded text-[10px] font-bold text-amber-300">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-300">{log.entityName} {log.entityId ? `(${log.entityId})` : ''}</td>
                    <td className="py-3 px-4 text-slate-300 max-w-xs truncate">{log.details}</td>
                    <td className="py-3 px-4 font-mono text-slate-500">{log.ipAddress}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
};

export default AuditorDashboard;
