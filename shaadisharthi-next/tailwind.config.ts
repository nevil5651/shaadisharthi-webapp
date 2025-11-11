/** @type {import('tailwindcss').Config} */
const config = {
  content: [

    './src/app/(@public)/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/(@protected)/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    
  ],
  darkMode: "class",
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {},
  },
  plugins: [require("@tailwindcss/typography"), require("@tailwindcss/forms")],
};

export default config;
