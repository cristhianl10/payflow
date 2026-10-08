import { useState } from 'react';
import { Download, FileText } from 'lucide-react';
import { downloadFile } from '../../services/api';
import { Notice } from '../../components/ui';

function isoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

export function ReportsPage() {
  const today = new Date();
  const monthAgo = new Date();
  monthAgo.setDate(today.getDate() - 30);

  const [from, setFrom] = useState(isoDate(monthAgo));
  const [to, setTo] = useState(isoDate(today));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();

  async function downloadStatement() {
    setBusy(true);
    setError(undefined);
    try {
      const blob = await downloadFile(
        `/reports/statement.pdf?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      );
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `payflow-statement-${from}-to-${to}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Statements & reports.</h1>
          <p className="page-intro">
            Generate a PDF statement for any period up to one year.
          </p>
        </div>
      </div>

      <Notice error={error} />

      <section className="content-panel">
        <div className="section-heading">
          <div>
            <h2>Account statement</h2>
            <p>Includes opening and closing balances, totals and movements.</p>
          </div>
          <FileText size={22} aria-hidden="true" />
        </div>

        <div className="form-row">
          <div className="field">
            <label htmlFor="report-from">From</label>
            <input
              id="report-from"
              type="date"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="report-to">To</label>
            <input
              id="report-to"
              type="date"
              value={to}
              onChange={(event) => setTo(event.target.value)}
            />
          </div>
        </div>

        <p className="field-hint">
          Statements are generated from PayFlow's immutable ledger and include
          simulated funds only.
        </p>

        <button
          className="button button-primary"
          type="button"
          disabled={busy || !from || !to}
          onClick={() => void downloadStatement()}
        >
          <Download size={17} aria-hidden="true" />
          {busy ? 'Generating PDF…' : 'Download PDF statement'}
        </button>
      </section>
    </>
  );
}
