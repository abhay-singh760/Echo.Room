/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#028090',
          light: '#00A896',
          accent: '#02C39A',
        },
        surface: {
          DEFAULT: '#0F2027',
          card: '#16262E',
          border: '#233640',
        },
      },
    },
  },
  plugins: [],
}
