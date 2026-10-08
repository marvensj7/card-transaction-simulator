export default function CircuitMark() {
  return (
    <svg className="circuit-mark" viewBox="0 0 40 40" fill="none" aria-hidden="true" focusable="false">
      <path
        d="M29 8H15A7 7 0 0 0 8 15V25A7 7 0 0 0 15 32H29M29 16H18A2 2 0 0 0 16 18V22A2 2 0 0 0 18 24H29"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      />
      <circle cx="29" cy="8" r="2" fill="currentColor" />
      <circle cx="29" cy="24" r="2" fill="currentColor" />
    </svg>
  )
}
