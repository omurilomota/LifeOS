export type User = {
  id: string
  email: string
  displayName: string
  locale: string
  timezone: string
}

export type HouseholdSummary = {
  id: string
  name: string
  role: string
}

export type AuthResponse = {
  user: User
  currentHousehold: HouseholdSummary
}
