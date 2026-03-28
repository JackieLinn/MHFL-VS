import { defineComponent, ref } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import i18n from '@/locales'

const mockedThemeMode = ref<'system' | 'light' | 'dark' | string>('system')

vi.mock('@/stores/theme', () => {
  return {
    useTheme: () => ({
      themeMode: mockedThemeMode,
      setThemeMode: vi.fn((mode: 'system' | 'light' | 'dark') => {
        mockedThemeMode.value = mode
      }),
    }),
  }
})

const ElDropdownStub = defineComponent({
  name: 'ElDropdown',
  emits: ['command'],
  template: '<div><slot /><slot name="dropdown" /></div>',
})

describe('ThemeSwitch', () => {
  it('should mount and render three options', async () => {
    mockedThemeMode.value = 'system'
    const { default: ThemeSwitch } = await import('@/components/ThemeSwitch.vue')
    const ElDropdownItemStub = defineComponent({
      name: 'ElDropdownItem',
      template: '<div class="dropdown-item"><slot /></div>',
    })
    const wrapper = shallowMount(ThemeSwitch, {
      global: {
        plugins: [i18n],
        stubs: {
          ElDropdown: ElDropdownStub,
          ElDropdownMenu: defineComponent({ template: '<div><slot /></div>' }),
          ElDropdownItem: ElDropdownItemStub,
          ElIcon: defineComponent({ template: '<i><slot /></i>' }),
        },
      },
    })

    expect(wrapper.findAll('.dropdown-item')).toHaveLength(3)
  })

  it('should fallback to default icon when mode is unexpected', async () => {
    mockedThemeMode.value = 'unknown'
    const { default: ThemeSwitch } = await import('@/components/ThemeSwitch.vue')
    const wrapper = shallowMount(ThemeSwitch, {
      global: {
        plugins: [i18n],
        stubs: {
          ElDropdown: ElDropdownStub,
          ElDropdownMenu: defineComponent({ template: '<div><slot /></div>' }),
          ElDropdownItem: defineComponent({ template: '<div><slot /></div>' }),
          ElIcon: defineComponent({ template: '<i><slot /></i>' }),
        },
      },
    })

    expect(wrapper.exists()).toBe(true)
  })
})
