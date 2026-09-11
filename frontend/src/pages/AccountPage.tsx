import { ShieldCheck } from 'lucide-react';
import { useSession } from '../hooks/useSession';
import { Avatar } from '../components/ui';

export function AccountPage() {
  const session = useSession();
  if (!session) return null;
  const { user } = session;
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Your account.</h1>
          <p className="page-intro">The details behind your PayFlow wallet.</p>
        </div>
      </div>
      <section className="content-panel account-panel">
        <div className="account-identity">
          <Avatar name={`${user.firstName} ${user.lastName}`} />
          <div>
            <h2>
              {user.firstName} {user.lastName}
            </h2>
            <p>Personal sandbox account</p>
          </div>
        </div>
        <dl className="detail-list">
          <div>
            <dt>Email address</dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>Environment</dt>
            <dd>Sandbox · Simulated funds</dd>
          </div>
          <div className="reference-row">
            <dt>Account reference</dt>
            <dd>{user.publicId}</dd>
          </div>
        </dl>
        <div className="notice notice-info">
          <ShieldCheck size={21} aria-hidden="true" />
          <span>
            Use Sign out in the navigation to end this session. Profile editing
            and password recovery are not available in this version.
          </span>
        </div>
      </section>
    </>
  );
}
