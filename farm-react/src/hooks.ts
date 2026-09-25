import { useCallback, useEffect, useState } from 'react'

export function useLoad<T>(loader: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const run = useCallback(loader, deps)

  const reload = useCallback(() => {
    setLoading(true)
    setError('')
    run()
      .then(setData)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false))
  }, [run])

  useEffect(() => { reload() }, [reload])
  return { data, error, loading, reload, setData }
}
