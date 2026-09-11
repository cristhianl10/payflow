import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { useSession } from '../../hooks/useSession';
import type { Wallet } from '../../types/api';

export function useWallet() {
  const session = useSession();
  return useQuery({
    queryKey: ['wallet', session?.user.publicId],
    queryFn: () => api<Wallet>('/wallets/me'),
    enabled: !!session,
  });
}
