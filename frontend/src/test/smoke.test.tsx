import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PlaceholderPage } from '@/app/PlaceholderPage'

describe('PlaceholderPage', () => {
  it('renders title and description', () => {
    render(<PlaceholderPage title="Inbox" description="Capture" />)
    expect(screen.getByText('Inbox')).toBeInTheDocument()
    expect(screen.getByText('Capture')).toBeInTheDocument()
  })
})
