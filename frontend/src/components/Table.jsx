/** @param {{caption: string, headers: string[], children: import('react').ReactNode}} props */
export default function Table({ caption, headers, children }) {
  return <div className="table-scroll" role="region" aria-label={caption} tabIndex={0}><table>
    <caption>{caption}</caption><thead><tr>{headers.map(header => <th scope="col" key={header}>{header}</th>)}</tr></thead>
    <tbody>{children}</tbody>
  </table></div>
}
