import { ArrowDown, ArrowRightLeft, ListOrdered, Wallet } from 'lucide-react';

const features = [
  {
    number: '01',
    Icon: Wallet,
    title: 'A wallet to explore',
    description:
      'Start with simulated USD funds. Get familiar with your balance, without connecting a bank account.',
  },
  {
    number: '02',
    Icon: ArrowRightLeft,
    title: 'Send with clarity',
    description:
      'Choose a recipient, review the amount, and confirm. Every step keeps the details in view.',
  },
  {
    number: '03',
    Icon: ListOrdered,
    title: 'Every movement, recorded',
    description:
      'Follow your transfers with a clear history of what you sent and received.',
  },
];

export function WelcomePage() {
  return (
    <div className="min-h-screen bg-canvas text-ink">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <header className="border-b border-line">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-6 sm:px-10">
          <a
            href="/"
            aria-label="PayFlow home"
            className="flex min-h-11 items-center gap-3"
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand text-white">
              <ArrowRightLeft size={20} aria-hidden="true" />
            </span>
            <span className="text-xl font-semibold tracking-tight">
              PayFlow<span className="text-muted"> / </span>
            </span>
          </a>
          <span className="rounded-md border border-line bg-surface px-3 py-2 text-xs font-semibold uppercase tracking-widest">
            Sandbox
          </span>
        </div>
      </header>
      <main id="main-content">
        <section className="mx-auto max-w-6xl px-6 pb-16 pt-16 sm:px-10 sm:pb-24 sm:pt-24">
          <p className="mb-6 text-xs font-semibold uppercase tracking-widest text-muted">
            A space to learn. Room to explore.
          </p>
          <h1 className="max-w-3xl text-4xl font-semibold leading-tight tracking-tight sm:text-6xl">
            Get a feel for
            <br />
            moving money.
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed text-muted">
            A digital wallet built for curiosity. Explore payments and transfers
            in a sandbox where every dollar is simulated.
          </p>
          <a
            href="#experience"
            className="mt-9 inline-flex min-h-12 items-center gap-4 rounded-lg bg-brand px-5 py-3 font-medium text-white hover:opacity-90"
          >
            Explore the sandbox <ArrowDown size={18} aria-hidden="true" />
          </a>
          <p className="mt-5 text-sm text-muted">
            PayFlow is taking shape. Account registration is coming next.
          </p>
        </section>
        <section
          id="experience"
          aria-labelledby="experience-heading"
          className="border-t border-line bg-surface"
        >
          <div className="mx-auto max-w-6xl px-6 py-14 sm:px-10">
            <h2
              id="experience-heading"
              className="text-2xl font-semibold tracking-tight"
            >
              The experience we’re building
            </h2>
            <div className="mt-10 grid gap-10 md:grid-cols-3">
              {features.map(({ number, Icon, title, description }) => (
                <article key={number} className="border-t border-line pt-5">
                  <div className="mb-6 flex items-center justify-between text-muted">
                    <span className="text-xs font-medium">{number}</span>
                    <Icon size={21} aria-hidden="true" />
                  </div>
                  <h3 className="text-lg font-semibold">{title}</h3>
                  <p className="mt-3 max-w-prose text-sm leading-7 text-muted">
                    {description}
                  </p>
                </article>
              ))}
            </div>
          </div>
        </section>
      </main>
      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-6xl flex-col gap-3 px-6 py-7 text-sm text-muted sm:flex-row sm:justify-between sm:px-10">
          <span>PayFlow · Educational sandbox</span>
          <p>No real money is processed by PayFlow.</p>
        </div>
      </footer>
    </div>
  );
}
