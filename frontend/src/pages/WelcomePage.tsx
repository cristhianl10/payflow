import {
  ArrowRight,
  ArrowUpRight,
  ShieldCheck,
  Wallet,
  List,
  ArrowRightLeft,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { Logo } from '../components/ui';
import { useSession } from '../hooks/useSession';

export function WelcomePage() {
  const session = useSession();
  return (
    <div className="welcome">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <header className="welcome-header">
        <Logo />
        <nav aria-label="Welcome navigation">
          <span className="sandbox-badge">Sandbox</span>
          <Link className="text-link" to={session ? '/app' : '/login'}>
            {session ? 'Your workspace' : 'Sign in'}
            <ArrowUpRight size={17} aria-hidden="true" />
          </Link>
        </nav>
      </header>
      <main id="main-content">
        <section className="welcome-hero">
          <div>
            <h1>
              Get a feel for
              <br />
              moving money.
            </h1>
            <p>
              A digital wallet built for curiosity. Send, receive, and follow
              every movement in a sandbox where every dollar is simulated.
            </p>
            <Link
              className="button button-primary"
              to={session ? '/app' : '/register'}
            >
              {session ? 'Open your workspace' : 'Create your account'}
              <ArrowRight size={19} aria-hidden="true" />
            </Link>
            <span className="welcome-promise">
              <ShieldCheck size={18} aria-hidden="true" />
              No bank account. No real money.
            </span>
          </div>
          <div className="welcome-route">
            <h2>A clearer way to explore.</h2>
            <div>
              <Wallet size={23} aria-hidden="true" />
              <section>
                <h3>Start with your wallet</h3>
                <p>Create an account and receive your opening sandbox funds.</p>
              </section>
            </div>
            <div>
              <ArrowRightLeft size={23} aria-hidden="true" />
              <section>
                <h3>Make a considered move</h3>
                <p>
                  Choose a recipient, review the details, and confirm your
                  transfer.
                </p>
              </section>
            </div>
            <div>
              <List size={23} aria-hidden="true" />
              <section>
                <h3>Keep the whole picture</h3>
                <p>
                  See your balance and a receipt for every completed movement.
                </p>
              </section>
            </div>
          </div>
        </section>
      </main>
      <footer className="welcome-footer">
        <span>PayFlow · Educational sandbox</span>
        <p>No real money is processed by PayFlow.</p>
      </footer>
    </div>
  );
}
