<template>
  <div class="job-list-page">
    <!-- ====== 页面标题区 ====== -->
    <section class="page-hero">
      <div class="container">
        <div class="section-header">
          <h1 class="section-title">
            <span class="section-title__cn">在招岗位</span>
            <span class="section-title__en">Open Positions</span>
          </h1>
          <p class="section-desc">探索遨天科技的开放岗位，找到属于你的星辰大海</p>
        </div>
      </div>
    </section>

    <!-- ====== 筛选栏 ====== -->
    <section class="filter-bar">
      <div class="container">
        <!-- 桌面端筛选栏 -->
        <div class="filter-bar__inner" v-show="!isMobile">
          <!-- 关键词搜索 -->
          <div class="filter-bar__search">
            <svg class="filter-bar__search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
            </svg>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="搜索岗位名称、关键词..."
              class="filter-bar__search-input"
              @keyup.enter="handleSearch"
            />
          </div>

          <!-- 部门下拉（优化停用，保留代码便于恢复）
          <div class="filter-bar__select-wrapper" :class="{ 'is-open': openDropdown === 'dept' }">
            <button class="filter-bar__select-trigger" @click="toggleDropdown('dept')">
              <span>{{ selectedDeptLabel || '全部部门' }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <div class="filter-bar__select-dropdown" v-if="openDropdown === 'dept'">
              <div
                class="filter-bar__select-option"
                :class="{ 'is-active': !filters.deptId }"
                @click="selectDept('')"
              >全部部门</div>
              <div
                v-for="dept in flattenedDepts"
                :key="dept.deptId"
                class="filter-bar__select-option"
                :class="{ 'filter-bar__select-option--child': dept.level > 0, 'is-active': filters.deptId === dept.deptId }"
                @click="selectDept(dept.deptId)"
              ><span v-for="i in dept.level" :key="i">&emsp;</span>{{ dept.deptName }}</div>
            </div>
          </div>
          -->

          <!-- 岗位类别（树形选择） -->
          <div class="filter-bar__select-wrapper" :class="{ 'is-open': openDropdown === 'category' }">
            <button class="filter-bar__select-trigger" @click="toggleDropdown('category')">
              <span>{{ selectedCategoryLabel || '全部类别' }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <div class="filter-bar__select-dropdown filter-bar__select-dropdown--tree" v-if="openDropdown === 'category'">
              <div
                class="filter-bar__select-option"
                :class="{ 'is-active': !filters.categoryId }"
                @click="selectCategory('')"
              >全部类别</div>
              <template v-for="cat in filterOptions.categories" :key="cat.categoryId">
                <div
                  class="filter-bar__select-option filter-bar__select-option--parent"
                  :class="{ 'is-active': filters.categoryId === cat.categoryId }"
                  @click="selectCategory(cat.categoryId)"
                >{{ cat.categoryName }}</div>
                <div
                  v-for="child in cat.children"
                  :key="child.categoryId"
                  class="filter-bar__select-option filter-bar__select-option--child"
                  :class="{ 'is-active': filters.categoryId === child.categoryId }"
                  @click="selectCategory(child.categoryId)"
                >&emsp;{{ child.categoryName }}</div>
              </template>
            </div>
          </div>

          <!-- 工作地点下拉 -->
          <div class="filter-bar__select-wrapper" :class="{ 'is-open': openDropdown === 'location' }">
            <button class="filter-bar__select-trigger" @click="toggleDropdown('location')">
              <span>{{ selectedLocationLabel || '全部地点' }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <div class="filter-bar__select-dropdown" v-if="openDropdown === 'location'">
              <div
                class="filter-bar__select-option"
                :class="{ 'is-active': !filters.location }"
                @click="selectLocation('')"
              >全部地点</div>
              <div
                v-for="loc in filterOptions.locations"
                :key="loc.value"
                class="filter-bar__select-option"
                :class="{ 'is-active': filters.location === loc.value }"
                @click="selectLocation(loc.value)"
              >{{ loc.label }}</div>
            </div>
          </div>

          <!-- 学历要求下拉（优化停用，保留代码便于恢复）
          <div class="filter-bar__select-wrapper" :class="{ 'is-open': openDropdown === 'degree' }">
            <button class="filter-bar__select-trigger" @click="toggleDropdown('degree')">
              <span>{{ formatDegree(filters.degree) || '全部学历' }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
            </button>
            <div class="filter-bar__select-dropdown" v-if="openDropdown === 'degree'">
              <div
                class="filter-bar__select-option"
                :class="{ 'is-active': !filters.degree }"
                @click="selectDegree('')"
              >全部学历</div>
              <div
                v-for="deg in filterOptions.degrees"
                :key="deg.value"
                class="filter-bar__select-option"
                :class="{ 'is-active': filters.degree === deg.value }"
                @click="selectDegree(deg.value)"
              >{{ deg.label }}</div>
            </div>
          </div>
          -->

          <!-- 搜索按钮 -->
          <button class="filter-bar__btn" @click="handleSearch">搜索</button>

          <!-- 重置 -->
          <button class="filter-bar__btn-reset" @click="handleReset">重置</button>
        </div>

        <!-- 移动端筛选区 -->
        <div class="filter-bar__mobile" v-show="isMobile">
          <div class="filter-bar__search filter-bar__search--mobile">
            <svg class="filter-bar__search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
            </svg>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="搜索岗位..."
              class="filter-bar__search-input"
              @keyup.enter="handleSearch"
            />
          </div>
          <button class="filter-bar__filter-btn" @click="showFilterSheet = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="8" x2="20" y2="8"/><line x1="7" y1="13" x2="17" y2="13"/><line x1="10" y1="18" x2="14" y2="18"/></svg>
            筛选
            <span class="filter-bar__filter-badge" v-if="activeFilterCount > 0">{{ activeFilterCount }}</span>
          </button>
        </div>

        <!-- 已选筛选标签 -->
        <div class="filter-bar__active-tags" v-if="activeFilterCount > 0">
          <span class="filter-bar__active-label">已选条件：</span>
          <span v-if="filters.keyword" class="filter-bar__tag">
            关键词：{{ filters.keyword }}
            <button @click="filters.keyword = ''; handleSearch()">&times;</button>
          </span>
          <!-- 部门已选标签（优化停用）
          <span v-if="filters.deptId" class="filter-bar__tag">
            {{ selectedDeptLabel }}
            <button @click="filters.deptId = ''; handleSearch()">&times;</button>
          </span>
          -->
          <span v-if="filters.categoryId" class="filter-bar__tag">
            {{ selectedCategoryLabel }}
            <button @click="filters.categoryId = ''; handleSearch()">&times;</button>
          </span>
          <span v-if="filters.location" class="filter-bar__tag">
            {{ selectedLocationLabel }}
            <button @click="filters.location = ''; handleSearch()">&times;</button>
          </span>
          <!-- 学历已选标签（优化停用）
          <span v-if="filters.degree" class="filter-bar__tag">
            {{ formatDegree(filters.degree) }}
            <button @click="filters.degree = ''; handleSearch()">&times;</button>
          </span>
          -->
        </div>
      </div>
    </section>

    <!-- ====== Bottom Sheet（移动端筛选面板） ====== -->
    <Teleport to="body">
      <Transition name="sheet-slide">
        <div v-if="showFilterSheet" class="filter-sheet-overlay" @click="closeFilterSheet">
          <div class="filter-sheet" @click.stop>
            <div class="filter-sheet__header">
              <h3>筛选条件</h3>
              <button class="filter-sheet__close" @click="closeFilterSheet">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="filter-sheet__body">
              <!-- 部门（优化停用，保留代码便于恢复）
              <div class="filter-sheet__group">
                <label class="filter-sheet__label">部门</label>
                <select v-model="filters.deptId" class="filter-sheet__select">
                  <option value="">全部部门</option>
                  <option v-for="dept in flattenedDepts" :key="dept.deptId" :value="dept.deptId">
                    {{ '　'.repeat(dept.level) }}{{ dept.deptName }}
                  </option>
                </select>
              </div>
              -->
              <!-- 岗位类别 -->
              <div class="filter-sheet__group">
                <label class="filter-sheet__label">岗位类别</label>
                <select v-model="filters.categoryId" class="filter-sheet__select">
                  <option value="">全部类别</option>
                  <template v-for="cat in filterOptions.categories" :key="cat.categoryId">
                    <option :value="cat.categoryId">{{ cat.categoryName }}</option>
                    <option v-for="child in cat.children" :key="child.categoryId" :value="child.categoryId">&nbsp;&nbsp;{{ child.categoryName }}</option>
                  </template>
                </select>
              </div>
              <!-- 工作地点 -->
              <div class="filter-sheet__group">
                <label class="filter-sheet__label">工作地点</label>
                <select v-model="filters.location" class="filter-sheet__select">
                  <option value="">全部地点</option>
                  <option v-for="loc in filterOptions.locations" :key="loc.value" :value="loc.value">{{ loc.label }}</option>
                </select>
              </div>
              <!-- 学历要求（优化停用，保留代码便于恢复）
              <div class="filter-sheet__group">
                <label class="filter-sheet__label">学历要求</label>
                <select v-model="filters.degree" class="filter-sheet__select">
                  <option value="">全部学历</option>
                  <option v-for="deg in filterOptions.degrees" :key="deg.value" :value="deg.value">{{ deg.label }}</option>
                </select>
              </div>
              -->
            </div>
            <div class="filter-sheet__footer">
              <button class="filter-sheet__btn filter-sheet__btn--reset" @click="handleReset(); closeFilterSheet();">重置</button>
              <button class="filter-sheet__btn filter-sheet__btn--apply" @click="closeFilterSheet(); handleSearch();">应用筛选</button>
            </div>
            <div class="filter-sheet__safe"></div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ====== 加载态 ====== -->
    <LoadingSpinner :visible="loading" text="加载岗位列表..." />

    <!-- ====== 岗位卡片列表 ====== -->
    <section class="jobs-section" v-if="!loading">
      <div class="container">
        <div class="jobs-section__header" v-if="total > 0">
          <span class="jobs-section__count">共 <strong>{{ total }}</strong> 个岗位</span>
        </div>

        <div class="jobs__grid" v-if="jobs.length > 0">
          <article
            v-for="(job, i) in jobs"
            :key="job.jobId"
            class="job-card sys-card-conic-glow"
            v-motion-fade="{ y: 24, delay: i * 0.06 }"
            @click="$router.push(`/jobs/${job.jobId}`)"
          >
            <div class="job-card__header">
              <h3 class="job-card__title">{{ job.title }}</h3>
              <div class="job-card__tags">
                <span v-if="parseTags(job.tags).includes('急聘')" class="job-card__tag job-card__tag--urgent">急聘</span>
                <span class="job-card__tag job-card__tag--deadline">{{ formatDeadline(job.deadline) }}</span>
              </div>
            </div>
            <div class="job-card__meta">
              <span class="job-card__meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                {{ job.deptName || '--' }}{{ job.categoryName ? ' · ' + job.categoryName : '' }}
              </span>
              <span class="job-card__meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/></svg>
                {{ formatLoc(job.location) || '北京' }}
              </span>
              <span class="job-card__meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5"/></svg>
                {{ formatDegree(job.degreeRequirement) || '本科及以上' }}
              </span>
            </div>
            <p class="job-card__desc">{{ truncateText(job.description, 120) }}</p>
          </article>
        </div>

        <!-- 空态 -->
        <div class="jobs__empty" v-if="jobs.length === 0">
          <div class="jobs__empty-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" opacity="0.3">
              <rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </div>
          <p class="jobs__empty-text">暂无符合条件的岗位</p>
          <p class="jobs__empty-hint">试试调整筛选条件或清空关键词</p>
        </div>

        <!-- ====== 分页 ====== -->
        <div class="pagination" v-if="totalPages > 1">
          <button
            class="pagination__btn"
            :disabled="pagination.pageNum <= 1"
            @click="goToPage(pagination.pageNum - 1)"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
            上一页
          </button>

          <div class="pagination__pages">
            <button
              v-for="page in displayPages"
              :key="page"
              class="pagination__page"
              :class="{ 'is-active': page === pagination.pageNum, 'is-ellipsis': page === '...' }"
              :disabled="page === '...'"
              @click="page !== '...' && goToPage(page)"
            >{{ page }}</button>
          </div>

          <button
            class="pagination__btn"
            :disabled="pagination.pageNum >= totalPages"
            @click="goToPage(pagination.pageNum + 1)"
          >
            下一页
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/utils/axios';
import { formatLoc, formatDegree, parseTags } from '@/utils/location';
import LoadingSpinner from '@/components/LoadingSpinner.vue';

const router = useRouter();

// ---- 移动端检测 ----
const isMobile = ref(false);
const showFilterSheet = ref(false);

function checkMobile() {
  isMobile.value = window.matchMedia('(max-width: 767px)').matches;
}

function closeFilterSheet() {
  showFilterSheet.value = false;
}

// ---- 筛选条件 ----
const filters = reactive({
  keyword: '',
  deptId: '',
  categoryId: '',
  location: '',
  degree: '',
});

// ---- 分页 ----
const pagination = reactive({
  pageNum: 1,
  pageSize: 12,
});

// ---- 数据 ----
const jobs = ref([]);
const total = ref(0);
const loading = ref(true);

const filterOptions = ref({
  depts: [],
  categories: [],
  locations: [],
  degrees: [],
});

// ---- 下拉面板控制 ----
const openDropdown = ref(null);

function toggleDropdown(name) {
  openDropdown.value = openDropdown.value === name ? null : name;
}

function closeDropdown() {
  openDropdown.value = null;
}

// ---- 筛选选择 ----
function selectDept(deptId) {
  filters.deptId = deptId;
  closeDropdown();
  handleSearch();
}

function selectCategory(categoryId) {
  filters.categoryId = categoryId;
  closeDropdown();
  handleSearch();
}

function selectLocation(loc) {
  filters.location = loc;
  closeDropdown();
  handleSearch();
}

function selectDegree(deg) {
  filters.degree = deg;
  closeDropdown();
  handleSearch();
}

// ---- 计算属性 ----
// 部门树展平为带缩进级别的列表（支持任意层级），供下拉渲染
const flattenedDepts = computed(() => {
  const result = [];
  const walk = (nodes, level) => {
    for (const n of nodes || []) {
      result.push({ ...n, level });
      if (n.children && n.children.length) walk(n.children, level + 1);
    }
  };
  walk(filterOptions.value.depts, 0);
  return result;
});

const selectedDeptLabel = computed(() => {
  if (!filters.deptId) return '';
  const find = (nodes) => {
    for (const n of nodes || []) {
      if (n.deptId === filters.deptId) return n.deptName;
      if (n.children && n.children.length) {
        const r = find(n.children);
        if (r) return r;
      }
    }
    return '';
  };
  return find(filterOptions.value.depts);
});

const selectedCategoryLabel = computed(() => {
  if (!filters.categoryId) return '';
  for (const cat of filterOptions.value.categories) {
    if (cat.categoryId === filters.categoryId) return cat.categoryName;
    if (cat.children) {
      const child = cat.children.find(c => c.categoryId === filters.categoryId);
      if (child) return child.categoryName;
    }
  }
  return '';
});

const selectedLocationLabel = computed(() => {
  if (!filters.location) return '';
  const loc = filterOptions.value.locations.find(item => item.value === filters.location);
  return loc ? loc.label : '';
});

const activeFilterCount = computed(() => {
  let count = 0;
  if (filters.keyword) count++;
  if (filters.deptId) count++;
  if (filters.categoryId) count++;
  if (filters.location) count++;
  if (filters.degree) count++;
  return count;
});

const totalPages = computed(() => Math.ceil(total.value / pagination.pageSize) || 0);

const displayPages = computed(() => {
  const pages = [];
  const current = pagination.pageNum;
  const totalP = totalPages.value;
  if (totalP <= 7) {
    for (let i = 1; i <= totalP; i++) pages.push(i);
  } else {
    pages.push(1);
    if (current > 3) pages.push('...');
    const start = Math.max(2, current - 1);
    const end = Math.min(totalP - 1, current + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (current < totalP - 2) pages.push('...');
    pages.push(totalP);
  }
  return pages;
});

// ---- 方法 ----
function handleSearch() {
  pagination.pageNum = 1;
  fetchJobs();
}

function handleReset() {
  filters.keyword = '';
  filters.deptId = '';
  filters.categoryId = '';
  filters.location = '';
  filters.degree = '';
  pagination.pageNum = 1;
  fetchJobs();
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  pagination.pageNum = page;
  fetchJobs();
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function fetchJobs() {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    };
    if (filters.keyword) params.keyword = filters.keyword;
    if (filters.deptId) params.deptId = filters.deptId;
    if (filters.categoryId) params.categoryId = filters.categoryId;
    if (filters.location) params.location = filters.location;
    if (filters.degree) params.degree = filters.degree;

    const res = await api.get('/jobs/list', { params });
    if (res.code === 200) {
      jobs.value = res.data?.rows || [];
      total.value = res.data?.total || 0;
    }
  } catch {
    // 业务/网络错误已由 axios 拦截器统一 toast，这里仅清空列表
    jobs.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function fetchFilterOptions() {
  try {
    const res = await api.get('/jobs/filter-options');
    if (res.code === 200) {
      filterOptions.value = {
        depts: res.data?.departments || [],
        categories: res.data?.categories || [],
        locations: res.data?.locations || [],
        degrees: res.data?.degreeRequirements || [],
      };
    }
  } catch {
    // 筛选选项加载失败不影响主列表（拦截器已 toast）
  }
}

// ---- 工具函数 ----
function formatDeadline(deadline) {
  if (!deadline) return '';
  const d = new Date(deadline);
  const now = new Date();
  const diff = Math.ceil((d - now) / (1000 * 60 * 60 * 24));
  // 列表接口已由后端按 deadline > NOW() 过滤，过期岗位不会出现（'已截止' 分支不可达）
  if (diff === 0) return '今日截止';
  if (diff <= 7) return `${diff}天后截止`;
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} 截止`;
}

function truncateText(text, len) {
  if (!text) return '';
  const plain = text.replace(/<[^>]+>/g, '');
  return plain.length > len ? plain.slice(0, len) + '...' : plain;
}

// ---- 点击外部关闭下拉 ----
function onDocumentClick(e) {
  if (!e.target.closest('.filter-bar__select-wrapper')) {
    closeDropdown();
  }
}

// ---- 生命周期 ----
onMounted(async () => {
  checkMobile();
  const mediaQuery = window.matchMedia('(max-width: 767px)');
  mediaQuery.addEventListener('change', checkMobile);
  document.addEventListener('click', onDocumentClick);
  await Promise.all([fetchJobs(), fetchFilterOptions()]);
});

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick);
});
</script>

<style scoped>
.container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }

/* ====== 页面标题区 ====== */
.page-hero {
  position: relative;
  padding: 80px 0 48px;
  text-align: center;
  overflow: hidden;
}
.section-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.section-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.section-title__cn {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  text-shadow: 0 0 12px var(--glow-strong), 0 0 20px var(--glow-color);
}
.section-title__en {
  font-size: 12px;
  color: #5FB8D6;
  letter-spacing: 3px;
  font-family: 'Share Tech Mono', 'Courier New', monospace;
  text-transform: uppercase;
}
.section-desc {
  font-size: 15px;
  color: #9CA3AF;
  margin-top: 8px;
}

/* ====== 筛选栏 ====== */
.filter-bar {
  padding-bottom: 32px;
  position: relative;
  z-index: 50;
}
.filter-bar__inner {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: center;
}

/* 搜索输入框 */
.filter-bar__search {
  position: relative;
  flex: 1 1 200px;
  min-width: 180px;
  max-width: 320px;
}
.filter-bar__search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #6E7D8A;
  pointer-events: none;
}
.filter-bar__search-input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid rgba(95, 184, 214, 0.15);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}
.filter-bar__search-input::placeholder {
  color: #6E7D8A;
}
.filter-bar__search-input:focus {
  border-color: #5FB8D6;
}

/* 自定义下拉选择器 */
.filter-bar__select-wrapper {
  position: relative;
}
.filter-bar__select-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid rgba(95, 184, 214, 0.15);
  border-radius: 8px;
  color: #9CA3AF;
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.2s, color 0.2s;
}
.filter-bar__select-trigger:hover,
.filter-bar__select-wrapper.is-open .filter-bar__select-trigger {
  border-color: #5FB8D6;
  color: #fff;
}

.filter-bar__select-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  min-width: 160px;
  max-height: 280px;
  overflow-y: auto;
  background: var(--bg-glass-strong);
  backdrop-filter: blur(var(--glass-blur-heavy));
  border: 1px solid rgba(95, 184, 214, 0.2);
  border-radius: 8px;
  padding: 6px 0;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}
.filter-bar__select-dropdown--tree {
  min-width: 200px;
}
.filter-bar__select-option {
  padding: 8px 16px;
  font-size: 13px;
  color: #9CA3AF;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.filter-bar__select-option:hover {
  background: rgba(95, 184, 214, 0.1);
  color: #fff;
}
.filter-bar__select-option.is-active {
  color: #5FB8D6;
  background: rgba(95, 184, 214, 0.08);
}
.filter-bar__select-option--parent {
  font-weight: 600;
  color: #ccc;
  border-top: 1px solid rgba(95, 184, 214, 0.08);
}
.filter-bar__select-option--child {
  padding-left: 28px;
  font-size: 12px;
}

/* 按钮 */
.filter-bar__btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  color: #0A0E17;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: transform 0.2s, box-shadow 0.2s;
}
.filter-bar__btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(95, 184, 214, 0.3);
}
.filter-bar__btn-reset {
  padding: 10px 16px;
  background: transparent;
  color: #9CA3AF;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s, border-color 0.2s;
}
.filter-bar__btn-reset:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}

/* 已选筛选标签 */
.filter-bar__active-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  justify-content: center;
}
.filter-bar__active-label {
  font-size: 12px;
  color: #6E7D8A;
}
.filter-bar__tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(95, 184, 214, 0.12);
  color: #5FB8D6;
  border-radius: 4px;
  font-size: 12px;
}
.filter-bar__tag button {
  background: none;
  border: none;
  color: #5FB8D6;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0 2px;
  opacity: 0.7;
  transition: opacity 0.2s;
}
.filter-bar__tag button:hover {
  opacity: 1;
}

/* ====== 岗位卡片区域 ====== */
.jobs-section {
  padding-bottom: 60px;
}
.jobs-section__header {
  margin-bottom: 20px;
}
.jobs-section__count {
  font-size: 14px;
  color: #9CA3AF;
}
.jobs-section__count strong {
  color: #5FB8D6;
  font-family: 'Share Tech Mono', 'Courier New', monospace;
  font-size: 16px;
}

.jobs__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

/* 岗位卡片 — 玻璃态 + 光锥旋转边框（sys-card-conic-glow 处理 hover） */
.job-card {
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border-radius: 12px;
  padding: 28px;
  border: 1px solid rgba(95, 184, 214, 0.1);
  cursor: pointer;
  transition: border-color 0.3s, transform 0.3s, box-shadow 0.3s;
}
.job-card:hover {
  border-color: rgba(95, 184, 214, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}
.job-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.job-card__title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  transition: color 0.2s;
}
.job-card:hover .job-card__title {
  color: #5FB8D6;
}
.job-card__tags {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.job-card__tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 4px;
}
.job-card__tag--urgent {
  background: rgba(224, 82, 82, 0.15);
  color: #E05252;
}
.job-card__tag--deadline {
  background: rgba(95, 184, 214, 0.1);
  color: #5FB8D6;
}
.job-card__meta {
  display: flex;
  gap: 20px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.job-card__meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #9CA3AF;
}
.job-card__meta-item svg {
  color: #6E7D8A;
  flex-shrink: 0;
}
.job-card__desc {
  font-size: 13px;
  color: #6E7D8A;
  line-height: 1.7;
}

/* ====== 空态 ====== */
.jobs__empty {
  text-align: center;
  padding: 80px 0;
}
.jobs__empty-icon {
  margin-bottom: 16px;
  color: #6E7D8A;
}
.jobs__empty-text {
  font-size: 16px;
  color: #9CA3AF;
  margin-bottom: 8px;
}
.jobs__empty-hint {
  font-size: 13px;
  color: #6E7D8A;
}

/* ====== 分页 ====== */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 40px;
  padding-top: 24px;
}
.pagination__btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid rgba(95, 184, 214, 0.12);
  border-radius: 8px;
  color: #9CA3AF;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.pagination__btn:hover:not(:disabled) {
  border-color: #5FB8D6;
  color: #fff;
}
.pagination__btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.pagination__pages {
  display: flex;
  gap: 4px;
}
.pagination__page {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #9CA3AF;
  font-size: 14px;
  font-family: 'Share Tech Mono', 'Courier New', monospace;
  cursor: pointer;
  transition: all 0.2s;
}
.pagination__page:hover:not(:disabled):not(.is-ellipsis) {
  border-color: rgba(95, 184, 214, 0.3);
  color: #fff;
}
.pagination__page.is-active {
  background: #5FB8D6;
  color: #0A0E17;
  border-color: #5FB8D6;
  font-weight: 700;
}
.pagination__page.is-ellipsis {
  cursor: default;
  color: #6E7D8A;
}

/* ====== 移动端筛选区域 ====== */
.filter-bar__mobile {
  display: flex;
  gap: 10px;
  align-items: center;
}
.filter-bar__search--mobile {
  flex: 1;
  max-width: none;
}
.filter-bar__filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 48px;
  padding: 0 16px;
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  position: relative;
}
.filter-bar__filter-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.filter-bar__filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: var(--color-primary);
  color: var(--color-bg);
  font-size: 11px;
  font-weight: 700;
  padding: 0 5px;
}

/* ====== Bottom Sheet 筛选面板 ====== */
.filter-sheet-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 300;
  display: flex;
  align-items: flex-end;
}
.filter-sheet {
  width: 100%;
  max-height: 70vh;
  background: var(--bg-glass-strong);
  backdrop-filter: blur(var(--glass-blur-heavy));
  border-radius: 16px 16px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.filter-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}
.filter-sheet__header h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
}
.filter-sheet__close {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
}
.filter-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-sheet__group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.filter-sheet__label {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}
.filter-sheet__select {
  min-height: 48px;
  width: 100%;
  padding: 12px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text);
  font-size: 16px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
  appearance: auto;
}
.filter-sheet__select:focus {
  border-color: var(--color-primary);
}
.filter-sheet__select option {
  background: var(--color-card);
  color: var(--color-text);
}
.filter-sheet__footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
}
.filter-sheet__btn {
  flex: 1;
  min-height: 48px;
  border-radius: var(--radius);
  font-size: 16px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-sheet__btn--reset {
  background: none;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}
.filter-sheet__btn--reset:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.filter-sheet__btn--apply {
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  border: none;
  color: var(--color-bg);
}
.filter-sheet__btn--apply:hover {
  opacity: 0.9;
}
.filter-sheet__safe {
  height: env(safe-area-inset-bottom, 16px);
  flex-shrink: 0;
}

/* Bottom Sheet 动画 */
.sheet-slide-enter-active { transition: all 0.3s ease; }
.sheet-slide-leave-active { transition: all 0.3s ease; }
.sheet-slide-enter-from { opacity: 0; }
.sheet-slide-enter-from .filter-sheet { transform: translateY(100%); }
.sheet-slide-leave-to { opacity: 0; }
.sheet-slide-leave-to .filter-sheet { transform: translateY(100%); }
.filter-sheet { transition: transform 0.3s ease; }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .page-hero {
    padding: 40px 0 24px;
  }
  .section-title__cn {
    font-size: 24px;
  }
  .filter-bar__inner {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-bar__search {
    max-width: none;
  }
  .filter-bar__select-trigger {
    width: 100%;
    justify-content: space-between;
  }
  .filter-bar__select-dropdown {
    left: 0;
    right: 0;
  }
  .jobs__grid {
    grid-template-columns: 1fr;
  }
  .pagination {
    flex-wrap: wrap;
  }
}
</style>
