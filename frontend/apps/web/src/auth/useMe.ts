import type { CurrentUser } from '@schoolos/api-client';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../api/ApiProvider';

/** The signed-in user's own account, shared by the home screen and the role gate (one request, one cache entry). */
export function useMe() {
  const api = useApi();
  return useQuery<CurrentUser>({
    queryKey: ['me'],
    queryFn: ({ signal }) => api.getMe(signal),
    retry: false,
    refetchOnWindowFocus: false,
  });
}
