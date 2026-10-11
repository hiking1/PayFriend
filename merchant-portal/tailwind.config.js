/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#00897B', dark: '#00695C', light: '#B2DFDB' },
      },
    },
  },
  plugins: [],
}
