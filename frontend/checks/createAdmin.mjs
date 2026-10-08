import bcrypt from 'bcryptjs'
import { connectDatabase } from './verificationFixtures.mjs'

// Private local setup only. Public registration can never assign ADMIN.
const email = (process.env.CAPSTONE_ADMIN_EMAIL || '').trim().toLowerCase()
let password = process.env.CAPSTONE_ADMIN_PASSWORD || ''
if (!/^[^\s@]+@example\.test$/.test(email) || email.length > 150 || password.length < 12 || Buffer.byteLength(password, 'utf8') > 72) {
  throw new Error('Supply a fictional @example.test admin email and a private password of 12 characters through 72 UTF-8 bytes.')
}
const connection = await connectDatabase()
try {
  const [users] = await connection.execute('SELECT role FROM app_users WHERE email=?', [email])
  if (users.length && users[0].role !== 'ADMIN') throw new Error('This email belongs to a customer. Choose a different fictional admin email.')
  const hash = await bcrypt.hash(password, 12)
  password = ''; delete process.env.CAPSTONE_ADMIN_PASSWORD
  if (users.length) await connection.execute('UPDATE app_users SET password_hash=? WHERE email=? AND role=\'ADMIN\'', [hash, email])
  else await connection.execute('INSERT INTO app_users(display_name,email,password_hash,role) VALUES(?,?,?,\'ADMIN\')', ['Demo Administrator', email, hash])
  console.log('The local administrator is ready. Keep the sign-in details private.')
} finally {
  password = ''; delete process.env.CAPSTONE_ADMIN_PASSWORD
  await connection.end()
}
