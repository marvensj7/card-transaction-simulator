/**
 * @param {{ eyebrow: string, title: string, description: string }} props
 */
export default function PageHeading({ eyebrow, title, description }) {
  return (
    <div className="page-heading">
      <p className="eyebrow">{eyebrow}</p>
      <h1>{title}</h1>
      <p className="page-description">{description}</p>
    </div>
  )
}
