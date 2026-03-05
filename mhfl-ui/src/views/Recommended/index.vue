<script setup lang="ts">
import {ref} from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import RecommendedContent from './components/RecommendedContent.vue'

type DatasetType = 'cifar100' | 'tiny-imagenet'

const activeDataset = ref<DatasetType>('cifar100')

const setDataset = (ds: DatasetType) => {
  activeDataset.value = ds
}
</script>

<template>
  <div class="recommended-page p-8 pb-4 h-full flex flex-col min-h-0">
    <PageHeader
        class="mb-5"
        :title="$t('pages.recommended.title')"
        :desc="$t('pages.recommended.desc')"
    />

    <!-- 中间切换：CIFAR-100 / Tiny-ImageNet -->
    <div class="flex justify-center mb-6">
      <div class="recommended-switch">
        <button
            type="button"
            class="recommended-switch-btn"
            :class="{ active: activeDataset === 'cifar100' }"
            @click="setDataset('cifar100')"
        >
          {{ $t('pages.recommended.datasetCifar100') }}
        </button>
        <button
            type="button"
            class="recommended-switch-btn"
            :class="{ active: activeDataset === 'tiny-imagenet' }"
            @click="setDataset('tiny-imagenet')"
        >
          {{ $t('pages.recommended.datasetTinyImageNet') }}
        </button>
      </div>
    </div>

    <!-- 内容区：根据切换显示不同占位 -->
    <div class="flex-1 min-h-0 flex flex-col">
      <RecommendedContent :dataset="activeDataset"/>
    </div>
  </div>
</template>

<style scoped>
.recommended-page {
  animation: recommendedFadeIn 0.5s ease 0.1s both;
}

@keyframes recommendedFadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

<style src="./recommended.css"></style>
