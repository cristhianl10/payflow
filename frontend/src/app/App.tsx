import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { WelcomePage } from '../pages/WelcomePage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1 },
    mutations: { retry: false },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<WelcomePage />} />
          <Route
            path="*"
            element={
              <main className="mx-auto max-w-3xl px-6 py-24">
                <h1 className="text-3xl font-semibold">Page not found</h1>
                <p className="mt-4 text-muted">This page is not available.</p>
                <Link
                  className="mt-8 inline-block font-medium underline underline-offset-4"
                  to="/"
                >
                  Back to PayFlow
                </Link>
              </main>
            }
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
