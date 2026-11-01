<template>
  <section
    class="campus-story"
    :class="{ 'campus-story--compact': compact }"
    aria-labelledby="campus-story-title"
  >
    <header class="campus-story__head">
      <div>
        <p class="hz-eyebrow">
          HZNU · CAMPUS NOTES
        </p>
        <h2 id="campus-story-title">
          湖光书卷，<em>四时校园</em>
        </h2>
      </div>
      <p class="campus-story__desc">
        从仓前的“叠书成塔”，到下沙的蕙风荷塘；图书馆的灯、廊桥的影、湖面的风，都是杭师大日常的一部分。
      </p>
    </header>

    <div class="campus-story__grid">
      <figure class="story-tile story-tile--main">
        <img
          :src="cqLibrary"
          alt="杭州师范大学仓前校区图书馆与校门"
          loading="lazy"
        >
        <figcaption>
          <span>仓前校区</span>
          <strong>叠书成塔 · 图书馆</strong>
        </figcaption>
      </figure>

      <figure class="story-tile story-tile--bridge">
        <img
          :src="cqShenyuan"
          alt="杭州师范大学仓前校区慎园红砖教学楼与廊桥"
          loading="lazy"
        >
        <figcaption>
          <span>仓前 · 慎园</span>
          <strong>红砖与廊桥</strong>
        </figcaption>
      </figure>

      <figure class="story-tile story-tile--lake">
        <img
          :src="xsLake"
          alt="杭州师范大学下沙校区蕙风湖与图书馆"
          loading="lazy"
        >
        <figcaption>
          <span>下沙 · 蕙风湖</span>
          <strong>荷风与书声</strong>
        </figcaption>
      </figure>

      <figure class="story-tile story-tile--swan">
        <img
          :src="xsSwan"
          alt="杭州师范大学下沙校区蕙风湖的黑天鹅"
          loading="lazy"
        >
        <figcaption>
          <span>校园日常</span>
          <strong>黑天鹅一家</strong>
        </figcaption>
      </figure>

      <aside class="campus-story__motto">
        <img
          :src="hznuMotto"
          alt="杭州师范大学校训：勤慎诚恕，博雅精进"
          loading="lazy"
        >
        <div class="campus-story__motto-copy">
          <span>校训</span>
          <strong>勤慎诚恕 · 博雅精进</strong>
          <small>HANGZHOU NORMAL UNIVERSITY · 1908</small>
        </div>
      </aside>
    </div>

    <p class="campus-story__source">
      影像 / 杭州师范大学官网 · 校园风光
    </p>
  </section>
</template>

<script setup lang="ts">
import cqLibrary from '@/assets/images/campus/cq-library.jpg'
import cqShenyuan from '@/assets/images/campus/cq-shenyuan.jpg'
import xsLake from '@/assets/images/campus/xs-lake.jpg'
import xsSwan from '@/assets/images/campus/xs-swan.jpg'
import hznuMotto from '@/assets/images/brand/hznu-motto.png'

defineProps<{
  /** 服务页使用：只保留三张校园横图，减少页面高度 */
  compact?: boolean
}>()
</script>

<style scoped lang="scss">
.campus-story {
  position: relative;
  padding: 26px 26px 18px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-2xl);
  background:
    linear-gradient(135deg, rgba(255, 253, 247, 0.96), rgba(240, 234, 219, 0.9)),
    var(--surface);
  box-shadow: var(--shadow-card);
}

.campus-story::after {
  content: "HZNU";
  position: absolute;
  right: 18px;
  bottom: -34px;
  z-index: 0;
  color: rgba(24, 49, 59, 0.035);
  font-family: var(--font-latin);
  font-size: 150px;
  line-height: 1;
  letter-spacing: 0.02em;
  pointer-events: none;
}

.campus-story__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
}

.campus-story__head h2 {
  margin: 6px 0 0;
  color: var(--ink);
  font-family: var(--font-serif);
  font-size: clamp(22px, 2.2vw, 30px);
  font-weight: 600;
  letter-spacing: 0.06em;
}

.campus-story__head h2 em {
  color: var(--gold);
  font-style: normal;
}

.campus-story__desc {
  max-width: 440px;
  margin: 0;
  color: var(--ink-3);
  font-size: 12.5px;
  line-height: 1.9;
}

.campus-story__grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.35fr 1fr 1fr;
  grid-template-rows: 158px 158px;
  gap: 10px;
}

.story-tile {
  position: relative;
  margin: 0;
  overflow: hidden;
  border: 1px solid rgba(24, 49, 59, 0.1);
  border-radius: var(--radius-lg);
  background: var(--ink);
}

.story-tile--main {
  grid-row: span 2;
}

.story-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s var(--ease-out);
}

.story-tile::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 38%, rgba(13, 32, 39, 0.76) 100%);
  pointer-events: none;
}

.story-tile figcaption {
  position: absolute;
  right: 14px;
  bottom: 12px;
  left: 14px;
  z-index: 1;
  display: grid;
  gap: 2px;
  color: #f7f1e4;
}

.story-tile figcaption span {
  color: rgba(214, 193, 154, 0.86);
  font-family: var(--font-latin);
  font-size: 8.5px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.story-tile figcaption strong {
  font-family: var(--font-serif);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.08em;
}

.story-tile:hover img {
  transform: scale(1.035);
}

.campus-story__motto {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  overflow: hidden;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(255, 253, 247, 0.8), rgba(214, 225, 222, 0.42)),
    var(--paper-2);
}

.campus-story__motto::before {
  content: "1908";
  position: absolute;
  right: -6px;
  bottom: -18px;
  color: rgba(24, 49, 59, 0.04);
  font-family: var(--font-latin);
  font-size: 92px;
  line-height: 1;
}

.campus-story__motto > img {
  position: relative;
  z-index: 1;
  width: auto;
  height: 118px;
  object-fit: contain;
  opacity: 0.82;
}

.campus-story__motto-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 5px;
}

.campus-story__motto-copy span {
  color: var(--seal);
  font-size: 9px;
  letter-spacing: 0.24em;
}

.campus-story__motto-copy strong {
  color: var(--ink);
  font-family: var(--font-serif);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  line-height: 1.6;
}

.campus-story__motto-copy small {
  color: var(--ink-4);
  font-family: var(--font-latin);
  font-size: 7.5px;
  letter-spacing: 0.12em;
}

.campus-story__source {
  position: relative;
  z-index: 1;
  margin: 12px 2px 0;
  color: var(--ink-4);
  font-family: var(--font-latin);
  font-size: 8px;
  letter-spacing: 0.14em;
  text-align: right;
}

.campus-story--compact {
  padding: 20px 22px 14px;
}

.campus-story--compact .campus-story__head {
  margin-bottom: 12px;
}

.campus-story--compact .campus-story__desc,
.campus-story--compact .story-tile--swan,
.campus-story--compact .campus-story__motto,
.campus-story--compact .campus-story__source {
  display: none;
}

.campus-story--compact .campus-story__grid {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: 150px;
}

.campus-story--compact .story-tile--main {
  grid-column: span 1;
  grid-row: span 1;
}

@media (max-width: 900px) {
  .campus-story__head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .campus-story__grid {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: 180px 150px 150px;
  }

  .story-tile--main {
    grid-column: span 2;
    grid-row: span 1;
  }

  .campus-story__motto {
    grid-column: span 2;
  }
}

@media (max-width: 600px) {
  .campus-story {
    padding: 20px 18px 14px;
  }

  .campus-story__grid {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(4, 170px);
  }

  .story-tile--main,
  .campus-story__motto {
    grid-column: span 1;
  }

  .campus-story__motto {
    min-height: 170px;
  }

  .campus-story__motto > img {
    height: 132px;
  }

  .campus-story--compact .campus-story__grid {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(3, 160px);
  }

  .campus-story--compact .story-tile--main {
    grid-column: span 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .story-tile img {
    transition: none;
  }

  .story-tile:hover img {
    transform: none;
  }
}
</style>
