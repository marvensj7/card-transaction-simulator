import { useEffect, useRef } from 'react'
import { useLocation } from 'react-router'

export default function RouteFocus() {
  const { pathname } = useLocation()
  const previousPath = useRef(pathname)

  useEffect(() => {
    const main = document.getElementById('main-content')
    const heading = main?.querySelector('h1')
    document.title = pathname === '/' ? 'Credit Circuit' : `${heading?.textContent} | Credit Circuit`

    // Keep the initial tab order; move to the content after changing pages.
    if (previousPath.current !== pathname) {
      main?.focus()
      window.scrollTo(0, 0)
      previousPath.current = pathname
    }
  }, [pathname])

  return null
}
