/** @param {{title: string, children: import('react').ReactNode}} props */
export default function Card({ title, children }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {children}
    </section>
  )
}
