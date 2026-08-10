<template>
  <div class="home">
    <!-- ====== 主视觉区 ====== -->
    <section class="hero">
      <div class="hero__bg">
        <div class="hero__stars"></div>
        <div class="hero__nebula"></div>
      </div>
      <div class="hero__content">
        <p class="hero__eyebrow">国家级专精特新"小巨人"企业</p>
        <h1 class="hero__title">
          <span class="hero__title-line" v-motion-fade>赋能人类</span>
          <span class="hero__title-line hero__title-line--accent" v-motion-fade>太空活动</span>
        </h1>
        <p class="hero__subtitle">
          构建连接太空的基础设施，以电推进动力与全域态势感知技术，重新定义空间探索的边界。
        </p>
        <div class="hero__stats">
          <div class="hero__stat" v-motion-fade :style="{ transitionDelay: '0.2s' }">
            <span class="hero__stat-num">200+</span>
            <span class="hero__stat-label">产品交付数量</span>
          </div>
          <div class="hero__stat-divider"></div>
          <div class="hero__stat" v-motion-fade :style="{ transitionDelay: '0.4s' }">
            <span class="hero__stat-num">5kW+</span>
            <span class="hero__stat-label">最大功率</span>
          </div>
          <div class="hero__stat-divider"></div>
          <div class="hero__stat" v-motion-fade :style="{ transitionDelay: '0.6s' }">
            <span class="hero__stat-num">100%</span>
            <span class="hero__stat-label">自主研发</span>
          </div>
          <div class="hero__stat-divider"></div>
          <div class="hero__stat" v-motion-fade :style="{ transitionDelay: '0.8s' }">
            <span class="hero__stat-num">LEO</span>
            <span class="hero__stat-label">轨道范围</span>
          </div>
        </div>
        <router-link to="/jobs" class="hero__cta" v-motion-fade>
          探索岗位
          <span class="hero__cta-arrow">→</span>
        </router-link>
      </div>
      <div class="hero__scroll-hint">
        <span class="hero__scroll-line"></span>
      </div>
      <div class="hero__gradient-mask"></div>
    </section>

    <!-- ====== 公告栏 ====== -->
    <section class="announce" v-if="banners.length">
      <div class="container">
        <div class="announce__list">
          <div v-for="b in banners" :key="b.id" class="announce__item"
               :class="{ 'announce__item--highlight': b.status === '1' }">
            <span class="announce__tag">{{ b.status === '1' ? '最新' : '公告' }}</span>
            <span class="announce__text">{{ b.title }}</span>
            <span class="announce__date">{{ b.createTime?.slice(0, 10) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== 在招岗位 ====== -->
    <section class="jobs">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">
            <span class="section-title__cn">在招岗位</span>
            <span class="section-title__en">Open Positions</span>
          </h2>
          <router-link to="/jobs" class="section-more">
            查看全部 <span class="section-more__arrow">→</span>
          </router-link>
        </div>
        <div class="jobs__grid">
          <article v-for="job in featuredJobs" :key="job.jobId" class="job-card" v-motion-fade>
            <div class="job-card__header">
              <h3 class="job-card__title">
                <router-link :to="`/jobs/${job.jobId}`">{{ job.title }}</router-link>
              </h3>
              <div class="job-card__tags">
                <span v-if="job.tags?.includes('急聘')" class="job-card__tag job-card__tag--urgent">急聘</span>
                <span class="job-card__tag job-card__tag--deadline">{{ formatDeadline(job.deadline) }}</span>
              </div>
            </div>
            <div class="job-card__meta">
              <span class="job-card__meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                {{ job.deptName || '--' }}
              </span>
              <span class="job-card__meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/></svg>
                {{ job.location || '北京' }}
              </span>
              <span class="job-card__meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5"/></svg>
                {{ job.degreeRequirement || '本科及以上' }}
              </span>
            </div>
            <p class="job-card__desc">{{ truncateText(job.description, 120) }}</p>
          </article>
        </div>
        <div class="jobs__bottom-cta" v-if="featuredJobs.length >= 6">
          <router-link to="/jobs">查看全部岗位 →</router-link>
        </div>
      </div>
    </section>

    <!-- ====== 关于遨天 ====== -->
    <section class="about">
      <div class="container">
        <div class="about__grid">
          <div class="about__text">
            <h2 class="section-title">
              <span class="section-title__cn">关于遨天</span>
              <span class="section-title__en">About Us</span>
            </h2>
            <p class="about__desc">
              遨天科技（北京）有限公司，国家级专精特新"小巨人"企业，以推进系统为切入点，
              围绕在轨资产的安全与高效运营开展体系级服务的宇航公司。
              已建成国内首条商用电推进智能制造产线。
            </p>
            <p class="about__desc">
              公司掌握国际领先的电推进技术，专注于研发高品质、低成本、全自主的商业卫星用
              电推进系统解决方案，为卫星在轨全生命周期提供动力和轨道服务支持。
            </p>
            <div class="about__values">
              <span v-for="v in ['团结', '务实', '厚德', '尚道']" :key="v" class="about__value">{{ v }}</span>
            </div>
          </div>
          <div class="about__business">
            <h3 class="about__biz-title">业务板块</h3>
            <div class="about__biz-grid">
              <div class="about__biz-card" v-for="biz in businesses" :key="biz.name">
                <div class="about__biz-icon">{{ biz.icon }}</div>
                <h4>{{ biz.name }}</h4>
                <p>{{ biz.desc }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '@/utils/axios';

const banners = ref([]);
const featuredJobs = ref([]);

const businesses = [
  { icon: '🚀', name: '空间推进系统', desc: '霍尔电推进 / 电脉冲推进 / 电阻推进，从微牛级到牛级全功率谱系' },
  { icon: '🛰', name: '星座业务', desc: '甘德星座，低轨巨型星座组网计划' },
  { icon: '🔭', name: '数智业务', desc: '天境/天巡/天盾/天犀/数字孪生，太空资产全生命周期服务' },
];

function formatDeadline(deadline) {
  if (!deadline) return '';
  const d = new Date(deadline), now = new Date();
  const diff = Math.ceil((d - now) / (1000 * 60 * 60 * 24));
  if (diff < 0) return '已截止';
  if (diff === 0) return '今日截止';
  if (diff <= 7) return `${diff}天后截止`;
  return `${d.getMonth() + 1}/${d.getDate()} 截止`;
}

function truncateText(text, len) {
  if (!text) return '';
  const plain = text.replace(/<[^>]+>/g, '');
  return plain.length > len ? plain.slice(0, len) + '...' : plain;
}

onMounted(async () => {
  document.querySelectorAll('[v-motion-fade]').forEach(el => {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('motion-visible'); observer.unobserve(e.target); } });
    }, { threshold: 0.15 });
    observer.observe(el);
  });
  try { const res = await api.get('/brand/banners'); if (res.code === 200) banners.value = (res.data || []).slice(0, 3); } catch {}
  try { const res = await api.get('/jobs/list', { params: { pageNum: 1, pageSize: 6 } }); if (res.code === 200) featuredJobs.value = res.data?.rows || []; } catch {}
});
</script>

<style scoped>
.container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }

/* Hero */
.hero { position: relative; min-height: 100vh; display: flex; align-items: center; justify-content: center; overflow: hidden; padding: 120px 24px 80px; }
.hero__bg { position: absolute; inset: 0; }
.hero__stars { position: absolute; inset: 0; background-image: radial-gradient(1px 1px at 20% 30%, rgba(255,255,255,0.4), transparent), radial-gradient(1px 1px at 40% 70%, rgba(255,255,255,0.3), transparent), radial-gradient(1px 1px at 60% 20%, rgba(255,255,255,0.5), transparent), radial-gradient(1px 1px at 80% 50%, rgba(255,255,255,0.3), transparent), radial-gradient(1.5px 1.5px at 10% 80%, rgba(95,184,214,0.6), transparent), radial-gradient(1px 1px at 70% 90%, rgba(255,255,255,0.4), transparent), radial-gradient(1px 1px at 90% 10%, rgba(95,184,214,0.5), transparent); background-size: 200px 200px; }
.hero__nebula { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 600px; height: 600px; background: radial-gradient(ellipse, rgba(95,184,214,0.10) 0%, transparent 70%); pointer-events: none; }
.hero__gradient-mask { position: absolute; bottom: 0; left: 0; right: 0; height: 40%; background: linear-gradient(to bottom, transparent 0%, rgba(10,14,23,0.6) 60%, #0A0E17 100%); pointer-events: none; z-index: 0; }
.hero__content { position: relative; z-index: 1; text-align: center; max-width: 800px; }
.hero__eyebrow { font-size: 13px; color: #5FB8D6; letter-spacing: 4px; text-transform: uppercase; margin-bottom: 24px; }
.hero__title { font-size: clamp(40px, 8vw, 80px); font-weight: 700; line-height: 1.15; letter-spacing: 2px; margin-bottom: 24px; }
.hero__title-line { display: block; color: #fff; }
.hero__title-line--accent { color: #5FB8D6; }
.hero__subtitle { font-size: 16px; color: #9CA3AF; line-height: 1.8; max-width: 600px; margin: 0 auto 48px; }
.hero__stats { display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 56px; flex-wrap: wrap; }
.hero__stat { text-align: center; padding: 0 32px; }
.hero__stat-num { display: block; font-family: 'Share Tech Mono', 'Courier New', monospace; font-size: 36px; color: #6BB3FF; font-weight: 700; letter-spacing: 1px; }
.hero__stat-label { display: block; font-size: 12px; color: #6E7D8A; margin-top: 6px; letter-spacing: 1px; }
.hero__stat-divider { width: 1px; height: 40px; background: rgba(95,184,214,0.2); }
.hero__cta { display: inline-flex; align-items: center; gap: 12px; padding: 14px 40px; border-radius: 8px; background: linear-gradient(135deg, #5FB8D6, #6BB3FF); color: #0A0E17; font-size: 16px; font-weight: 700; text-decoration: none; letter-spacing: 2px; transition: transform 0.3s, box-shadow 0.3s; }
.hero__cta:hover { transform: translateY(-2px); box-shadow: 0 8px 32px rgba(95,184,214,0.3); }
.hero__cta:active { transform: scale(0.97); box-shadow: 0 2px 12px rgba(95,184,214,0.35); transition: transform 0.1s ease, box-shadow 0.1s ease; }
.hero__cta-arrow { transition: transform 0.3s; }
.hero__cta:hover .hero__cta-arrow { transform: translateX(4px); }
.hero__scroll-hint { position: absolute; bottom: 40px; left: 50%; transform: translateX(-50%); }
.hero__scroll-line { display: block; width: 1px; height: 48px; background: linear-gradient(to bottom, rgba(95,184,214,0.6), transparent); animation: scrollPulse 2s ease-in-out infinite; }
@keyframes scrollPulse { 0%, 100% { opacity: 0.4; transform: scaleY(1); } 50% { opacity: 1; transform: scaleY(1.2); } }

/* 公告 */
.announce { padding: 0 0 40px; }
.announce__list { display: flex; flex-direction: column; gap: 8px; }
.announce__item { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #152535; border-radius: 8px; border-left: 3px solid transparent; transition: border-color 0.3s; }
.announce__item--highlight { border-left-color: #5FB8D6; }
.announce__tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: rgba(95,184,214,0.15); color: #5FB8D6; flex-shrink: 0; }
.announce__text { flex: 1; font-size: 14px; color: #ccc; }
.announce__date { font-size: 12px; color: #6E7D8A; flex-shrink: 0; }

/* 区块标题 */
.section-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 40px; }
.section-title { display: flex; flex-direction: column; gap: 4px; }
.section-title__cn { font-size: 28px; font-weight: 700; color: #fff; letter-spacing: 2px; }
.section-title__en { font-size: 12px; color: #5FB8D6; letter-spacing: 3px; font-family: 'Share Tech Mono', 'Courier New', monospace; text-transform: uppercase; }
.section-more { color: #5FB8D6; font-size: 14px; text-decoration: none; display: flex; align-items: center; gap: 6px; transition: gap 0.3s; }
.section-more:hover { gap: 12px; }

/* 岗位 */
.jobs { padding: 80px 0; }
.jobs__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 20px; }
.job-card { background: #152535; border-radius: 12px; padding: 28px; border: 1px solid rgba(95,184,214,0.08); border-left: 3px solid transparent; transition: border-color 0.3s, border-left-color 0.3s, transform 0.3s, box-shadow 0.3s; }
.job-card:hover { border-color: rgba(95,184,214,0.3); border-left-color: #5FB8D6; transform: translateY(-2px); box-shadow: 0 12px 40px rgba(0,0,0,0.3); }
.job-card__header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.job-card__title { font-size: 18px; font-weight: 600; }
.job-card__title a { color: #fff; text-decoration: none; transition: color 0.2s; }
.job-card__title a:hover { color: #5FB8D6; }
.job-card__tags { display: flex; gap: 8px; flex-shrink: 0; }
.job-card__tag { font-size: 11px; padding: 3px 10px; border-radius: 4px; }
.job-card__tag--urgent { background: rgba(224,82,82,0.15); color: #E05252; }
.job-card__tag--deadline { background: rgba(95,184,214,0.1); color: #5FB8D6; }
.job-card__meta { display: flex; gap: 20px; margin-bottom: 14px; flex-wrap: wrap; }
.job-card__meta-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #9CA3AF; }
.job-card__meta-item svg { color: #6E7D8A; flex-shrink: 0; }
.job-card__desc { font-size: 13px; color: #6E7D8A; line-height: 1.7; }

/* 关于我们 */
.about { padding: 80px 0; border-top: 1px solid rgba(95,184,214,0.08); }
.about__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 80px; align-items: start; }
.about__desc { font-size: 15px; color: #9CA3AF; line-height: 1.9; margin-bottom: 16px; }
.about__values { display: flex; gap: 12px; margin-top: 24px; }
.about__value { padding: 8px 20px; border-radius: 6px; border: 1px solid rgba(95,184,214,0.2); color: #5FB8D6; font-size: 14px; letter-spacing: 2px; }
.about__biz-title { font-size: 18px; font-weight: 600; color: #fff; margin-bottom: 20px; }
.about__biz-grid { display: grid; grid-template-columns: 1fr; gap: 16px; }
.about__biz-card { background: #152535; padding: 20px; border-radius: 10px; border: 1px solid transparent; transition: border-color 0.3s; }
.about__biz-card:hover { border-color: rgba(95,184,214,0.2); }
.about__biz-icon { font-size: 24px; margin-bottom: 8px; }
.about__biz-card h4 { font-size: 15px; color: #fff; margin-bottom: 6px; }
.about__biz-card p { font-size: 13px; color: #6E7D8A; line-height: 1.6; }

/* 滚动动画 */
[v-motion-fade] { opacity: 0; transform: translateY(24px); transition: opacity 0.8s ease, transform 0.8s ease; }
[v-motion-fade].motion-visible { opacity: 1; transform: translateY(0); }

/* 响应式 */
@media (max-width: 767px) {
  .hero {
    min-height: auto;
    padding: 64px 16px 48px;
  }
  .hero__title { font-size: 28px; }
  .hero__stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-bottom: 40px;
  }
  .hero__stat {
    background: rgba(21, 37, 53, 0.85);
    border: 1px solid rgba(95, 184, 214, 0.12);
    border-radius: 10px;
    padding: 16px 8px;
    backdrop-filter: blur(4px);
  }
  .hero__stat-divider { display: none; }
  .hero__stat-num { font-size: 32px; }
  .hero__cta {
    width: 100%;
    justify-content: center;
    padding: 16px 0;
    font-size: 17px;
    border-radius: 10px;
    letter-spacing: 3px;
    box-shadow: 0 4px 24px rgba(95, 184, 214, 0.25);
  }
  .hero__stars { background-size: 300px 300px; }
  .hero__nebula { width: min(600px, 80vw); height: min(600px, 80vw); }
  .hero__scroll-hint { bottom: 24px; }
  .hero__scroll-line { height: 36px; }
  .hero__subtitle { padding: 0; }

  .section-header { margin-bottom: 24px; }

  .jobs { padding: 48px 0; }
  .jobs__grid { grid-template-columns: 1fr; gap: 14px; }
  .job-card { border-left-color: rgba(95, 184, 214, 0.25); padding: 24px 20px; border-radius: 10px; }
  .job-card:active { transform: scale(0.98); border-left-color: #5FB8D6; transition: transform 0.1s ease, border-left-color 0.15s ease; }

  .section-more { display: none; }
  .jobs__bottom-cta { display: flex; justify-content: center; margin-top: 24px; }
  .jobs__bottom-cta a { display: inline-flex; align-items: center; gap: 8px; padding: 14px 48px; border-radius: 10px; border: 1px solid rgba(95, 184, 214, 0.4); color: #5FB8D6; font-size: 15px; font-weight: 600; letter-spacing: 2px; text-decoration: none; transition: background 0.2s, border-color 0.2s; }
  .jobs__bottom-cta a:active { background: rgba(95, 184, 214, 0.1); border-color: #5FB8D6; }

  .about { padding: 48px 0 56px; border-top: none; position: relative; }
  .about::before { content: ''; position: absolute; top: 0; left: 50%; transform: translateX(-50%); width: 70%; height: 1px; background: linear-gradient(to right, transparent, rgba(95, 184, 214, 0.2), transparent); }
  .about__grid { grid-template-columns: 1fr; gap: 40px; }
  .about__desc { font-size: 14px; line-height: 1.8; margin-bottom: 12px; }
  .about__values { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .about__value { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 14px 10px; background: rgba(21, 37, 53, 0.6); border: 1px solid rgba(95, 184, 214, 0.15); border-radius: 10px; font-size: 15px; letter-spacing: 3px; }
  .about__value::before { content: ''; display: block; width: 6px; height: 6px; border-radius: 50%; background: #5FB8D6; opacity: 0.7; }
  .about__biz-grid { display: flex; gap: 12px; overflow-x: auto; scroll-snap-type: x mandatory; -webkit-overflow-scrolling: touch; scrollbar-width: none; padding-bottom: 4px; }
  .about__biz-grid::-webkit-scrollbar { display: none; }
  .about__biz-card { flex: 0 0 75vw; max-width: 300px; scroll-snap-align: start; padding: 20px 18px; }
  .about__biz-card:active { border-color: rgba(95, 184, 214, 0.3); background: rgba(21, 37, 53, 0.8); }

  [v-motion-fade] { transform: translateY(16px); transition: opacity 0.5s ease, transform 0.5s ease; }
}

/* 动效降级 */
@media (prefers-reduced-motion: reduce) {
  .hero__stars {
    background-image: radial-gradient(1px 1px at 20% 30%, rgba(255,255,255,0.2), transparent);
  }
}
</style>
