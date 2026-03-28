import {ref, watch} from 'vue'

export type RecommendedDatasetType = 'cifar100' | 'tiny-imagenet'

const RECOMMENDED_SIGMA_PREFIX = 'recommended_curve_sigma_'
const SIGMA_MIN = 0
const SIGMA_MAX = 5
const SIGMA_STEP = 0.5
const SIGMA_DEFAULT = 2.5

const normalizeSigma = (value: number): number => {
    if (!Number.isFinite(value)) return SIGMA_DEFAULT
    const clamped = Math.max(SIGMA_MIN, Math.min(SIGMA_MAX, value))
    return Math.round(clamped / SIGMA_STEP) * SIGMA_STEP
}

const parseSigma = (value: string | null): number => {
    if (value == null || value.trim() === '') return SIGMA_DEFAULT
    return normalizeSigma(Number(value))
}

export const useRecommendedCurveSigma = (dataset: RecommendedDatasetType) => {
    const key = `${RECOMMENDED_SIGMA_PREFIX}${dataset}`
    const sigma = ref<number>(parseSigma(localStorage.getItem(key)))

    watch(sigma, (value) => {
        localStorage.setItem(key, String(normalizeSigma(value)))
    })

    return {
        sigma,
        normalizeSigma
    }
}
