import { readFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
import mysql from 'mysql2/promise'
import bcrypt from 'bcryptjs'

// Test tooling only. Credentials and generated passwords stay in process memory.
export async function connectDatabase() {
  const text = await readFile(new URL('../../backend/src/main/resources/application-local.properties', import.meta.url), 'utf8').catch(() => '')
  const properties = Object.fromEntries(text.split(/\r?\n/).filter(line => line && !line.startsWith('#')).map(line => {
    const at = line.indexOf('='); return [line.slice(0, at).trim(), line.slice(at + 1).trim()]
  }))
  function value(key, fallback) {
    const setting = properties[key] || fallback
    const variable = setting.match(/^\$\{([^:}]+)(?::(.*))?\}$/)
    return variable ? process.env[variable[1]] || variable[2] || '' : setting
  }
  const url = new URL((process.env.DB_URL || value('spring.datasource.url', 'jdbc:mysql://localhost:3306/card_transaction_simulator')).replace(/^jdbc:/, ''))
  return mysql.createConnection({host: url.hostname, port: Number(url.port || 3306), database: url.pathname.slice(1), user: process.env.DB_USERNAME || value('spring.datasource.username', ''), password: process.env.DB_PASSWORD || value('spring.datasource.password', ''), timezone: 'Z'})
}

export async function createFixtures() {
  const connection = await connectDatabase()
  const run = randomUUID()
  const password = randomUUID()
  const hash = await bcrypt.hash(password, 12)
  const people = {}
  const emails = ['user', 'other', 'admin', 'signup', 'postman-user', 'postman-other'].map(name => `cc-check-${run}-${name}@example.test`)
  for (const [index, name] of ['user', 'other', 'admin'].entries()) {
    const [saved] = await connection.execute('INSERT INTO app_users(display_name,email,password_hash,role) VALUES(?,?,?,?)', [`Verification ${name}`, emails[index], hash, name === 'admin' ? 'ADMIN' : 'USER'])
    people[name] = {id: saved.insertId, email: emails[index]}
    if (name !== 'admin') {
      const [account] = await connection.execute("INSERT INTO credit_accounts(user_id,credit_limit,outstanding_balance,status) VALUES(?,1000,0,'ACTIVE')", [saved.insertId])
      const [card] = await connection.execute("INSERT INTO demo_cards(account_id,test_profile,label,last_four,expiry_month,expiry_year) VALUES(?,'DEMO_4242','Verification Card','4242',12,2035)", [account.insertId])
      people[name].accountId = account.insertId; people[name].cardId = card.insertId
    }
  }
  async function cleanup() {
    const [users] = await connection.query('SELECT id FROM app_users WHERE email IN (?)', [emails])
    const ids = users.map(user => user.id)
    if (ids.length) {
      const [accounts] = await connection.query('SELECT id FROM credit_accounts WHERE user_id IN (?)', [ids])
      const accountIds = accounts.map(account => account.id)
      if (accountIds.length) {
        await connection.query("DELETE FROM card_transactions WHERE account_id IN (?) AND type='REFUND'", [accountIds])
        await connection.query('DELETE FROM card_transactions WHERE account_id IN (?)', [accountIds])
        await connection.query('DELETE FROM demo_cards WHERE account_id IN (?)', [accountIds])
        await connection.query('DELETE FROM credit_accounts WHERE id IN (?)', [accountIds])
      }
      await connection.query('DELETE FROM app_users WHERE id IN (?)', [ids])
    }
    await connection.end()
  }
  return {connection, password, ...people, signupEmail: emails[3], postmanEmail: emails[4], postmanOtherEmail: emails[5], cleanup}
}
