<script setup lang="ts">
import {computed} from 'vue'
import {useI18n} from 'vue-i18n'

interface Conversation {
  id: number
  title: string
  preview: string
  time: string
}

const {t} = useI18n()

const props = defineProps<{
  conversations: Conversation[]
  activeConvId: number | null
  collapsed: boolean
  isSending: boolean
  searchKeyword: string
}>()

const emit = defineEmits<{
  select: [id: number]
  edit: [id: number, title: string]
  delete: [id: number]
  newChat: []
  toggle: []
  'update:searchKeyword': [val: string]
}>()

const keyword = computed({
  get: () => props.searchKeyword,
  set: (val) => emit('update:searchKeyword', val)
})

const handleSelect = (id: number) => {
  if (props.isSending) return
  emit('select', id)
}

const handleEdit = (e: MouseEvent, id: number, title: string) => {
  e.stopPropagation()
  if (props.isSending) return
  emit('edit', id, title)
}

const handleDelete = (e: MouseEvent, id: number) => {
  e.stopPropagation()
  if (props.isSending) return
  emit('delete', id)
}
</script>

<template>
  <aside class="conv-sidebar flex flex-col shrink-0 overflow-hidden"
         :class="{ 'conv-sidebar--collapsed': collapsed }">

    <!-- 展开状态 -->
    <template v-if="!collapsed">
      <!-- 品牌头部 -->
      <div class="conv-sidebar-header flex items-center justify-between shrink-0 px-3 py-3.5">
        <div class="flex items-center gap-2">
          <span class="i-mdi-robot-outline text-xl text-indigo-500 shrink-0"></span>
          <span class="text-[15px] font-bold tracking-[0.02em] whitespace-nowrap"
                style="color: var(--home-text-primary)">AI 助手</span>
        </div>
        <div class="flex gap-1">
          <button class="icon-btn" @click="emit('newChat')" :title="t('assistant.newChat')">
            <span class="i-mdi-square-edit-outline"></span>
          </button>
          <button class="icon-btn" @click="emit('toggle')" :title="t('assistant.collapseSidebar')">
            <span class="i-mdi-chevron-double-left"></span>
          </button>
        </div>
      </div>

      <!-- 搜索框 -->
      <div class="conv-search relative shrink-0 px-2.5 py-2">
        <span
            class="conv-search-icon i-mdi-magnify absolute left-5 top-1/2 -translate-y-1/2 text-[14px] pointer-events-none flex"
            style="color: var(--home-text-muted); line-height: 1;"></span>
        <input
            v-model="keyword"
            class="conv-search-input w-full text-[13px] rounded-lg outline-none"
            :placeholder="t('assistant.searchPlaceholder')"
        />
      </div>

      <!-- 会话列表 -->
      <div class="flex-1 overflow-y-auto px-1.5 py-1">
        <div class="text-[11px] font-semibold uppercase tracking-[0.08em] px-2 pt-2 pb-1.5"
             style="color: var(--home-text-muted)">
          {{ t('assistant.recentChats') }}
        </div>
        <div
            v-for="conv in props.conversations"
            :key="conv.id"
            class="conv-item flex items-start gap-2.5 p-2.5 rounded-[9px] cursor-pointer mb-0.5 group"
            :class="{ 'conv-item--active': activeConvId === conv.id }"
            @click="handleSelect(conv.id)"
        >
          <div
              class="conv-item-icon flex shrink-0 items-center justify-center w-[30px] h-[30px] rounded-[7px] text-[15px] mt-px text-indigo-500">
            <span class="i-mdi-chat-outline"></span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-[13px] font-semibold truncate" style="color: var(--home-text-primary)">{{
                conv.title
              }}
            </div>
            <div class="text-[12px] truncate mt-0.5" style="color: var(--home-text-muted)">{{ conv.preview }}</div>
            <div class="text-[11px] mt-0.5 opacity-65" style="color: var(--home-text-muted)">{{ conv.time }}</div>
          </div>
          <div class="flex flex-col gap-0.5 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
                class="conv-item-edit icon-btn icon-btn--sm"
                :title="t('assistant.editConvTitle')"
                @click="handleEdit($event, conv.id, conv.title)"
            >
              <span class="i-mdi-pencil-outline"></span>
            </button>
            <button
                class="conv-item-delete icon-btn icon-btn--sm"
                :title="t('assistant.deleteConv')"
                @click="handleDelete($event, conv.id)"
            >
              <span class="i-mdi-delete-outline"></span>
            </button>
          </div>
        </div>

        <div v-if="props.conversations.length === 0" class="flex flex-col items-center gap-2 py-8 px-4 text-[13px]"
             style="color: var(--home-text-muted)">
          <span class="i-mdi-chat-sleep-outline text-[26px] opacity-45"></span>
          <span>{{ t('assistant.noConvFound') }}</span>
        </div>
      </div>

      <!-- 底部提示 -->
      <div class="conv-sidebar-footer flex items-center gap-1.5 shrink-0 px-3.5 py-2.5 text-[11px]"
           style="color: var(--home-text-muted); line-height: 1;">
        <span class="i-mdi-information-outline text-[13px] shrink-0 flex items-center" style="line-height: 1;"></span>
        <span class="flex items-center" style="line-height: 1;">{{ t('assistant.footerTip') }}</span>
      </div>
    </template>

    <!-- 收起状态 -->
    <template v-else>
      <div class="flex flex-col items-center gap-1 py-3">
        <button class="icon-btn icon-btn--collapsed" @click="emit('toggle')" :title="t('assistant.expandSidebar')">
          <span class="i-mdi-chevron-double-right"></span>
        </button>
        <button class="icon-btn icon-btn--collapsed" @click="emit('newChat')" :title="t('assistant.newChat')">
          <span class="i-mdi-square-edit-outline"></span>
        </button>
      </div>
    </template>

  </aside>
</template>

<style scoped>
.conv-sidebar {
  width: 272px;
  background: var(--home-card-bg);
  border-right: 1px solid var(--home-border);
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.conv-sidebar--collapsed {
  width: 52px;
}

.conv-sidebar-header {
  border-bottom: 1px solid var(--home-border);
}

.conv-sidebar-footer {
  border-top: 1px solid var(--home-border);
}

.conv-search-input {
  padding: 6px 10px 6px 32px;
  border: 1px solid var(--home-border);
  background: var(--home-bg);
  color: var(--home-text-primary);
  transition: border-color 0.2s;
}

.conv-search-input::placeholder {
  color: var(--home-text-muted);
}

.conv-search-input:focus {
  border-color: rgba(99, 102, 241, 0.5);
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--home-border);
  background: transparent;
  border-radius: 8px;
  color: var(--home-text-muted);
  font-size: 16px;
  cursor: pointer;
  transition: all 0.18s;
  flex-shrink: 0;
}

.icon-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
}

.icon-btn--collapsed {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  font-size: 17px;
}

.icon-btn--sm {
  width: 24px;
  height: 24px;
  font-size: 14px;
}

.conv-item-edit,
.conv-item-delete {
  transition: opacity 0.18s;
}

.conv-item-edit:hover {
  color: #6366f1;
  border-color: rgba(99, 102, 241, 0.4);
  background: rgba(99, 102, 241, 0.08);
}

.conv-item-delete:hover {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.4);
  background: rgba(239, 68, 68, 0.08);
}

.conv-item {
  border: 1px solid transparent;
  transition: background 0.16s;
}

.conv-item:hover {
  background: var(--home-hover-bg);
}

.conv-item--active {
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.18);
}

.conv-item-icon {
  background: rgba(99, 102, 241, 0.08);
}

.conv-item--active .conv-item-icon {
  background: rgba(99, 102, 241, 0.18);
}
</style>
