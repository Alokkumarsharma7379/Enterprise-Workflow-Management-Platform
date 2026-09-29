import { useCallback, useEffect, useRef, useState } from 'react';
export function useResource(loader, dependencies = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const sequence = useRef(0);
  const reload = useCallback(async () => {
    const current = ++sequence.current;
    setLoading(true); setError(null);
    try { const result = await loader(); if (current === sequence.current) setData(result); }
    catch (failure) { if (current === sequence.current) setError(failure); }
    finally { if (current === sequence.current) setLoading(false); }
  }, dependencies);
  useEffect(() => { setData(null); reload(); return () => { sequence.current++; }; }, [reload]);
  return { data, error, loading, reload, setData };
}
