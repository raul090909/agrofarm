import { useCallback, useEffect, useState } from 'react'

/** Загрузка данных с состояниями «загрузка / ошибка / данные» и функцией повтора. */
export function useLoad<T>(loader: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  // eslint-disable-next-line react-hooks/exhaustive-deps
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
