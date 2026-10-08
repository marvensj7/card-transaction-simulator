import Button from './Button.jsx'
/** @param {{page: number, totalPages: number, pending: boolean, onPage: (page: number) => void}} props */
export default function Pagination({ page, totalPages, pending, onPage }) {
  if (totalPages < 2) return null
  return <nav className="pagination" aria-label="List pages">
    <Button disabled={pending || page === 0} onClick={() => onPage(page - 1)}>Previous page</Button>
    <span>Page {page + 1} of {totalPages}</span>
    <Button disabled={pending || page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next page</Button>
  </nav>
}
