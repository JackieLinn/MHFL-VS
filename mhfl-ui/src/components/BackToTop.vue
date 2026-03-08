<script setup lang="ts">
import {ref, onMounted, onUnmounted} from 'vue'
import {useI18n} from 'vue-i18n'

const {t} = useI18n()

/** 滚动目标：CSS 选择器，默认为 .content-area（Home 主内容区） */
const props = withDefaults(
    defineProps<{
      scrollTarget?: string
      visibilityThreshold?: number
    }>(),
    {
      scrollTarget: '.content-area',
      visibilityThreshold: 200
    }
)

const visible = ref(false)

const scrollToTop = () => {
  const el = document.querySelector(props.scrollTarget) as HTMLElement | null
  if (el) {
    el.scrollTo({top: 0, behavior: 'smooth'})
  } else {
    window.scrollTo({top: 0, behavior: 'smooth'})
  }
}

const checkVisibility = () => {
  const el = document.querySelector(props.scrollTarget) as HTMLElement | null
  if (el) {
    visible.value = el.scrollTop > props.visibilityThreshold
  } else {
    visible.value = window.scrollY > props.visibilityThreshold
  }
}

onMounted(() => {
  const el = document.querySelector(props.scrollTarget) as HTMLElement | null
  const target = el || window
  target.addEventListener('scroll', checkVisibility, {passive: true})
  checkVisibility()
})

onUnmounted(() => {
  const el = document.querySelector(props.scrollTarget) as HTMLElement | null
  const target = el || window
  target.removeEventListener('scroll', checkVisibility)
})
</script>

<template>
  <Transition name="back-to-top-fade">
    <button
        v-show="visible"
        type="button"
        class="back-to-top"
        :aria-label="t('common.backToTop')"
        @click="scrollToTop"
    >
      <span class="i-mdi-arrow-up text-[22px]"/>
    </button>
  </Transition>
</template>

<style scoped>
.back-to-top {
  position: fixed;
  right: 28px;
  bottom: 32px;
  z-index: 100;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  transition: all 0.3s ease;
}

.back-to-top:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
}

.back-to-top:active {
  transform: translateY(-1px) scale(1.02);
}

/* 深色模式 */
html.dark .back-to-top {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.9) 0%, rgba(139, 92, 246, 0.9) 100%);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
  border: 1px solid rgba(165, 180, 252, 0.2);
}

html.dark .back-to-top:hover {
  box-shadow: 0 6px 24px rgba(99, 102, 241, 0.5);
  border-color: rgba(165, 180, 252, 0.35);
}

.back-to-top-fade-enter-active,
.back-to-top-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.back-to-top-fade-enter-from,
.back-to-top-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
