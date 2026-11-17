import type { SystemStatus } from '@schoolos/api-client';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../api/ApiProvider';

export type StatusView =
  { kind: 'checking' } | { kind: 'running'; version: string } | { kind: 'unreachable' };

/** Spec 0001: checking → running | unreachable; retry goes back to checking. */
export function useSystemStatus(): { view: StatusView; retry: () => void } {
  const api = useApi();
  const query = useQuery<SystemStatus>({
    queryKey: ['system-status'],
    queryFn: ({ signal }) => api.getSystemStatus(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });

  const view: StatusView =
    query.isPending || query.isFetching
      ? { kind: 'checking' }
      : query.isError
        ? { kind: 'unreachable' }
        : { kind: 'running', version: query.data.version };

  return { view, retry: () => void query.refetch() };
}
