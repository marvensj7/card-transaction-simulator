/** @param {{skeleton?: boolean}} props */
export default function Loading({ skeleton = false }) {
  return <div role="status" className="loading"><span className="spinner" aria-hidden="true" /> Loading…
    {skeleton && <div className="skeleton" aria-hidden="true"><span /><span /><span /></div>}
  </div>
}
