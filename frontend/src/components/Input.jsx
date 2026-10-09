import { useId } from 'react'

/** @param {import('react').InputHTMLAttributes<HTMLInputElement> & {label: string, error?: string}} props */
export default function Input({ label, error, ...props }) {
  const inputId = useId()
  return (
    <div className="field">
      <label htmlFor={inputId}>{label}</label>
      <input
        {...props}
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${inputId}-error` : undefined}
      />
      {error && (
        <span className="field-error" id={`${inputId}-error`}>{error}</span>
      )}
    </div>
  )
}
