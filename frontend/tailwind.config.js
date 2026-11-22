/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: '#f5f7f5',
        ink: '#18211d',
        muted: '#5f6d66',
        line: '#dce5df',
        accent: '#2f6f5e',
        accentSoft: '#e7f1ee',
        danger: '#b8473d',
      },
      boxShadow: {
        panel: '0 1px 2px rgba(24,33,29,0.04), 0 8px 24px rgba(24,33,29,0.04)',
      },
    },
  },
  plugins: [],
}
