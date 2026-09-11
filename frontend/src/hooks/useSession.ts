import { useSyncExternalStore } from 'react';
import { sessionStore } from '../services/api';

export function useSession() {
  return useSyncExternalStore(sessionStore.subscribe, sessionStore.snapshot);
}
