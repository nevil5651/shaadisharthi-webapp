import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  base: '/admin/',
  plugins: [react()],
  resolve: {
    alias: {
      '@assets': path.resolve(__dirname, 'src/assets'),
      '@components': path.resolve(__dirname, 'src/components'),
      '@pages': path.resolve(__dirname, 'src/pages'),
    },
  },
});
// This configuration file sets up Vite for a React project, enabling the use of JSX and TypeScript.
// It also defines path aliases for easier imports, allowing you to use `@assets`, `@components`, and `@pages` instead of relative paths.
// The `react` plugin is included to handle React-specific features, and the `path` module is used to resolve directory paths.
// This setup helps in organizing the project structure and improving code readability by avoiding deep relative paths.
// The `defineConfig` function is used to create a configuration object that Vite will use when building and serving the application.
// The `plugins` array includes the React plugin, which is essential for processing React components.