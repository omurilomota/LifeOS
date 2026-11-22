import { Buffer } from 'node:buffer'
import process from 'node:process'
import { expect, test } from '@playwright/test'

const apiBaseUrl = process.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
const runId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
const recordTitle = `Registro E2E ${runId}`
const obligationTitle = `Obrigação E2E ${runId}`
const fileName = `comprovante-e2e-${runId}.txt`

function yesterdayDateTime(): string {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

test('fluxo crítico: demo -> registro -> documento -> vínculo -> obrigação -> dashboard -> busca -> conclusão', async ({ page, request }) => {
  // Garante que a cadeia de segurança/CSRF do backend está pronta antes da primeira mutação.
  // Sem isso, um POST /auth/demo no início do boot pode falhar com 403 intermitente.
  await expect
    .poll(async () => {
      const res = await request.get(`${apiBaseUrl}/auth/me`).catch(() => undefined)
      return res?.status() ?? 0
    }, { message: 'backend /auth/me must respond before starting the flow' })
    .toBe(401)

  // 1. Entrar no modo demo (cria usuário + household isolados por execução)
  await page.goto('/login')
  await page.getByRole('button', { name: 'Entrar sem conta (demo)' }).click()
  await expect(page.getByRole('heading', { name: 'Bom ver você, Demo.' })).toBeVisible()

  // 2. Criar registro e capturar seu ID pelo detalhe (histórico também é validado)
  await page.getByRole('link', { name: 'Registros' }).click()
  await expect(page.getByRole('heading', { name: 'O que você administra' })).toBeVisible()
  await page.getByRole('button', { name: 'Novo registro' }).click()
  await page.getByLabel('Título').fill(recordTitle)
  await page.getByLabel('Tipo').selectOption('ASSET')
  await page.getByLabel('Categoria').fill('E2E')
  await page.getByLabel('Descrição').fill('Registro criado pelo smoke test E2E.')
  await page.getByRole('button', { name: 'Salvar', exact: true }).click()

  const recordRow = page.locator('li').filter({ has: page.getByRole('link', { name: recordTitle }) })
  await expect(recordRow).toBeVisible()
  await recordRow.getByRole('link', { name: 'Abrir' }).click()
  await expect(page).toHaveURL(/\/records\/[0-9a-f-]+$/i)
  const recordId = page.url().split('/').pop() ?? ''
  expect(recordId).toMatch(/^[0-9a-f-]{36}$/i)
  await expect(page.getByRole('heading', { name: 'Histórico' })).toBeVisible()

  // 3. Fazer upload de documento e vincular ao registro criado
  await page.getByRole('link', { name: 'Documentos' }).click()
  await expect(page.getByRole('heading', { name: 'Arquivos e comprovantes' })).toBeVisible()
  await page.locator('input[type="file"]').setInputFiles({
    name: fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from(`Comprovante de teste LifeOS E2E ${runId}\nRegistro: ${recordTitle}\n`, 'utf8'),
  })
  await page.getByRole('button', { name: 'Enviar', exact: true }).click()

  const docRow = page.locator('li').filter({ hasText: fileName })
  await expect(docRow).toBeVisible()
  await expect(docRow.getByText('READY')).toBeVisible()
  await docRow.getByRole('button', { name: 'Vincular' }).click()
  await docRow.getByPlaceholder('Record ID').fill(recordId)
  await docRow.getByRole('button', { name: 'Salvar vínculo' }).click()
  await expect(docRow.getByText(`ATTACHMENT → ${recordId}`)).toBeVisible()

  // 4. Criar obrigação vencida (aparece como atrasada no dashboard e gera notificação)
  await page.getByRole('link', { name: 'Obrigações' }).click()
  await expect(page.getByRole('heading', { name: 'O que precisa ser resolvido' })).toBeVisible()
  await page.getByLabel('Título').fill(obligationTitle)
  await page.getByLabel('Vencimento').fill(yesterdayDateTime())
  await page.getByLabel('Prioridade').selectOption('HIGH')
  await page.getByRole('button', { name: 'Criar obrigação' }).click()

  const obligationRow = page.locator('li').filter({ hasText: obligationTitle })
  await expect(obligationRow).toBeVisible()
  await expect(obligationRow.getByText('OPEN')).toBeVisible()

  // 5. Verificar dashboard: obrigação atrasada, registro recente e notificação gerada
  await page.getByRole('link', { name: 'Hoje' }).click()
  const overdueSection = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Atrasadas / Próximas' }) })
  const recordsSection = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Registros recentes' }) })
  const notificationsSection = page.locator('section').filter({ has: page.getByRole('heading', { name: 'Notificações', exact: true }) })
  await expect(overdueSection.getByText(obligationTitle)).toBeVisible()
  await expect(recordsSection.getByRole('link', { name: recordTitle })).toBeVisible()
  await expect(notificationsSection.getByText('Obrigação criada')).toBeVisible()
  await expect(notificationsSection.getByText(obligationTitle)).toBeVisible()

  // 6. Busca global encontra o registro criado
  await page.getByPlaceholder('Buscar...').fill(recordTitle)
  await page.getByPlaceholder('Buscar...').press('Enter')
  await expect(page).toHaveURL(/\/search\?q=/)
  await expect(page.getByRole('heading', { name: 'Encontre tudo' })).toBeVisible()
  await expect(page.getByRole('link', { name: recordTitle })).toBeVisible()

  // 7. Concluir a obrigação e confirmar a mudança de estado
  await page.getByRole('link', { name: 'Obrigações' }).click()
  const finalRow = page.locator('li').filter({ hasText: obligationTitle })
  await expect(finalRow).toBeVisible()
  await finalRow.getByRole('button', { name: 'Concluir' }).click()
  await expect(finalRow.getByText('COMPLETED')).toBeVisible()
})
