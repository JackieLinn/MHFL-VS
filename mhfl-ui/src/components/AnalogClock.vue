<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount} from 'vue'

const props = withDefaults(
    defineProps<{
      timezone: string
      label: string
      size?: number
      variant?: 'tick' | 'roman'
    }>(),
    {size: 64, variant: 'tick'}
)

const cx = 32
const cy = 32

const ROMAN_NUMERALS = ['XII', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI']

const hourDeg = ref(0)
const minuteDeg = ref(0)
const secondDeg = ref(0)

let timer: ReturnType<typeof window.setInterval> | null = null

const getTimeInTimezone = (tz: string) => {
  const now = new Date()
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: tz,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
  const parts = formatter.formatToParts(now)
  const hour = parseInt(parts.find((p) => p.type === 'hour')?.value ?? '0', 10)
  const minute = parseInt(parts.find((p) => p.type === 'minute')?.value ?? '0', 10)
  const second = parseInt(parts.find((p) => p.type === 'second')?.value ?? '0', 10)
  return {hour, minute, second}
}

const updateHands = () => {
  const {hour, minute, second} = getTimeInTimezone(props.timezone)
  secondDeg.value = second * 6
  minuteDeg.value = minute * 6 + second * 0.1
  hourDeg.value = (hour % 12) * 30 + minute * 0.5
}

onMounted(() => {
  updateHands()
  timer = window.setInterval(updateHands, 100)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <div class="analog-clock" :class="{ 'analog-clock--roman': variant === 'roman' }">
    <svg
        :width="size"
        :height="size"
        viewBox="0 0 64 64"
        class="analog-clock-svg"
    >
      <!-- 外圈装饰环 -->
      <circle
          :cx="cx"
          :cy="cy"
          :r="cx - 1"
          fill="none"
          stroke="currentColor"
          stroke-width="0.5"
          opacity="0.15"
      />
      <!-- 主表盘圈 -->
      <circle
          :cx="cx"
          :cy="cy"
          :r="cx - 3"
          fill="none"
          stroke="currentColor"
          stroke-width="1.2"
          opacity="0.35"
      />
      <!-- 刻度或罗马数字 -->
      <g v-if="variant === 'tick'">
        <line
            v-for="i in 12"
            :key="i"
            :x1="cx"
            :y1="5"
            :x2="cx"
            :y2="9"
            stroke="currentColor"
            stroke-width="1"
            opacity="0.5"
            :transform="`rotate(${(i - 1) * 30} ${cx} ${cy})`"
        />
      </g>
      <g v-else class="analog-clock-roman">
        <g
            v-for="(num, i) in ROMAN_NUMERALS"
            :key="i"
            :transform="`rotate(${i * 30} ${cx} ${cy})`"
        >
          <text
              :x="cx"
              :y="9"
              text-anchor="middle"
              dominant-baseline="middle"
              class="analog-clock-roman-text"
              :transform="`rotate(${-i * 30} ${cx} 9)`"
          >
            {{ num }}
          </text>
        </g>
      </g>
      <!-- 时针（缩短避免遮挡数字） -->
      <line
          :x1="cx"
          :y1="cy"
          :x2="cx"
          :y2="cy - 10"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          :transform="`rotate(${hourDeg} ${cx} ${cy})`"
      />
      <!-- 分针 -->
      <line
          :x1="cx"
          :y1="cy"
          :x2="cx"
          :y2="cy - 16"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          :transform="`rotate(${minuteDeg} ${cx} ${cy})`"
      />
      <!-- 秒针 -->
      <line
          :x1="cx"
          :y1="cy"
          :x2="cx"
          :y2="cy - 21"
          stroke="#6366f1"
          stroke-width="1"
          stroke-linecap="round"
          :transform="`rotate(${secondDeg} ${cx} ${cy})`"
      />
      <!-- 中心轴 -->
      <circle :cx="cx" :cy="cy" r="2.5" fill="currentColor" opacity="0.7"/>
    </svg>
    <div class="analog-clock-label">{{ label }}</div>
  </div>
</template>

<style scoped>
.analog-clock {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.analog-clock-svg {
  color: var(--home-text-primary);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.06));
}

.analog-clock--roman .analog-clock-svg {
  filter: drop-shadow(0 2px 6px rgba(99, 102, 241, 0.08));
}

.analog-clock-roman-text {
  font-size: 5.5px;
  font-weight: 600;
  fill: currentColor;
  opacity: 0.8;
  font-family: 'Times New Roman', Georgia, 'DejaVu Serif', serif;
}

.analog-clock-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--home-text-muted);
  white-space: nowrap;
  letter-spacing: 0.3px;
}
</style>
