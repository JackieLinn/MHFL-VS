import {ref, watch} from 'vue'

const PAGE_SIZE_PREFIX = 'page_size_'
const PAGE_SIZE_OPTIONS = [5, 10, 20, 50] as const
const DEFAULT_PAGE_SIZE = 10

const parsePageSize = (v: string | null): number => {
    const n = Number(v)
    return (PAGE_SIZE_OPTIONS as readonly number[]).includes(n) ? n : DEFAULT_PAGE_SIZE
}

/**
 * 分页每页条数记忆：按 key 分别存储，各页面互不影响；登出时清除恢复默认 10。
 * @param key 页面标识，如 'task'、'account'、'dataset'、'algorithm'
 */
export const usePageSize = (key: string) => {
    const storageKey = PAGE_SIZE_PREFIX + key
    const pageSize = ref(parsePageSize(localStorage.getItem(storageKey)))
    watch(pageSize, (v) => localStorage.setItem(storageKey, String(v)))
    return {
        pageSize,
        pageSizeOptions: PAGE_SIZE_OPTIONS
    }
}

/** 登出时清除所有分页记忆 */
export const clearAllPageSizePreferences = (): void => {
    Object.keys(localStorage)
        .filter((k) => k.startsWith(PAGE_SIZE_PREFIX))
        .forEach((k) => localStorage.removeItem(k))
}
