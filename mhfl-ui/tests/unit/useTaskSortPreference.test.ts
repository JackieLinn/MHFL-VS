import { nextTick } from 'vue'
import { describe, expect, it } from 'vitest'

import { clearAllTaskSortPreferences, useTaskSortPreference } from '@/composables/useTaskSortPreference'

describe('useTaskSortPreference', () => {
  it('should parse invalid values as DEFAULT', () => {
    localStorage.setItem('task_sort_list_create', 'BAD')
    localStorage.setItem('task_sort_list_update', 'BAD')
    const { createTimeSort, updateTimeSort } = useTaskSortPreference('list')

    expect(createTimeSort.value).toBe('DEFAULT')
    expect(updateTimeSort.value).toBe('DEFAULT')
  })

  it('should keep create and update mutually exclusive', async () => {
    const { createTimeSort, updateTimeSort } = useTaskSortPreference('list')

    createTimeSort.value = 'ASC'
    await nextTick()
    expect(updateTimeSort.value).toBe('DEFAULT')

    updateTimeSort.value = 'DESC'
    await nextTick()
    expect(createTimeSort.value).toBe('DEFAULT')
  })

  it('should guard old data where both are non-default', () => {
    localStorage.setItem('task_sort_list_create', 'ASC')
    localStorage.setItem('task_sort_list_update', 'DESC')

    const { createTimeSort, updateTimeSort } = useTaskSortPreference('list')
    expect(createTimeSort.value).toBe('ASC')
    expect(updateTimeSort.value).toBe('DEFAULT')
  })

  it('should clear all task sort preferences', () => {
    localStorage.setItem('task_sort_a_create', 'ASC')
    localStorage.setItem('task_sort_a_update', 'DESC')
    localStorage.setItem('other', 'keep')

    clearAllTaskSortPreferences()

    expect(localStorage.getItem('task_sort_a_create')).toBeNull()
    expect(localStorage.getItem('task_sort_a_update')).toBeNull()
    expect(localStorage.getItem('other')).toBe('keep')
  })
})

