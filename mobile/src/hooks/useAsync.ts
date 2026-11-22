import { useCallback, useEffect, useRef, useState } from 'react';

import { errorMessage } from '../services/apiError';

export interface AsyncState<T> {
  data: T | undefined;
  loading: boolean;
  /** True while reloading with data already on screen (pull-to-refresh). */
  refreshing: boolean;
  error: string | null;
  reload: () => Promise<void>;
}

/** Runs `load` on mount and whenever it changes; exposes loading/error/data. */
export function useAsync<T>(load: () => Promise<T>): AsyncState<T> {
  const [data, setData] = useState<T | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);
  const hasData = useRef(false);

  const run = useCallback(async () => {
    const id = ++requestId.current;
    if (hasData.current) setRefreshing(true);
    else setLoading(true);
    try {
      const result = await load();
      if (id !== requestId.current) return; // a newer request superseded this one
      hasData.current = true;
      setData(result);
      setError(null);
    } catch (e) {
      if (id !== requestId.current) return;
      setError(errorMessage(e));
    } finally {
      if (id === requestId.current) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, [load]);

  useEffect(() => {
    hasData.current = false;
    void run();
    return () => {
      requestId.current++;
    };
  }, [run]);

  return { data, loading, refreshing, error, reload: run };
}
