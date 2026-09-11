import { Link } from 'react-router-dom';
import { ArrowUpRight, ShieldCheck } from 'lucide-react';
import { useWallet } from './useWallet';
import { useSession } from '../../hooks/useSession';
import { CopyButton, Loading, MoneyDisplay, Notice } from '../../components/ui';

export function WalletPage() {
  const wallet = useWallet();
  const session = useSession();
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Your wallet.</h1>
          <p className="page-intro">One place for your simulated USD funds.</p>
        </div>
      </div>
      <Notice error={wallet.error} />
      {wallet.isPending ? (
        <Loading />
      ) : wallet.data ? (
        <section className="content-panel wallet-details">
          <div className="wallet-details-heading">
            <div>
              <h2>Available balance</h2>
              <div className="wallet-large-amount">
                <MoneyDisplay amount={wallet.data.availableBalance} />
              </div>
            </div>
            <Link to="/app/send" className="button button-primary">
              Send money <ArrowUpRight size={18} aria-hidden="true" />
            </Link>
          </div>
          <dl className="detail-list">
            <div>
              <dt>Status</dt>
              <dd>
                {wallet.data.status === 'ACTIVE'
                  ? 'Active'
                  : wallet.data.status}
              </dd>
            </div>
            <div>
              <dt>Currency</dt>
              <dd>US dollar · USD</dd>
            </div>
            <div>
              <dt>Receive using</dt>
              <dd>
                {session?.user.email}
                <CopyButton
                  value={session?.user.email ?? ''}
                  label="Copy email"
                />
              </dd>
            </div>
            <div className="reference-row">
              <dt>Wallet reference</dt>
              <dd>
                {wallet.data.publicId}
                <CopyButton
                  value={wallet.data.publicId}
                  label="Copy reference"
                />
              </dd>
            </div>
          </dl>
          <div className="notice notice-info">
            <ShieldCheck size={21} aria-hidden="true" />
            <span>
              Send and receive within PayFlow using registered email addresses.
              Funds cannot be deposited from or withdrawn to a bank.
            </span>
          </div>
        </section>
      ) : (
        <button
          className="button button-secondary"
          onClick={() => void wallet.refetch()}
        >
          Try again
        </button>
      )}
    </>
  );
}
