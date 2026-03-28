import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
    include: ['tests/**/*.test.ts'],
    reporters: ['default', 'junit'],
    outputFile: {
      junit: 'tests/reports/junit.xml',
    },
    coverage: {
      provider: 'v8',
      reportsDirectory: 'tests/reports/coverage',
      reporter: ['text', 'html', 'json-summary', 'lcov'],
      include: [
        'src/utils/trainError.ts',
        'src/composables/useAssistantState.ts',
        'src/composables/usePageSize.ts',
        'src/composables/useRecommendedCurveSigma.ts',
        'src/composables/useSystemHealth.ts',
        'src/composables/useTaskSortPreference.ts',
        'src/stores/theme.ts',
        'src/stores/locale.ts',
        'src/locales/index.ts',
        'src/components/ThemeSwitch.vue',
        'src/components/LocaleSwitch.vue',
      ],
      exclude: [
        'src/main.ts',
        'src/App.vue',
        'src/assets/**',
        'src/views/**',
        'src/router/**',
      ],
      thresholds: {
        lines: 90,
        functions: 90,
        statements: 90,
        branches: 80,
      },
    },
  },
})
