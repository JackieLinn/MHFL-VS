<script setup lang="ts">
import {ref} from 'vue'
import ThemeSwitch from '@/components/ThemeSwitch.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import LoginForm from './components/LoginForm.vue'
import RegisterForm from './components/RegisterForm.vue'
import ResetPasswordForm from './components/ResetPasswordForm.vue'
import {Connection, DataAnalysis, Monitor, TrendCharts} from "@element-plus/icons-vue";

type PanelType = 'login' | 'register' | 'reset'

const currentPanel = ref<PanelType>('login')

const switchPanel = (panel: PanelType) => {
  currentPanel.value = panel
}

// 打开GitHub链接
const openGitHub = () => {
  window.open('https://github.com/JackieLinn/MHFL-VS', '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div class="login-page">
    <!-- 动态背景 -->
    <div class="bg-layer">
      <div class="gradient-bg"></div>
      <div class="grid-pattern"></div>

      <!-- 浮动装饰 -->
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>

      <!-- 光晕 -->
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>

      <!-- 银河带 -->
      <div class="milkyway"></div>

      <!-- 星空 -->
      <div class="starfield">
        <div class="stars stars-sm"></div>
        <div class="stars stars-md"></div>
        <div class="stars stars-lg"></div>
      </div>

      <!-- 流星 -->
      <div class="meteor meteor-1"></div>
      <div class="meteor meteor-2"></div>
      <div class="meteor meteor-3"></div>
    </div>

    <!-- GitHub链接、语言切换和主题切换 -->
    <div class="theme-switch-btn flex items-center gap-2">
      <button
          type="button"
          class="w-9 h-9 rounded-lg flex-center bg-white/60 dark:bg-gray-800/60 backdrop-blur border border-gray-200/50 dark:border-gray-700/50 hover:border-indigo-400 dark:hover:border-indigo-500 text-gray-600 dark:text-gray-300 hover:text-indigo-500 transition-all cursor-pointer"
          :title="$t('common.viewSource')"
          @click="openGitHub"
      >
        <i class="i-mdi-github text-lg"></i>
      </button>
      <LocaleSwitch/>
      <ThemeSwitch/>
    </div>

    <!-- 主内容 -->
    <div class="main-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-content">
          <!-- Logo -->
          <div class="logo-wrapper">
            <div class="logo-ring"></div>
            <div class="logo">
              <el-icon :size="44" color="white">
                <DataAnalysis/>
              </el-icon>
            </div>
          </div>

          <!-- 标题 -->
          <h1 class="brand-title">MHFL-VS</h1>
          <p class="brand-abbr">Model Heterogeneous Federated Learning</p>
          <p class="brand-abbr">End-to-End Visualization and Simulation</p>

          <div class="divider"></div>

          <h2 class="brand-subtitle">{{ $t('login.brandSubtitle') }}</h2>
          <p class="brand-desc">{{ $t('login.brandDesc') }}</p>

          <!-- 核心能力 -->
          <div class="capabilities">
            <div class="capability-item">
              <div class="capability-icon">
                <el-icon :size="20">
                  <Connection/>
                </el-icon>
              </div>
              <div class="capability-text">
                <span class="capability-title">{{ $t('login.capability1Title') }}</span>
                <span class="capability-desc">{{ $t('login.capability1Desc') }}</span>
              </div>
            </div>
            <div class="capability-item">
              <div class="capability-icon">
                <el-icon :size="20">
                  <Monitor/>
                </el-icon>
              </div>
              <div class="capability-text">
                <span class="capability-title">{{ $t('login.capability2Title') }}</span>
                <span class="capability-desc">{{ $t('login.capability2Desc') }}</span>
              </div>
            </div>
            <div class="capability-item">
              <div class="capability-icon">
                <el-icon :size="20">
                  <TrendCharts/>
                </el-icon>
              </div>
              <div class="capability-text">
                <span class="capability-title">{{ $t('login.capability3Title') }}</span>
                <span class="capability-desc">{{ $t('login.capability3Desc') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区域 -->
      <div class="form-section">
        <div class="form-card">
          <div class="card-decoration">
            <div class="deco-line deco-line-1"></div>
            <div class="deco-line deco-line-2"></div>
          </div>

          <Transition name="slide" mode="out-in">
            <LoginForm v-if="currentPanel === 'login'" key="login" @switch="switchPanel"/>
            <RegisterForm v-else-if="currentPanel === 'register'" key="register" @switch="switchPanel"/>
            <ResetPasswordForm v-else key="reset" @switch="switchPanel"/>
          </Transition>
        </div>

        <p class="copyright">{{ $t('common.copyright') }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ============ 基础布局 ============ */
.login-page {
  width: 100vw;
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--login-bg);
}

/* ============ 背景层 ============ */
.bg-layer {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.gradient-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
  var(--login-gradient-1) 0%,
  var(--login-gradient-2) 25%,
  var(--login-gradient-3) 50%,
  var(--login-gradient-2) 75%,
  var(--login-gradient-1) 100%
  );
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(var(--login-grid-color) 1px, transparent 1px),
  linear-gradient(90deg, var(--login-grid-color) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
}

.floating-shapes {
  position: absolute;
  inset: 0;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: var(--login-shape-opacity);
  filter: blur(1px);
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
  top: -100px;
  left: -100px;
  animation: float1 20s ease-in-out infinite;
}

.shape-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.25) 0%, transparent 70%);
  top: 20%;
  right: 10%;
  animation: float2 25s ease-in-out infinite;
}

.shape-3 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.2) 0%, transparent 70%);
  bottom: 20%;
  left: 15%;
  animation: float3 18s ease-in-out infinite;
}

.shape-4 {
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.2) 0%, transparent 70%);
  top: 60%;
  right: 25%;
  animation: float1 22s ease-in-out infinite reverse;
}

.shape-5 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%);
  bottom: -50px;
  right: -50px;
  animation: float2 30s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

@keyframes float2 {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-40px, 30px) scale(1.1);
  }
}

@keyframes float3 {
  0%, 100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(20px, -20px);
  }
  50% {
    transform: translate(40px, 0);
  }
  75% {
    transform: translate(20px, 20px);
  }
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: var(--login-glow-opacity);
}

.glow-1 {
  width: 500px;
  height: 500px;
  background: #6366f1;
  top: -200px;
  left: 20%;
  animation: glowPulse 8s ease-in-out infinite;
}

.glow-2 {
  width: 400px;
  height: 400px;
  background: #8b5cf6;
  bottom: -150px;
  right: 10%;
  animation: glowPulse 10s ease-in-out infinite reverse;
}

@keyframes glowPulse {
  0%, 100% {
    opacity: var(--login-glow-opacity);
    transform: scale(1);
  }
  50% {
    opacity: calc(var(--login-glow-opacity) * 1.3);
    transform: scale(1.1);
  }
}

/* ============ 银河带 ============ */
.milkyway {
  position: absolute;
  inset: -30% -20%;
  background:
    radial-gradient(ellipse 60% 40% at 30% 40%, var(--milkyway-via) 0%, transparent 60%),
    radial-gradient(ellipse 50% 35% at 70% 50%, var(--milkyway-to) 0%, transparent 55%),
    radial-gradient(ellipse 45% 30% at 50% 45%, var(--milkyway-via) 0%, transparent 50%),
    linear-gradient(
      155deg,
      transparent 10%,
      var(--milkyway-from) 22%,
      var(--milkyway-via) 35%,
      var(--milkyway-to) 45%,
      var(--milkyway-via) 55%,
      var(--milkyway-from) 68%,
      transparent 82%
    );
  filter: blur(30px);
  animation: milkywayDrift 60s ease-in-out infinite alternate;
}

@keyframes milkywayDrift {
  from {
    transform: rotate(-2deg) scale(1);
  }
  to {
    transform: rotate(2deg) scale(1.04);
  }
}

/* ============ 星空 ============ */
.starfield {
  position: absolute;
  inset: 0;
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 50%;
}

.stars-sm {
  width: 1px;
  height: 1px;
  color: var(--star-color-sm);
  background: currentColor;
  animation: twinkle1 4s ease-in-out infinite;
  box-shadow: 25px 18px, 102px 192px, 253px 97px, 410px 248px, 547px 73px,
  688px 312px, 841px 148px, 993px 43px, 1147px 276px, 1304px 118px,
  1451px 342px, 1598px 197px, 1743px 92px, 1891px 308px, 48px 402px,
  197px 497px, 348px 443px, 502px 548px, 643px 472px, 797px 517px,
  941px 397px, 1093px 502px, 1247px 417px, 1397px 543px, 1548px 477px,
  1693px 402px, 1842px 523px, 73px 647px, 223px 697px, 372px 673px,
  518px 752px, 667px 643px, 822px 718px, 973px 677px, 1118px 747px,
  1272px 643px, 1422px 697px, 1573px 677px, 1718px 747px, 1872px 647px,
  148px 847px, 447px 897px, 747px 867px, 1047px 917px, 1347px 847px,
  1647px 897px, 323px 43px, 723px 173px, 1123px 67px, 1523px 143px;
}

.stars-md {
  width: 1.5px;
  height: 1.5px;
  color: var(--star-color-md);
  background: currentColor;
  animation: twinkle2 5s ease-in-out infinite;
  box-shadow: 178px 117px, 477px 178px, 778px 77px, 1077px 218px, 1378px 157px,
  1677px 277px, 118px 378px, 418px 477px, 718px 418px, 1018px 517px,
  1318px 378px, 1618px 477px, 278px 618px, 578px 718px, 878px 677px,
  1178px 618px, 1478px 718px, 1778px 618px, 348px 878px, 1248px 878px,
  58px 157px, 858px 257px, 1558px 437px, 758px 837px, 1858px 157px;
}

.stars-lg {
  width: 2px;
  height: 2px;
  color: var(--star-color-lg);
  background: currentColor;
  animation: twinkle3 6s ease-in-out infinite;
  box-shadow: 298px 148px, 898px 118px, 1498px 198px, 598px 448px, 1198px 398px,
  1798px 348px, 148px 698px, 748px 748px, 1348px 698px, 1048px 848px;
}

@keyframes twinkle1 {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.85;
  }
}

@keyframes twinkle2 {
  0%, 100% {
    opacity: 0.35;
  }
  30% {
    opacity: 0.95;
  }
  70% {
    opacity: 0.2;
  }
}

@keyframes twinkle3 {
  0%, 100% {
    opacity: 0.45;
  }
  40% {
    opacity: 1;
  }
  75% {
    opacity: 0.25;
  }
}

/* ============ 流星 ============ */
.meteor {
  position: absolute;
  width: 120px;
  height: 1.5px;
  background: linear-gradient(to right, transparent 0%, var(--meteor-tail) 60%, var(--meteor-head) 100%);
  border-radius: 2px;
  opacity: 0;
}

.meteor::before {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  width: 5px;
  height: 5px;
  margin-top: -2.5px;
  border-radius: 50%;
  background: var(--meteor-head);
  box-shadow: 0 0 8px 3px var(--meteor-head);
}

.meteor-1 {
  top: 8%;
  right: 5%;
  animation: meteorShoot1 11s linear infinite;
  animation-delay: 1s;
}

.meteor-2 {
  top: 18%;
  right: 25%;
  animation: meteorShoot2 15s linear infinite;
  animation-delay: 5s;
}

.meteor-3 {
  top: 12%;
  right: 45%;
  animation: meteorShoot3 19s linear infinite;
  animation-delay: 9s;
}

@keyframes meteorShoot1 {
  0% {
    opacity: 0;
    transform: rotate(-32deg) translateX(0);
  }
  1% {
    opacity: 1;
  }
  7% {
    opacity: 0.5;
    transform: rotate(-32deg) translateX(-600px);
  }
  8% {
    opacity: 0;
    transform: rotate(-32deg) translateX(-650px);
  }
  100% {
    opacity: 0;
  }
}

@keyframes meteorShoot2 {
  0% {
    opacity: 0;
    transform: rotate(-38deg) translateX(0);
  }
  0.8% {
    opacity: 1;
  }
  5.5% {
    opacity: 0.5;
    transform: rotate(-38deg) translateX(-550px);
  }
  6.3% {
    opacity: 0;
    transform: rotate(-38deg) translateX(-600px);
  }
  100% {
    opacity: 0;
  }
}

@keyframes meteorShoot3 {
  0% {
    opacity: 0;
    transform: rotate(-28deg) translateX(0);
  }
  0.6% {
    opacity: 1;
  }
  4% {
    opacity: 0.5;
    transform: rotate(-28deg) translateX(-520px);
  }
  4.6% {
    opacity: 0;
    transform: rotate(-28deg) translateX(-560px);
  }
  100% {
    opacity: 0;
  }
}

/* ============ 主题切换按钮 ============ */
.theme-switch-btn {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 100;
  animation: fadeSlideUp 0.5s ease 0.1s both;
}

/* ============ 主容器 ============ */
.main-container {
  position: relative;
  z-index: 10;
  width: 100%;
  min-height: 100vh;
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px;
}

/* ============ 品牌区域 ============ */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.brand-content {
  max-width: 500px;
}

.brand-content > * {
  animation: fadeSlideUp 0.6s ease both;
}

.brand-content > :nth-child(1) {
  animation-delay: 0.1s;
}

.brand-content > :nth-child(2) {
  animation-delay: 0.2s;
}

.brand-content > :nth-child(3) {
  animation-delay: 0.25s;
}

.brand-content > :nth-child(4) {
  animation-delay: 0.3s;
}

.brand-content > :nth-child(5) {
  animation-delay: 0.35s;
}

.brand-content > :nth-child(6) {
  animation-delay: 0.4s;
}

.brand-content > :nth-child(7) {
  animation-delay: 0.45s;
}

.brand-content > :nth-child(8) {
  animation-delay: 0.55s;
}

.logo-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  margin-bottom: 32px;
}

.logo-ring {
  position: absolute;
  inset: -8px;
  border: 2px solid transparent;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 3s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.logo {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 40px rgba(99, 102, 241, 0.3);
  animation: logoFloat 6s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.brand-title {
  font-size: 56px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 12px;
  color: var(--login-text-primary);
}

.brand-abbr {
  font-size: 12px;
  color: var(--login-text-muted);
  letter-spacing: 1px;
  line-height: 1.6;
  white-space: nowrap;
}

.divider {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #6366f1, #a855f7);
  border-radius: 2px;
  margin: 24px 0;
}

.brand-subtitle {
  font-size: 26px;
  font-weight: 600;
  color: var(--login-text-primary);
  margin-bottom: 8px;
  white-space: nowrap;
}

.brand-desc {
  font-size: 16px;
  color: var(--login-text-secondary);
  margin-bottom: 32px;
}

/* 核心能力 */
.capabilities {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.capability-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--login-capability-bg);
  border: 1px solid var(--login-capability-border);
  border-radius: 12px;
  transition: all 0.3s;
}

.capability-item:hover {
  background: var(--login-capability-hover-bg);
  border-color: var(--login-capability-hover-border);
  transform: translateX(8px);
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.1);
}

.capability-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.2));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #818cf8;
  flex-shrink: 0;
}

.capability-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.capability-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--login-text-primary);
}

.capability-desc {
  font-size: 13px;
  color: var(--login-text-muted);
}

/* ============ 表单区域 ============ */
.form-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: var(--login-card-bg);
  backdrop-filter: blur(24px) saturate(1.8);
  border: 1px solid var(--login-card-border);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 25px 50px var(--login-card-shadow), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  animation: fadeSlideUp 0.7s ease 0.3s both;
  transition: box-shadow 0.4s ease;
}

.card-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: 24px;
}

.deco-line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.5), transparent);
  height: 1px;
}

.deco-line-1 {
  top: 0;
  left: 0;
  right: 0;
  animation: lineMove 3s ease-in-out infinite;
}

.deco-line-2 {
  bottom: 0;
  left: 0;
  right: 0;
  animation: lineMove 3s ease-in-out infinite reverse;
}

@keyframes lineMove {
  0%, 100% {
    transform: translateX(-100%);
    opacity: 0;
  }
  50% {
    transform: translateX(0);
    opacity: 1;
  }
}

.copyright {
  margin-top: 24px;
  font-size: 12px;
  color: var(--login-text-muted);
  animation: fadeSlideUp 0.5s ease 0.55s both;
  letter-spacing: 0.3px;
}

/* ============ 过渡动画 ============ */
.slide-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(24px) scale(0.98);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(-16px) scale(0.98);
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .main-container {
    flex-direction: column;
    padding: 24px;
    min-height: auto;
  }

  .brand-section {
    padding: 24px;
    flex: none;
  }

  .brand-content {
    text-align: center;
    max-width: 100%;
  }

  .logo-wrapper {
    margin: 0 auto 24px;
  }

  .brand-title {
    font-size: 42px;
  }

  .brand-subtitle {
    font-size: 20px;
    white-space: nowrap;
  }

  .brand-desc {
    font-size: 14px;
    margin-bottom: 24px;
  }

  .capabilities {
    max-width: 400px;
    margin: 0 auto;
  }

  .form-section {
    padding: 24px;
    flex: none;
  }

  .form-card {
    padding: 32px 24px;
  }
}

@media (max-width: 640px) {
  .main-container {
    padding: 16px;
  }

  .brand-section {
    padding: 16px;
  }

  .brand-title {
    font-size: 32px;
  }

  .brand-abbr {
    font-size: 11px;
  }

  .divider {
    margin: 16px auto;
  }

  .capabilities {
    display: none;
  }

  .form-section {
    padding: 16px;
  }

  .form-card {
    padding: 24px 20px;
  }
}
</style>
