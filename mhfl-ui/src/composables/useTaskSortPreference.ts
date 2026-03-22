import {ref, watch} from 'vue'

export type TimeSortOrder = 'DEFAULT' | 'ASC' | 'DESC'

const TASK_SORT_PREFIX = 'task_sort_'
const DEFAULT_SORT_ORDER: TimeSortOrder = 'DEFAULT'

const parseSortOrder = (value: string | null): TimeSortOrder => {
    if (value === 'ASC' || value === 'DESC' || value === 'DEFAULT') {
        return value
    }
    return DEFAULT_SORT_ORDER
}

/**
 * Task list sort preference with per-page storage.
 * createTimeSort and updateTimeSort are mutually exclusive.
 */
export const useTaskSortPreference = (key: string) => {
    const createKey = `${TASK_SORT_PREFIX}${key}_create`
    const updateKey = `${TASK_SORT_PREFIX}${key}_update`

    const createTimeSort = ref<TimeSortOrder>(parseSortOrder(localStorage.getItem(createKey)))
    const updateTimeSort = ref<TimeSortOrder>(parseSortOrder(localStorage.getItem(updateKey)))

    // Guard old data where both sorts were enabled at once.
    if (createTimeSort.value !== 'DEFAULT' && updateTimeSort.value !== 'DEFAULT') {
        updateTimeSort.value = 'DEFAULT'
    }

    watch(createTimeSort, (value) => {
        if (value !== 'DEFAULT' && updateTimeSort.value !== 'DEFAULT') {
            updateTimeSort.value = 'DEFAULT'
        }
        localStorage.setItem(createKey, value)
    })

    watch(updateTimeSort, (value) => {
        if (value !== 'DEFAULT' && createTimeSort.value !== 'DEFAULT') {
            createTimeSort.value = 'DEFAULT'
        }
        localStorage.setItem(updateKey, value)
    })

    return {
        createTimeSort,
        updateTimeSort
    }
}

/** Clear all task sort preferences (called on logout). */
export const clearAllTaskSortPreferences = (): void => {
    Object.keys(localStorage)
        .filter((k) => k.startsWith(TASK_SORT_PREFIX))
        .forEach((k) => localStorage.removeItem(k))
}
