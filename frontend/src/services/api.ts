import type { Session } from '../types/api';

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public traceId?: string,
  ) {
    super(message);
  }
}

let session: Session | null | undefined;
let csrf: { headerName: string; token: string } | undefined;
let csrfRequest: Promise<void> | undefined;
let refreshRequest: Promise<Session> | undefined;
const listeners = new Set<() => void>();

export const sessionStore = {
  snapshot: () => session,
  subscribe: (listener: () => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};

export function updateSessionUser(user: Session['user']) {
  if (session) setSession({ ...session, user });
}

function setSession(value: Session | null) {
  session = value;
  listeners.forEach((listener) => listener());
}

async function response<T>(path: string, init: RequestInit): Promise<T> {
  let result: Response;
  try {
    result = await fetch(`/api/v1${path}`, {
      ...init,
      credentials: 'include',
      signal: AbortSignal.timeout(25_000),
    });
  } catch {
    throw new ApiError(
      0,
      'CONNECTION_ERROR',
      'We could not reach PayFlow. Check your connection and try again.',
    );
  }
  if (!result.ok) {
    const body = await result.json().catch(() => ({}));
    throw new ApiError(
      result.status,
      body.code ?? 'REQUEST_FAILED',
      body.message ?? 'We could not complete this request. Please try again.',
      body.traceId,
    );
  }
  if (result.status === 204 || result.status === 205) return undefined as T;
  const payload = await result.text();
  if (!payload) return undefined as T;
  return JSON.parse(payload) as T;
}

async function ensureCsrf(force = false) {
  if (force) csrf = undefined;
  if (csrf) return;
  csrfRequest ??= response<{ headerName: string; token: string }>(
    '/auth/csrf',
    { method: 'GET' },
  )
    .then((value) => {
      csrf = value;
    })
    .finally(() => {
      csrfRequest = undefined;
    });
  await csrfRequest;
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  access?: string,
  csrfRetry = true,
): Promise<T> {
  const write = init.method && init.method !== 'GET';
  if (write) await ensureCsrf();
  const headers = new Headers(init.headers);
  if (init.body) headers.set('Content-Type', 'application/json');
  if (access) headers.set('Authorization', `Bearer ${access}`);
  if (write && csrf) headers.set(csrf.headerName, csrf.token);
  try {
    return await response<T>(path, { ...init, headers });
  } catch (error) {
    if (
      error instanceof ApiError &&
      error.status === 403 &&
      write &&
      csrfRetry
    ) {
      await ensureCsrf(true);
      return request<T>(path, init, access, false);
    }
    throw error;
  }
}

export function refreshSession(): Promise<Session> {
  if (refreshRequest) return refreshRequest;
  const refresh = () => request<Session>('/auth/refresh', { method: 'POST' });
  refreshRequest = (
    navigator.locks
      ? navigator.locks.request('payflow-refresh', refresh)
      : refresh()
  )
    .then(async (value) => {
      const resolved = await value;
      setSession(resolved);
      return resolved;
    })
    .catch((error: unknown) => {
      setSession(null);
      throw error;
    })
    .finally(() => {
      refreshRequest = undefined;
    });
  return refreshRequest;
}

export async function bootstrapSession() {
  if (session !== undefined) return;
  await refreshSession().catch(() => undefined);
}

export async function authenticate(
  kind: 'login' | 'register',
  data: Record<string, string>,
) {
  const value = await request<Session>(`/auth/${kind}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
  setSession(value);
  return value;
}

export async function verifyEmail(token: string) {
  await request('/auth/verify-email', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export async function resendEmailVerification() {
  return api<void>('/users/me/email-verification', { method: 'POST' });
}

export async function forgotPassword(email: string) {
  await request('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export async function resetPassword(token: string, newPassword: string) {
  await request('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}

export async function logout() {
  await request('/auth/logout', { method: 'POST' });
  setSession(null);
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  let current = session;
  if (!current || Date.parse(current.expiresAt) - Date.now() < 30_000)
    current = await refreshSession();
  try {
    return await request<T>(path, init, current.accessToken);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      const renewed = await refreshSession();
      return request<T>(path, init, renewed.accessToken);
    }
    throw error;
  }
}

async function downloadResponse(path: string, access: string): Promise<Blob> {
  const result = await fetch(`/api/v1${path}`, {
    credentials: 'include',
    headers: { Authorization: `Bearer ${access}` },
    signal: AbortSignal.timeout(25_000),
  });
  if (!result.ok) {
    const body = await result.json().catch(() => ({}));
    throw new ApiError(
      result.status,
      body.code ?? 'REQUEST_FAILED',
      body.message ?? 'We could not complete this request. Please try again.',
      body.traceId,
    );
  }
  return result.blob();
}

export async function downloadCsv(path: string): Promise<Blob> {
  let current = session;
  if (!current || Date.parse(current.expiresAt) - Date.now() < 30_000)
    current = await refreshSession();
  try {
    return await downloadResponse(path, current.accessToken);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      const renewed = await refreshSession();
      return downloadResponse(path, renewed.accessToken);
    }
    throw error;
  }
}
