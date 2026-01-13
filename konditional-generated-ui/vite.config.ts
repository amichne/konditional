import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  base: "/ui/",
  server: {
    host: "::",
    port: 8080,
    fs: {
      allow: [path.resolve(__dirname, "..")],
    },
  },
  plugins: [react(), mode === "development" ].filter(Boolean),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
}));
