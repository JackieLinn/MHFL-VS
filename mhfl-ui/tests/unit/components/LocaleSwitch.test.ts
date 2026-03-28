import { defineComponent, ref } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

vi.mock('@/stores/locale', () => {
  const currentLocale = ref<'zh-CN' | 'en-US'>('zh-CN')
  return {
    useLocale: () => ({
      currentLocale,
      setLocale: vi.fn((locale: 'zh-CN' | 'en-US') => {
        currentLocale.value = locale
      }),
    }),
  }
})

const ElDropdownStub = defineComponent({
  name: 'ElDropdown',
  emits: ['command'],
  template: '<div><slot /><slot name="dropdown" /></div>',
})

describe('LocaleSwitch', () => {
  it('should mount and render two locale options', async () => {
    const { default: LocaleSwitch } = await import('@/components/LocaleSwitch.vue')
    const ElDropdownItemStub = defineComponent({
      name: 'ElDropdownItem',
      template: '<div class="dropdown-item"><slot /></div>',
    })
    const wrapper = shallowMount(LocaleSwitch, {
      global: {
        stubs: {
          ElDropdown: ElDropdownStub,
          ElDropdownMenu: defineComponent({ template: '<div><slot /></div>' }),
          ElDropdownItem: ElDropdownItemStub,
        },
      },
    })

    expect(wrapper.findAll('.dropdown-item')).toHaveLength(2)
  })
})

