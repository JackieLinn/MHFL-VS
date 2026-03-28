import { nextTick } from 'vue'
import { describe, expect, it } from 'vitest'

import { useRecommendedCurveSigma } from '@/composables/useRecommendedCurveSigma'

describe('useRecommendedCurveSigma', () => {
  it('should use default sigma when no stored value', () => {
    const { sigma } = useRecommendedCurveSigma('cifar100')
    expect(sigma.value).toBe(2.5)
  })

  it('should normalize invalid stored value to default', () => {
    localStorage.setItem('recommended_curve_sigma_cifar100', 'abc')
    const { sigma } = useRecommendedCurveSigma('cifar100')
    expect(sigma.value).toBe(2.5)
  })

  it('should clamp and snap value when persisted', async () => {
    const { sigma } = useRecommendedCurveSigma('tiny-imagenet')
    sigma.value = 4.74
    await nextTick()

    expect(localStorage.getItem('recommended_curve_sigma_tiny-imagenet')).toBe('4.5')
  })

  it('should isolate storage key by dataset', async () => {
    const cifar = useRecommendedCurveSigma('cifar100')
    const tiny = useRecommendedCurveSigma('tiny-imagenet')

    cifar.sigma.value = 1
    tiny.sigma.value = 3.5
    await nextTick()

    expect(localStorage.getItem('recommended_curve_sigma_cifar100')).toBe('1')
    expect(localStorage.getItem('recommended_curve_sigma_tiny-imagenet')).toBe('3.5')
  })
})

