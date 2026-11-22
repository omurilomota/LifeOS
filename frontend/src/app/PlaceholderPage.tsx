type Props = { title: string; description: string }

export function PlaceholderPage({ title, description }: Props) {
  return (
    <div className="max-w-3xl rounded-2xl border border-line bg-white p-6 shadow-panel">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-2 text-sm text-muted">{description}</p>
      <div className="mt-6 rounded-xl border border-dashed border-line bg-canvas p-5 text-sm text-muted">
        Fundação pronta. Este módulo será implementado nas próximas fases verticais.
      </div>
    </div>
  )
}
