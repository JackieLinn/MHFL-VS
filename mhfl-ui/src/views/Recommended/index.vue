<script setup lang="ts">
import {ref} from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import BackToTop from '@/components/BackToTop.vue'
import RecommendedContent from './components/RecommendedContent.vue'

type DatasetType = 'cifar100' | 'tiny-imagenet'

const activeDataset = ref<DatasetType>('cifar100')

const setDataset = (ds: DatasetType) => {
  activeDataset.value = ds
}
</script>

<template>
  <div class="recommended-page p-8 pb-4 min-h-full flex flex-col">
    <!-- 整体参与滚动的区域 -->
    <div id="recommended-scroll" class="recommended-scroll flex-1 flex flex-col min-h-0 overflow-y-auto">
      <PageHeader
          class="mb-5 shrink-0"
          :title="$t('pages.recommended.title')"
          :desc="$t('pages.recommended.desc')"
      />

      <!-- 中间切换：CIFAR-100 / Tiny-ImageNet -->
      <div class="flex justify-center mb-6 shrink-0">
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

      <RecommendedContent :dataset="activeDataset"/>
    </div>

    <BackToTop scroll-target="#recommended-scroll"/>
  </div>
</template>

<style scoped>
.recommended-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  animation: recommendedFadeIn 0.5s ease 0.1s both;
}

.recommended-scroll {
  min-height: 0;
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
