import { useEffect, useId, useRef } from 'react'
import Button from './Button.jsx'
/** @param {{open: boolean, title: string, children: import('react').ReactNode, pending: boolean, onConfirm: () => void, onClose: () => void}} props */
export default function ConfirmModal({ open, title, children, pending, onConfirm, onClose }) {
  const dialog = useRef(/** @type {HTMLDialogElement | null} */ (null))
  const cancel = useRef(/** @type {HTMLButtonElement | null} */ (null))
  const titleId = useId()
  useEffect(() => {
    if (open) { dialog.current?.showModal(); cancel.current?.focus() }
    else dialog.current?.close()
  }, [open])
  return <dialog ref={dialog} aria-labelledby={titleId} onCancel={event => { event.preventDefault(); if (!pending) onClose() }}>
    <h2 id={titleId}>{title}</h2>{children}
    <Button onClick={onConfirm} pending={pending}>Confirm</Button>
    <Button ref={cancel} onClick={onClose} disabled={pending}>Cancel</Button>
  </dialog>
}
