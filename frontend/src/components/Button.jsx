/** @param {import('react').ButtonHTMLAttributes<HTMLButtonElement> & {pending?: boolean}} props */
export default function Button({ pending = false, disabled, children, ...props }) {
  return <button className="button" {...props} disabled={disabled || pending} aria-busy={pending || undefined}>
    {pending && <span className="spinner" aria-hidden="true" />}{children}
  </button>
}
