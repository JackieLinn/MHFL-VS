import { nextTick } from 'vue'
import { describe, expect, it } from 'vitest'

import { clearAllPageSizePreferences, usePageSize } from '@/composables/usePageSize'

describe('usePageSize', () => {
  it('should fallback to default when storage invalid', () => {
    localStorage.setItem('page_size_task', '999')
    const { pageSize } = usePageSize('task')
    expect(pageSize.value).toBe(10)
  })

  it('should load valid stored value', () => {
    localStorage.setItem('page_size_task', '20')
    const { pageSize } = usePageSize('task')
    expect(pageSize.value).toBe(20)
  })

  it('should persist changed page size', async () => {
    const { pageSize } = usePageSize('dataset')
    pageSize.value = 50
    await nextTick()

    expect(localStorage.getItem('page_size_dataset')).toBe('50')
  })

  it('should clear all saved page-size preferences', () => {
    localStorage.setItem('page_size_task', '10')
    localStorage.setItem('page_size_dataset', '20')
    localStorage.setItem('other_key', 'keep')

    clearAllPageSizePreferences()

    expect(localStorage.getItem('page_size_task')).toBeNull()
    expect(localStorage.getItem('page_size_dataset')).toBeNull()
    expect(localStorage.getItem('other_key')).toBe('keep')
  })
})

