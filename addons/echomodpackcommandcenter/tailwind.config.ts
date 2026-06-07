import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/client/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        deck: {
          950: "#080b0f",
          900: "#0d1218",
          850: "#121821",
          800: "#17202a",
          line: "#26323f"
        },
        signal: {
          cyan: "#58d7ff",
          green: "#7ee787",
          amber: "#f6c177",
          red: "#ff6b6b",
          violet: "#b9a7ff"
        }
      },
      boxShadow: {
        glow: "0 0 28px rgba(88, 215, 255, 0.14)"
      }
    }
  },
  plugins: []
} satisfies Config;
