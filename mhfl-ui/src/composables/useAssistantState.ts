import {ref, watch} from 'vue'

const STORAGE_KEY_CID = 'mhfl_assistant_active_cid'
const STORAGE_KEY_COLLAPSED = 'mhfl_assistant_sidebar_collapsed'

/**
 * 智能助手页面状态记忆：当前会话 ID、侧边栏收起状态。
 * 首次进入默认新建对话 + 侧边栏展开；离开再回来时恢复。
 */
export const useAssistantState = () => {
    const activeConvId = ref<number | null>(null)
    const sidebarCollapsed = ref(
        sessionStorage.getItem(STORAGE_KEY_COLLAPSED) === 'true'
    )

    watch(sidebarCollapsed, (v) =>
        sessionStorage.setItem(STORAGE_KEY_COLLAPSED, String(v))
    )

    const saveActiveCid = (id: number | null) => {
        if (id != null) sessionStorage.setItem(STORAGE_KEY_CID, String(id))
        else sessionStorage.removeItem(STORAGE_KEY_CID)
    }

    const getStoredActiveCid = (): number | null => {
        const stored = sessionStorage.getItem(STORAGE_KEY_CID)
        if (!stored) return null
        const n = parseInt(stored, 10)
        return Number.isNaN(n) ? null : n
    }

    return {
        activeConvId,
        sidebarCollapsed,
        saveActiveCid,
        getStoredActiveCid,
    }
}
