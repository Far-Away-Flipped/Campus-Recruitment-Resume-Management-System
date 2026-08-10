<template>
  <div class="home">
    <!-- ====== Section 1: Hero 区 ====== -->
    <section class="hero">
      <div class="hero__bg">
        <div class="hero__stars"></div>
        <div class="hero__nebula"></div>
      </div>
      <div class="hero__content">
        <p class="hero__company">遨天科技（北京）有限公司</p>
        <p class="hero__tagline">团结、务实、厚德、尚道</p>
        <h1 class="hero__title">
          <span class="hero__title-line">赋能人类</span>
          <span class="hero__title-line">太空活动</span>
        </h1>
        <p class="hero__desc">构建连接太空的基础设施，以电推进动力与全域态势感知技术，重新定义空间探索的边界。</p>
        <p class="hero__positioning">国家级专精特新"小巨人"企业</p>
        <router-link to="/jobs" class="hero__cta">查看在招岗位 →</router-link>
      </div>
      <div class="hero__gradient-mask"></div>
    </section>

    <!-- ====== Section 2: 数据背书区 ====== -->
    <section class="stats">
      <div class="stats__grid">
        <div class="stats__item">
          <div class="stats__value">200+</div>
          <div class="stats__label">产品交付数量</div>
        </div>
        <div class="stats__item">
          <div class="stats__value">5kW+</div>
          <div class="stats__label">最大功率</div>
        </div>
        <div class="stats__item">
          <div class="stats__value">100%</div>
          <div class="stats__label">自主研发</div>
        </div>
        <div class="stats__item">
          <div class="stats__value">LEO</div>
          <div class="stats__label">轨道范围</div>
        </div>
      </div>
    </section>

    <!-- ====== Section 3: 在招岗位区 ====== -->
    <section class="jobs">
      <div class="jobs__header">
        <span class="jobs__title-cn">在招岗位</span>
        <span class="jobs__title-en">Open Positions</span>
      </div>

      <!-- 空状态 -->
      <div v-if="featuredJobs.length === 0" class="jobs__empty">
        <p class="jobs__empty-text">暂无在招岗位</p>
        <p class="jobs__empty-text">请持续关注遨天科技校招动态</p>
      </div>

      <!-- 岗位卡片列表（最多 3 张） -->
      <div v-else class="jobs__list">
        <router-link
          v-for="job in featuredJobs.slice(0, 3)"
          :key="job.jobId"
          :to="`/jobs/${job.jobId}`"
          class="jobs__card"
        >
          <h3 class="jobs__card-name">{{ job.title }}</h3>
          <div class="jobs__card-tags">
            <span v-if="job.deptName" class="jobs__card-tag">{{ job.deptName }}</span>
            <span v-if="job.location" class="jobs__card-tag">{{ job.location }}</span>
            <span v-if="job.degreeRequirement" class="jobs__card-tag">{{ job.degreeRequirement }}</span>
            <span
              v-if="job.deadline"
              class="jobs__card-tag"
              :class="{ 'jobs__card-tag--expired': isExpired(job.deadline) }"
            >{{ formatDeadline(job.deadline) }}</span>
          </div>
        </router-link>
      </div>

      <router-link v-if="featuredJobs.length > 0" to="/jobs" class="jobs__more">查看全部岗位 →</router-link>
    </section>

    <!-- ====== Section 4: 关于遨天区 ====== -->
    <section class="about">
      <div class="about__header">
        <span class="about__title-cn">关于遨天</span>
        <span class="about__title-en">About Us</span>
      </div>
      <p class="about__desc">遨天科技（北京）有限公司，国家级专精特新"小巨人"企业，以推进系统为切入点，围绕在轨资产的安全与高效运营开展体系级服务的宇航公司。已建成国内商用电推进智能制造产线。</p>
      <p class="about__desc">公司掌握国际领先的电推进技术，专注于研发高品质、低成本、全自主的商业卫星用电推进系统解决方案，为卫星在轨全生命周期提供动力和轨道服务支持。</p>
      <div class="about__spirit">
        <div class="about__spirit-item">
          <span class="about__spirit-word">团 结</span>
        </div>
        <div class="about__spirit-item">
          <span class="about__spirit-word">务 实</span>
        </div>
        <div class="about__spirit-item">
          <span class="about__spirit-word">厚 德</span>
        </div>
        <div class="about__spirit-item">
          <span class="about__spirit-word">尚 道</span>
        </div>
      </div>
    </section>

    <!-- ====== Section 5: 业务板块区 ====== -->
    <section class="business">
      <div class="business__header">
        <span class="business__title-cn">业务板块</span>
        <span class="business__title-en">Business Units</span>
      </div>
      <div class="business__scroll">
        <div
          v-for="biz in businesses"
          :key="biz.name"
          class="business__card"
        >
          <span class="business__card-icon">{{ biz.icon }}</span>
          <h4 class="business__card-title">{{ biz.name }}</h4>
          <p class="business__card-desc">{{ biz.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ====== Section 6: 页脚 ====== -->
    <PortalFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/axios'
import PortalFooter from '@/components/PortalFooter.vue'

const banners = ref([])
const featuredJobs = ref([])

const businesses = [
  { icon: '🚀', name: '空间推进系统', desc: '霍尔电推进 / 电脉冲推进 / 电阻推进，从微牛级到牛级全功率谱系' },
  { icon: '🛰', name: '星座业务', desc: '甘德星座，低轨巨型星座组网计划' },
  { icon: '🔭', name: '数智业务', desc: '天境 / 天巡 / 天盾 / 天犀 / 数字孪生，太空资产全生命周期服务' }
]

/**
 * 格式化截止日期为 YYYY-MM-DD
 */
function formatDeadline(deadline) {
  if (!deadline) return ''
  const d = new Date(deadline)
  if (isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 去除 HTML 标签并截断文本
 */
function truncateText(text, len) {
  if (!text) return ''
  const plain = text.replace(/<[^>]+>/g, '')
  return plain.length > len ? plain.slice(0, len) + '...' : plain
}

/**
 * 判断截止日期是否已过期
 */
function isExpired(deadline) {
  if (!deadline) return false
  const d = new Date(deadline)
  if (isNaN(d.getTime())) return false
  return d < new Date()
}

onMounted(async () => {
  try {
    const res = await api.get('/brand/banners')
    if (res.code === 200) banners.value = (res.data || []).slice(0, 3)
  } catch (_) { /* 公告非关键数据，静默失败 */ }

  try {
    const res = await api.get('/jobs/list', { params: { pageNum: 1, pageSize: 6 } })
    if (res.code === 200) featuredJobs.value = res.data?.rows || []
  } catch (_) { /* 岗位非关键数据，静默失败 */ }
})
</script>

<style scoped>
/* ===== 全局容器 ===== */
.home {
  max-width: 428px;
  margin: 0 auto;
  overflow-x: hidden;
}

/* ================================================================
   Section 1: Hero 区
   ================================================================ */
.hero {
  position: relative;
  min-height: 65vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: var(--header-h) 16px 0;
  overflow: hidden;
}

/* 背景层 */
.hero__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: var(--color-bg);
}

/* 星空 - 纯 CSS 星点 */
.hero__stars {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(1px 1px at 10% 8%, rgba(255,255,255,0.6), transparent),
    radial-gradient(1px 1px at 25% 15%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1px 1px at 40% 22%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 55% 8%, rgba(255,255,255,0.2), transparent),
    radial-gradient(1px 1px at 70% 18%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 85% 10%, rgba(95,184,214,0.5), transparent),
    radial-gradient(1px 1px at 15% 32%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1px 1px at 30% 42%, rgba(95,184,214,0.6), transparent),
    radial-gradient(1px 1px at 50% 38%, rgba(255,255,255,0.2), transparent),
    radial-gradient(1px 1px at 65% 32%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 80% 45%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1.5px 1.5px at 90% 28%, rgba(95,184,214,0.7), transparent),
    radial-gradient(1px 1px at 5% 52%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 20% 62%, rgba(255,255,255,0.2), transparent),
    radial-gradient(1px 1px at 38% 55%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 52% 68%, rgba(95,184,214,0.4), transparent),
    radial-gradient(1px 1px at 68% 58%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1px 1px at 82% 65%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 12% 78%, rgba(255,255,255,0.2), transparent),
    radial-gradient(1px 1px at 35% 82%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 58% 75%, rgba(95,184,214,0.3), transparent),
    radial-gradient(1px 1px at 72% 88%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 88% 80%, rgba(255,255,255,0.2), transparent),
    radial-gradient(1px 1px at 8% 92%, rgba(255,255,255,0.3), transparent),
    radial-gradient(1px 1px at 45% 90%, rgba(255,255,255,0.5), transparent);
  background-size: 300px 300px;
  opacity: 0.6;
}

/* 星云光晕（微妙） */
.hero__nebula {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 300px;
  height: 300px;
  background: radial-gradient(ellipse, rgba(95,184,214,0.04) 0%, transparent 70%);
  pointer-events: none;
}

/* 底部渐变遮罩 */
.hero__gradient-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(to bottom, transparent, var(--color-bg));
  z-index: 1;
  pointer-events: none;
}

/* 内容层 */
.hero__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

/* 企业全称 */
.hero__company {
  font-size: 12px;
  color: var(--color-text-weak);
  letter-spacing: 2px;
  margin-bottom: 8px;
}

/* 企业精神 tagline */
.hero__tagline {
  font-size: 13px;
  color: #fff;
  letter-spacing: 6px;
  font-weight: 500;
  margin-bottom: 12px;
}

/* 主标题容器 */
.hero__title {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16px;
}

/* 主标题单行 */
.hero__title-line {
  font-size: clamp(32px, 8.53vw, 36px);
  font-weight: 700;
  color: #fff;
  letter-spacing: 4px;
  line-height: 1.2;
}

.hero__title-line + .hero__title-line {
  margin-top: 4px;
}

/* 描述文字 */
.hero__desc {
  font-size: clamp(14px, 3.73vw, 15px);
  color: var(--color-text-secondary);
  text-align: center;
  line-height: 1.7;
  max-width: 343px;
  margin-bottom: 12px;
}

/* 公司定位 */
.hero__positioning {
  font-size: 12px;
  color: var(--color-primary);
  letter-spacing: 2px;
  font-weight: 500;
  margin-bottom: 28px;
}

/* CTA 按钮 */
.hero__cta {
  display: block;
  width: 100%;
  height: clamp(48px, 12.8vw, 52px);
  background: linear-gradient(135deg, var(--color-primary), var(--color-bright));
  color: var(--color-bg);
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: var(--radius);
  letter-spacing: 2px;
  text-align: center;
  line-height: clamp(48px, 12.8vw, 52px);
  text-decoration: none;
  transition: transform 0.1s var(--ease-out);
}

.hero__cta:active {
  transform: scale(0.97);
}

/* ===== Hero 入场动画 ===== */
.hero__company,
.hero__tagline,
.hero__title-line,
.hero__desc,
.hero__positioning,
.hero__cta {
  opacity: 0;
  animation: fadeInUp 0.6s var(--ease-out) forwards;
}

.hero__company       { animation-delay: 0s; }
.hero__tagline       { animation-delay: 0.1s; }
.hero__title-line:first-child  { animation-delay: 0.2s; }
.hero__title-line:last-child   { animation-delay: 0.25s; }
.hero__desc          { animation-delay: 0.3s; }
.hero__positioning   { animation-delay: 0.35s; }
.hero__cta           { animation-delay: 0.4s; }

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ================================================================
   Section 2: 数据背书区
   ================================================================ */
.stats {
  padding: 32px 16px;
  border-top: 1px solid var(--color-primary-15);
  margin-bottom: var(--section-gap);
}

.stats__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.stats__item {
  text-align: center;
  padding: 16px 8px;
}

/* 网格分隔线 */
.stats__item {
  border-right: 1px solid var(--color-primary-10);
  border-bottom: 1px solid var(--color-primary-10);
}

.stats__item:nth-child(2n) {
  border-right: none;
}

.stats__item:nth-child(n+3) {
  border-bottom: none;
}

.stats__value {
  font-family: var(--font-mono);
  font-size: clamp(28px, 7.47vw, 32px);
  font-weight: 600;
  color: #fff;
  line-height: 1.2;
}

.stats__label {
  font-size: 10px;
  color: var(--color-text-weak);
  margin-top: 6px;
  letter-spacing: 1px;
}

/* ================================================================
   Section 3: 在招岗位区
   ================================================================ */
.jobs {
  padding: 0 16px;
  margin-bottom: var(--section-gap);
}

.jobs__header {
  margin-bottom: 20px;
}

.jobs__title-cn {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  display: block;
}

.jobs__title-en {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-secondary);
  letter-spacing: 3px;
  text-transform: uppercase;
  margin-top: 4px;
  display: block;
}

/* 岗位卡片列表 */
.jobs__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.jobs__card {
  display: block;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 16px;
  cursor: pointer;
  transition: transform 0.1s ease, background-color 0.1s ease;
  text-decoration: none;
}

.jobs__card:active {
  transform: scale(0.98);
  background: #1A3045;
}

.jobs__card-name {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 10px;
}

.jobs__card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.jobs__card-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--color-primary-10);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.jobs__card-tag--expired {
  color: var(--color-danger);
  background: rgba(224, 82, 82, 0.1);
}

/* 查看全部链接 */
.jobs__more {
  display: block;
  text-align: right;
  margin-top: 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary);
  letter-spacing: 1px;
  text-decoration: none;
}

.jobs__more:active {
  opacity: 0.8;
}

/* 空状态 */
.jobs__empty {
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 32px 16px;
  text-align: center;
}

.jobs__empty-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.jobs__empty-text + .jobs__empty-text {
  margin-top: 4px;
}

/* ================================================================
   Section 4: 关于遨天区
   ================================================================ */
.about {
  padding: 0 16px;
  margin-bottom: var(--section-gap);
}

.about__header {
  margin-bottom: 20px;
}

.about__title-cn {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
}

.about__title-en {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-secondary);
  letter-spacing: 3px;
  text-transform: uppercase;
  margin-top: 4px;
}

/* 公司简介 */
.about__desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  text-align: justify;
  margin-bottom: 0;
}

.about__desc + .about__desc {
  margin-top: 12px;
}

.about__desc:last-of-type {
  margin-bottom: 20px;
}

/* 企业精神四词网格 */
.about__spirit {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--color-primary-10);
  border-radius: var(--radius);
  overflow: hidden;
}

.about__spirit-item {
  background: var(--color-card);
  text-align: center;
  padding: 20px 16px;
}

.about__spirit-word {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 8px;
}

/* ================================================================
   Section 5: 业务板块区
   ================================================================ */
.business {
  margin-bottom: var(--section-gap);
}

.business__header {
  padding: 0 16px;
  margin-bottom: 20px;
}

.business__title-cn {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
}

.business__title-en {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-text-secondary);
  letter-spacing: 3px;
  text-transform: uppercase;
  margin-top: 4px;
}

/* 横向滚动容器 */
.business__scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  padding: 0 16px;
}

.business__scroll::-webkit-scrollbar {
  display: none;
}

.business__scroll::after {
  content: '';
  flex: 0 0 1px;
}

/* 业务卡片 */
.business__card {
  flex: 0 0 75vw;
  max-width: 300px;
  background: var(--color-card);
  border-radius: var(--radius);
  padding: 20px;
  scroll-snap-align: start;
}

.business__card-icon {
  font-size: 28px;
  display: block;
  margin-bottom: 12px;
}

.business__card-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
}

.business__card-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 8px 0 0;
}

/* ================================================================
   动效降级
   ================================================================ */
@media (prefers-reduced-motion: reduce) {
  .hero__company,
  .hero__tagline,
  .hero__title-line,
  .hero__desc,
  .hero__positioning,
  .hero__cta {
    animation: none !important;
    opacity: 1 !important;
  }

  .hero__cta {
    transition: none !important;
  }
}
</style>
