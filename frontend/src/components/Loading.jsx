/** @param {{skeleton?: boolean}} props */
export default function Loading({ skeleton = false }) {
  return <div className="loading"><output><span className="spinner" aria-hidden="true" /> Loading…</output>
    {skeleton && <div className="skeleton" aria-hidden="true"><span /><span /><span /></div>}
  </div>
}
