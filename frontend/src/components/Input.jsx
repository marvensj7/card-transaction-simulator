import { useId } from 'react'
/** @param {import('react').InputHTMLAttributes<HTMLInputElement> & {label: string, error?: string}} props */
export default function Input({ label, error, ...props }) {
  const id = useId()
  return <div className="field"><label htmlFor={id}>{label}</label>
    <input {...props} id={id} aria-invalid={error ? true : undefined} aria-describedby={error ? `${id}-error` : undefined} />
    {error && <span className="field-error" id={`${id}-error`}>{error}</span>}
  </div>
}
