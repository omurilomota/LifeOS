import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { ProtectedRoute } from '@/features/auth/ProtectedRoute'
import { TodayPage } from '@/features/dashboard/TodayPage'
import { RecordsPage } from '@/features/records/RecordsPage'
import { RecordDetailPage } from '@/features/records/RecordDetailPage'
import { InboxPage } from '@/features/inbox/InboxPage'
import { DocumentsPage } from '@/features/documents/DocumentsPage'
import { ObligationsPage } from '@/features/obligations/ObligationsPage'
import { SearchPage } from '@/features/search/SearchPage'
import { HouseholdPage } from '@/features/household/HouseholdPage'
import { SettingsPage } from '@/features/settings/SettingsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<TodayPage />} />
          <Route path="/inbox" element={<InboxPage />} />
          <Route path="/records" element={<RecordsPage />} />
          <Route path="/records/:id" element={<RecordDetailPage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/obligations" element={<ObligationsPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/household" element={<HouseholdPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
