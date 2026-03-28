import { nextTick } from 'vue'
import { describe, expect, it } from 'vitest'

import { useAssistantState } from '@/composables/useAssistantState'

describe('useAssistantState', () => {
  it('should read sidebar collapsed from session storage', () => {
    sessionStorage.setItem('mhfl_assistant_sidebar_collapsed', 'true')
    const { sidebarCollapsed } = useAssistantState()
    expect(sidebarCollapsed.value).toBe(true)
  })

  it('should persist sidebar collapsed when changed', async () => {
    const { sidebarCollapsed } = useAssistantState()
    sidebarCollapsed.value = true
    await nextTick()
    expect(sessionStorage.getItem('mhfl_assistant_sidebar_collapsed')).toBe('true')
  })

  it('should save and load active conversation id', () => {
    const { saveActiveCid, getStoredActiveCid } = useAssistantState()
    saveActiveCid(123)
    expect(getStoredActiveCid()).toBe(123)

    saveActiveCid(null)
    expect(getStoredActiveCid()).toBeNull()
  })

  it('should return null when stored id is invalid', () => {
    sessionStorage.setItem('mhfl_assistant_active_cid', 'abc')
    const { getStoredActiveCid } = useAssistantState()
    expect(getStoredActiveCid()).toBeNull()
  })
})

