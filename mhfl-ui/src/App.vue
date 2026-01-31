<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

// --- Element Plus 测试数据 ---
const sliderValue = ref(50)
const handleButtonClick = () => {
  ElMessage.success('Element Plus 弹窗成功！')
}

// --- ECharts 测试逻辑 ---
// TypeScript 写法：定义 ref 来绑定 DOM 元素
const chartRef = ref<HTMLElement | null>(null)

onMounted(() => {
  // 确保 DOM 已经渲染
  if (chartRef.value) {
    const myChart = echarts.init(chartRef.value)

    // 图表配置
    const option = {
      title: { text: 'ECharts 示例' },
      tooltip: {},
      xAxis: { data: ['A', 'B', 'C', 'D', 'E'] },
      yAxis: {},
      series: [
        {
          name: '数量',
          type: 'bar',
          data: [5, 20, 36, 10, 10],
          itemStyle: { color: '#409EFF' } // 使用 Element 的蓝色
        }
      ]
    }

    myChart.setOption(option)

    // 监听窗口缩放，自动调整图表大小
    window.addEventListener('resize', () => myChart.resize())
  }
})
</script>

<template>
  <div class="p-4"> <h1>You did it!</h1>
    <p class="mb-4">
      Visit <a href="https://vuejs.org/" target="_blank" rel="noopener">vuejs.org</a> to read the documentation
    </p>

    <div class="w-20 h-20 bg-blue-500 rounded-md hover:bg-red-500 transition-colors cursor-pointer mb-8 flex items-center justify-center text-white">
      UnoCSS
    </div>

    <div class="flex-container">

      <div class="card">
        <h3>Element Plus 测试</h3>
        <el-button type="primary" @click="handleButtonClick">点击测试弹窗</el-button>

        <div style="margin-top: 20px;">
          <span>滑块值：{{ sliderValue }}</span>
          <el-slider v-model="sliderValue" />
        </div>
      </div>

      <div class="card">
        <h3>ECharts 测试</h3>
        <div ref="chartRef" class="chart-box"></div>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* 简单的左右布局样式 */
.flex-container {
  display: flex;
  gap: 20px; /* 两个盒子之间的间距 */
}

.card {
  flex: 1; /* 让两个盒子平分宽度 */
  border: 1px solid #ddd;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chart-box {
  width: 100%;
  height: 300px; /* ECharts 容器必须显式指定高度 */
}

.mb-4 {
  margin-bottom: 1rem;
}
.mb-8 {
  margin-bottom: 2rem;
}
</style>